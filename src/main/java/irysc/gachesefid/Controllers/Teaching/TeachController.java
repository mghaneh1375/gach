package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Models.ActiveMode;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Models.TeachRequestStatus;
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
import static irysc.gachesefid.Controllers.Teaching.Utility.convertScheduleToJSONDigestForTeacher;
import static irysc.gachesefid.Controllers.Teaching.Utility.getScheduleForCreateSkyRoom;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.SkyRoomUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class TeachController {

    // ######################## ADMIN SECTION ######################
    public static String setAdvisorIRYSCPercent(ObjectId advisorId, Integer teachPercent, Integer advicePercent) {
        Document user = userRepository.findById(advisorId);
        if (user == null)
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
        if (user == null)
            return JSON_NOT_VALID_ID;

        if (user.containsKey("irysc_teach_percent"))
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
        Document newDoc = new Document("_id", new ObjectId())
                .append("user_id", userId)
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

        teachScheduleRepository.insertOneWithReturnId(newDoc);
        if (user.containsKey("teach")) {
            user.put("teach", true);
            userRepository.updateOne(userId, set("teach", true));
        }

        return generateSuccessMsg("data",
                convertScheduleToJSONDigestForTeacher(newDoc, false, null, false)
        );
    }

    public static String copySchedule(ObjectId userId, ObjectId scheduleId, JSONObject jsonObject) {

        if (jsonObject.getLong("start") < System.currentTimeMillis())
            return generateErr("زمان باید بزرگ تر از امروز باشد");

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if(schedule == null || !schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        Document newDoc = new Document("_id", new ObjectId())
                .append("user_id", userId)
                .append("start_at", jsonObject.getLong("start"))
                .append("length", schedule.getInteger("length"))
                .append("created_at", System.currentTimeMillis())
                .append("visibility", schedule.getBoolean("visibility"))
                .append("price", schedule.getInteger("price"))
                .append("teach_mode", schedule.getString("teach_mode"))
                .append("need_registry_confirmation", schedule.getBoolean("need_registry_confirmation"))
                .append("can_request", true);

        if (schedule.containsKey("description"))
            newDoc.append("description", schedule.getString("description"));

        if (schedule.containsKey("title"))
            newDoc.append("title", schedule.getString("title"));

        if (schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName())) {
            newDoc.append("max_cap", schedule.getInteger("maxCap"));
            newDoc.append("min_cap", schedule.getInteger("minCap"));
        }

        teachScheduleRepository.insertOne(newDoc);

        return generateSuccessMsg("data",
                convertScheduleToJSONDigestForTeacher(newDoc, false, null, false)
        );
    }

    public static String updateSchedule(ObjectId userId, ObjectId scheduleId, JSONObject jsonObject) {

        if (jsonObject.getLong("start") < System.currentTimeMillis())
            return generateErr("زمان باید بزرگ تر از امروز باشد");

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName()) &&
                !jsonObject.has("minCap")
        )
            return generateErr("لطفا حداقل تعداد نفرات برای تشکیل جلسه را تعیین نمایید");

        if (jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName()) &&
                !jsonObject.has("maxCap")
        )
            return generateErr("لطفا حداکثر تعداد نفرات برای تشکیل جلسه را تعیین نمایید");

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        boolean isPrivate = jsonObject.getString("teachMode").equalsIgnoreCase(TeachMode.PRIVATE.getName());

        if (!isPrivate && schedule.getInteger("max_cap") != jsonObject.getInt("maxCap")) {
            Document config = getConfig();
            if (jsonObject.getInt("maxCap") > config.getInteger("max_teach_cap"))
                return generateErr("حداکثر تعداد نفرات در هر جلسه نباید بیشتر از " + config.getInteger("max_teach_cap") + " باشد");
        }

        schedule.put("start_at", jsonObject.getLong("start"));
        schedule.put("length", jsonObject.getInt("length"));
        schedule.put("visibility", jsonObject.getBoolean("visibility"));
        schedule.put("price", jsonObject.getInt("price"));
        schedule.put("teach_mode", jsonObject.getString("teachMode"));
        schedule.put("need_registry_confirmation",
                isPrivate &&
                        (
                                !jsonObject.has("needRegistryConfirmation") ||
                                        jsonObject.getBoolean("needRegistryConfirmation")
                        )
        );

        if (jsonObject.has("description"))
            schedule.put("description", jsonObject.getString("description"));
        else
            schedule.remove("description");

        if (jsonObject.has("title"))
            schedule.put("title", jsonObject.getString("title"));
        else
            schedule.remove("title");

        if (isPrivate) {
            schedule.remove("min_cap");
            schedule.remove("max_cap");
        } else {
            schedule.put("max_cap", jsonObject.getInt("maxCap"));
            schedule.put("min_cap", jsonObject.getInt("minCap"));
        }

        teachScheduleRepository.replaceOneWithoutClearCache(scheduleId, schedule);

        return generateSuccessMsg("data",
                convertScheduleToJSONDigestForTeacher(schedule, false, null, false)
        );
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

    public static String createMeetingRoom(
            ObjectId userId, String advisorName,
            String NID, ObjectId scheduleId
    ) {
        try {
            Document schedule = getScheduleForCreateSkyRoom(scheduleId, userId);
            int teacherSkyRoomId = createUser(NID, advisorName);

            if (teacherSkyRoomId == -1)
                return generateErr("امکان ایجاد دبیر در سایت اسکای روم در حال حاضر وجود ندارد");

            List<Object> studentsId = schedule.getList("students", Document.class)
                    .stream().map(document -> document.getObjectId("_id")).collect(Collectors.toList());

            List<Document> users = userRepository.findByIds(studentsId, false,
                    new BasicDBObject("first_name", 1).append("NID", 1)
                            .append("last_name", 1)
            );
            if(users == null)
                return JSON_NOT_UNKNOWN;

            List<Integer> studentsSkyRoomId = new ArrayList<>();
            for(Document user : users) {
                String username = user.getString("first_name") + " " + user.getString("last_name");
                int studentSkyRoomId = createUser(user.getString("NID"), username);
                if (studentSkyRoomId == -1)
                    return generateErr("امکان ایجاد دانش آموز/دانش آموزان در سایت اسکای روم در حال حاضر وجود ندارد");
                studentsSkyRoomId.add(studentSkyRoomId);
            }

            String roomUrl = "teaching-" + scheduleId.toString() + "-" + schedule.getLong("start_at");

            int roomId = createMeeting("جلسه تدریس استاد " + advisorName + " - " + getSolarDate(schedule.getLong("start_at")), roomUrl, users.size() + 1, true);
            if (roomId == -1)
                return generateErr("امکان ساخت اتاق جلسه در حال حاضر وجود ندارد");

            String url = SKY_ROOM_PUBLIC_URL + roomUrl;
            addUserToClass(studentsSkyRoomId, teacherSkyRoomId, roomId);
            schedule.put("sky_room_url", url);
            teachScheduleRepository.updateOne(scheduleId, set("sky_room_url", url));

            List<WriteModel<Document>> writeModels = new ArrayList<>();
            for(Document user : users) {
                createNotifAndSendSMS(user, url, "createTeachRoom");
                writeModels.add(new UpdateOneModel<>(
                        eq("_id", user.getObjectId("_id")),
                        set("events", user.get("events"))
                ));
            }

            userRepository.bulkWrite(writeModels);
            for(Document user : users)
                userRepository.clearFromCache(user.getObjectId("_id"));

            Document document = new Document("advisor_id", userId)
                    .append("created_at", System.currentTimeMillis())
                    .append("room_id", roomId)
                    .append("url", url)
                    .append("advisor_sky_id", teacherSkyRoomId);

            advisorMeetingRepository.insertOne(document);
            return generateSuccessMsg("data", url);
        }
        catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String setRequestStatus(
            ObjectId userId, String teacherName,
            ObjectId scheduleId, ObjectId studentId, Boolean status
    ) {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        if (schedule.getString("teach_mode").equalsIgnoreCase(
                TeachMode.SEMI_PRIVATE.getName()
        ))
            return generateErr("در جلسات نیمه خصوصی مدیریت وضعیت ثبت نام برعهده سیستم است");

        if (!schedule.getBoolean("need_registry_confirmation"))
            return generateErr("تعیین وضعیت ثبت نام این تدریس برعهده سیستم است");

        long curr = System.currentTimeMillis();
        if (schedule.getLong("start_at") < curr)
            return generateErr("این جلسه به اتمام رسیده و امکان تغییر وضعیت ثبت نام افراد در آن وجود ندارد");

        if (!schedule.containsKey("requests"))
            return JSON_NOT_VALID_ID;

        if (schedule.containsKey("students") && schedule.getList("students", Document.class).size() > 0)
            return generateErr("برای این جلسه یک دانش آموز وجود دارد و امکان افزودن دانش آموز جدیدی وجود ندارد");

        List<Document> requests = schedule.getList("requests", Document.class);
        Document request = Utility.searchInDocumentsKeyVal(requests, "_id", studentId);
        if (request == null)
            return JSON_NOT_VALID_ID;

        if (!request.getString("status").equalsIgnoreCase(TeachRequestStatus.PENDING.getName()))
            return generateErr("این درخواست قبلا پاسخ داده شده است");

        request.put("status", status
                ? TeachRequestStatus.ACCEPT.getName()
                : TeachRequestStatus.REJECT.getName()
        );
        request.put("answer_at", curr);
        Document student = userRepository.findById(studentId);

        if (status) {
            schedule.put("can_request", false);
            request.put("expire_at", curr + PAY_SCHEDULE_EXPIRATION_MSEC);
            createNotifAndSendSMS(student, teacherName, "acceptTeach");
        } else
            createNotifAndSendSMS(student, teacherName, "rejectTeach");

        userRepository.updateOne(studentId, set("events", student.get("events")));
        teachScheduleRepository.replaceOneWithoutClearCache(
                scheduleId, schedule
        );

        return JSON_OK;
    }

    public static String getRequests(
            String activeMode,
            String statusMode,
            String teachMode,
            ObjectId userId
    ) {

        List<Bson> filters = new ArrayList<>();
        filters.add(eq("user_id", userId));
        filters.add(exists("requests", true));
        filters.add(exists("requests.0", true));

        long curr = System.currentTimeMillis();

        if (teachMode != null) {
            if (!EnumValidatorImp.isValid(teachMode, TeachMode.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("teach_mode", teachMode));
        }

        if (statusMode != null) {
            if (!EnumValidatorImp.isValid(statusMode, TeachRequestStatus.class) &&
                    !statusMode.equalsIgnoreCase("answered")
            )
                return JSON_NOT_VALID_PARAMS;
        }

        if (activeMode != null) {
            if (!EnumValidatorImp.isValid(activeMode, ActiveMode.class))
                return JSON_NOT_VALID_PARAMS;

            if (activeMode.equalsIgnoreCase("active"))
                filters.add(gt("start_at", curr));
            else
                filters.add(lt("start_at", curr));
        }

        List<Document> schedules = teachScheduleRepository.find(and(filters), null);

        JSONArray jsonArray = new JSONArray();
        List<Object> usersId = new ArrayList<>();

        for (Document schedule : schedules) {
            for (Document request : schedule.getList("requests", Document.class)) {

                if (statusMode != null &&
                        (
                                !statusMode.equalsIgnoreCase("answered") &&
                                        !request.getString("status").equalsIgnoreCase(statusMode)
                        ) ||
                        (
                                Objects.equals(statusMode, "answered") &&
                                        request.getString("status").equalsIgnoreCase("pending")
                        )
                )
                    continue;

                usersId.add(request.getObjectId("_id"));
            }
        }

        List<Document> users = userRepository.findByIds(usersId, true, USER_PUBLIC_INFO);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        for (Document schedule : schedules) {

            List<Document> requests = schedule.getList("requests", Document.class);
            List<Document> wantedRequests = new ArrayList<>();

            if (statusMode != null) {
                for (Document request : requests) {
                    if (
                            (
                                    !statusMode.equalsIgnoreCase("answered") &&
                                            !request.getString("status").equalsIgnoreCase(statusMode)
                            ) || (
                                    Objects.equals(statusMode, "answered") &&
                                            request.getString("status").equalsIgnoreCase("pending")
                            )
                    )
                        continue;
                    wantedRequests.add(request);
                }
            } else
                wantedRequests.addAll(requests);

            if (wantedRequests.size() == 0)
                continue;

            wantedRequests.sort(Comparator.comparing(o -> o.getLong("created_at")));
            JSONObject scheduleJSON = new JSONObject()
                    .put("start", getSolarDate(schedule.getLong("start_at")))
                    .put("teachMode", schedule.getString("teach_mode"))
                    .put("requestCounts", requests.size())
                    .put("id", schedule.getObjectId("_id").toString())
                    .put("minCap", schedule.getOrDefault("min_cap", 1))
                    .put("maxCap", schedule.getOrDefault("max_cap", 1))
                    .put("title", schedule.getOrDefault("title", ""));

            boolean canChangeStatus = schedule.getBoolean("need_registry_confirmation") &&
                    schedule.getLong("start_at") > curr;

            JSONArray requestsJSON = new JSONArray();

            for (Document request : wantedRequests) {

                Document user = users.stream()
                        .filter(document -> document.getObjectId("_id").equals(request.getObjectId("_id")))
                        .findFirst().get();

                JSONObject jsonObject = new JSONObject()
                        .put("createdAt", getSolarDate(request.getLong("created_at")))
                        .put("canChangeStatue",
                                canChangeStatus &&
                                        request.getString("status").equals("pending")
                        )
                        .put("answerAt", request.containsKey("answer_at") ? getSolarDate(request.getLong("answer_at")) : "")
                        .put("expireAt", request.containsKey("expire_at") ? getSolarDate(request.getLong("expire_at")) : "")
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
            String activeMode,
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

        if (activeMode != null) {
            if (!activeMode.equals("active") && !activeMode.equals("expired"))
                return JSON_NOT_VALID_PARAMS;

            if (activeMode.equals("active"))
                filters.add(gt("start_at", System.currentTimeMillis()));
            else
                filters.add(lt("start_at", System.currentTimeMillis()));
        }

        if (justHasStudents != null)
            filters.add(exists("students.0"));

        if (justHasRequests != null)
            filters.add(exists("requests.0"));

        if (teachMode != null)
            filters.add(eq("teach_mode", teachMode));

        List<Document> schedules = teachScheduleRepository.find(
                filters.size() == 0 ? null : and(filters),
                SCHEDULE_DIGEST_FOR_TEACHER
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
            jsonArray.put(convertScheduleToJSONDigestForTeacher(
                    schedule, userId == null, users, false
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

        JSONObject jsonObject = convertScheduleToJSONDigestForTeacher(schedule, false, null, true);
        jsonObject.put("description", schedule.getOrDefault("description", ""));

        return generateSuccessMsg("data", jsonObject);
    }

    public static String getScheduleStudents(ObjectId userId, ObjectId scheduleId) {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (userId != null && !schedule.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        if (!schedule.containsKey("students"))
            return generateSuccessMsg("data", new JSONArray());

        List<Object> usersId = new ArrayList<>();

        for (Document student : schedule.getList("students", Document.class))
            usersId.add(student.getObjectId("_id"));

        List<Document> users = userRepository.findByIds(usersId, true, USER_PUBLIC_INFO);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        JSONArray students = new JSONArray();
        for (Document student : schedule.getList("students", Document.class)) {
            users.stream()
                    .filter(user -> user.getObjectId("_id").equals(student.getObjectId("_id")))
                    .findFirst().ifPresent(user -> {
                        JSONObject tmp = new JSONObject();
                        fillJSONWithUserPublicInfo(tmp, user);
                        students.put(tmp);
                    });
        }

        return generateSuccessMsg("data", students);
    }

}
