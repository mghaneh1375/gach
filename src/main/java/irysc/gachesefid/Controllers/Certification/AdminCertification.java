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
import static irysc.gachesefid.Utility.Utility.*;

public class AdminCertification {

    public static String store(JSONObject jsonObject) {

        Document newDoc = new Document("users", new ArrayList<>())
                .append("img", null)
                .append("visibility", true)
                .append("is_landscape", jsonObject.getBoolean("isLandscape"))
                .append("title", jsonObject.getString("title"))
                .append("qr_x", jsonObject.getInt("qrX"))
                .append("qr_y", jsonObject.getInt("qrY"))
                .append("qr_size", jsonObject.getInt("qrSize"))
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
            System.out.println(x.getMessage());
            x.printStackTrace();
            return Utility.generateErr(x.getMessage());
        }

        certificate.put("is_landscape", jsonObject.getBoolean("isLandscape"));
        certificate.put("title", jsonObject.getString("title"));
        certificate.put("qr_x", jsonObject.getInt("qrX"));
        certificate.put("qr_y", jsonObject.getInt("qrY"));
        certificate.put("qr_size", jsonObject.getInt("qrSize"));

        if(jsonObject.has("visibility"))
            certificate.put("visibility", jsonObject.getBoolean("visibility"));

        certificateRepository.replaceOne(certId, certificate);
        return JSON_OK;
    }

    private static ArrayList<Document> fetchParams(JSONArray jsonArray
    ) throws Exception {

        ArrayList<Document> params = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject param = Utility.convertPersian(jsonArray.getJSONObject(i));

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

        String fileType = FileUtils.uploadImageFile(file);

        if (fileType == null)
            return JSON_NOT_VALID_FILE;

        String filename = FileUtils.uploadFile(file, CertificateRepository.FOLDER);

        if (filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        if (certificate.get("img") != null)
            FileUtils.removeFile(
                    certificate.getString("img"),
                    CertificateRepository.FOLDER
            );

        certificate.put("img", filename);
        certificateRepository.updateOne(certId, set("img", filename));

        return generateSuccessMsg("url", STATICS_SERVER + CertificateRepository.FOLDER + "/" + filename);
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
                .append("_id", new ObjectId())
                .append("params", values)
        );

        if (needUpdate)
            certificateRepository.replaceOne(cert.getObjectId("_id"), cert);

        return JSON_OK;
    }

    public static String editUserInCert(ObjectId certId,
                                       String NID, JSONArray values) {

        Document cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = cert.getList("users", Document.class);
        Document user = Utility.searchInDocumentsKeyVal(users, "NID", NID);

        if (user == null)
            return Utility.generateErr("فرد موردنظر در لیست نفرات گواهی موردنظر موجود ندارد.");

        List<Document> params = cert.getList("params", Document.class);
        if (params.size() != values.length())
            return JSON_NOT_VALID_PARAMS;

        user.put("params", values);
        certificateRepository.replaceOne(cert.getObjectId("_id"), cert);

        return JSON_OK;
    }

    private static String removeUserFromCert(Document cert,
                                             ObjectId certId,
                                             ObjectId id) {

        boolean needUpdate = cert == null;

        if (cert == null)
            cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = cert.getList("users", Document.class);

        Document user = Utility.searchInDocumentsKeyVal(
                users, "_id", id
        );

        if (user == null)
            return JSON_NOT_VALID_ID;

        users.remove(user);

        if (needUpdate)
            certificateRepository.replaceOne(cert.getObjectId("_id"), cert);

        return JSON_OK;
    }

    public static String removeUsersFromCert(ObjectId certId, JSONArray students) {

        Document cert = certificateRepository.findById(certId);

        if (cert == null)
            return JSON_NOT_VALID_ID;

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();


        for (int i = 0; i < students.length(); i++) {

            String id = students.getString(i);

            if(!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            if (removeUserFromCert(cert, null, new ObjectId(id)).contains("nok"))
                excepts.put(i + 1);
            else
                doneIds.put(id);
        }

        if(doneIds.length() > 0)
            certificateRepository.replaceOne(certId, cert);

        return returnRemoveResponse(excepts, doneIds);
    }

    public static String getAll(String title) {

        ArrayList<Document> docs = certificateRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            if (title != null && !doc.getString("title").contains(title))
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("id", doc.getObjectId("_id").toString())
                            .put("title", doc.getString("title"))
                            .put("visibility", doc.getBoolean("visibility"))
                            .put("createdAt", Utility.getSolarDate(doc.getLong("created_at")))
                            .put("studentsCount", doc.containsKey("users") ? doc.getList("users", Document.class).size() : 0)
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String get(ObjectId certificateId) {

        Document certificate = certificateRepository.findById(certificateId);

        if(certificate == null)
            return JSON_NOT_VALID_ID;

        JSONObject jsonObject = new JSONObject()
                .put("createdAt", Utility.getSolarDate(certificate.getLong("created_at")));

        if(certificate.containsKey("users")) {
            List<Document> students = certificate.getList("users", Document.class);
            JSONArray jsonArray = new JSONArray();
            for(Document student : students) {
                jsonArray.put(
                        new JSONObject()
                                .put("id", student.getObjectId("_id").toString())
                                .put("NID", student.getString("NID"))
                                .put("downloadAt", student.containsKey("download_at") ? getSolarDate(student.getLong("download_at")) : "")
                                .put("params", student.getList("params", Object.class))
                );
            }
            jsonObject.put("users", jsonArray);
        }
        else
            jsonObject.put("users", new ArrayList<>());

//        JSONArray studentsJSON = new JSONArray();
//        for(Document student : students)
//            studentsJSON.put();

        for (String key : certificate.keySet()) {

            if (key.equals("created_at") || key.equals("users") || key.equals("params"))
                continue;

            if(key.equals("img"))
                jsonObject.put("img", STATICS_SERVER + CertificateRepository.FOLDER + "/" + certificate.getString("img"));
            else
                jsonObject.put(Utility.camel(key, false), certificate.get(key));
        }


        if(certificate.containsKey("params")) {
            List<Document> params = certificate.getList("params", Document.class);
            JSONArray jsonArray = new JSONArray();
            for(Document param : params) {
                JSONObject jsonObject1 = new JSONObject();
                for(String key : param.keySet()) {
                    jsonObject1.put(Utility.camel(key, false), param.get(key));
                }
                jsonArray.put(jsonObject1);
            }
            jsonObject.put("params", jsonArray);
        }
        else
            jsonObject.put("params", new ArrayList<>());

        return generateSuccessMsg("data", jsonObject);
    }

    public static String remove(JSONArray jsonArray) {


        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if(!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId certificateId = new ObjectId(id);

            Document certificate = certificateRepository.findOneAndDelete(
                    eq("_id", certificateId)
            );

            if(certificate == null) {
                excepts.put(i + 1);
                continue;
            }

            if (certificate.get("img") != null)
                FileUtils.removeFile(
                        certificate.getString("img"),
                        CertificateRepository.FOLDER
                );

            doneIds.put(id);
        }

        return returnRemoveResponse(excepts, doneIds);
    }
}
