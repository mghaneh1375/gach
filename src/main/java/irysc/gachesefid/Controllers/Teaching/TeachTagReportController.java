package irysc.gachesefid.Controllers.Teaching;

import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.teachTagReportRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;

public class TeachTagReportController {

    public static String createTag(JSONObject jsonObject) {

        if (teachTagReportRepository.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        Document newDoc = new Document("label", jsonObject.getString("label"))
                .append("priority", jsonObject.getInt("priority"))
                .append("mode", jsonObject.getString("mode"));

        return teachTagReportRepository.insertOneWithReturn(newDoc);
    }

    public static String removeTags(JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = teachTagReportRepository.findOneAndUpdate(
                        new ObjectId(id),
                        set("deleted_at", System.currentTimeMillis())
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

    public static String editTag(ObjectId id, JSONObject jsonObject) {

        Document doc = teachTagReportRepository.findById(id);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        if (!doc.getString("label").equals(jsonObject.getString("label")) &&
                teachTagReportRepository.exist(
                        and(
                                exists("deleted_at", false),
                                eq("label", jsonObject.getString("label"))
                        )
                ))
            return generateErr("این تگ در سیستم موجود است");

        doc.put("label", jsonObject.getString("label"));
        doc.put("priority", jsonObject.getInt("priority"));
        doc.put("mode", jsonObject.getString("mode"));

        teachTagReportRepository.replaceOne(id, doc);
        return JSON_OK;
    }

    public static String getAllTags(String mode) {

        List<Bson> filters = new ArrayList<>();
        filters.add(exists("deleted_at", false));
        if(mode != null) {
            if(!EnumValidatorImp.isValid(mode, TeachReportTagMode.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("mode", mode));
        }

        ArrayList<Document> tags = teachTagReportRepository.find(
                and(filters), null
        );
        JSONArray jsonArray = new JSONArray();

        for (Document tag : tags) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"))
                    .put("priority", tag.getInteger("priority"))
                    .put("mode", tag.getString("mode"));

            jsonArray.put(jsonObject);
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }
}
