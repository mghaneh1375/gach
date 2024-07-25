package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Teaching.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentTeachController {

    public static String submitRequest(
            Document user, ObjectId scheduleId
    ) {
        ObjectId userId = user.getObjectId("_id");

        try {
            Document schedule = getSchedule(userId, scheduleId, false, false);

            if (!schedule.getBoolean("can_request") ||
                    !schedule.getBoolean("need_registry_confirmation")
            )
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
//            if (schedule.getString("teach_mode").equalsIgnoreCase(
//                    TeachMode.SEMI_PRIVATE.getName()
//            ))
//                return prePayForSemiPrivateSchedule(schedule, user);

            Document newReqDoc = new Document("_id", userId)
                    .append("created_at", System.currentTimeMillis())
                    .append("status", "pending")
                    .append("expire_at", curr + SET_STATUS_TEACH_REQUEST_EXPIRATION_MSEC);

            requests.add(newReqDoc);
            schedule.put("requests", requests);
            teachScheduleRepository.updateOne(scheduleId, set("requests", requests));

            Document advisor = userRepository.findById(schedule.getObjectId("user_id"));
            createNotifAndSendSMS(
                    advisor,
                    getSolarDate(schedule.getLong("start_at")) + "__" + user.getString("first_name") + " " + user.getString("last_name"),
                    "newTeachRequest"
            );
            userRepository.updateOne(
                    advisor.getObjectId("_id"),
                    set("events", advisor.get("events"))
            );

            return generateSuccessMsg("data", "pending");

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String cancelRequest(
            ObjectId userId, String name, ObjectId scheduleId
    ) {
        try {
            Document schedule = getSchedule(userId, scheduleId, false, false);

            if (!schedule.containsKey("requests"))
                return JSON_NOT_ACCESS;

            List<Document> requests = schedule.getList("requests", Document.class);
            Document req = searchInDocumentsKeyVal(requests, "_id", userId);

            if (req == null)
                return JSON_NOT_ACCESS;

            //todo: cancel pre pay scenario
            if (!req.getString("status").equalsIgnoreCase(TeachRequestStatus.PENDING.getName()) &&
                    !req.getString("status").equalsIgnoreCase(TeachRequestStatus.ACCEPT.getName())
            )
                return generateErr("درخواست شما در وضعیت کنسلی قرار ندارد");

            BasicDBObject update = new BasicDBObject("requests", requests);

            //todo: consider semi-private
            if (req.getString("status").equalsIgnoreCase(TeachRequestStatus.ACCEPT.getName()) &&
                    schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.PRIVATE.getName())
            ) {
                schedule.put("can_request", true);
                update.append("can_request", true);
            }

            if (req.getString("status").equalsIgnoreCase(TeachRequestStatus.ACCEPT.getName())) {
                Document advisor = userRepository.findById(schedule.getObjectId("user_id"));
                createNotifAndSendSMS(
                        advisor,
                        getSolarDate(schedule.getLong("start_at")) + "__" + name,
                        "cancelRequest"
                );
                userRepository.updateOne(advisor.getObjectId("_id"), set("events", advisor.get("events")));
            }

            req.put("status", TeachRequestStatus.CANCEL.getName());
            teachScheduleRepository.updateOne(
                    scheduleId, new BasicDBObject("$set", update)
            );

            return JSON_OK;
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
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

    public static String myScheduleRequests(
            ObjectId userId, String activeMode,
            String statusMode, String scheduleActiveMode
    ) {
        try {
            List<Document> myRequests = teachScheduleRepository.find(
                    buildMyScheduleRequestsFilters(userId, activeMode, statusMode, scheduleActiveMode),
                    null
            );

            JSONArray jsonArray = new JSONArray();
            Set<ObjectId> teachersId = new HashSet<>();

            for (Document request : myRequests)
                teachersId.add(request.getObjectId("user_id"));

            List<Document> teachers = userRepository.findByIds(new ArrayList<>(teachersId), false, JUST_NAME);

            for (Document schedule : myRequests) {
                Document teacher = teachers
                        .stream()
                        .filter(teacherIter -> teacherIter.getObjectId("_id").equals(schedule.getObjectId("user_id")))
                        .findFirst().get();

                Document req = schedule.getList("requests", Document.class)
                        .stream().filter(reqIter -> reqIter.getObjectId("_id").equals(userId))
                        .findFirst().get();

                JSONObject jsonObject = new JSONObject()
                        .put("id", schedule.getObjectId("_id").toString())
                        .put("teacher", teacher.getString("first_name") + " " + teacher.getString("last_name"))
                        .put("startAt", getSolarDate(schedule.getLong("start_at")))
                        .put("length", schedule.get("length"))
                        .put("price", schedule.get("price"))
                        .put("teachMode", schedule.get("teach_mode"))
                        .put("createdAt", getSolarDate(req.getLong("created_at")))
                        .put("status", req.getString("status"))
                        .put("expireAt", req.containsKey("expire_at") ? getSolarDate(req.getLong("expire_at")) : "")
                        .put("answerAt", req.containsKey("answer_at") ? getSolarDate(req.getLong("answer_at")) : "");

                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("data", jsonArray);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getMySchedules(ObjectId userId, String activeMode) {

        if (activeMode != null && !EnumValidatorImp.isValid(activeMode, ActiveMode.class))
            return JSON_NOT_VALID_PARAMS;

        List<Bson> filters = new ArrayList<>() {{
            add(eq("students._id", userId));
        }};
        if (activeMode != null) {
            if (activeMode.equalsIgnoreCase(ActiveMode.ACTIVE.getName()))
                filters.add(gt("start_at", System.currentTimeMillis() - ONE_DAY_MIL_SEC));
            else
                filters.add(lt("start_at", System.currentTimeMillis() - ONE_DAY_MIL_SEC));
        }

        List<Document> schedules = teachScheduleRepository.find(
                and(filters), null
        );

        Set<ObjectId> teachersId = new HashSet<>();

        for (Document schedule : schedules)
            teachersId.add(schedule.getObjectId("user_id"));

        List<Document> teachers = userRepository.findByIds(new ArrayList<>(teachersId), false,
                new BasicDBObject("first_name", 1).append("last_name", 1)
                        .append("teach_rate", 1).append("pic", 1)
        );

        JSONArray jsonArray = new JSONArray();
        for (Document schedule : schedules) {
            Document stdDoc = schedule.getList("students", Document.class)
                    .stream()
                    .filter(student -> student.get("_id").equals(userId))
                    .findFirst().get();
            jsonArray.put(
                    convertMySchedule(
                            schedule,
                            teachers.stream()
                                    .filter(teacher -> teacher.getObjectId("_id").equals(schedule.getObjectId("user_id")))
                                    .findFirst().get(),
                            (Integer) stdDoc.getOrDefault("rate", 0)
                    )
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

        schedules.sort(Comparator.comparing(o -> o.getLong("start_at")));

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
                    userId, scheduleId, false, true
            );

            int price = schedule.getInteger("price");

            double offAmount = 0;
            double shouldPayDouble = price * 1.0;
            Document offDoc = findOff(offCode, userId);

            if (offDoc != null) {
                offAmount +=
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount");

                shouldPayDouble = Math.max(price - offAmount, 0);
            }

            Document request = null;
            if (schedule.containsKey("requests")) {
                request = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        schedule.getList("requests", Document.class), "_id", userId
                );
            }

            boolean isPrivate = schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.PRIVATE.getName());

            int shouldPay = (int) shouldPayDouble;
            if (!isPrivate && request != null)
                shouldPay -= (Integer) request.getOrDefault("wallet_paid", 0) +
                        (Integer) request.getOrDefault("paid", 0);

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

                new Thread(() -> {
                    Document advisor = userRepository.findById(schedule.getObjectId("_id"));
                    createNotifAndSendSMS(
                            advisor,
                            user.getString("first_name") + " " + user.getString("last_name"),
                            "finalizeTeach"
                    );
                }).start();

                teachScheduleRepository.updateOne(
                        scheduleId,
                        new BasicDBObject(
                                "$set",
                                register(schedule, userId, curr, request, isPrivate)
                        )
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
            // todo: set can request false for auto requests and create a request

            return goToPayment((int) (shouldPay - money), doc);

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String rateToTeacher(ObjectId userId, ObjectId advisorId, int rate) {

        Document advisor = userRepository.findById(advisorId);
        if (advisor == null)
            return JSON_NOT_VALID_ID;

        long curr = System.currentTimeMillis();

        if (!teachScheduleRepository.exist(and(
                eq("user_id", advisorId),
                eq("students._id", userId),
                lt("start_at", curr)
        )))
            return JSON_NOT_ACCESS;

        Document teachRate = teachRateRepository.findOne(and(
                eq("student_id", userId),
                eq("teacher_id", advisorId)
        ), null);

        double oldTotalRate = (double) advisor.getOrDefault("teach_rate", (double) 0);
        int rateCount = (int) advisor.getOrDefault("teach_rate_count", 0);
        oldTotalRate *= rateCount;
        oldTotalRate += rate;
        boolean isNew = false;

        if (teachRate == null) {
            teachRate = new Document("student_id", userId)
                    .append("teacher_id", advisorId);
            rateCount++;
            isNew = true;
        } else
            oldTotalRate -= teachRate.getInteger("rate");

        teachRate
                .append("rate_at", curr)
                .append("rate", rate);

        if (isNew)
            teachRateRepository.insertOne(teachRate);
        else
            teachRateRepository.replaceOne(
                    teachRate.getObjectId("_id"), teachRate
            );

        double newRate = Math.round(oldTotalRate / rateCount * 100.0) / 100.0;
        advisor.put("teach_rate", newRate);
        advisor.put("teach_rate_count", rateCount);

        userRepository.updateOne(
                advisorId,
                new BasicDBObject("$set",
                        new BasicDBObject("teach_rate", newRate)
                                .append("teach_rate_count", rateCount)
                )
        );

        return generateSuccessMsg("rate", newRate);
    }

    public static String rateToSchedule(ObjectId userId, ObjectId scheduleId, int userRate) {
        try {
            Document schedule = getSchedule(userId, scheduleId, true, false);

            List<Document> students = schedule.getList("students", Document.class);
            Document studentDoc = searchInDocumentsKeyVal(students, "_id", userId);
            studentDoc.put("rate", userRate);
            studentDoc.put("rate_at", System.currentTimeMillis());

            teachScheduleRepository.updateOne(
                    scheduleId, set("students", students)
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
        long oneYearMs = ONE_DAY_MIL_SEC * 365;

        boolean isAllFiltersOff = (returnFilters == null || returnFilters) && maxAge == null && minAge == null &&
                tag == null && minRate == null && maxRate == null;

        int minAgeFilter = -1, maxAgeFilter = -1;
        HashMap<ObjectId, String> branches = new HashMap<>();
        HashMap<ObjectId, Document> grades = new HashMap<>();

        for (Document teacher : teachers) {

            JSONObject jsonObject = convertTeacherToJSONDigest(
                    null, teacher, branches, grades
            );

            int age = -1;
            if (teacher.containsKey("birth_day")) {
                age = (int) ((curr - teacher.getLong("birth_day")) / oneYearMs);
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
        docs.forEach(jsonArray::put);

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

    public static String setMyTeachScheduleReportProblems(
            final ObjectId userId, final ObjectId scheduleId,
            final JSONArray tagIds, final String desc
    ) {

        List<Object> tagOIdsList = null;

        if(tagIds != null && tagIds.length() > 0) {
            Set<ObjectId> tagOIds = new HashSet<>();
            try {
                for (int i = 0; i < tagIds.length(); i++) {
                    if (!ObjectId.isValid(tagIds.getString(i)))
                        return JSON_NOT_VALID_PARAMS;
                    tagOIds.add(new ObjectId(tagIds.getString(i)));
                }
            } catch (Exception ex) {
                return JSON_NOT_VALID_PARAMS;
            }

            tagOIdsList = new ArrayList<>(tagOIds);
            if (teachTagReportRepository.findByIds(tagOIdsList, false, JUST_ID) == null)
                return JSON_NOT_VALID_PARAMS;
        }

        if(tagOIdsList == null)
            tagOIdsList = new ArrayList<>();

        Document myTeachReport = teachReportRepository.findOne(
                and(
                        eq("send_from", "student"),
                        eq("student_id", userId),
                        eq("schedule_id", scheduleId)
                ), null
        );

        boolean isFirstReport = false;

        if(myTeachReport == null) {
            isFirstReport = true;
            Document schedule = teachScheduleRepository.findById(scheduleId);
            long curr = System.currentTimeMillis();

            if(schedule == null || !schedule.containsKey("students") ||
                    schedule.getLong("start_at") > curr ||
                    schedule.getLong("start_at") + 30 * ONE_DAY_MIL_SEC < curr ||
                    searchInDocumentsKeyValIdx(
                            schedule.getList("students", Document.class), "_id", userId
                    ) == -1
            )
                return JSON_NOT_ACCESS;

            myTeachReport = new Document("student_id", userId)
                    .append("schedule_id", scheduleId)
                    .append("send_from", "student")
                    .append("seen", false)
                    .append("teacher_id", schedule.getObjectId("user_id"))
                    .append("created_at", System.currentTimeMillis());
        }
        else if(desc == null)
            myTeachReport.remove("desc");

        myTeachReport.put("tag_ids", tagOIdsList);
        if(desc != null)
            myTeachReport.put("desc", desc);

        if(isFirstReport)
            teachReportRepository.insertOne(myTeachReport);
        else
            teachReportRepository.replaceOne(
                    myTeachReport.getObjectId("_id"),
                    myTeachReport
            );

        return JSON_OK;
    }

    public static String getMyTeachScheduleReportProblems(
            ObjectId userId, ObjectId scheduleId
    ) {
        Document myTeachReport = teachReportRepository.findOne(
                and(
                        eq("student_id", userId),
                        eq("schedule_id", scheduleId)
                ), new BasicDBObject("tag_ids", 1).append("desc", 1)
        );

        if(myTeachReport == null)
            return generateSuccessMsg("data", new JSONArray());

        List<Document> tags = teachTagReportRepository.findByIds(
                myTeachReport.getList("tag_ids", Object.class),
                false, null
        );

        if(tags == null)
            return JSON_NOT_UNKNOWN;

        JSONArray jsonArray = new JSONArray();
        for (Document tag : tags)
            jsonArray.put(tag.getString("label"));

        return generateSuccessMsg("data",
                new JSONObject()
                        .put("tags", jsonArray)
                        .put("desc", myTeachReport.getOrDefault("desc", ""))
        );
    }

    public static String getMyRate(ObjectId userId, ObjectId teacherId) {
        Document myRate = teachRateRepository.findOne(
                and(
                        eq("student_id", userId),
                        eq("teacher_id", teacherId)
                ), new BasicDBObject("rate", 1)
        );

        if (myRate != null)
            return generateSuccessMsg("data", myRate.getInteger("rate"));

        return generateSuccessMsg("data", 0);
    }

}
