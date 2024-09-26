package irysc.gachesefid.Controllers;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.GiftTarget;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;

public class AlertController {

    public static String newAlerts() {

        JSONArray jsonArray= new JSONArray();
        for(String itr : newThingsCache.keySet()) {
            if(newThingsCache.get(itr) > 0)
                jsonArray.put(new JSONObject()
                        .put("key", itr)
                        .put("value", newThingsCache.get(itr))
                );
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    private static ObjectId getGifts(ObjectId userId) throws InvalidFieldsException {

        long curr = System.currentTimeMillis();
        Document config = Utility.getConfig();
        String mode = "site";
        boolean isForSite = mode.equalsIgnoreCase("site");

        if(
                (isForSite && !config.containsKey("web_gift_days")) ||
                        (!isForSite && !config.containsKey("app_gift_days"))
        )
            throw new InvalidFieldsException("mode is not valid");

        ArrayList<Document> allAvailables = new ArrayList<>();
        List<Document> dates = isForSite ? config.getList("web_gift_days", Document.class) :
                config.getList("app_gift_days", Document.class);

        for(Document itr : dates) {

            if (curr < itr.getLong("date"))
                continue;

            if (curr - itr.getLong("date") > ONE_DAY_MIL_SEC)
                continue;

            Document tmp = userGiftRepository.findBySecKey(itr.getObjectId("_id").toString() + "_" + userId.toString());

            if(tmp != null && !tmp.getString("status").equalsIgnoreCase("finish"))
                return tmp.getObjectId("_id");

            if(tmp == null)
                allAvailables.add(itr);
        }

        for(Document itr : allAvailables) {

            if(itr.getString("target").equalsIgnoreCase(GiftTarget.PUBLIC.getName())) {
                return userGiftRepository.insertOneWithReturnId(new Document("status", "init")
                        .append("ref_id", itr.getObjectId("_id"))
                        .append("unique_key", itr.getObjectId("_id").toString() + "_" + userId.toString())
                        .append("user_id", userId)
                        .append("mode", mode)
                        .append("expire_at", itr.getLong("date") + ONE_DAY_MIL_SEC)
                );
            }


            if(itr.containsKey("ref_id")) {
                if(itr.getString("target").equalsIgnoreCase(GiftTarget.PACKAGE.getName())) {
                    if(!contentRepository.exist(and(
                            eq("_id", itr.getObjectId("ref_id")),
                            in("users._id", userId)
                    )))
                        continue;
                }
                else if(itr.getString("target").equalsIgnoreCase(GiftTarget.QUIZ.getName())) {
                    if(!iryscQuizRepository.exist(and(
                            eq("_id", itr.getObjectId("ref_id")),
                            in("students._id", userId)
                    )))
                        continue;
                }
            }
            else {
                if(itr.getString("target").equalsIgnoreCase(GiftTarget.ALL_PACKAGE.getName())) {
                    if(!contentRepository.exist(
                            in("users._id", userId)
                    ))
                        continue;
                }
                else if(itr.getString("target").equalsIgnoreCase(GiftTarget.ALL_QUIZ.getName())) {
                    if(!iryscQuizRepository.exist(
                            in("students._id", userId)
                    ))
                        continue;
                }
            }

            return userGiftRepository.insertOneWithReturnId(new Document("status", "init")
                    .append("ref_id", itr.getObjectId("_id"))
                    .append("unique_key", itr.getObjectId("_id").toString() + "_" + userId.toString())
                    .append("user_id", userId)
                    .append("mode", mode)
                    .append("expire_at", itr.getLong("date") + ONE_DAY_MIL_SEC)
            );
        }

        throw new InvalidFieldsException("not found");
    }

    public static String getMyAlerts(Document user) {

        JSONObject data = new JSONObject();

        try {
            ObjectId refId = getGifts(user.getObjectId("_id"));
            data.put("gift_id", refId.toString());
        } catch (InvalidFieldsException ignore) {}

        if(!user.containsKey("events"))
            return Utility.generateSuccessMsg("data", data.put("events", new JSONArray()));

        List<Document> events = user.getList("events", Document.class);
        JSONArray jsonArray = new JSONArray();

        for (int i = events.size() - 1; i >= 0; i--) {

            Document event = events.get(i);

            if(!event.getBoolean("seen")) {

                Document notif = notifRepository.findById(event.getObjectId("notif_id"));
                if(notif == null)
                    continue;

                jsonArray.put(new JSONObject()
                        .put("value", notif.getString("title"))
                        .put("id", event.getObjectId("notif_id").toString())
                        .put("key", "notif")
                );
            }
        }

        return Utility.generateSuccessMsg("data", data.put("events", jsonArray));
    }
}
