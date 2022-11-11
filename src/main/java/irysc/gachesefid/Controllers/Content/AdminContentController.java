package irysc.gachesefid.Controllers.Content;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class AdminContentController {

    public static String getAll(ObjectId userId,
                                boolean isAdmin,
                                String tag,
                                String title,
                                String teacher,
                                Boolean visibility,
                                Boolean hasCert,
                                Integer minPrice,
                                Integer maxPrice) {

        ArrayList<Bson> filters = new ArrayList<>();
        if(!isAdmin)
            filters.add(eq("visibility", true));

        if(!isAdmin && userId != null)
            filters.add(nin("users._id", userId));

        if(title != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(title), Pattern.CASE_INSENSITIVE)));

        if(tag != null)
            filters.add(in("tags", tag));

        if(hasCert != null)
            filters.add(exists("cert_id", hasCert));

        if(minPrice != null)
            filters.add(lte("price", minPrice));

        if(maxPrice != null)
            filters.add(gte("price", maxPrice));

        if(isAdmin) {

            if(visibility != null)
                filters.add(eq("visibility", visibility));

            if(teacher != null)
                filters.add(eq("teacher", teacher));

        }

        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                filters.size() == 0 ? null : and(filters),
                isAdmin ? CONTENT_DIGEST_FOR_ADMIN : CONTENT_DIGEST
        );

        for(Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));

        return generateSuccessMsg("data", data);
    }

    public static String get(boolean isAdmin, ObjectId userId, ObjectId contentId) {

        Document content = contentRepository.findById(contentId);
        if(content == null)
            return JSON_NOT_VALID_ID;

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convert(
                content, isAdmin,
                isAdmin || userId != null && Utility.searchInDocumentsKeyValIdx(
                        content.getList("users", Document.class), "_id", userId
                ) != -1)
        );
    }

    public static String store(JSONObject data) {

        Document newDoc = new Document("created_at", System.currentTimeMillis())
                .append("c", new ArrayList<>())
                .append("sessions", new ArrayList<>());

        for(String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        if(newDoc.containsKey("finalExamId") != newDoc.containsKey("finalExamMinMark"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("duration") && !newDoc.containsKey("certId"))
            return JSON_NOT_VALID_PARAMS;

        if(newDoc.containsKey("price") && newDoc.get("price") instanceof String) {
            try {
                newDoc.put("price", Integer.parseInt(newDoc.getString("price")));
            }
            catch (Exception x) {
                newDoc.put("price", 0);
            }
        }

        ObjectId oId = contentRepository.insertOneWithReturnId(newDoc);
        return generateSuccessMsg("id", oId.toString());
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
