package irysc.gachesefid.Controllers.Point;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.Action;
import org.bson.Document;
import org.json.JSONObject;

public class Utility {

    static JSONObject convertToJSON(Document point) {
        return new JSONObject()
                .put("point", point.getInteger("point"))
                .put("action", point.getString("action"))
                .put("actionFa", Action.valueOf(point.getString("action").toUpperCase()).getFaTranslate())
                .put("id", point.getObjectId("_id").toString());
    }

    static String getActionTranslate(String action) {
        try {
            return Action.valueOf(action.toUpperCase()).getFaTranslate();
        }
        catch (Exception x) {
            throw new InvalidFieldsException("فعالیت مدنظر نامعتبر است");
        }
    }

}
