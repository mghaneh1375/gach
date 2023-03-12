package irysc.gachesefid.Controllers.QuestionReport;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class QuestionReportController {

    public static String getReports(ObjectId tagId) {

        Document tag = questionReportRepository.findById(tagId);
        if(tag == null)
            return JSON_NOT_VALID_ID;

        List<Document> reports = tag.getList("reports", Document.class);
        List<Object> userIds = new ArrayList<>();
        JSONArray data = new JSONArray();

        for (Document report : reports) {
            userIds.add(report.getObjectId("user_id"));
        }

        List<Document> users = userRepository.findByIds(userIds, true,
                new BasicDBObject("NID", 1).append("first_name", 1)
                        .append("last_name", 1)
        );

        if(users == null)
            return JSON_NOT_UNKNOWN;

        int idx = 0;
        for(Document report : reports) {
            data.put(
                    irysc.gachesefid.Controllers.QuestionReport.Utility.convertToJSON(report, users.get(idx))
            );
            idx++;
        }

        return generateSuccessMsg("data", data);
    }

    public static String getAllTags(boolean isAdmin) {

        ArrayList<Document> tags = questionReportRepository.find(
                isAdmin ? null : eq("visibility", true), null, Sorts.ascending("priority")
        );
        JSONArray jsonArray = new JSONArray();

        if(isAdmin) {
            for (Document tag : tags)
                jsonArray.put(new JSONObject()
                        .put("id", tag.getObjectId("_id").toString())
                        .put("label", tag.getString("label"))
                        .put("canHasDesc", tag.getBoolean("can_has_desc"))
                        .put("visibility", tag.getBoolean("visibility"))
                        .put("priority", tag.getInteger("priority"))
                );
        }
        else {
            for (Document tag : tags)
                jsonArray.put(new JSONObject()
                        .put("id", tag.getObjectId("_id").toString())
                        .put("label", tag.getString("label"))
                        .put("canHasDesc", tag.getBoolean("can_has_desc"))
                        .put("priority", tag.getInteger("priority"))
                );
        }

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
                        .append("visibility", jsonObject.getBoolean("visibility"))
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

    public static String storeReport(ObjectId userId, ObjectId questionId,
                                     ObjectId tagId, String desc) {

        Document tag = questionReportRepository.findById(tagId);
        if (tag == null || !tag.getBoolean("visibility"))
            return JSON_NOT_VALID_ID;

        Document question = questionRepository.findById(questionId);
        if(question == null || !question.getBoolean("visibility"))
            return JSON_NOT_VALID_ID;

        if(!tag.getBoolean("can_has_desc") && desc != null)
            return JSON_NOT_VALID_PARAMS;

        List<Document> reports = tag.getList("reports", Document.class);

        if(searchInDocumentsKeyValIdx(reports, "user_id", userId,
                "question_code", question.getString("organization_id")) != -1)
            return generateErr("شما قبلا این سوال را با این تگ گزارش کرده اید");

        Document newDoc = new Document("user_id", userId)
                .append("created_at", System.currentTimeMillis())
                .append("question_code", question.getString("organization_id"));

        if(desc != null)
            newDoc.put("description", desc);

        reports.add(newDoc);
        questionReportRepository.replaceOne(tagId, tag);

        return JSON_OK;
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
        doc.put("visibility", jsonObject.getBoolean("visibility"));
        doc.put("priority", jsonObject.getInt("priority"));
        doc.put("can_has_desc", jsonObject.getBoolean("canHasDesc"));

        questionReportRepository.replaceOne(id, doc);

        return JSON_OK;
    }

}
