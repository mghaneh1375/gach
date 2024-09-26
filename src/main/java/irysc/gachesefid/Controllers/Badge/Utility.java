package irysc.gachesefid.Controllers.Badge;

import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Controllers.Badge.BadgeController.FOLDER;
import static irysc.gachesefid.Main.GachesefidApplication.userBadgeRepository;

public class Utility {

    static JSONObject convertToJSON(Document badge, boolean isForAdmin, Boolean hasIt) {
        JSONObject jsonObject = new JSONObject()
                .put("name", badge.getString("name"))
                .put("award", badge.getDouble("award"));

        if (isForAdmin) {
            jsonObject
                    .put("id", badge.getObjectId("_id").toString())
                    .put("priority", badge.getInteger("priority"))
                    .put("lockedImg", StaticValues.STATICS_SERVER + FOLDER + "/" + badge.getString("locked_img"))
                    .put("unlockedImg", StaticValues.STATICS_SERVER + FOLDER + "/" + badge.getString("unlocked_img"))
                    .put("createdAt", irysc.gachesefid.Utility.Utility.getSolarDate(badge.getLong("created_at")))
                    .put("userCount", userBadgeRepository.count(
                            eq("badges._id", badge.getObjectId("_id"))
                    ));
        }
        else {
            jsonObject.put("img", hasIt
                    ? StaticValues.STATICS_SERVER + FOLDER + "/" + badge.getString("unlocked_img")
                    : StaticValues.STATICS_SERVER + FOLDER + "/" + badge.getString("locked_img")
            );
        }

        List<Document> actions =
                badge.getList("actions", Document.class);

        JSONArray actionsJSON = new JSONArray();
        actions.forEach(document -> {
            Action actionEnum = Action.valueOf(document.getString("action").toUpperCase());
            actionsJSON.put(new JSONObject()
                    .put("action", actionEnum.getName())
                    .put("actionFa", actionEnum.getFaTranslate())
                    .put("count", document.get("count"))
            );
        });

        jsonObject.put("actions", actionsJSON);
        return jsonObject;
    }

}
