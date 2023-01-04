package irysc.gachesefid.Controllers.Content;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class AdminContentController {

    public static String fetchContentDigests() {

        ArrayList<Document> docs = contentRepository.find(null,
                new BasicDBObject("_id", 1).append("title", 1)
        );

        JSONArray jsonArray = new JSONArray();
        for(Document doc : docs) {
            jsonArray.put(
                    new JSONObject()
                            .put("title", doc.getString("title"))
                            .put("id", doc.getObjectId("_id").toString())
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    private static JSONObject convertStudentDocToJSON(
            Document student, Document user
    ) {

        JSONObject jsonObject = new JSONObject()
                .put("paid", student.get("paid"))
                .put("id", user.getObjectId("_id").toString())
                .put("registerAt", getSolarDate(student.getLong("register_at")));

        irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);

        return jsonObject;
    }

    public static String buyers(ObjectId id) {

        Document content = contentRepository.findById(id);

        JSONArray jsonArray = new JSONArray();

        List<Document> students = content.getList("users", Document.class);

        for (int j = students.size() - 1; j >= 0; j--) {

            Document student = students.get(j);

            Document user = userRepository.findById(student.getObjectId("_id"));
            if (user == null)
                continue;

            jsonArray.put(convertStudentDocToJSON(student, user));
        }

        return irysc.gachesefid.Utility.Utility.generateSuccessMsg("data", jsonArray);
    }

    public static String forceRegistry(ObjectId id, JSONArray jsonArray, int paid) {

        Document content = contentRepository.findById(id);
        if(content == null)
            return JSON_NOT_VALID_ID;

        JSONArray excepts = new JSONArray();
        JSONArray addedItems = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String NID = jsonArray.getString(i);

            if (!irysc.gachesefid.Utility.Utility.validationNationalCode(NID)) {
                excepts.put(i + 1);
                continue;
            }

            Document student = userRepository.findBySecKey(NID);
            Document tmp = StudentContentController.registry(
                    id, student.getObjectId("_id"), paid,
                    student.getString("phone"), student.getString("mail")
            );

            if(tmp != null)
                addedItems.put(convertStudentDocToJSON(tmp, student));
        }

        return irysc.gachesefid.Utility.Utility.returnAddResponse(excepts, addedItems);

    }

}
