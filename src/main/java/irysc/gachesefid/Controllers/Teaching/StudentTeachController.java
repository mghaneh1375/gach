package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Teaching.Utility.*;
import static irysc.gachesefid.Controllers.Teaching.Utility.publicConvertSchedule;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentTeachController {

    public static String submitRequest(
            Document user, ObjectId scheduleId
    ) {
        ObjectId userId = user.getObjectId("_id");

        try {
            Document schedule = getSchedule(userId, scheduleId, false);

            if (!schedule.getBoolean("can_request"))
                return JSON_NOT_ACCESS;

            long curr = System.currentTimeMillis();
            if (schedule.getLong("start_at") < curr)
                return generateErr("این جلسه منقضی شده است");

            List<Document> requests;
            if (!schedule.containsKey("requests"))
                requests = new ArrayList<>();
            else {
                requests = schedule.getList("requests", Document.class);
                if (Utility.searchInDocumentsKeyValIdx(requests, "_id", userId) != -1)
                    return generateErr("شما قبلا برای این برنامه زمانی درخواست داده اید");
            }

            if (schedule.getString("teach_mode").equalsIgnoreCase(
                    TeachMode.SEMI_PRIVATE.getName()
            ))
                return prePayForSemiPrivateSchedule(schedule, user);

            String registryStatus = schedule.getBoolean("need_registry_confirmation") ? "pending" : "accept";
            Document newReqDoc = new Document("_id", userId)
                    .append("created_at", System.currentTimeMillis())
                    .append("status", registryStatus);

            if (registryStatus.equals("accept")) {
                newReqDoc.append("expire_at", curr + PAY_SCHEDULE_EXPIRATION_MSEC);
                schedule.put("can_request", false);
            }

            requests.add(newReqDoc);

            teachScheduleRepository.replaceOneWithoutClearCache(scheduleId, schedule);
            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    private static String prePayForSemiPrivateSchedule(
            Document schedule, Document user
    ) {
        double money = ((Number) user.get("money")).doubleValue();
        Document config = getConfig();
        int prePayAmount = config.getInteger("pre_pay_amount");

        if (prePayAmount - money <= 100) {
            money = irysc.gachesefid.Controllers.Teaching.Utility.payFromWallet(
                    prePayAmount, money, user.getObjectId("_id")
            );

            ObjectId tId = transactionRepository.insertOneWithReturnId(
                    createPrePayTransactionDoc(
                            prePayAmount, user.getObjectId("_id"),
                            schedule.getObjectId("_id")
                    )
            );

            completePrePayForSemiPrivateSchedule(
                    null, schedule, user.getObjectId("_id"),
                    0, prePayAmount
            );

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", money),
                    new PairValue("transactionId", tId.toString())
            );
        }

        //todo: complete
        return null;
    }

    public static String getMySchedules(ObjectId userId) {

        List<Document> schedules = teachScheduleRepository.find(
                eq("students._id", userId),
                null
        );

        JSONArray jsonArray = new JSONArray();
        for (Document schedule : schedules) {
            jsonArray.put(
                    publicConvertSchedule(schedule)
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getSchedules(ObjectId teacherId) {

        List<Document> schedules = teachScheduleRepository.find(
                and(
                        eq("user_id", teacherId),
                        eq("can_request", true),
                        gt("start_at", System.currentTimeMillis()),
                        eq("visibility", true)
                ),
                null
        );

        JSONArray jsonArray = new JSONArray();
        for (Document schedule : schedules) {
            jsonArray.put(
                    publicConvertScheduleToJSONDigest(schedule)
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }


    public static String payForSchedule(
            Document user, ObjectId scheduleId,
            String offCode
    ) {
        ObjectId userId = user.getObjectId("_id");

        try {
            Document schedule = getSchedule(
                    userId, scheduleId, false
            );

            if (!schedule.containsKey("requests"))
                return JSON_NOT_VALID_ID;

            Document request = Utility.searchInDocumentsKeyVal(
                    schedule.getList("requests", Document.class), "_id", userId
            );

            if (request == null)
                return JSON_NOT_VALID_ID;

            if (request.getString("status").equalsIgnoreCase("reject"))
                return generateErr("درخواست شما رد شده است");

            if (request.getString("status").equalsIgnoreCase("pending"))
                return generateErr("درخواست شما در حال بررسی می باشد");

            if (schedule.containsKey("students") &&
                    searchInDocumentsKeyValIdx(
                            schedule.getList("students", Document.class),
                            "_id", userId
                    ) != -1
            )
                return generateErr("شما قبلا در این جلسه ثبت نام شده اید");

            int price = schedule.getInteger("price");


            double offAmount = 0;
            double shouldPayDouble = price * 1.0;
            Document offDoc;

            try {
                offDoc = findOff(
                        offCode, userId
                );
            } catch (Exception x) {
                return generateErr(x.getMessage());
            }

            if (offDoc != null) {
                offAmount +=
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount");

                shouldPayDouble = Math.max(price - offAmount, 0);
            }

            int shouldPay = (int) shouldPayDouble;
            if (schedule.getString("teach_mode").equalsIgnoreCase(
                    TeachMode.SEMI_PRIVATE.getName()
            ))
                shouldPay -= request.getInteger("wallet_paid") + request.getInteger("paid");

            double money = ((Number) user.get("money")).doubleValue();
            long curr = System.currentTimeMillis();

            if (shouldPay - money <= 100) {

                if (shouldPay > 100)
                    money = irysc.gachesefid.Controllers.Teaching.Utility.payFromWallet(shouldPay, money, userId);

                ObjectId tId = transactionRepository.insertOneWithReturnId(
                        createTransactionDoc(
                                shouldPay, userId, scheduleId, offDoc, offAmount
                        )
                );

                List<Document> students = schedule.containsKey("students") ?
                        schedule.getList("students", Document.class) :
                        new ArrayList<>();

                students.add(new Document("_id", userId)
                        .append("created_at", curr)
                        .append("paid", 0)
                        .append("wallet_paid", shouldPay > 100 ? shouldPay : 0)
                );
                teachScheduleRepository.updateOne(scheduleId,
                        set("students", students)
                );

                if (offDoc != null)
                    logForOffCodeUsage(offDoc, userId, OffCodeSections.CLASSES.getName(), scheduleId);

                return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                        "action", "success",
                        new PairValue("refId", money),
                        new PairValue("transactionId", tId.toString())
                );
            }

            long orderId = Math.abs(new Random().nextLong());
            while (transactionRepository.exist(
                    eq("order_id", orderId)
            )) {
                orderId = Math.abs(new Random().nextLong());
            }

            Document doc =
                    new Document("user_id", userId)
                            .append("account_money", money)
                            .append("amount", (int) (shouldPay - money))
                            .append("created_at", curr)
                            .append("status", "init")
                            .append("order_id", orderId)
                            .append("products", scheduleId)
                            .append("section", OffCodeSections.CLASSES.getName());

            if (offDoc != null) {
                doc.append("off_code", offDoc.getObjectId("_id"));
                doc.append("off_amount", (int) offAmount);
            }

            return goToPayment((int) (shouldPay - money), doc);

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String getMySchedule(ObjectId userId, ObjectId scheduleId) {
        try {
            Document schedule = getSchedule(userId, scheduleId, true);
            //todo: complete
            return null;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    //todo: rate to teacher not schedule
    public static String rate(ObjectId userId, ObjectId scheduleId, int userRate) {
        try {
            Document schedule = getSchedule(userId, scheduleId, true);
            int ratesCount = (int) schedule.getOrDefault("rates_count", 0);
            double rate = (double) schedule.getOrDefault("rate", 0);
            double rateSum = rate * ratesCount;

            List<Document> students = schedule.getList("students", Document.class);
            Document studentDoc = searchInDocumentsKeyVal(students, "_id", userId);
            if (studentDoc.containsKey("rate")) {
                rateSum -= studentDoc.getInteger("rate");
                rateSum += userRate;
            } else {
                rateSum += userRate;
                ratesCount++;
            }

            rate = rateSum / ratesCount;
            schedule.put("rate", rate);
            schedule.put("rates_count", ratesCount);
            studentDoc.put("rate", userRate);
            teachScheduleRepository.replaceOneWithoutClearCache(
                    scheduleId, schedule
            );

            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String getTeachers(
            Boolean justHasFreeSchedule,
            Integer minAge, Integer maxAge,
            Integer minRate, Integer maxRate,
            String sortBy, String tag, Boolean returnFilters,
            ObjectId gradeId, ObjectId branchId, ObjectId lessonId
    ) {

        if (sortBy != null &&
                !sortBy.equalsIgnoreCase("age") &&
                !sortBy.equalsIgnoreCase("rate") &&
                !sortBy.equalsIgnoreCase("student")
        )
            return JSON_NOT_VALID_PARAMS;

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("accesses", Access.ADVISOR.getName()));
        filters.add(exists("teach"));
        long curr = System.currentTimeMillis();

        if (justHasFreeSchedule != null) {
            List<ObjectId> userIds = teachScheduleRepository.find(
                    and(
                            eq("visibility", true),
                            eq("can_request", true),
                            gt("start_at", curr)
                    ), new BasicDBObject("user_id", 1)
            ).stream().map(document -> document.getObjectId("user_id")).collect(Collectors.toList());
            filters.add(in("_id", userIds));
        }

        if (tag != null) {
            filters.add(exists("teach_tags"));
            filters.add(eq("teach_tags", tag));
        }

        if (minRate != null)
            filters.add(and(
                    exists("teach_rate"),
                    gte("teach_rate", minRate)
            ));

        if (maxRate != null)
            filters.add(and(
                    exists("teach_rate"),
                    lte("teach_rate", maxRate)
            ));

        if (minAge != null) {
            long age = System.currentTimeMillis() - minAge * ONE_DAY_MIL_SEC * 365;
            filters.add(lte("birth_day", age));
        }

        if (maxAge != null) {
            long age = System.currentTimeMillis() - maxAge * ONE_DAY_MIL_SEC * 365;
            filters.add(gte("birth_day", age));
        }

        if (branchId != null) {
            filters.add(exists("teach_branches"));
            filters.add(eq("teach_branches", branchId));
        }

        if (gradeId != null) {
            filters.add(exists("teach_grades"));
            filters.add(eq("teach_grades", gradeId));
        }

        if (lessonId != null) {
            filters.add(exists("teach_lessons"));
            filters.add(eq("teach_lessons", lessonId));
        }

        List<Document> teachers = userRepository.find(
                and(filters),
                TEACH_PUBLIC_DIGEST
        );

        List<JSONObject> docs = new ArrayList<>();
        long one_year_ms = ONE_DAY_MIL_SEC * 365;

        boolean isAllFiltersOff = (returnFilters == null || returnFilters) && maxAge == null && minAge == null &&
                tag == null && minRate == null && maxRate == null;

        int minAgeFilter = -1, maxAgeFilter = -1;
        HashMap<ObjectId, Document> branches = new HashMap<>();
        HashMap<ObjectId, String> grades = new HashMap<>();

        for (Document teacher : teachers) {

            JSONObject jsonObject = convertTeacherToJSONDigest(
                    null, teacher, branches, grades
            );

            int age = -1;
            if (teacher.containsKey("birth_day")) {
                age = (int) ((curr - teacher.getLong("birth_day")) / one_year_ms);
                jsonObject.put("age", age);
            }

            docs.add(jsonObject);

            if (isAllFiltersOff) {
                if (age != -1) {
                    if (minAgeFilter == -1 || minAgeFilter > age)
                        minAgeFilter = age;

                    if (maxAgeFilter == -1 || maxAgeFilter < age)
                        maxAgeFilter = age;
                }
            }
        }

        String sortKey = sortBy == null || sortBy.equalsIgnoreCase("rate") ? "rate" :
                sortBy.equalsIgnoreCase("age") ? "age" : "stdCount";

        docs.sort((o1, o2) -> {
            int a = o1.has(sortKey) ? o1.getInt(sortKey) : -1;
            int b = o2.has(sortKey) ? o2.getInt(sortKey) : -1;
            return b - a;
        });

        JSONArray jsonArray = new JSONArray();

        if (isAllFiltersOff)
            return generateSuccessMsg("data", new JSONObject()
                    .put("data", jsonArray)
                    .put("filters", new JSONObject()
                            .put("minAge", minAgeFilter)
                            .put("maxAge", maxAgeFilter)
                    )
            );

        return generateSuccessMsg("data", jsonArray);
    }


}
