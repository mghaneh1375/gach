package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.contentConfigRepository;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.StaticValues.SERVER;
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

    static JSONObject convertFAQDigest(Document item, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("question", item.get("question"))
                .put("answer", item.get("answer"));

        if(isAdmin) {
            jsonObject.put("id", item.getObjectId("_id").toString())
                    .put("priority", item.get("priority"))
                    .put("visibility", item.get("visibility"));
        }

        return jsonObject;
    }

    static JSONObject convertDigest(Document doc, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("tags", doc.get("tags"))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("certDuration", doc.getOrDefault("cert_duration", ""))
                .put("duration", isAdmin ? doc.getInteger("duration") : doc.getInteger("duration") * 60)
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

    static JSONObject convert(Document doc, boolean isAdmin, boolean afterBuy, boolean includeFAQ) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("description", doc.getString("description"))
                .put("teacherBio", doc.getOrDefault("teacher_bio", ""))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("tags", doc.getOrDefault("tags", new ArrayList<>()))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("certDuration", doc.getOrDefault("duration", ""))
                .put("duration", isAdmin ? doc.getInteger("duration") : doc.getInteger("duration") * 60)
                .put("preReq", doc.get("pre_req"))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if(includeFAQ) {
            Document config = contentConfigRepository.findBySecKey("first");
            if(config != null) {

                List<Document> faqs = config.getList("faq", Document.class);
                JSONArray faqJSON = new JSONArray();

                for(Document faq : faqs) {

                    if(!faq.getBoolean("visibility") && !isAdmin)
                        continue;

                    faqJSON.put(convertFAQDigest(faq, isAdmin));
                }

                jsonObject.put("faqs", faqJSON);
            }
        }

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
        else {
            List<Document> sessions = doc.getList("sessions", Document.class);
            JSONArray sessionsJSON = new JSONArray();

            for (Document session : sessions)
                sessionsJSON.put(sessionDigest(session, false, afterBuy));

            jsonObject.put("afterBuy", afterBuy);
            jsonObject.put("sessions", sessionsJSON);
        }

        return jsonObject;
    }

    static JSONObject sessionDigest(Document doc, boolean isAdmin, boolean afterBuy) {

        List<String> attaches = doc.containsKey("attaches") ? doc.getList("attaches", String.class)
                : new ArrayList<>();

        JSONObject jsonObject = new JSONObject()
                .put("id", doc.getObjectId("_id").toString())
                .put("title", doc.get("title"))
                .put("duration", doc.get("duration"))
                .put("price", doc.getOrDefault("price", -1))
                .put("description", doc.get("description"))
                .put("attachesCount", attaches.size());

        if(doc.containsKey("exam_id")) {
            jsonObject
                    .put("examId", doc.getObjectId("exam_id").toString())
                    .put("hasExam", true)
                    .put("minMark", doc.getOrDefault("min_mark", -1))
            ;
        }
        else
            jsonObject.put("hasExam", false);

        if(isAdmin)
            jsonObject.put("visibility", doc.get("visibility"))
                    .put("priority", doc.get("priority"))
                    .put("hasVideo", doc.containsKey("video") && doc.get("video") != null);

        if(isAdmin || afterBuy) {

            JSONArray attachesJSONArr = new JSONArray();
            for(String itr : attaches)
                attachesJSONArr.put(itr);

            jsonObject.put("attaches", attachesJSONArr);
            String video = null;

            if(isAdmin)
                video = doc.containsKey("video") ? doc.getString("video") : null;
            else if(doc.containsKey("chunk_at") && doc.containsKey("video")) {
                String folderName = doc.getString("video").split("\\.mp4")[0];
                video = STATICS_SERVER + "videos/" + folderName + "/playlist.m3u8";
            }

            jsonObject.put("video", video);
        }

        return jsonObject;

    }

}
