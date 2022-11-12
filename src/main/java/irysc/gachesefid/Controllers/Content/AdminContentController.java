package irysc.gachesefid.Controllers.Content;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class AdminContentController {

    public static String store(JSONObject data) {

        Document newDoc = new Document("created_at", System.currentTimeMillis())
                .append("users", new ArrayList<>())
                .append("sessions", new ArrayList<>());

        for(String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        if(newDoc.containsKey("final_exam_id") != newDoc.containsKey("final_exam_min_mark"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("duration") && !newDoc.containsKey("cert_id"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("price") && newDoc.get("price") instanceof String) {
            try {
                newDoc.put("price", Integer.parseInt(newDoc.getString("price")));
            }
            catch (Exception x) {
                newDoc.put("price", 0);
            }
        }

        contentRepository.insertOne(newDoc);
        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertDigest(
                newDoc, true
        ));
    }

    public static String update(ObjectId id, JSONObject data) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        Document newDoc = new Document("created_at", doc.getLong("created_at"))
                .append("users", doc.getList("users", Document.class))
                .append("_id", id)
                .append("sessions", doc.getList("sessions", Document.class));

        for(String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        if(newDoc.containsKey("final_exam_id") != newDoc.containsKey("final_exam_min_mark"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("duration") && !newDoc.containsKey("cert_id"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("price") && newDoc.get("price") instanceof String) {
            try {
                newDoc.put("price", Integer.parseInt(newDoc.getString("price")));
            }
            catch (Exception x) {
                newDoc.put("price", 0);
            }
        }

        contentRepository.replaceOne(id, newDoc);

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertDigest(
                newDoc, true
        ));
    }

    public static String addSession(ObjectId id, JSONObject data) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = doc.getList("sessions", Document.class);

        ObjectId oId = new ObjectId();
        Document newDoc = new Document("_id", oId)
                .append("videos", new ArrayList<>())
                .append("attaches", new ArrayList<>());

        for(String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        sessions.add(newDoc);
        sessions.sort(Comparator.comparing(o -> o.getInteger("priority")));

        doc.put("sessions", sessions);
        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("id", oId);
    }

    public static String setImg(ObjectId id, MultipartFile file) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        String type = FileUtils.uploadImageFile(file);
        if(type == null)
            return JSON_NOT_VALID_FILE;

        String filename = FileUtils.uploadFile(file, ContentRepository.FOLDER);
        if(filename == null)
            return JSON_NOT_UNKNOWN;

        if(doc.containsKey("img"))
            FileUtils.removeFile(doc.getString("img"), ContentRepository.FOLDER);

        doc.put("img", filename);
        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("link", STATICS_SERVER + ContentRepository.FOLDER + "/" + filename);
    }


    public static String addٰVideoToSession(ObjectId id, ObjectId sessionId, MultipartFile file) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        Document session = Utility.searchInDocumentsKeyVal(
                doc.getList("sessions", Document.class), "_id", sessionId
        );

        if(session == null)
            return JSON_NOT_VALID_ID;

        String type = FileUtils.uploadMultimediaFile(file);
        if(type == null)
            return JSON_NOT_VALID_FILE;

        String filename = FileUtils.uploadFile(file, ContentRepository.FOLDER);
        if(filename == null)
            return JSON_NOT_UNKNOWN;

        List<String> videos = session.getList("videos", String.class);
        videos.add(filename);
        session.put("videos", videos);

        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("link", STATICS_SERVER + ContentRepository.FOLDER + "/" + filename);
    }

    public static String addٰAttachToSession(ObjectId id, ObjectId sessionId, MultipartFile file) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        Document session = Utility.searchInDocumentsKeyVal(
                doc.getList("sessions", Document.class), "_id", sessionId
        );

        if(session == null)
            return JSON_NOT_VALID_ID;

        String type = FileUtils.uploadDocOrMultimediaFile(file);
        if(type == null)
            return JSON_NOT_VALID_FILE;

        String filename = FileUtils.uploadFile(file, ContentRepository.FOLDER);
        if(filename == null)
            return JSON_NOT_UNKNOWN;

        List<String> attaches = session.getList("attaches", String.class);
        attaches.add(filename);
        session.put("attaches", attaches);

        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("link", STATICS_SERVER + ContentRepository.FOLDER + "/" + filename);
    }

    public static String removeSession(ObjectId id, JSONArray ids) {

        try {

            Document doc = irysc.gachesefid.Controllers.Content.Utility.returnIfNoRegistry(id);

            JSONArray excepts = new JSONArray();
            JSONArray removeIds = new JSONArray();
            List<Document> sessions = doc.getList("sessions", Document.class);

            for(int i = 0; i < ids.length(); i++) {

                String tmpId = ids.getString(i);
                if(!ObjectId.isValid(tmpId)) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId oId = new ObjectId(tmpId);

                int idx = Utility.searchInDocumentsKeyValIdx(
                        sessions, "_id", oId
                );

                if(idx == -1) {
                    excepts.put(i + 1);
                    continue;
                }

                sessions.remove(idx);
                removeIds.put(oId);
            }

            return Utility.returnRemoveResponse(excepts, removeIds);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }


}
