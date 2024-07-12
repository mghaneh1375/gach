package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Teaching.Utility.convertScheduleToJSONDigest;
import static irysc.gachesefid.Main.GachesefidApplication.teachScheduleRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class TeachController {

    // ######################## ADMIN SECTION ######################
    public static String setAdvisorIRYSCPercent(ObjectId advisorId, Integer teachPercent, Integer advicePercent) {
        Document user = userRepository.findById(advisorId);
        if(user == null)
            return JSON_NOT_VALID_ID;

        user.put("irysc_teach_percent", teachPercent);
        user.put("irysc_advice_percent", advicePercent);
        Bson update = new BasicDBObject("$set",
                new BasicDBObject("irysc_teach_percent", teachPercent)
                        .append("irysc_advice_percent", advicePercent)
        );

        userRepository.updateOne(user, update);
        return JSON_OK;
    }

    public static String getAdvisorIRYSCPercent(ObjectId advisorId) {
        Document user = userRepository.findById(advisorId);
        if(user == null)
            return JSON_NOT_VALID_ID;

        if(user.containsKey("irysc_teach_percent"))
            return generateSuccessMsg("data",
                    new JSONObject()
                            .put("iryscTeachPercent", user.get("irysc_teach_percent"))
                            .put("iryscAdvicePercent", user.get("irysc_advice_percent"))
            );

        Document config = getConfig();
        return generateSuccessMsg("data",
                new JSONObject()
                        .put("iryscTeachPercent", config.get("irysc_teach_percent"))
                        .put("iryscAdvicePercent", config.get("irysc_advice_percent"))
        );
    }


    // ######################## ADVISOR SECTION ######################

    public static String createNewSchedule(Document user, JSONObject jsonObject) {

        if (jsonObject.getLong("start") < System.currentTimeMillis())
            return generateErr("زمان باید بزرگ تر از امروز باشد");

        int defaultPrice = (int) user.getOrDefault("default_teach_price", -1);

        if (!jsonObject.has("price") && defaultPrice == -1)
            return generateErr("شما باید یا هزینه پیش فرض جلسات را تعیین کرده باشید و یا هزینه این جلسه را تعیین نمایید");

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName()) &&
                !jsonObject.has("minCap")
        )
            return generateErr("لطفا حداقل تعداد نفرات برای تشکیل جلسه را تعیین نمایید");

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName()) &&
                !jsonObject.has("maxCap")
        )
            return generateErr("لطفا حداکثر تعداد نفرات برای تشکیل جلسه را تعیین نمایید");

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName())) {
            Document config = getConfig();
            if (jsonObject.getInt("maxCap") > config.getInteger("max_teach_cap"))
                return generateErr("حداکثر تعداد نفرات در هر جلسه نباید بیشتر از " + config.getInteger("max_teach_cap") + " باشد");
        }

        ObjectId userId = user.getObjectId("_id");
        Document newDoc = new Document("user_id", userId)
                .append("start_at", jsonObject.getLong("start"))
                .append("length", jsonObject.getInt("length"))
                .append("created_at", System.currentTimeMillis())
                .append("visibility", jsonObject.getBoolean("visibility"))
                .append("price", jsonObject.has("price") ? jsonObject.getInt("price") : defaultPrice)
                .append("teach_mode", jsonObject.getString("teachMode"))
                .append("need_registry_confirmation",
                        !jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName()) &&
                                (
                                        !jsonObject.has("needRegistryConfirmation") ||
                                                jsonObject.getBoolean("needRegistryConfirmation")
                                )
                )
                .append("can_request", true);

        if (jsonObject.has("description"))
            newDoc.append("description", jsonObject.getString("description"));

        if (jsonObject.has("title"))
            newDoc.append("title", jsonObject.getString("title"));

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName())) {
            newDoc.append("max_cap", jsonObject.getInt("maxCap"));
            newDoc.append("min_cap", jsonObject.getInt("minCap"));
        }

        ObjectId id = teachScheduleRepository.insertOneWithReturnId(newDoc);
        if (user.containsKey("teach")) {
            user.put("teach", true);
            userRepository.updateOne(userId, set("teach", true));
        }

        return generateSuccessMsg("id", id);
    }

    public static String removeSchedule(ObjectId userId, ObjectId scheduleId) {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        if (schedule.containsKey("students") &&
                schedule.getList("students", Document.class).size() > 0
        )
            return generateErr("دانش آموزانی در این جلسه حضور دارند و امکان حذف این جلسه وجود ندارد");

        teachScheduleRepository.deleteOne(eq("_id", scheduleId));
        return JSON_OK;
    }

