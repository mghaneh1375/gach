package irysc.gachesefid.Controllers.QuestionReport;

import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.questionReportRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateErr;

public class QuestionReportController {


    public static String getAllTags() {

        ArrayList<Document> tags = questionReportRepository.find(
                null, null
        );
        JSONArray jsonArray = new JSONArray();

        for (Document tag : tags)
            jsonArray.put(new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"))
                    .put("canHasDesc", tag.getBoolean("can_has_desc"))
                    .put("priority", tag.getInteger("priority"))
            );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    public static String create(JSONObject jsonObject) {

        if (questionReportRepository.exist(
                eq("label", jsonObject.getString("label"))
        ))
            return generateErr("این تگ در سیستم موجود است");

        return questionReportRepository.insertOneWithReturn(
                new Document("label", jsonObject.getString("label"))
                        .append("priority", jsonObject.getInt("priority"))
                        .append("can_has_desc", jsonObject.getBoolean("canHasDesc"))
                        .append("reports_count", 0)
                        .append("visibility", true)
                        .append("reports", new ArrayList<>())
        );
    }

    public static String remove(JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = questionReportRepository.findOneAndDelete(
                        eq("_id", new ObjectId(id))
                );

                if (tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            } catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String edit(ObjectId id, JSONObject jsonObject) {

        Document doc = questionReportRepository.findById(id);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        if (!doc.getString("label").equals(jsonObject.getString("label")) &&
                questionReportRepository.exist(
                        eq("label", jsonObject.getString("label"))
                )
        )
            return generateErr("این تگ در سیستم موجود است");

        doc.put("label", jsonObject.getString("label"));
        doc.put("visibility", jsonObject.getString("visibility"));
        doc.put("priority", jsonObject.getString("priority"));
        doc.put("can_has_desc", jsonObject.getString("canHasDesc"));

        questionReportRepository.replaceOne(id, doc);

        return JSON_OK;
    }

}
