package irysc.gachesefid.Controllers.Advisor;


import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Utility {

    public static JSONObject convertToJSONDigest(ObjectId stdId, Document advisor) {

        List<Document> students = (List<Document>)advisor.getOrDefault("students", new ArrayList<>());

        JSONObject jsonObject = new JSONObject()
                .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                .put("acceptStd", advisor.getOrDefault("accept_std", true))
                .put("stdCount", students.size())
                .put("rate", advisor.getOrDefault("rate", 0))
                .put("bio", advisor.getString("bio"))
                .put("id", advisor.getObjectId("_id").toString())
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic"))
                ;

        if(stdId != null) {

            Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", stdId
            );

            if(std != null)
                jsonObject.put("myRate", std.getOrDefault("rate", 0));

        }


        return jsonObject;
    }

    private static String getWeekDay(int dayIdx) {

        switch (dayIdx) {
            case 0:
            default:
                return "شنبه";
            case 1:
                return "یک شنبه";
            case 2:
                return "دو شنبه";
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

    static JSONArray convertLifeScheduleToJSON(Document schedule) {

        JSONArray jsonArray = new JSONArray();

        for(Document day : schedule.getList("days", Document.class)) {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("label", schedule.getString("label"))
                    .put("day", getWeekDay(day.getInteger("day")))
                    .put("items", day.getList("items", Document.class));

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

}
