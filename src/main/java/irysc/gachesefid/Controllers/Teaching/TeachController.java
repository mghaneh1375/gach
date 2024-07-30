package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Models.ActiveMode;
import irysc.gachesefid.Models.SettledStatus;
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

    public static String getTeachReports(
            Long from, Long to, Boolean showJustUnSeen,
            ObjectId teacherId, Boolean justSendFromStudent,
            Boolean justSendFromTeacher, ObjectId teachId
    ) {
        List<Bson> filters = new ArrayList<>();

        if (teachId != null)
            filters.add(eq("schedule_id", teachId));

        if (showJustUnSeen != null && showJustUnSeen)
            filters.add(eq("seen", false));

        if (justSendFromStudent != null && justSendFromStudent)
            filters.add(eq("send_from", "student"));

        if (justSendFromTeacher != null && justSendFromTeacher)
            filters.add(eq("send_from", "teacher"));

        if (teacherId != null)
            filters.add(eq("teacher_id", teacherId));

        if (from != null)
            filters.add(gte("created_at", from));

        if (to != null)
            filters.add(lte("created_at", to));

        List<Document> reports =
                teachReportRepository.find(filters.size() == 0 ? null : and(filters), null);

        Set<ObjectId> userIds = new HashSet<>();
        Set<ObjectId> scheduleIds = new HashSet<>();

        for (Document report : reports) {
            userIds.add(report.getObjectId("student_id"));
            userIds.add(report.getObjectId("teacher_id"));
            scheduleIds.add(report.getObjectId("schedule_id"));
        }

        List<Document> users = userRepository.findByIds(new ArrayList<>(userIds), false, JUST_NAME);
        if (users == null)
            return generateErr(JSON_NOT_UNKNOWN);

        List<Document> schedules = teachScheduleRepository.findByIds(new ArrayList<>(scheduleIds), false, new BasicDBObject("start_at", 1));
        if (schedules == null)
            return generateErr(JSON_NOT_UNKNOWN);

        List<Document> tags = teachTagReportRepository.find(null, new BasicDBObject("label", 1));
        HashMap<ObjectId, String> tagsHashMap = new HashMap<>();
        for (Document tag : tags)
            tagsHashMap.put(tag.getObjectId("_id"), tag.getString("label"));

        JSONArray jsonArray = new JSONArray();
        for (Document report : reports) {

            List<String> reportTags = null;
            if (report.containsKey("tag_ids")) {
                reportTags = report.getList("tag_ids", ObjectId.class)
                        .stream().map(tagsHashMap::get)
                        .collect(Collectors.toList());
            }

            jsonArray.put(
                    new JSONObject()
                            .put("id", report.getObjectId("_id").toString())
                            .put("student", users
                                    .stream()
                                    .filter(user -> user.getObjectId("_id").equals(report.getObjectId("student_id")))
                                    .map(user -> user.getString("first_name") + " " + user.getString("last_name"))
                                    .findFirst().get()
                            )
                            .put("teacher", users
                                    .stream()
                                    .filter(user -> user.getObjectId("_id").equals(report.getObjectId("teacher_id")))
                                    .map(user -> user.getString("first_name") + " " + user.getString("last_name"))
                                    .findFirst().get()
                            )
                            .put("startAt", schedules
                                    .stream()
                                    .filter(schedule -> schedule.getObjectId("_id").equals(report.getObjectId("schedule_id")))
                                    .map(schedule -> getSolarDate(schedule.getLong("start_at")))
                                    .findFirst().get()
                            )
                            .put("tags", reportTags)
                            .put("desc", report.getOrDefault("desc", ""))
                            .put("seen", report.getBoolean("seen"))
                            .put("sendFrom", report.getString("send_from"))
                            .put("createdAt", getSolarDate(report.getLong("created_at")))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String setTeachReportAsSeen(ObjectId id) {
        teachReportRepository.updateOne(eq("_id", id), set("seen", true));
        return JSON_OK;
    }

    public static String getAllTeachersDigest() {

        JSONArray jsonArray = new JSONArray();
        userRepository.find(and(
                        exists("teach", true),
                        eq("teach", true)
                ), JUST_NAME)
                .forEach(document ->
                        jsonArray.put(
                                new JSONObject().put("id", document.getObjectId("_id").toString())
                                        .put("name", document.getString("first_name") + " " + document.getString("last_name"))
                        )
                );

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getTransactions(
            ObjectId teacherId,
            Long from, Long to,
            Boolean justSettlements
    ) {
        List<Bson> filters = new ArrayList<>() {{
            add(exists("students.0"));
            add(lt("start_at", System.currentTimeMillis()));
        }};

        if(teacherId != null)
            filters.add(eq("user_id", teacherId));

        if (from != null)
            filters.add(gte("start_at", from));

        if (to != null)
            filters.add(lte("start_at", to));

        if (justSettlements != null)
            filters.add(exists("settled_at", justSettlements));

        List<Document> schedules = teachScheduleRepository.find(and(filters),
                new BasicDBObject("settled_at", 1).append("start_at", 1)
                        .append("price", 1).append("students", 1)
                        .append("teach_mode", 1).append("created_at", 1)
                        .append("title", 1).append("irysc_percent", 1)
                        .append("length", 1).append("user_id", 1)
        );

        JSONArray jsonArray = new JSONArray();
        double totalPrice = 0;
        double totalSettled = 0;

        Set<ObjectId> userIds = new HashSet<>();
        for(Document schedule : schedules)
            userIds.add(schedule.getObjectId("user_id"));

        List<Document> users = userRepository.findByIds(new ArrayList<>(userIds), false, JUST_NAME);
        if(users == null) return JSON_NOT_UNKNOWN;

        for (Document schedule : schedules) {

            String teacher = users
                    .stream()
                    .filter(e -> e.getObjectId("_id").equals(schedule.getObjectId("user_id")))
                    .findFirst().map(e -> e.getString("first_name") + " " + e.getString("last_name"))
                    .get();

            int studentsCount = schedule.getList("students", Document.class).size();

            double p =
                    (
                            studentsCount * schedule.getInteger("price") *
                                    (100.0 - schedule.getInteger("irysc_percent"))
                    ) / 100.0;

            totalPrice += p;
            if (schedule.containsKey("settled_at"))
                totalSettled += p;

            JSONObject jsonObject = new JSONObject()
                    .put("username", teacher)
                    .put("teachMode", schedule.getString("teach_mode"))
                    .put("title", schedule.getOrDefault("title", ""))
                    .put("price", schedule.getInteger("price"))
                    .put("teacherShare", p)
                    .put("iryscPercent", schedule.getInteger("irysc_percent"))
                    .put("length", schedule.getInteger("length"))
                    .put("studentsCount", studentsCount)
                    .put("startAt", getSolarDate(schedule.getLong("start_at")))
                    .put("createdAt", getSolarDate(schedule.getLong("created_at")));

            if (schedule.containsKey("settled_at"))
                jsonObject
                        .put("settledAt", getSolarDate(schedule.getLong("settled_at")));

            jsonArray.put(jsonObject);
        }

        JSONObject result = new JSONObject()
                .put("data", jsonArray)
                .put("totalPrice", totalPrice)
                .put("totalSettled", totalSettled);

        return generateSuccessMsg("data", result);
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

        int iryscTeachPercent;
        if (user.containsKey("irysc_teach_percent"))
            iryscTeachPercent = user.getInteger("irysc_teach_percent");
        else {
            Document config = getConfig();
            iryscTeachPercent = config.getInteger("irysc_teach_percent");
        }

        ObjectId userId = user.getObjectId("_id");
        Document newDoc = new Document("_id", new ObjectId())
                .append("user_id", userId)
                .append("start_at", jsonObject.getLong("start"))
                .append("length", jsonObject.getInt("length"))
                .append("created_at", System.currentTimeMillis())
                .append("irysc_percent", iryscTeachPercent)
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
        if (schedule == null || !schedule.getObjectId("user_id").equals(userId))
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
            if (users == null)
                return JSON_NOT_UNKNOWN;

            List<Integer> studentsSkyRoomId = new ArrayList<>();
            for (Document user : users) {
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
            for (Document user : users) {
                createNotifAndSendSMS(user, url, "createTeachRoom");
                writeModels.add(new UpdateOneModel<>(
                        eq("_id", user.getObjectId("_id")),
                        set("events", user.get("events"))
                ));
            }

            userRepository.bulkWrite(writeModels);
            for (Document user : users)
                userRepository.clearFromCache(user.getObjectId("_id"));

            Document document = new Document("advisor_id", userId)
                    .append("created_at", System.currentTimeMillis())
                    .append("room_id", roomId)
                    .append("url", url)
                    .append("advisor_sky_id", teacherSkyRoomId);

            advisorMeetingRepository.insertOne(document);
            return generateSuccessMsg("data", url);
        } catch (Exception x) {
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

            users = userRepository.findByIds(userIds, true, JUST_NAME);
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

        List<Document> users = userRepository.findByIds(usersId, false, USER_PUBLIC_INFO);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        JSONArray students = new JSONArray();
        for (Document student : schedule.getList("students", Document.class)) {
            users.stream()
                    .filter(user -> user.getObjectId("_id").equals(student.getObjectId("_id")))
                    .findFirst().ifPresent(user -> {
                        JSONObject tmp = new JSONObject()
                                .put("createdAt", student.containsKey("created_at") ? getSolarDate(student.getLong("created_at")) : "");
                        if (userId == null) {
                            tmp
                                    .put("rate", student.getOrDefault("rate", 0))
                                    .put("rateAt", student.containsKey("rate_at") ? getSolarDate(student.getLong("rate_at")) : "");
                        }
                        fillJSONWithUserPublicInfo(tmp, user);
                        students.put(tmp);
                    });
        }

        return generateSuccessMsg("data", students);
    }

    public static String setTeachScheduleReportProblems(
            final ObjectId userId, final ObjectId scheduleId,
            final JSONArray tagIds, final String desc
    ) {

        List<Object> tagOIdsList = null;

        if (tagIds != null && tagIds.length() > 0) {
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

        if (tagOIdsList == null)
            tagOIdsList = new ArrayList<>();

        Document myTeachReport = teachReportRepository.findOne(
                and(
                        eq("send_from", "teacher"),
                        eq("teacher_id", userId),
                        eq("schedule_id", scheduleId)
                ), null
        );

        boolean isFirstReport = false;

        if (myTeachReport == null) {
            isFirstReport = true;
            Document schedule = teachScheduleRepository.findById(scheduleId);
            long curr = System.currentTimeMillis();
            if (!schedule.getObjectId("user_id").equals(userId) ||
                    schedule.getLong("start_at") > curr ||
                    schedule.getLong("start_at") + 30 * ONE_DAY_MIL_SEC < curr
            )
                return JSON_NOT_ACCESS;

            myTeachReport = new Document("student_id", userId)
                    .append("send_from", "teacher")
                    .append("seen", false)
                    .append("schedule_id", scheduleId)
                    .append("teacher_id", schedule.getObjectId("user_id"))
                    .append("created_at", curr);
        } else if (desc == null)
            myTeachReport.remove("desc");

        myTeachReport.put("tag_ids", tagOIdsList);
        if (desc != null)
            myTeachReport.put("desc", desc);

        if (isFirstReport)
            teachReportRepository.insertOne(myTeachReport);
        else
            teachReportRepository.replaceOne(
                    myTeachReport.getObjectId("_id"),
                    myTeachReport
            );

        return JSON_OK;
    }

    private static Double canRequestSettlement(
            Integer minAmountForSettlement, ObjectId userId
    ) {
        List<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", userId));
            add(exists("students.0"));
            add(exists("settled_at", false));
            add(lt("start_at", System.currentTimeMillis()));
        }};

        List<Document> schedules = teachScheduleRepository.find(and(filters),
                new BasicDBObject("price", 1).append("irysc_percent", 1)
                        .append("students", 1)
        );

        double total = 0;

        for (Document schedule : schedules) {
            int studentsCount = schedule.getList("students", Document.class).size();

            total +=
                    (
                            studentsCount * schedule.getInteger("price") *
                                    (100.0 - schedule.getInteger("irysc_percent"))
                    ) / 100.0;
        }

        return total > 0 && total >= minAmountForSettlement ? total : 0;
    }

    public static String getMyTransactions(
            ObjectId userId,
            Long from, Long to,
            Boolean justSettlements,
            boolean needCanRequestSettlement
    ) {

        List<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", userId));
            add(exists("students.0"));
            add(lt("start_at", System.currentTimeMillis()));
        }};

        if (from != null)
            filters.add(gte("start_at", from));

        if (to != null)
            filters.add(lte("start_at", to));

        if (justSettlements != null)
            filters.add(exists("settled_at", justSettlements));

        List<Document> schedules = teachScheduleRepository.find(and(filters),
                new BasicDBObject("settled_at", 1).append("start_at", 1)
                        .append("price", 1).append("students", 1)
                        .append("teach_mode", 1).append("created_at", 1)
                        .append("title", 1).append("irysc_percent", 1)
                        .append("length", 1)
        );

        JSONArray jsonArray = new JSONArray();
        double totalPrice = 0;
        double totalSettled = 0;

        for (Document schedule : schedules) {
            int studentsCount = schedule.getList("students", Document.class).size();

            double p =
                    (
                            studentsCount * schedule.getInteger("price") *
                                    (100.0 - schedule.getInteger("irysc_percent"))
                    ) / 100.0;

            totalPrice += p;
            if (schedule.containsKey("settled_at"))
                totalSettled += p;

            JSONObject jsonObject = new JSONObject()
                    .put("teachMode", schedule.getString("teach_mode"))
                    .put("title", schedule.getOrDefault("title", ""))
                    .put("price", schedule.getInteger("price"))
                    .put("teacherShare", p)
                    .put("iryscPercent", schedule.getInteger("irysc_percent"))
                    .put("length", schedule.getInteger("length"))
                    .put("studentsCount", studentsCount)
                    .put("startAt", getSolarDate(schedule.getLong("start_at")))
                    .put("createdAt", getSolarDate(schedule.getLong("created_at")));

            if (schedule.containsKey("settled_at"))
                jsonObject
                        .put("settledAt", getSolarDate(schedule.getLong("settled_at")));

            jsonArray.put(jsonObject);
        }

        JSONObject result = new JSONObject()
                .put("data", jsonArray)
                .put("totalPrice", totalPrice)
                .put("totalSettled", totalSettled);

        if (needCanRequestSettlement) {

            if (settlementRequestRepository.exist(
                    and(
                            eq("user_id", userId),
                            eq("status", "pending")
                    )
            ))
                result.put("canRequestSettlement", false);
            else {
                Document config = getConfig();
                Integer minAmountForSettlement = (Integer) config.getOrDefault("min_amount_for_settlement", 0);
                boolean canRequestSettlement;

                if (from == null && to == null &&
                        (justSettlements == null || !justSettlements)
                ) {
                    canRequestSettlement = totalPrice - totalSettled > 0 &&
                            totalPrice - totalSettled >= minAmountForSettlement;
                    result.put("canRequestSettlement", canRequestSettlement);
                    if (canRequestSettlement)
                        result.put("settlementAmount", totalPrice);
                } else {
                    canRequestSettlement = canRequestSettlement(
                            minAmountForSettlement, userId
                    ) > 0;
                    result.put("canRequestSettlement", canRequestSettlement);
                    if (canRequestSettlement)
                        result.put("settlementAmount", totalPrice);
                }
            }
        }

        return generateSuccessMsg("data", result);
    }

    public static String settlementRequest(ObjectId userId) {

        if (settlementRequestRepository.exist(
                and(
                        eq("user_id", userId),
                        eq("status", "pending")
                )
        ))
            return JSON_NOT_ACCESS;

        Document config = getConfig();
        Integer minAmountForSettlement = (Integer) config.getOrDefault("min_amount_for_settlement", 0);
        double amount = canRequestSettlement(minAmountForSettlement, userId);

        if (amount == 0)
            return JSON_NOT_ACCESS;

        settlementRequestRepository.insertOne(
                new Document("section", "class")
                        .append("user_id", userId)
                        .append("created_at", System.currentTimeMillis())
                        .append("status", "pending")
                        .append("amount", amount)
        );

        return JSON_OK;
    }

    public static String cancelSettlementRequest(ObjectId userId) {
        settlementRequestRepository.deleteOne(and(
                eq("user_id", userId),
                eq("status", "pending")
        ));
        return JSON_OK;
    }

    public static String getMySettledRequests(
            ObjectId userId, String status,
            Long createdFrom, Long createdTo,
            Long answerFrom, Long answerTo
    ) {
        List<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", userId));
        }};

        if (status != null) {
            if (!EnumValidatorImp.isValid(status, SettledStatus.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("status", status));
        }

        if (createdFrom != null)
            filters.add(gte("created_at", createdFrom));

        if (createdTo != null)
            filters.add(lte("created_at", createdTo));

        if (answerFrom != null || answerTo != null)
            filters.add(exists("answer_at"));

        if (answerFrom != null)
            filters.add(gte("answer_at", answerFrom));

        if (answerTo != null)
            filters.add(lte("answer_at", answerTo));

        List<Document> requests = settlementRequestRepository.find(
                and(filters), null
        );

        JSONArray jsonArray = new JSONArray();
        for (Document request : requests) {
            jsonArray.put(new JSONObject()
                    .put("id", request.getObjectId("_id").toString())
                    .put("status", request.getString("status"))
                    .put("amount", request.get("amount"))
                    .put("desc", request.getOrDefault("desc", ""))
                    .put("createdAt", getSolarDate(request.getLong("created_at")))
                    .put("answerAt", request.containsKey("answer_at") ? getSolarDate(request.getLong("answer_at")) : "")
                    .put("paidAt", request.containsKey("paid_at") ? getSolarDate(request.getLong("paid_at")) : ""));
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
