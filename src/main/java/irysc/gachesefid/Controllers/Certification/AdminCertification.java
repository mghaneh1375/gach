package irysc.gachesefid.Controllers.Certification;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.CertificateRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.certificateRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class AdminCertification {

    public static String store(JSONObject jsonObject) {

        Document newDoc = new Document("users", new ArrayList<>())
                .append("img", null)
                .append("visibility", true)
                .append("is_landscape", jsonObject.getBoolean("isLandscape"))
                .append("title", jsonObject.getString("title"))
                .append("qr_x", jsonObject.getInt("qrX"))
                .append("qr_y", jsonObject.getInt("qrY"))
                .append("created_at", System.currentTimeMillis());

        try {
            newDoc.append("params", fetchParams(jsonObject.getJSONArray("params")));
        } catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }

        return certificateRepository.insertOneWithReturn(newDoc);
    }

    // overwrite all old data
    public static String update(ObjectId certId, JSONObject jsonObject) {

        Document certificate = certificateRepository.findById(certId);

        if (certificate == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = certificate.getList("users", Document.class);
        JSONArray params = jsonObject.getJSONArray("params");

        if (users.size() > 0 && users.get(0).getList("params", String.class).size() != params.length())
            return Utility.generateErr("سایز پارامترها باید " + users.get(0).keySet().size() + " باشد.");

        try {
            certificate.put("params", fetchParams(params));
        } catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }

        certificate.put("is_landscape", jsonObject.getBoolean("isLandscape"));
        certificate.put("title", jsonObject.getString("title"));
        certificate.put("qr_x", jsonObject.getInt("qrX"));
        certificate.put("qr_y", jsonObject.getInt("qrY"));
        certificate.put("visibility", jsonObject.getBoolean("visibility"));

        certificateRepository.replaceOne(certId, certificate);
        return JSON_OK;
    }

    private static ArrayList<Document> fetchParams(JSONArray jsonArray
    ) throws Exception {

        ArrayList<Document> params = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject param = jsonArray.getJSONObject(i);

            if (!param.has("fontSize") ||
                    !param.has("isBold") ||
                    !param.has("title") ||
                    !param.has("y")
            )
                throw new InvalidFieldsException("not valid params");

            if (!param.has("isCenter") && !param.has("x"))
                throw new InvalidFieldsException("not valid params");

            Document doc = new Document("font_size", param.getInt("fontSize"))
                            .append("is_bold", param.getBoolean("isBold"))
                            .append("y", param.getInt("y"))
                            .append("title", param.getString("title"));

            if(param.has("x"))
                doc.append("x", param.getInt("x"));

            else if(param.has("isCenter")) {

                doc.append("is_center", param.getBoolean("isCenter"));

                if(param.has("centerOffset"))
                    doc.append("center_offset", param.getInt("centerOffset"));
            }

            params.add(doc);
        }

        return params;
    }

    public static String setImg(ObjectId certId, MultipartFile file) {

        Document certificate = certificateRepository.findById(certId);

        if (certificate == null)
            return JSON_NOT_VALID_ID;

        if (certificate.get("img") != null)
            FileUtils.removeFile(
                    certificate.getString("img"),
                    CertificateRepository.FOLDER
            );

        String fileType = FileUtils.uploadImageFile(file);

        if (fileType == null)
            return JSON_NOT_VALID_FILE;

        String filename = FileUtils.uploadFile(file, CertificateRepository.FOLDER);

        if (filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        certificate.put("img", filename);
        certificateRepository.updateOne(certId, set("img", filename));

        return JSON_OK;
    }

    public static String addUserToCert(Document cert, ObjectId certId,
                                       String NID, JSONArray values) {

        boolean needUpdate = cert == null;

        if (cert == null)
            cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        if (cert.get("img") == null)
            return Utility.generateErr("لطفا ابتدا تصویر تم گواهی را آپلود نمایید.");

        List<Document> users = cert.getList("users", Document.class);

        if (Utility.searchInDocumentsKeyValIdx(users, "NID", NID) != -1)
            return Utility.generateErr("فرد موردنظر در لیست نفرات گواهی موردنظر موجود است.");

        List<Document> params = cert.getList("params", Document.class);
        if (params.size() != values.length())
            return JSON_NOT_VALID_PARAMS;

        users.add(new Document("NID", NID)
                .append("params", values));

        if (needUpdate)
            certificateRepository.replaceOne(cert.getObjectId("_id"), cert);

        return JSON_OK;
    }

    private static String removeUserFromCert(Document cert,
                                             ObjectId certId,
                                             String NID) {

        boolean needUpdate = cert == null;

        if (cert == null)
            cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = cert.getList("users", Document.class);

        Document user = Utility.searchInDocumentsKeyVal(
                users, "NID", NID
        );

        if (user == null)
            return JSON_NOT_VALID_ID;

        users.remove(user);

        if (needUpdate)
            certificateRepository.replaceOne(cert.getObjectId("_id"), cert);

        return JSON_OK;
    }

    public static String removeUsersFromCert(ObjectId certId, JSONArray NIDs) {

        Document cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        JSONArray excepts = new JSONArray();

        for (int i = 0; i < NIDs.length(); i++) {
            if (removeUserFromCert(cert, null, NIDs.getString(i)).contains("nok"))
                excepts.put(NIDs.get(i));
        }

        if (excepts.length() == 0)
            return JSON_OK;

        return generateSuccessMsg("excepts", excepts);
    }

    public static String getAll(String title) {

        ArrayList<Document> docs = certificateRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            if (title != null && !doc.getString("title").contains(title))
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("title", doc.getString("title"))
                            .put("createdAt", Utility.getSolarDate(doc.getLong("created_at")))
                            .put("studentsCount", doc.getList("students", Document.class).size())
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String get(ObjectId certificateId) {

        Document certificate = certificateRepository.findById(certificateId);
        List<Document> students = certificate.getList("students", Document.class);

        JSONObject jsonObject = new JSONObject()
                .put("createdAt", Utility.getSolarDate(certificate.getLong("created_at")))
                .put("studentsCount", students.size());

//        JSONArray studentsJSON = new JSONArray();
//        for(Document student : students)
//            studentsJSON.put();

        for (String key : certificate.keySet()) {

            if (key.equals("created_at") || key.equals("students"))
                continue;

            jsonObject.put(Utility.camel(key, false), certificate.get(key));
        }

        return generateSuccessMsg("data", jsonObject);
    }
}
