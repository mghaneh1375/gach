package irysc.gachesefid.Controllers.DailyAdv;

import org.bson.Document;
import org.json.JSONObject;

import static irysc.gachesefid.Controllers.DailyAdv.DailyAdvController.FOLDER;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class Utility {

    static JSONObject convertToJSON(Document document) {
        return new JSONObject()
                .put("id", document.getObjectId("_id").toString())
                .put("expireAt", getSolarDate(document.getLong("expire_at")))
                .put("title", document.getString("title"))
                .put("path", STATICS_SERVER + FOLDER + "/" + document.getString("filename"));
    }

}
