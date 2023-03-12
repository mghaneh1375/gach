package irysc.gachesefid.Controllers.QuestionReport;

import org.bson.Document;
import org.json.JSONObject;

public class Utility {

    public static JSONObject convertToJSON(Document doc, Document user) {

        JSONObject jsonObject = new JSONObject()
                .put("createdAt", irysc.gachesefid.Utility.Utility.getSolarDate(doc.getLong("created_at")))
                .put("questionCode", doc.getString("question_code"))
                .put("NID", user.get("NID"))
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("description", doc.getOrDefault("description", ""));

        irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);

        return jsonObject;
    }

}
