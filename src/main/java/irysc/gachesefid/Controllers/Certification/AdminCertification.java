package irysc.gachesefid.Controllers.Certification;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.certificateRepository;

public class AdminCertification {

    public static String store(JSONObject jsonObject) {

        Document newDoc = new Document("students", new ArrayList<>())
                .append("created_at", System.currentTimeMillis());

        for(String key : jsonObject.keySet())
            newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), jsonObject.get(key));

        return certificateRepository.insertOneWithReturn(newDoc);
    }

    public static String getAll(String title) {

        ArrayList<Document> docs = certificateRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();

        for(Document doc : docs) {

            if(title != null && !doc.getString("title").contains(title))
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("title", doc.getString("title"))
                            .put("createdAt", Utility.getSolarDate(doc.getLong("created_at")))
                            .put("studentsCount", doc.getList("students", Document.class).size())
            );
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    public static String get(ObjectId certificateId) {

        Document certificate = certificateRepository.findOne(eq("_id", certificateId), null);
        List<Document> students = certificate.getList("students", Document.class);

        JSONObject jsonObject = new JSONObject()
                .put("createdAt", Utility.getSolarDate(certificate.getLong("created_at")))
                .put("studentsCount", students.size());

//        JSONArray studentsJSON = new JSONArray();
//        for(Document student : students)
//            studentsJSON.put();

        for(String key : certificate.keySet()) {

            if(key.equals("created_at") || key.equals("students"))
                continue;

            jsonObject.put(Utility.camel(key, false), certificate.get(key));
        }

        return Utility.generateSuccessMsg("data", jsonObject);
    }
}
