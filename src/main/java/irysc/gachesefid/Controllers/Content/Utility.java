package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

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
                .put("duration", doc.get("duration"));

        if(doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if(isAdmin) {
//            jsonObject.put("visibility", doc.getBoolean("visibility"))
//                    .put("");
        }

        return jsonObject;
    }

}
