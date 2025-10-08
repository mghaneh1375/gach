package irysc.gachesefid.Service.advice;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Advisor.Utility.validateDay;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Service.advice.ScheduleUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

@Service
public class ScheduleService {

    public String notifyStudentForSchedule(ObjectId scheduleId,
                                           String advisorName,
                                           ObjectId advisorId
    ) {
        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.containsKey("advisors") ||
                !schedule.getList("advisors", ObjectId.class).contains(advisorId)
        )
            return JSON_NOT_ACCESS;

        schedule.put("ready_for_use", true);
        scheduleRepository.updateOne(scheduleId, set("ready_for_use", true));

        Document student = userRepository.findById(schedule.getObjectId("user_id"));
        if (student == null)
            return JSON_NOT_UNKNOWN;

        createNotifAndSendSMS(student, advisorName, "karbarg");
        userRepository.updateOne(
                student.getObjectId("_id"),
                set("events", student.get("events"))
        );
        return JSON_OK;
    }

    public File exportPDF(ObjectId id, ObjectId advisorId, ObjectId userId) {
        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return null;

        if (advisorId != null && !schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return null;

        if (userId != null && !schedule.getObjectId("user_id").equals(userId))
            return null;

        return PDFUtils.exportSchedule(
                schedule,
                userRepository.findById(schedule.getObjectId("user_id"))
        );
    }

    private void createNewScheduleFromExistProgram(
            ObjectId userId, ObjectId advisorId,
            String weekStartAt, HashMap<Integer, List<Document>> items
    ) {
        Document schedule = new Document("user_id", userId)
                .append("week_start_at", weekStartAt)
                .append("week_start_at_int", Utility.convertStringToDate(weekStartAt))
                .append("advisors", new ArrayList<>() {{
                    add(advisorId);
                }});

        List<Document> days = new ArrayList<>();

        for (Integer day : items.keySet())
            days.add(new Document("day", day).append("items", items.get(day)));

        schedule.append("days", days);
        scheduleRepository.insertOne(schedule);
    }

    private void mergeScheduleFromExistProgram(
            Document schedule, ObjectId advisorId,
            HashMap<Integer, List<Document>> items
    ) {
        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            schedule.getList("advisors", ObjectId.class).add(advisorId);

        List<Document> days = schedule.getList("days", Document.class);

        for (Integer day : items.keySet()) {

            Document dayDoc = searchInDocumentsKeyVal(days, "day", day);
            if (dayDoc == null)
                days.add(new Document("day", day).append("items", items.get(day)));
            else
                dayDoc.getList("items", Document.class).addAll(items.get(day));
        }

        schedule.put("days", days);
        scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
    }

    public String copy(
            ObjectId advisorId, ObjectId scheduleId,
            JSONArray users, int scheduleFor
    ) {
        if (scheduleFor > 4 || scheduleFor < 0)
            return JSON_NOT_VALID_PARAMS;

        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return JSON_NOT_ACCESS;

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();
        List<ObjectId> students = new ArrayList<>();

        for (int i = 0; i < users.length(); i++) {

            String id = users.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(users.getString(i));

            if (!Authorization.hasAccessToThisStudent(oId, advisorId)) {
                excepts.put(i + 1);
                continue;
            }

            students.add(oId);
            doneIds.put(id);
        }

        if (students.isEmpty())
            return JSON_NOT_VALID_PARAMS;

        long curr = System.currentTimeMillis();
        HashMap<Integer, List<Document>> items = new HashMap<>();

        for (Document day : schedule.getList("days", Document.class)) {

            List<Document> tmp = new ArrayList<>();

            for (Document item : day.getList("items", Document.class)) {

                if (!item.getObjectId("advisor_id").equals(advisorId))
                    continue;

                Document newDoc = Document.parse(item.toJson());
                newDoc.put("created_at", curr);
                newDoc.remove("done_duration");
                newDoc.remove("done_additional");

                tmp.add(newDoc);
            }

            items.put(day.getInteger("day"), tmp);
        }

        if (items.isEmpty())
            return JSON_NOT_ACCESS;

        String weekStartAt;
        if (scheduleFor == 0)
            weekStartAt = getFirstDayOfCurrWeek();
        else
            weekStartAt = getFirstDayOfFutureWeek(scheduleFor);

        for (ObjectId studentId : students) {
            Document sc = scheduleRepository.findOne(
                    and(
                            eq("user_id", studentId),
                            eq("week_start_at", weekStartAt)
                    ), null
            );

            if (sc == null)
                createNewScheduleFromExistProgram(studentId, advisorId, weekStartAt, items);
            else
                mergeScheduleFromExistProgram(sc, advisorId, items);
        }

        return returnAddResponse(excepts, doneIds);
    }

    public String setScheduleDesc(ObjectId advisorId, ObjectId id, String desc) {

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return JSON_NOT_ACCESS;

        List<Document> advisorsDesc = (List<Document>) schedule.getOrDefault("advisors_desc", new ArrayList<>());

        Document advisorDesc = searchInDocumentsKeyVal(advisorsDesc, "advisor_id", advisorId);

        if (advisorDesc == null)
            advisorsDesc.add(new Document("advisor_id", advisorId)
                    .append("description", desc));
        else
            advisorDesc.put("description", desc);

        if (!schedule.containsKey("advisors_desc"))
            schedule.put("advisors_desc", advisorsDesc);

        scheduleRepository.replaceOne(id, schedule);

        return JSON_OK;
    }

    public String addItemToSchedule(
            ObjectId advisorId,
            ObjectId userId,
            JSONObject data
    ) {
        String day = data.getString("day");
        int dayIndex;

        try {
            dayIndex = validateDay(day);
        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

        int duration = data.getInt("duration");
        if (duration < 15 || duration > 240)
            return generateErr("زمان هر برنامه باید بین 15 الی 240 دقیقه باشد");

        if (!Authorization.hasAccessToThisStudent(userId, advisorId))
            return JSON_NOT_ACCESS;

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = adviseTagRepository.findById(tagId);
        if (tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        if (tag.containsKey("number_label") && !data.has("additional"))
            return generateErr("لطفا " + tag.getString("number_label") + " را وارد نمایید");

        ObjectId lessonId = new ObjectId(data.getString("lessonId"));
        Document grade = gradeRepository.findOne(eq("lessons._id", lessonId), null);
        if (grade == null) {
            grade = branchRepository.findOne(eq("lessons._id", lessonId), null);
            if (grade == null)
                return JSON_NOT_VALID;
        }

        Document lesson = searchInDocumentsKeyVal(
                grade.getList("lessons", Document.class),
                "_id", lessonId
        );

        Document schedule;

        if (data.has("id")) {
            if (!ObjectId.isValid(data.getString("id")))
                return JSON_NOT_VALID_PARAMS;

            ObjectId oId = new ObjectId(data.getString("id"));
            schedule = scheduleRepository.findById(oId);

            if (schedule == null ||
                    !schedule.getObjectId("user_id").equals(userId)
            )
                return JSON_NOT_VALID_PARAMS;

            if (advisorId != null && schedule.containsKey("advisors") && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
                schedule.getList("advisors", ObjectId.class).add(advisorId);
            }
        } else {
            if (!data.has("scheduleFor"))
                return JSON_NOT_VALID_PARAMS;

            int scheduleFor = data.getInt("scheduleFor");

            if (scheduleFor < 0 || scheduleFor > 4)
                return JSON_NOT_VALID_PARAMS;

            String weekStartAt;

            if (scheduleFor == 0)
                weekStartAt = getFirstDayOfCurrWeek();
            else
                weekStartAt = getFirstDayOfFutureWeek(scheduleFor);

            schedule = scheduleRepository.findOne(
                    and(
                            eq("user_id", userId),
                            eq("week_start_at", weekStartAt)
                    ), null
            );
            if (schedule == null) {
                schedule = new Document("user_id", userId)
                        .append("week_start_at", weekStartAt)
                        .append("week_start_at_int", Utility.convertStringToDate(weekStartAt))
                        .append("days", new ArrayList<>())
                        .append("advisors", new ArrayList<>() {{
                            add(advisorId);
                        }});
            } else {
                schedule = scheduleRepository.findById(schedule.getObjectId("_id"));
                if (advisorId != null && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
                    schedule.getList("advisors", ObjectId.class).add(advisorId);
                }
            }
        }

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        List<Document> items = doc == null ? new ArrayList<>() :
                doc.getList("items", Document.class);

        ObjectId newId = new ObjectId();
        Document newDoc = new Document("_id", newId)
                .append("tag", tag.getString("label"))
                .append("advisor_id", advisorId)
                .append("created_at", System.currentTimeMillis())
                .append("lesson", lesson.getString("name"))
                .append("duration", data.getInt("duration"));

        if (data.has("startAt"))
            newDoc.put("start_at", data.getString("startAt"));

        if (data.has("description"))
            newDoc.put("description", data.getString("description"));

        if (tag.containsKey("number_label")) {
            newDoc.put("additional", data.getInt("additional"));
            newDoc.put("additional_label", tag.getString("number_label"));
        }

        items.add(newDoc);

        if (doc == null)
            days.add(new Document("day", dayIndex)
                    .append("items", items)
            );

        schedule.put("days", days);

        if (data.has("id"))
            scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
        else {
            ObjectId scheduleId = scheduleRepository.insertOneWithReturnId(schedule);
            return generateSuccessMsg("data", new JSONObject()
                    .put("scheduleId", scheduleId.toString())
                    .put("id", newId.toString())
            );
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("id", newId.toString())
        );
    }

    public String updateScheduleItem(ObjectId advisorId, ObjectId itemId, JSONObject data) {

        int duration = data.getInt("duration");
        if (duration < 15 || duration > 240)
            return generateErr("زمان هر برنامه باید بین 15 الی 240 دقیقه باشد");

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = adviseTagRepository.findById(tagId);
        if (tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        if (tag.containsKey("number_label") && !data.has("additional"))
            return generateErr("لطفا " + tag.getString("number_label") + " را وارد نمایید");

        Document schedule = scheduleRepository.findOne(eq("days.items._id", itemId), null);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        schedule = scheduleRepository.findById(schedule.getObjectId("_id"));

        Document item = null;

        for (Document day : schedule.getList("days", Document.class)) {
            item = searchInDocumentsKeyVal(day.getList("items", Document.class), "_id", itemId);
            if (item != null) break;
        }

        if (item == null)
            return JSON_NOT_UNKNOWN;

        if (!item.getObjectId("advisor_id").equals(advisorId))
            return JSON_NOT_ACCESS;

        item.put("tag", tag.getString("label"));
        item.put("duration", data.getInt("duration"));

        if (data.has("startAt"))
            item.put("start_at", data.getString("startAt"));

        if (data.has("description"))
            item.put("description", data.getString("description"));

        if (tag.containsKey("number_label")) {
            item.put("additional", data.getInt("additional"));
            item.put("additional_label", tag.getString("number_label"));
        }

        scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
        return JSON_OK;
    }

    public String getStudentSchedules(ObjectId advisorId,
                                      ObjectId studentId,
                                      Boolean notReturnPassed) {
        int today = getToday();
        List<Document> schedules = scheduleRepository.find(
                eq("user_id", studentId)
                , null, Sorts.descending("week_start_at_int")
        );

        JSONArray jsonArray = new JSONArray();
        for (Document schedule : schedules) {
            if (notReturnPassed != null) {
                int d = convertStringToDate(schedule.getString("week_start_at"));

                if (notReturnPassed && today - d > 7)
                    continue;

                if (!notReturnPassed && today - d < 7)
                    continue;
            }

            jsonArray.put(convertSchedulesToJSONObject(schedule, advisorId));
        }

        JSONObject jsonObject = new JSONObject()
                .put("items", jsonArray);

        if (advisorId != null) {
            Document user = userRepository.findById(studentId);
            if (user != null)
                jsonObject.put("student",
                        new JSONObject()
                                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                                .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                );
        }

        return generateSuccessMsg("data", jsonObject);
    }

    public String getStudentSchedulesDigest(ObjectId studentId) {

        List<Document> schedules = scheduleRepository.find(
                eq("user_id", studentId)
                , null, Sorts.descending("week_start_at_int")
        );

        JSONArray jsonArray = new JSONArray();

        for (Document schedule : schedules)
            jsonArray.put(new JSONObject()
                    .put("id", schedule.getObjectId("_id").toString())
                    .put("item", schedule.getString("week_start_at"))
            );

        return generateSuccessMsg("data", jsonArray);
    }


    public String lessonsInSchedule(ObjectId advisorId, ObjectId scheduleId, boolean isAdvisor) {

        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (isAdvisor &&
                !Authorization.hasAccessToThisStudent(schedule.getObjectId("user_id"), advisorId))
            return JSON_NOT_ACCESS;

        if (!isAdvisor && !schedule.getObjectId("user_id").equals(advisorId))
            return JSON_NOT_ACCESS;


        JSONArray jsonArray = new JSONArray();
        HashMap<String, ScheduleUtils.LessonStat> lessonStats = fetchLessonStats(schedule);

        for (String key : lessonStats.keySet()) {

            jsonArray.put(new JSONObject()
                    .put("lesson", key)
                    .put("stats", lessonStats.get(key).toJSON())
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public String removeSchedule(ObjectId advisorId, ObjectId id) {

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        List<Document> days = schedule.getList("days", Document.class);

        for (Document day : days) {

            if (!day.containsKey("items"))
                continue;

            for (Document item : day.getList("items", Document.class)) {
                if (!item.getObjectId("advisor_id").equals(advisorId))
                    return generateErr("شما تنها مشاور این کاربرگ نیستید و امکان حذف این کاربرگ برای شما وجود ندارد");
            }
        }

        scheduleRepository.deleteOne(schedule.getObjectId("_id"));

        return JSON_OK;
    }

    private static PairValue checkUpdatable(
            ObjectId advisorId, ObjectId userId,
            ObjectId id, boolean delete
    ) throws InvalidFieldsException {

        Document doc = scheduleRepository.findOne(
                and(
                        eq("user_id", userId),
                        eq("days.items._id", id)
                ), null
        );

        if (doc == null)
            throw new InvalidFieldsException("not access");

        String firstDayOfWeek = getFirstDayOfCurrWeek();

        if (!doc.getString("week_start_at").equals(firstDayOfWeek)) {
            int d = Utility.convertStringToDate(doc.getString("week_start_at"));
            int today = Utility.getToday();

            if (today > d)
                throw new InvalidFieldsException("زمان ویرایش/حذف به اتمام رسیده است");
        }

        List<Document> days = doc.getList("days", Document.class);
        for (Document day : days) {

            if (!day.containsKey("items"))
                continue;

            List<Document> items = day.getList("items", Document.class);
            int idx = searchInDocumentsKeyValIdx(
                    items, "_id", id
            );

            if (idx == -1)
                continue;

            if (!items.get(idx).getObjectId("advisor_id").equals(advisorId))
                throw new InvalidFieldsException("not access");

            if (delete)
                items.remove(idx);

            return new PairValue(doc, delete ? null : items.get(idx));
        }

        throw new InvalidFieldsException("unknown err");
    }

    public String removeItemFromSchedule(
            ObjectId advisorId,
            ObjectId userId,
            ObjectId id
    ) {
        try {
            PairValue p = checkUpdatable(advisorId, userId, id, true);
            Document doc = (Document) p.getKey();

            scheduleRepository.replaceOne(
                    doc.getObjectId("_id"), doc
            );

            return JSON_OK;
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public String progress(ObjectId userId, ObjectId lessonId, Long start, Long end) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("user_id", userId));

        if (start != null) {
            filters.add(gte("week_start_at_int", getFormattedDate(start)));
        }

        if (end != null) {
            filters.add(lte("week_start_at_int", getFormattedDate(end)));
        }

        List<Document> schedules = scheduleRepository.find(and(filters), null);
        List<ScheduleUtils.WeeklyStat> weeklyStats = new ArrayList<>();

        List<Integer> dailyTotalSum = new ArrayList<>();
        List<Integer> dailyDoneSum = new ArrayList<>();
        List<String> daily = new ArrayList<>();

        schedules.sort(Comparator.comparing(o -> o.getString("week_start_at")));

        for (Document schedule : schedules) {

            String weekStartAt = schedule.getString("week_start_at");

            weeklyStats.add(
                    new ScheduleUtils.WeeklyStat(weekStartAt,
                            fetchLessonStats(schedule)
                    ));

            String[] splited = weekStartAt.split("\\/");

            JalaliCalendar jalaliCalendar = new JalaliCalendar(
                    Integer.parseInt(splited[0]), Integer.parseInt(splited[1]), Integer.parseInt(splited[2])
            );

            int added = 0;
            List<Document> days = schedule.getList("days", Document.class);

            for (int k = days.size() - 1; k >= 0; k--) {

                if (daily.size() >= 14)
                    break;

                Document day = days.get(k);

                if (day.getInteger("day").equals(0))
                    daily.add(weekStartAt);
                else {
                    added = day.getInteger("day") - added;
                    jalaliCalendar.add(Calendar.DAY_OF_MONTH, added);
                    daily.add(jalaliCalendar.get(Calendar.YEAR) + "/" + jalaliCalendar.get(Calendar.MONTH) + "/" + jalaliCalendar.get(Calendar.DAY_OF_MONTH));
                }

                int totalSum = 0;
                int doneSum = 0;

                for (Document item : day.getList("items", Document.class)) {

                    doneSum += (int) item.getOrDefault("done_duration", 0);
                    totalSum += (int) item.getOrDefault("duration", 0);

                }

                dailyDoneSum.add(doneSum);
                dailyTotalSum.add(totalSum);

            }

        }

        weeklyStats.sort(Comparator.comparing(o -> o.weekStartAt));

        List<String> allLessons = new ArrayList<>();
        List<String> allTags = new ArrayList<>();
        HashMap<String, List<String>> allLessonTags = new HashMap<>();

        for (ScheduleUtils.WeeklyStat weeklyStat : weeklyStats) {

            HashMap<String, ScheduleUtils.LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            for (String key : lessonStatHashMap.keySet()) {

                if (!allLessons.contains(key))
                    allLessons.add(key);

                List<String> tmp = allLessonTags.containsKey(key) ? allLessonTags.get(key) : new ArrayList<>();

                for (String t : lessonStatHashMap.get(key).tagStats.keySet()) {
                    if (!tmp.contains(t))
                        tmp.add(t);

                    if (!allTags.contains(t))
                        allTags.add(t);
                }

                if (!allLessonTags.containsKey(key))
                    allLessonTags.put(key, tmp);
            }

        }

        for (ScheduleUtils.WeeklyStat weeklyStat : weeklyStats) {
            HashMap<String, ScheduleUtils.LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            for (String key : allLessons) {
                List<String> lessonTags = allLessonTags.get(key);

                if (lessonStatHashMap.containsKey(key)) {

                    ScheduleUtils.LessonStat lessonStat = lessonStatHashMap.get(key);
                    for (String tag : lessonTags) {
                        if (!lessonStat.tagStats.containsKey(tag))
                            lessonStat.tagStats.put(tag, null);
                    }

                } else {
                    ScheduleUtils.LessonStat lessonStat = new ScheduleUtils.LessonStat(0, 0);

                    for (String tag : lessonTags)
                        lessonStat.tagStats.put(tag, null);

                    lessonStatHashMap.put(key, lessonStat);
                }
            }
        }


        HashMap<String, List<Integer>> doneInEachLessonsWeekly = new HashMap<>();
        HashMap<String, List<Integer>> totalInEachLessonsWeekly = new HashMap<>();

        HashMap<String, List<Integer>> doneTagsGeneralStats = new HashMap<>();
        HashMap<String, List<Integer>> totalTagsGeneralStats = new HashMap<>();

        HashMap<String, List<Integer>> doneAdditionalTagsGeneralStats = new HashMap<>();
        HashMap<String, List<Integer>> totalAdditionalTagsGeneralStats = new HashMap<>();

        HashMap<String, HashMap<String, List<Integer>>> doneTagInEachLessonsWeekly = new HashMap<>();
        HashMap<String, HashMap<String, List<Integer>>> totalTagInEachLessonsWeekly = new HashMap<>();

        ArrayList<Integer> sumTotals = new ArrayList<>();
        ArrayList<Integer> sumDones = new ArrayList<>();
        ArrayList<String> weeks = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();

        for (ScheduleUtils.WeeklyStat weeklyStat : weeklyStats) {

            HashMap<String, Integer> doneTagsSum = new HashMap<>();
            HashMap<String, Integer> totalTagsSum = new HashMap<>();

            HashMap<String, Integer> doneAdditionalTagsSum = new HashMap<>();
            HashMap<String, Integer> totalAdditionalTagsSum = new HashMap<>();

            for (String tag : allTags) {
                doneTagsSum.put(tag, 0);
                totalTagsSum.put(tag, 0);
                doneAdditionalTagsSum.put(tag, 0);
                totalAdditionalTagsSum.put(tag, 0);
            }

            weeks.add(weeklyStat.weekStartAt);
            HashMap<String, ScheduleUtils.LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            int sumTotal = 0;
            int sumDone = 0;

            for (String lesson : lessonStatHashMap.keySet()) {

                ScheduleUtils.LessonStat lessonStat = lessonStatHashMap.get(lesson);

                List<Integer> list = doneInEachLessonsWeekly.containsKey(lesson) ?
                        doneInEachLessonsWeekly.get(lesson) : new ArrayList<>();

                List<Integer> totalList = totalInEachLessonsWeekly.containsKey(lesson) ?
                        totalInEachLessonsWeekly.get(lesson) : new ArrayList<>();

                list.add(lessonStat == null ? 0 : lessonStat.done);
                totalList.add(lessonStat == null ? 0 : lessonStat.total);

                sumTotal += lessonStat == null ? 0 : lessonStat.total;
                sumDone += lessonStat == null ? 0 : lessonStat.done;

                HashMap<String, List<Integer>> tmp;
                HashMap<String, List<Integer>> tmpTotal;

                if (!doneInEachLessonsWeekly.containsKey(lesson)) {

                    doneInEachLessonsWeekly.put(lesson, list);
                    totalInEachLessonsWeekly.put(lesson, totalList);

                    tmp = new HashMap<>();
                    tmpTotal = new HashMap<>();
                } else {
                    tmp = doneTagInEachLessonsWeekly.get(lesson);
                    tmpTotal = totalTagInEachLessonsWeekly.get(lesson);
                }

                for (String tag : lessonStat.tagStats.keySet()) {

                    List<Integer> tagList = tmp.containsKey(tag) ?
                            tmp.get(tag) : new ArrayList<>();

                    List<Integer> totalTagList = tmpTotal.containsKey(tag) ?
                            tmpTotal.get(tag) : new ArrayList<>();

                    ScheduleUtils.TagStat tagStat = lessonStat.tagStats.get(tag);

                    int tagDone = tagStat == null ? 0 : tagStat.done;
                    int tagTotal = tagStat == null ? 0 : tagStat.total;

                    tagList.add(tagDone);
                    totalTagList.add(tagTotal);

                    if (tagTotal > 0) {

                        if (doneTagsSum.containsKey(tag)) {

                            doneTagsSum.put(tag, doneTagsSum.get(tag) + tagDone);
                            totalTagsSum.put(tag, totalTagsSum.get(tag) + tagTotal);

                            if (tagStat != null && tagStat.additionalTotal > 0) {
                                doneAdditionalTagsSum.put(tag, doneAdditionalTagsSum.get(tag) + tagStat.additionalDone);
                                totalAdditionalTagsSum.put(tag, totalAdditionalTagsSum.get(tag) + tagStat.additionalTotal);
                            }

                        } else {

                            doneTagsSum.put(tag, tagDone);
                            totalTagsSum.put(tag, tagTotal);

                            doneAdditionalTagsSum.put(tag, tagStat == null ? 0 : tagStat.additionalDone);
                            totalAdditionalTagsSum.put(tag, tagStat == null ? 0 : tagStat.additionalTotal);

                        }
                    }

                    if (!tmp.containsKey(tag)) {
                        tmp.put(tag, tagList);
                        tmpTotal.put(tag, totalTagList);
                    }
                }

                if (!doneTagInEachLessonsWeekly.containsKey(lesson)) {
                    doneTagInEachLessonsWeekly.put(lesson, tmp);
                    totalTagInEachLessonsWeekly.put(lesson, tmpTotal);
                }

            }

            sumTotals.add(sumTotal);
            sumDones.add(sumDone);

            for (String tag : totalTagsSum.keySet()) {

                List<Integer> doneSum = doneTagsGeneralStats.containsKey(tag) ?
                        doneTagsGeneralStats.get(tag) : new ArrayList<>();

                List<Integer> totalSum = totalTagsGeneralStats.containsKey(tag) ?
                        totalTagsGeneralStats.get(tag) : new ArrayList<>();

                doneSum.add(doneTagsSum.get(tag));
                totalSum.add(totalTagsSum.get(tag));

                if (!doneTagsGeneralStats.containsKey(tag)) {
                    doneTagsGeneralStats.put(tag, doneSum);
                    totalTagsGeneralStats.put(tag, totalSum);
                }

                List<Integer> doneAdditionalSum = doneAdditionalTagsGeneralStats.containsKey(tag) ?
                        doneAdditionalTagsGeneralStats.get(tag) : new ArrayList<>();

                List<Integer> totalAdditionalSum = totalAdditionalTagsGeneralStats.containsKey(tag) ?
                        totalAdditionalTagsGeneralStats.get(tag) : new ArrayList<>();

                doneAdditionalSum.add(doneAdditionalTagsSum.get(tag));
                totalAdditionalSum.add(totalAdditionalTagsSum.get(tag));

                if (!doneAdditionalTagsGeneralStats.containsKey(tag)) {
                    doneAdditionalTagsGeneralStats.put(tag, doneAdditionalSum);
                    totalAdditionalTagsGeneralStats.put(tag, totalAdditionalSum);
                }
            }

        }

        for (String key : doneInEachLessonsWeekly.keySet()) {

            JSONObject jsonObject = new JSONObject()
                    .put("lesson", key)
                    .put("doneStats", doneInEachLessonsWeekly.get(key))
                    .put("totalStats", totalInEachLessonsWeekly.get(key));

            if (doneTagInEachLessonsWeekly.containsKey(key)) {

                HashMap<String, List<Integer>> tmp = doneTagInEachLessonsWeekly.get(key);
                HashMap<String, List<Integer>> tmpTotal = totalTagInEachLessonsWeekly.get(key);

                JSONArray jsonArray1 = new JSONArray();

                for (String tag : tmp.keySet()) {
                    jsonArray1.put(new JSONObject()
                            .put("tag", tag)
                            .put("done", tmp.get(tag))
                            .put("total", tmpTotal.get(tag))
                    );
                }

                jsonObject.put("tags", jsonArray1);

            }

            jsonArray.put(jsonObject);
        }

        JSONArray tagsGeneralStats = new JSONArray();

        for (String tag : doneTagsGeneralStats.keySet()) {

            tagsGeneralStats.put(new JSONObject()
                    .put("tag", tag)
                    .put("done", doneTagsGeneralStats.get(tag))
                    .put("total", totalTagsGeneralStats.get(tag))
            );

        }

        JSONArray additionalTagsGeneralStats = new JSONArray();

        for (String tag : doneAdditionalTagsGeneralStats.keySet()) {

            boolean findNonZero = false;

            for (Integer i : totalAdditionalTagsGeneralStats.get(tag)) {
                if (i > 0) {
                    findNonZero = true;
                    break;
                }
            }

            if (!findNonZero)
                continue;

            additionalTagsGeneralStats.put(new JSONObject()
                    .put("tag", tag)
                    .put("done", doneAdditionalTagsGeneralStats.get(tag))
                    .put("total", totalAdditionalTagsGeneralStats.get(tag))
            );

        }


        return generateSuccessMsg("data", new JSONObject()
                .put("stats", jsonArray)
                .put("generalStats", new JSONObject()
                        .put("doneStats", sumDones)
                        .put("totalStats", sumTotals))
                .put("tagsGeneralStats", tagsGeneralStats)
                .put("additionalTagsGeneralStats", additionalTagsGeneralStats)
                .put("daily", new JSONObject()
                        .put("done", dailyDoneSum)
                        .put("total", dailyTotalSum)
                        .put("labels", daily)
                )
                .put("weeks", weeks)
        );
    }
}