//    public static String createMeetingRoom() {
//
//    }

    public static String setRequestStatus(
            ObjectId userId, ObjectId scheduleId,
            ObjectId studentId, Boolean status
    ) {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        if(schedule.getString("teach_mode").equalsIgnoreCase(
                TeachMode.SEMI_PRIVATE.getName()
        ))
            return generateErr("در جلسات نیمه خصوصی مدیریت وضعیت ثبت نام برعهده سیستم است");

        long curr = System.currentTimeMillis();
        if (schedule.getLong("start_at") < curr)
            return generateErr("این جلسه به اتمام رسیده و امکان تغییر وضعیت ثبت نام افراد در آن وجود ندارد");

        if (!schedule.containsKey("requests"))
            return JSON_NOT_VALID_ID;

        if (schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.PRIVATE.getName()) &&
                schedule.containsKey("students") && schedule.getList("students", Document.class).size() > 0
        )
            return generateErr("برای این جلسه یک دانش آموز وجود دارد و امکان افزودن دانش آموز جدیدی وجود ندارد");

        List<Document> requests = schedule.getList("requests", Document.class);
        Document request = Utility.searchInDocumentsKeyVal(requests, "_id", studentId);
        if (request == null)
            return JSON_NOT_VALID_ID;

        if (!request.getString("status").equals("pending"))
            return generateErr("این درخواست قبلا پاسخ داده شده است");

        request.put("status", status ? "accept" : "reject");
        // todo: send sms and mail

        if (status) {
            schedule.put("can_request", false);
            request.put("expire_at", curr + PAY_SCHEDULE_EXPIRATION_MSEC);
        }

        teachScheduleRepository.replaceOneWithoutClearCache(
                scheduleId, schedule
        );

        return JSON_OK;
    }

    public static String getRequests(ObjectId userId) {

        List<Document> schedules = teachScheduleRepository.find(and(
                eq("user_id", userId),
                gt("start_at", System.currentTimeMillis()),
                exists("requests", true),
                exists("requests.0", true)
        ), null);

        JSONArray jsonArray = new JSONArray();
        List<Object> usersId = new ArrayList<>();

        for (Document schedule : schedules) {
            for (Document request : schedule.getList("requests", Document.class))
                usersId.add(request.getObjectId("_id"));
        }

        List<Document> users = userRepository.findByIds(usersId, true, USER_PUBLIC_INFO);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        for (Document schedule : schedules) {

            List<Document> requests = schedule.getList("requests", Document.class);

            JSONObject scheduleJSON = new JSONObject()
                    .put("start", getSolarDate(schedule.getLong("start_at")))
                    .put("teachMode", schedule.getString("teach_mode"))
                    .put("requestCounts", requests.size())
                    .put("minCap", schedule.getOrDefault("min_cap", 1))
                    .put("maxCap", schedule.getOrDefault("max_cap", 1))
                    .put("canRequest", schedule.getBoolean("can_request"))
                    .put("title", schedule.getOrDefault("title", ""))
                    .put("time", schedule.getOrDefault("time", ""));

            JSONArray requestsJSON = new JSONArray();

            for (Document request : requests) {

                Document user = users.stream()
                        .filter(document -> document.getObjectId("_id").equals(request.getObjectId("_id")))
                        .findFirst().get();

                JSONObject jsonObject = new JSONObject()
                        .put("createdAt", getSolarDate(request.getLong("created_at")))
                        .put("status", request.getString("status"));

                Utility.fillJSONWithUserPublicInfo(jsonObject, user);
                requestsJSON.put(jsonObject);
            }

            scheduleJSON.put("requests", requestsJSON);
            jsonArray.put(scheduleJSON);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getSchedules(
            ObjectId userId,
            Long from, Long to,
            Boolean justHasStudents,
            Boolean justHasRequests,
            String teachMode
    ) {
        List<Bson> filters = new ArrayList<>();

        if (userId != null)
            filters.add(eq("user_id", userId));

        if (from != null)
            filters.add(gte("start_at", from));

        if (to != null)
            filters.add(lte("start_at", to));

        if (justHasStudents != null)
            filters.add(exists("students.0"));

        if (justHasRequests != null)
            filters.add(exists("requests.0"));

        if (teachMode != null)
            filters.add(eq("teach_mode", teachMode));

        List<Document> schedules = teachScheduleRepository.find(
                filters.size() == 0 ? null : and(filters), SCHEDULE_DIGEST
        );
        List<Document> users = null;

        if (userId == null) {
            List<Object> userIds = new ArrayList<>();
            for (Document schedule : schedules)
                userIds.add(schedule.getObjectId("user_id"));

            users = userRepository.findByIds(userIds, true,
                    new BasicDBObject("first_name", 1).append("last_name", 1)
                            .append("_id", 1)
            );
        }

        JSONArray jsonArray = new JSONArray();
        for (Document schedule : schedules) {
            jsonArray.put(convertScheduleToJSONDigest(
                    schedule, userId == null, users
            ));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getSchedule(ObjectId userId, ObjectId scheduleId) {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (userId != null && !schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        List<Object> usersId = new ArrayList<>();

        for (Document student : schedule.getList("students", Document.class))
            usersId.add(student.getObjectId("_id"));

        List<Document> users = userRepository.findByIds(usersId, true, USER_PUBLIC_INFO);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        JSONObject jsonObject = convertScheduleToJSONDigest(schedule, false, null);
        jsonObject.put("description", schedule.getOrDefault("description", ""));

        JSONArray students = new JSONArray();
        for (Document student : schedule.getList("students", Document.class)) {
            users.stream().filter(user -> user.getObjectId("_id").equals(student.getObjectId("_id")))
                    .findFirst().ifPresent(user -> {
                        JSONObject tmp = new JSONObject();
                        fillJSONWithUserPublicInfo(tmp, user);
                        students.put(tmp);
                    });
        }

        jsonObject.put("students", students);

        return generateSuccessMsg("data", jsonObject);
    }


}
