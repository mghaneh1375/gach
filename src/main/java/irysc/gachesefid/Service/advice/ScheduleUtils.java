package irysc.gachesefid.Service.advice;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class ScheduleUtils {

    static class TagStat {

        int total;
        int done;
        int additionalTotal;
        int additionalDone;
        String additionalLabel;

        public TagStat(int total, int done, int additionalTotal, int additionalDone,
                       String additionalLabel) {
            this.total = total;
            this.done = done;
            this.additionalTotal = additionalTotal;
            this.additionalDone = additionalDone;
            this.additionalLabel = additionalLabel;
        }

        public JSONObject toJSON() {

            JSONObject jsonObject = new JSONObject()
                    .put("total", total)
                    .put("done", done);

            if (!additionalLabel.isEmpty()) {
                jsonObject.put("additionalLabel", additionalLabel)
                        .put("additionalTotal", additionalTotal)
                        .put("additionalDone", additionalDone);
            }

            return jsonObject;
        }

    }

    static class LessonStat {

        int total;
        int done;
        HashMap<String, TagStat> tagStats;

        public LessonStat(int total, int done) {
            this.total = total;
            this.done = done;
            tagStats = new HashMap<>();
        }

        public JSONObject toJSON() {

            JSONObject jsonObject = new JSONObject()
                    .put("total", total)
                    .put("done", done);

            JSONArray jsonArray = new JSONArray();
            for (String key : tagStats.keySet())
                jsonArray.put(new JSONObject()
                        .put("tag", key)
                        .put("stats", tagStats.get(key).toJSON())
                );

            jsonObject.put("tags", jsonArray);

            return jsonObject;
        }
    }

    static class WeeklyStat {

        HashMap<String, LessonStat> lessonStats;
        String weekStartAt;

        public WeeklyStat(String weekStartAt, HashMap<String, LessonStat> lessonStatHashMap) {
            this.weekStartAt = weekStartAt;
            lessonStats = lessonStatHashMap;
        }
    }

    static JSONObject convertSchedulesToJSONObject(Document doc, ObjectId advisorId) {

        JSONObject jsonObject = new JSONObject();

        List<Document> days = doc.getList("days", Document.class);
        int schedulesSum = 0;
        int doneSum = 0;
        HashMap<ObjectId, String> advisors = new HashMap<>();

        for (Document day : days) {

            if(!day.containsKey("items"))
                continue;

            for(Document item : day.getList("items", Document.class)) {

                schedulesSum += item.getInteger("duration");
                doneSum += (int)item.getOrDefault("done_duration", 0);

                if(advisors.containsKey(item.getObjectId("advisor_id")))
                    continue;

                Document advisor = userRepository.findById(item.getObjectId("advisor_id"));
                if(advisor == null)
                    continue;

                advisors.put(item.getObjectId("advisor_id"),
                        advisor.getString("first_name") + " " + advisor.getString("last_name")
                );
            }
        }

        JSONArray advisorsJSON = new JSONArray();
        boolean canDeleteSchedule = false;

        for(ObjectId oId : advisors.keySet()) {
            advisorsJSON.put(advisors.get(oId));

            if(advisorId != null && advisors.keySet().size() == 1 && oId.equals(advisorId))
                canDeleteSchedule = true;
        }

        jsonObject.put("weekStartAt", doc.getString("week_start_at"))
                .put("schedulesSum", schedulesSum).put("doneSum", doneSum)
                .put("canDelete", canDeleteSchedule).put("advisors", advisorsJSON)
                .put("id", doc.getObjectId("_id").toString());

        return jsonObject;
    }

    static HashMap<String, LessonStat> fetchLessonStats(Document schedule) {

        HashMap<String, LessonStat> lessonStats = new HashMap<>();

        for (Document day : schedule.getList("days", Document.class)) {

            for (Document item : day.getList("items", Document.class)) {

                if (!item.containsKey("lesson")) continue;

                String lesson = item.getString("lesson");
                String tag = item.getString("tag");

                if (lessonStats.containsKey(lesson)) {

                    LessonStat lessonStat = lessonStats.get(lesson);

                    lessonStat.total += item.getInteger("duration");
                    lessonStat.done += (int) item.getOrDefault("done_duration", 0);

                    if (lessonStat.tagStats.containsKey(tag)) {
                        TagStat tagStat = lessonStat.tagStats.get(tag);
                        tagStat.total += item.getInteger("duration");
                        tagStat.done += (int) item.getOrDefault("done_duration", 0);
                        if (item.containsKey("additional")) {
                            tagStat.additionalTotal += item.getInteger("additional");
                            tagStat.additionalDone += (int) item.getOrDefault("done_additional", 0);
                        }
                    } else {
                        lessonStat.tagStats.put(tag, new TagStat(
                                item.getInteger("duration"),
                                (int) item.getOrDefault("done_duration", 0),
                                (int) item.getOrDefault("additional", 0),
                                (int) item.getOrDefault("done_additional", 0),
                                (String) item.getOrDefault("additional_label", "")
                        ));
                    }

                } else {

                    LessonStat lessonStat = new LessonStat(
                            item.getInteger("duration"),
                            (int) item.getOrDefault("done_duration", 0)
                    );

                    TagStat tagStat = new TagStat(
                            item.getInteger("duration"),
                            (int) item.getOrDefault("done_duration", 0),
                            (int) item.getOrDefault("additional", 0),
                            (int) item.getOrDefault("done_additional", 0),
                            (String) item.getOrDefault("additional_label", "")
                    );

                    lessonStat.tagStats.put(tag, tagStat);
                    lessonStats.put(lesson, lessonStat);
                }

            }
        }

        return lessonStats;
    }

    public static int getFormattedDate(Long d) {
        String[] splited = getSolarDate(d).split("-")[0].replace(" ", "").split("\\/");
        String date = "";

        String y = splited[0];
        date += y;

        int month = Integer.parseInt(splited[1]);
        if (month < 10)
            date += "0" + month;
        else
            date += month;

        int day = Integer.parseInt(splited[2]);
        if (day < 10)
            date += "0" + day;
        else
            date += day;

        return Integer.parseInt(date);
    }
}
