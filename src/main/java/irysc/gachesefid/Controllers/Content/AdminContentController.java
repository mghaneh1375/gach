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
import static irysc.gachesefid.Utility.Utility.*;

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

        if(newDoc.containsKey("cert_id"))
            newDoc.put("cert_id", new ObjectId(newDoc.getString("cert_id")));

        if(newDoc.containsKey("final_exam_id"))
            newDoc.put("final_exam_id", new ObjectId(newDoc.getString("final_exam_id")));

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

        if(doc.containsKey("img"))
            newDoc.put("img", doc.get("img"));

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

        if(newDoc.containsKey("cert_id"))
            newDoc.put("cert_id", new ObjectId(newDoc.getString("cert_id")));

        if(newDoc.containsKey("final_exam_id"))
            newDoc.put("final_exam_id", new ObjectId(newDoc.getString("final_exam_id")));

        contentRepository.replaceOne(id, newDoc);
        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertDigest(
                newDoc, true
        ));
    }

    public static String remove(JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if(!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId oId = new ObjectId(id);
                Document doc = contentRepository.findById(oId);
                if(doc == null) {
                    excepts.put(i + 1);
                    continue;
                }

                if(doc.getList("users", Document.class).size() > 0) {
                    excepts.put((i + 1) + " " + "نفراتی این بسته را خریداری کرده اند و امکان حذف آن وجود ندارد.");
                    continue;
                }

                contentRepository.cleanRemove(doc);
                doneIds.put(oId);

            }
            catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return returnRemoveResponse(excepts, doneIds);
    }

    public static String updateSession(ObjectId id, ObjectId sessionId, JSONObject data) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = doc.getList("sessions", Document.class);
        Document session = Utility.searchInDocumentsKeyVal(
                sessions, "_id", sessionId
        );

        if(session == null)
            return JSON_NOT_VALID_ID;

        int idx = Utility.searchInDocumentsKeyValIdx(
                sessions, "_id", sessionId
        );

        Document newDoc = new Document()
                .append("_id", session.getObjectId("_id"))
                .append("attaches", session.get("attaches"));

        if(session.containsKey("video"))
            newDoc.put("video", session.get("video"));

        for(String key : data.keySet()) {
            if(key.equalsIgnoreCase("examId"))
                newDoc.put(
                        "exam_id",
                        new ObjectId(data.getString(key))
                );
            else
                newDoc.put(
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                        data.get(key)
                );
        }

        sessions.set(idx, newDoc);
        sessions.sort(Comparator.comparing(o -> o.getInteger("priority")));

        doc.put("sessions", sessions);
        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("data",
                irysc.gachesefid.Controllers.Content.Utility.sessionDigest(newDoc, true, false)
        );
    }

    public static String addSession(ObjectId id, JSONObject data) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = doc.getList("sessions", Document.class);

        ObjectId oId = new ObjectId();
        Document newDoc = new Document("_id", oId)
                .append("attaches", new ArrayList<>());

        for(String key : data.keySet()) {
            if(key.equalsIgnoreCase("examId"))
                newDoc.put(
                        "exam_id",
                        new ObjectId(data.getString(key))
                );
            else
                newDoc.put(
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                        data.get(key)
                );
        }

        sessions.add(newDoc);
        sessions.sort(Comparator.comparing(o -> o.getInteger("priority")));

        doc.put("sessions", sessions);
        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("data",
                irysc.gachesefid.Controllers.Content.Utility.sessionDigest(newDoc, true, false)
        );
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


    public static String removeImg(ObjectId id) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        if(doc.containsKey("img"))
            FileUtils.removeFile(doc.getString("img"), ContentRepository.FOLDER);

        doc.remove("img");
        contentRepository.replaceOne(id, doc);

        return JSON_OK;
    }

    public static String fetchSessions(ObjectId id) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = doc.getList("sessions", Document.class);
        JSONArray jsonArray = new JSONArray();

        for(Document session : sessions)
            jsonArray.put(irysc.gachesefid.Controllers.Content.Utility.sessionDigest(session, true, false));

        return generateSuccessMsg("data", jsonArray);
    }

    public static String setSessionVideo(ObjectId id, ObjectId sessionId, MultipartFile file) {

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

        if(session.containsKey("video"))
            FileUtils.removeFile(session.getString("video"), ContentRepository.FOLDER);

        session.put("video", filename);
        contentRepository.replaceOne(id, doc);

        return generateSuccessMsg("link", STATICS_SERVER + ContentRepository.FOLDER + "/" + filename);
    }

    public static String removeVideoFromSession(ObjectId id, ObjectId sessionId) {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        Document session = Utility.searchInDocumentsKeyVal(
                doc.getList("sessions", Document.class), "_id", sessionId
        );

        if(session == null)
            return JSON_NOT_VALID_ID;

        if(session.containsKey("video"))
            FileUtils.removeFile(session.getString("video"), ContentRepository.FOLDER);

        session.remove("video");
        contentRepository.replaceOne(id, doc);

        return JSON_OK;
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

                Document session = Utility.searchInDocumentsKeyVal(
                        sessions, "_id", oId
                );

                if(session == null) {
                    excepts.put(i + 1);
                    continue;
                }

                contentRepository.removeSession(session);
                sessions.remove(Utility.searchInDocumentsKeyValIdx(
                        sessions, "_id", oId
                ));
                removeIds.put(oId);
            }

            if(removeIds.length() > 0)
                contentRepository.replaceOne(id, doc);

            return Utility.returnRemoveResponse(excepts, removeIds);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }


}
