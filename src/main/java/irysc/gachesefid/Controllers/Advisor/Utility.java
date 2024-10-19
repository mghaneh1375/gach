package irysc.gachesefid.Controllers.Advisor;


import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.advisorRequestsRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.Utility.*;

public class Utility {


    public static JSONObject convertToJSONDigest(ObjectId stdId, Document advisor) {

        List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());

        JSONObject jsonObject = new JSONObject()
                .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                .put("acceptStd", advisor.getOrDefault("accept_std", true))
                .put("stdCount", advisorRequestsRepository.count(and(
                        eq("advisor_id", advisor.getObjectId("_id")),
                        exists("paid_at")
                )))
                .put("rate", advisor.getOrDefault("rate", 0))
                .put("bio", advisor.getString("advice_bio"))
                .put("videoLink", advisor.getOrDefault("advice_video_link", ""))
                .put("id", advisor.getObjectId("_id").toString())
                .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic"));

        if(advisor.containsKey("tags"))
            jsonObject.put("tags", advisor.getList("tags", String.class));

        if(advisor.containsKey("form_list")) {

            Document form = searchInDocumentsKeyVal(advisor.getList("form_list", Document.class), "role", "advisor");

            if(form != null) {
                jsonObject.put("form", new JSONObject()
                        .put("workLessons", form.getString("work_lessons"))
                        .put("workSchools", form.getString("work_schools"))
                );
            }

        }

        if (stdId != null) {

            Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", stdId
            );

            if (std != null)
                jsonObject.put("myRate", std.getOrDefault("rate", 0));

        }


        return jsonObject;
    }

    static JSONObject convertFinanceOfferToJSONObject(Document doc, boolean fullAccess) {

        JSONObject jsonObject = new JSONObject()
                .put("id", doc.containsKey("_id") ? doc.getObjectId("_id").toString() : "-1")
                .put("price", doc.getInteger("price"))
                .put("title", doc.getString("title"))
                .put("description", doc.getOrDefault("description", ""))
                .put("videoCalls", doc.getInteger("video_calls"))
                .put("maxKarbarg", doc.getOrDefault("max_karbarg", -1))
                .put("maxExam", doc.getOrDefault("max_exam", -1))
                .put("maxChat", doc.getOrDefault("max_chat", -1))
                .put("videoLink", doc.getOrDefault("video_link", ""));

        if(fullAccess) {
            jsonObject
                    .put("createdAt", getSolarDate(doc.getLong("created_at")))
                    .put("visibility", doc.getBoolean("visibility"))
                    .put("studentsCount", doc.getOrDefault("students", 0));
        }

        return jsonObject;
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

    public static String getWeekDay(int dayIdx) {

        switch (dayIdx) {
            case 0:
            default:
                return "شنبه";
            case 1:
                return "یک شنبه";
            case 2:
                return "دوشنبه";
            case 3:
                return "سه شنبه";
            case 4:
                return "چهار شنبه";
            case 5:
                return "پنج شنبه";
            case 6:
                return "جمعه";
        }

    }

    static JSONArray convertLifeScheduleToJSON(Document schedule, boolean isOwner) {
        JSONArray jsonArray = new JSONArray();

        for (Document day : schedule.getList("days", Document.class)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("label", schedule.getString("label"))
                    .put("day", getWeekDay(day.getInteger("day")));

            JSONArray jsonArray1 = new JSONArray();
            for(Document item : day.getList("items", Document.class)) {

                JSONObject jsonObject1 = new JSONObject()
                        .put("tag", item.getString("tag"))
                        .put("id", item.getObjectId("_id").toString())
                        .put("owner", isOwner)
                        .put("duration", item.get("duration"));

                if(item.containsKey("start_at"))
                    jsonObject1.put("startAt", item.get("start_at"));

                jsonArray1.put(jsonObject1);
            }

            jsonObject.put("items", jsonArray1);
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    static JSONArray convertScheduleToJSON(Document schedule, ObjectId advisorId) {

        JSONArray jsonArray = new JSONArray();
        List<Document> days =  schedule.getList("days", Document.class);
        HashMap<ObjectId, PairValue> avatars = new HashMap<>();

        for (int i = 0; i < 7; i++) {

            Document day = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    days, "day", i
            );

            JSONObject jsonObject = new JSONObject()
                    .put("day", getWeekDay(i));

            JSONArray jsonArray1 = new JSONArray();

            if(day != null) {

                for (Document item : day.getList("items", Document.class)) {

                    PairValue p = null;
                    ObjectId aId = item.getObjectId("advisor_id");

                    if(avatars.containsKey(aId))
                        p = avatars.get(aId);

                    else {
                        Document advisor = userRepository.findById(aId);
                        if(advisor != null) {
                            p = new PairValue(
                                    advisor.getString("first_name") + " " + advisor.getString("last_name"),
                                    STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic")
                            );

                            avatars.put(aId, p);
                        }
                    }

                    JSONObject jsonObject1 = new JSONObject()
                            .put("tag", item.getString("tag"))
                            .put("id", item.getObjectId("_id").toString())
                            .put("duration", item.get("duration"))
                            .put("lesson", item.getString("lesson"));

                    if (item.containsKey("start_at"))
                        jsonObject1.put("startAt", item.get("start_at"));

                    if (item.containsKey("description"))
                        jsonObject1.put("description", item.get("description"));

                    if (item.containsKey("additional_label")) {
                        jsonObject1.put("additionalLabel", item.getString("additional_label"))
                                .put("additional", item.getInteger("additional"));
                    }

                    if(item.containsKey("done_duration"))
                        jsonObject1.put("doneDuration", item.getInteger("done_duration"));

                    if(item.containsKey("done_additional"))
                        jsonObject1.put("doneAdditional", item.getInteger("done_additional"));

                    if(p != null) {
                        jsonObject1.put("advisor", new JSONObject()
                                .put("name", p.getKey().toString())
                                .put("pic", p.getValue().toString())
                                .put("id", aId)
                        );
                    }

                    jsonObject1.put("owner", advisorId != null && advisorId.equals(item.getObjectId("advisor_id")));

                    jsonArray1.put(jsonObject1);

                }

            }

            jsonObject.put("items", jsonArray1);
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    static int validateDay(String day) throws InvalidFieldsException {

        if (
                !day.equals("شنبه") &&
                        !day.equals("یک شنبه") &&
                        !day.equals("دوشنبه") &&
                        !day.equals("سه شنبه") &&
                        !day.equals("چهار شنبه") &&
                        !day.equals("پنج شنبه") &&
                        !day.equals("جمعه")
        )
            throw new InvalidFieldsException("not valid params");

        return getDayIndex(day);
    }

}
