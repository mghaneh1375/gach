package irysc.gachesefid.Controllers.Advisor;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Advisor.AdvisorController.returnRequests;
import static irysc.gachesefid.Controllers.Advisor.AdvisorController.setAdvisor;
import static irysc.gachesefid.Controllers.Advisor.Utility.*;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.transactionRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentAdviceController {

    public static String setDoneTime(ObjectId userId, ObjectId id, ObjectId itemId, JSONObject jsonObject) {

        if (!jsonObject.getBoolean("fullDone") && !jsonObject.has("duration"))
            return generateErr("لطفا زمان انجام شده را وارد نمایید");

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null || !schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        int d = Utility.convertStringToDate(schedule.getString("week_start_at"));
        int today = Utility.getToday();

        if (d > today)
            return generateErr("هنوز زمان ثبت عملکرد نرسیده است");

        List<Document> days = schedule.getList("days", Document.class);
        for (Document day : days) {

            if (!day.containsKey("items"))
                continue;

            List<Document> items = day.getList("items", Document.class);
            Document item = Utility.searchInDocumentsKeyVal(items, "_id", itemId);

            if (item == null)
                continue;

            if (item.containsKey("additional") && !jsonObject.has("additional"))
                return generateErr("لطفا " + item.getString("additional_label") + " را وارد نمایید");

            if (jsonObject.getBoolean("fullDone"))
                item.put("done_duration", item.getInteger("duration"));
            else if (jsonObject.getInt("duration") > item.getInteger("duration"))
                return generateErr("زمان انجام شده باید حداکثر " + item.getInteger("duration") + " باشد");
            else
                item.put("done_duration", jsonObject.getInt("duration"));

            if (item.containsKey("additional"))
                item.put("done_additional", jsonObject.getInt("additional"));

            scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

            return generateSuccessMsg("data", new JSONObject()
                    .put("days", convertScheduleToJSON(schedule, null))
            );

        }

        return JSON_NOT_VALID_ID;
    }

    public static String getMyAdvisors(Document user) {

        JSONArray jsonArray = new JSONArray();
        List<ObjectId> myAdvisors = (List<ObjectId>) user.getOrDefault("my_advisors", new ArrayList<ObjectId>());
        ObjectId userId = user.getObjectId("_id");

        for (ObjectId advisorId : myAdvisors) {

            Document advisor = userRepository.findById(advisorId);

            if (advisor == null)
                continue;

            JSONObject jsonObject = convertToJSONDigest(userId, advisor);

            Document request = advisorRequestsRepository.findOne(and(
                    eq("advisor_id", advisorId),
                    eq("user_id", userId),
                    exists("paid_at", true)
            ), null);

            if(request == null)
                return JSON_NOT_UNKNOWN;

            JSONObject tmp = new JSONObject();

            tmp
                    .put("id", "-1")
                    .put("createdAt", getSolarDate(request.getLong("paid_at")))
                    .put("finishAt", getSolarDate(request.getLong("paid_at") + ONE_DAY_MIL_SEC * 30))
                    .put("title", request.getString("title"))
                    .put("videoCalls", request.getInteger("video_calls"))
                    .put("maxKarbarg", request.getOrDefault("max_karbarg", -1))
                    .put("maxExam", request.getOrDefault("max_exam", -1))
                    .put("maxChat", request.getOrDefault("max_chat", -1));

            jsonObject.put("plan", tmp);

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String rate(ObjectId userId, ObjectId advisorId, int rate) {

        Document advisor = userRepository.findById(advisorId);
        if (advisor == null || !advisor.containsKey("students"))
            return JSON_NOT_UNKNOWN;

        List<Document> students = advisor.getList("students", Document.class);

        Document stdDoc = searchInDocumentsKeyVal(
                students, "_id", userId
        );

        if (stdDoc == null)
            return JSON_NOT_UNKNOWN;

        int oldRate = (int) stdDoc.getOrDefault("rate", 0);
        stdDoc.put("rate", rate);
        stdDoc.put("rate_at", System.currentTimeMillis());

        double oldTotalRate = (double) advisor.getOrDefault("rate", (double) 0);
        int rateCount = (int) advisor.getOrDefault("rate_count", 0);

        oldTotalRate *= rateCount;

        if (oldRate == 0)
            rateCount++;

        oldTotalRate -= oldRate;
        oldTotalRate += rate;

        double newRate = Math.round(oldTotalRate / rateCount * 100.0) / 100.0;
        advisor.put("rate", newRate);
        advisor.put("rate_count", rateCount);

        userRepository.replaceOne(advisorId, advisor);
        return generateSuccessMsg("rate", newRate);

    }

    public static String cancelRequest(ObjectId userId, ObjectId reqId) {

        Document doc = advisorRequestsRepository.findOneAndDelete(
                and(
                        eq("_id", reqId),
                        eq("user_id", userId),
                        or(
                                eq("answer", "pending"),
                                and(
                                        eq("answer", "accept"),
                                        exists("paid", false)
                                )
                        )
                )
        );

        if (doc == null)
            return generateErr("شما مجاز به حذف این درخواست نیستید");

        return JSON_OK;
    }

    public static String payAdvisorPrice(ObjectId userId, double userMoney,
                                         ObjectId advisorId, JSONObject jsonObject) {

        Document doc = advisorRequestsRepository.findOne(and(
                eq("answer", "accept"),
                eq("advisor_id", advisorId),
                eq("user_id", userId),
                exists("paid", false),
                exists("price", true)
        ), new BasicDBObject("price", 1));

        if (doc == null)
            return JSON_NOT_ACCESS;

        int shouldPay = doc.getInteger("price");

        Document off = null;
        long curr = System.currentTimeMillis();

        if (jsonObject != null && jsonObject.has("off")) {

            off = validateOffCode(
                    jsonObject.getString("off"), userId, curr,
                    OffCodeSections.COUNSELING.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");

        }

        if (jsonObject == null || !jsonObject.has("off")) {
            off = findAccountOff(
                    userId, curr, OffCodeSections.COUNSELING.getName()
            );
        }

        double offAmount = 0;

        if (off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPay * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPay -= offAmount;
        }

        if (shouldPay - userMoney <= 100) {

            double newUserMoney = userMoney;

            Document student = userRepository.findById(userId);

            if (shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, userMoney);
                student.put("money", newUserMoney);
            }

            Document transaction = new Document("user_id", userId)
                    .append("amount", 0)
                    .append("account_money", shouldPay)
                    .append("created_at", curr)
                    .append("status", "success")
                    .append("section", OffCodeSections.COUNSELING.getName())
                    .append("products", advisorId);

            if (off != null) {
                transaction.append("off_code", off.getObjectId("_id"));
                transaction.append("off_amount", (int) offAmount);
            }

            transactionRepository.insertOne(transaction);

            Document advisorRequest = advisorRequestsRepository.findById(doc.getObjectId("_id"));
            advisorRequest.put("paid", shouldPay);
            advisorRequest.put("paid_at", curr);
            advisorRequestsRepository.replaceOne(advisorRequest.getObjectId("_id"), advisorRequest);

            Document advisor = userRepository.findById(advisorId);

            setAdvisor(student, advisor);

            if (off != null) {

                BasicDBObject update;

                if (off.containsKey("is_public") &&
                        off.getBoolean("is_public")
                ) {
                    List<ObjectId> students = off.getList("students", ObjectId.class);
                    students.add(userId);
                    update = new BasicDBObject("students", students);
                } else {

                    update = new BasicDBObject("used", true)
                            .append("used_at", curr)
                            .append("used_section", OffCodeSections.GACH_EXAM.getName())
                            .append("used_for", userId);
                }

                offcodeRepository.updateOne(
                        off.getObjectId("_id"),
                        new BasicDBObject("$set", update)
                );
            }


            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", newUserMoney)
            );
        }

        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document transaction =
                new Document("user_id", userId)
                        .append("account_money", userMoney)
                        .append("amount", (int) (shouldPay - userMoney))
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", doc.getObjectId("_id"))
                        .append("section", OffCodeSections.COUNSELING.getName());

        if (off != null) {
            transaction.append("off_code", off.getObjectId("_id"));
            transaction.append("off_amount", (int) offAmount);
        }

        return goToPayment((int) (shouldPay - userMoney), doc);
    }

    public static String addItemToMyLifeStyle(ObjectId userId, JSONObject data) {

        String day = data.getString("day");

        int dayIndex;
        try {
            dayIndex = validateDay(day);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = lifeStyleTagRepository.findById(tagId);
        if (tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        if (doc != null) {

            List<Document> items = doc.getList("items", Document.class);

            if (Utility.searchInDocumentsKeyValIdx(
                    items, "tag", tag.getString("label")
            ) != -1)
                return generateErr("تگ وارد شده در روز موردنظر موجود است");

            ObjectId newId = new ObjectId();
            Document newDoc = new Document("_id", newId)
                    .append("tag", tag.getString("label"))
                    .append("duration", data.getInt("duration"));

            if (data.has("startAt"))
                newDoc.put("start_at", data.getString("start_at"));

            items.add(newDoc);
            lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

            return generateSuccessMsg("id", newId.toString());

        }


        return JSON_NOT_UNKNOWN;
    }

    public static String removeItemFromMyLifeStyle(ObjectId userId, JSONObject data) {

        int dayIndex;
        try {
            dayIndex = validateDay(data.getString("day"));
        } catch (InvalidFieldsException e) {
            return JSON_NOT_VALID_PARAMS;
        }

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        if (doc != null) {

            List<Document> items = doc.getList("items", Document.class);
            int idx = Utility.searchInDocumentsKeyValIdx(
                    items, "tag", data.getString("tag")
            );

            if (idx < 0)
                return JSON_NOT_VALID_PARAMS;

            items.remove(idx);
            lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

            return JSON_OK;

        }


        return JSON_NOT_UNKNOWN;
    }

    public static String setMyExamInLifeStyle(ObjectId userId, JSONArray exams) {

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<String> examTags = new ArrayList<>();

        for (int i = 0; i < exams.length(); i++) {

            if (!ObjectId.isValid(exams.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            Document examTag = adviseExamTagRepository.findById(new ObjectId(exams.getString(i)));
            if (examTag == null)
                return JSON_NOT_VALID_PARAMS;

            examTags.add(examTag.getString("label"));
        }

        schedule.put("exams", examTags);
        lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

        return JSON_OK;
    }

    public static String myLifeStyle(ObjectId userId) {

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null) {
            schedule = new Document("days", new ArrayList<>() {{
                add(new Document("day", 0).append("items", new ArrayList<>()));
                add(new Document("day", 1).append("items", new ArrayList<>()));
                add(new Document("day", 2).append("items", new ArrayList<>()));
                add(new Document("day", 3).append("items", new ArrayList<>()));
                add(new Document("day", 4).append("items", new ArrayList<>()));
                add(new Document("day", 5).append("items", new ArrayList<>()));
                add(new Document("day", 6).append("items", new ArrayList<>()));
            }}).append("user_id", userId).append("created_at", System.currentTimeMillis());

            lifeScheduleRepository.insertOne(schedule);
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("days", convertLifeScheduleToJSON(schedule))
                .put("exams", schedule.getList("exams", String.class))
        );
    }

    public static String myRequests(ObjectId userId) {

        List<Document> requests = advisorRequestsRepository.find(
                eq("user_id", userId), null, Sorts.descending("created_at")
        );

        return returnRequests("advisor_id", requests);
    }

    public static String mySchedule(ObjectId advisorId, ObjectId userId,
                                    Integer scheduleFor, ObjectId id) {

        Document schedule;

        if (id != null) {

            schedule = scheduleRepository.findById(id);
            if (schedule == null)
                return JSON_NOT_VALID_ID;

            if (userId == null)
                userId = schedule.getObjectId("user_id");
            else if (!schedule.getObjectId("user_id").equals(userId))
                return JSON_NOT_ACCESS;

            if (advisorId != null && !Authorization.hasAccessToThisStudent(userId, advisorId))
                return JSON_NOT_ACCESS;
        } else {

            String weekStartAt;

            if (scheduleFor == 0)
                weekStartAt = getFirstDayOfCurrWeek();
            else
                weekStartAt = getFirstDayOfFutureWeek(scheduleFor);

            schedule = scheduleRepository.findOne(and(
                    eq("user_id", userId),
                    eq("week_start_at", weekStartAt)
            ), null, Sorts.descending("week_start_at_int"));

        }

        if (schedule == null) {
            schedule = new Document("days", new ArrayList<>() {{
                add(new Document("day", 0).append("items", new ArrayList<>()));
                add(new Document("day", 1).append("items", new ArrayList<>()));
                add(new Document("day", 2).append("items", new ArrayList<>()));
                add(new Document("day", 3).append("items", new ArrayList<>()));
                add(new Document("day", 4).append("items", new ArrayList<>()));
                add(new Document("day", 5).append("items", new ArrayList<>()));
                add(new Document("day", 6).append("items", new ArrayList<>()));
            }}).append("user_id", userId).append("created_at", System.currentTimeMillis());
        }

        JSONObject jsonObject = new JSONObject()
                .put("days", convertScheduleToJSON(schedule, advisorId));

        if (schedule.containsKey("advisors_desc")) {

            if (advisorId != null) {

                Document advisorDesc = searchInDocumentsKeyVal(
                        schedule.getList("advisors_desc", Document.class),
                        "advisor_id", advisorId
                );

                if (advisorDesc != null)
                    jsonObject.put("advisorDesc", advisorDesc.getString("description"));

            }
            else {

                JSONArray descs = new JSONArray();

                for(Document advisorDesc : schedule.getList("advisors_desc", Document.class)) {

                    Document advisor = userRepository.findById(advisorDesc.getObjectId("advisor_id"));
                    if(advisor == null)
                        continue;

                    descs.put(
                            new JSONObject()
                                    .put("desc", advisorDesc.getString("description"))
                                    .put("advisor", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                    );
                }

                jsonObject.put("advisorsDesc", descs);
            }
        }


        return generateSuccessMsg("data", jsonObject);
    }

    public static String getMyCurrentRoom(ObjectId studentId) {

        long curr = System.currentTimeMillis();
        long limitTime = curr - ONE_HOUR_MIL_SEC * 5;

        List<Document> docs = advisorMeetingRepository.find(and(
                eq("student_id", studentId),
                gt("created_at", limitTime),
                lt("created_at", curr),
                exists("url")
        ), new BasicDBObject("url", 1).append("advisor_id", 1).append("created_at", 1));

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            Document advisor = userRepository.findById(doc.getObjectId("advisor_id"));

            if (advisor == null)
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("advisor", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                            .put("url", doc.getString("url"))
                            .put("createdAt", getSolarDate(doc.getLong("created_at")))
            );

        }

        return generateSuccessMsg("data", jsonArray);
    }
}
