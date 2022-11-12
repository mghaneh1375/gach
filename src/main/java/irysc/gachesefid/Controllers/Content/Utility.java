package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;

import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;

public class Utility {

    static Document returnIfNoRegistry(ObjectId id) throws InvalidFieldsException {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            throw new InvalidFieldsException("id is not valid");

        if(doc.getList("users", Document.class).size() > 0)
            throw new InvalidFieldsException("بسته موردنظر خریده شده است و این امکان وجود ندارد.");

        return doc;

    }

    static JSONObject convertDigest(Document doc, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("title", doc.get("title"))
                .put("tags", doc.get("tags"))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("duration", doc.getOrDefault("duration", ""))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if(!isAdmin && doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if(isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("createdAt", irysc.gachesefid.Utility.Utility.getSolarDate(doc.getLong("created_at")))
                    .put("buyers", doc.getList("users", Document.class).size());
        }

        return jsonObject;
    }

    static JSONObject convert(Document doc, boolean isAdmin, boolean afterBuy) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("description", doc.getString("description"))
                .put("teacherBio", doc.getOrDefault("teacher_bio", ""))
                .put("title", doc.get("title"))
                .put("tags", doc.getOrDefault("tags", new ArrayList<>()))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("duration", doc.getOrDefault("duration", ""))
                .put("preReq", doc.get("pre_req"))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if(doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if(isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("finalExamMinMark", doc.getOrDefault("final_exam_min_mark", -1))
                    .put("buyers", doc.getList("users", Document.class).size());

            if(doc.containsKey("cert_id"))
                jsonObject.put("certId", doc.getObjectId("cert_id").toString());

            if(doc.containsKey("final_exam_id"))
                jsonObject.put("finalExamId", doc.getObjectId("final_exam_id").toString());

            if(doc.containsKey("final_exam_min_mark"))
                jsonObject.put("finalExamMinMark", doc.get("final_exam_min_mark"));

        }

        return jsonObject;
    }

}
