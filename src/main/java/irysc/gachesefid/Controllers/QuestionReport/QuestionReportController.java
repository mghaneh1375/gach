package irysc.gachesefid.Controllers.QuestionReport;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.questionReportRepository;
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
                        .append("reports", new ArrayList<>())
        );
    }

    public static String remove(JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if(!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = questionReportRepository.findOneAndDelete(
                        eq("_id", new ObjectId(id))
                );

                if(tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }


}
