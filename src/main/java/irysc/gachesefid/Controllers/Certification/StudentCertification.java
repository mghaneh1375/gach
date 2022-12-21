package irysc.gachesefid.Controllers.Certification;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.certificateRepository;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class StudentCertification {

    public static String getMyCerts(String NID) {

        ArrayList<Document> docs = certificateRepository.find(
                and(
                        in("users.NID", NID),
                        eq("visibility", true)
                ),
                new BasicDBObject("_id", 1).append("title", 1)
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("title", doc.getString("title"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static File issueMyCert(ObjectId certId, String NID) {

        Document certificate = certificateRepository.findById(certId);

        if (certificate == null || !certificate.getBoolean("visibility"))
            return null;

        List<Document> users = certificate.getList("users", Document.class);
        Document userCert = Utility.searchInDocumentsKeyVal(
                users, "NID", NID
        );

        if (userCert == null)
            return null;

        userCert.put("download_at", System.currentTimeMillis());
        certificateRepository.replaceOne(certId, certificate);

        return PDFUtils.getCertificate(
                certificate.getList("params", Document.class),
                userCert.getList("params", String.class),
                certificate.getString("img"),
                certificate.getBoolean("is_landscape"),
                certificate.getInteger("qr_x"),
                certificate.getInteger("qr_y"),
                certificate.getInteger("qr_size"),
                certId
        );
    }

}
