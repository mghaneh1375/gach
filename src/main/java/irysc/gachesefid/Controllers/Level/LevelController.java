package irysc.gachesefid.Controllers.Level;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Level.Utility.returnFirstLevel;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class LevelController {

    // ####################### ADMIN SECTION #####################

    public static String add(JSONObject jsonObject) {
        Document newDoc = new Document("name", jsonObject.getString("name"))
                .append("min_point", jsonObject.getInt("minPoint"))
                .append("max_point", jsonObject.getInt("maxPoint"))
                .append("coin", jsonObject.getNumber("coin").doubleValue());
        levelRepository.insertOne(newDoc);
        return generateSuccessMsg("data", Utility.convertToJSON(newDoc));
    }

    public static String getAll() {
        List<Document> levels = levelRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();
        levels.forEach(level -> jsonArray.put(Utility.convertToJSON(level)));
        return generateSuccessMsg("data", jsonArray);
    }

    public static String update(ObjectId levelId, JSONObject jsonObject) {
        Document level = levelRepository.findById(levelId);
        if(level == null)
            return JSON_NOT_VALID_ID;

        level.put("min_point", jsonObject.getInt("minPoint"));
        level.put("max_point", jsonObject.getInt("maxPoint"));
        level.put("name", jsonObject.getString("name"));
        level.put("coin", jsonObject.getNumber("coin").doubleValue());
        levelRepository.replaceOneWithoutClearCache(levelId, level);
        return generateSuccessMsg("data", Utility.convertToJSON(level));
    }

    public static String remove(ObjectId levelId) {
        levelRepository.deleteOne(levelId);
        return JSON_OK;
    }

    // ######################## PUBLIC SECTION #################

    public static String getMyCurrLevel(ObjectId userId) {
        Document userLevel = userLevelRepository.findBySecKey(userId);
        if(userLevel == null)
            return returnFirstLevel();

        List<Document> levels = userLevel.getList("levels", Document.class);
        if(levels.size() == 0)
            return returnFirstLevel();

        Document currLevel = levels.get(levels.size() - 1);
        return generateSuccessMsg("data",
                new JSONObject()
                        .put("name", currLevel.getString("name"))
                        .put("point", userLevel.getInteger("point"))
                        .put("minPoint", currLevel.getInteger("min_point"))
                        .put("maxPoint", currLevel.getInteger("max_point"))
        );
    }

    synchronized
    public static void checkForUpgrade(ObjectId userId, int point) {

        BasicDBObject updateQuery = null;
        Document userLevel = userLevelRepository.findBySecKey(userId);
        if(userLevel == null) {
            userLevel = new Document("user_id", userId)
                    .append("levels", new ArrayList<>())
                    .append("point", point);
            updateQuery = new BasicDBObject("point", point);
        }
        else if(userLevel.getInteger("point") != point) {
            point += userLevel.getInteger("point");
            userLevel.put("point", point);
            updateQuery = new BasicDBObject("point", point);
        }

        Document level = levelRepository.findOne(and(
                lte("min_point", point),
                gt("max_point", point)
        ), null);

        if(level != null) {
            List<Document> levels = userLevel.getList("levels", Document.class);
            if (levels.size() == 0 ||
                    !levels.get(levels.size() - 1).getObjectId("_id").equals(level.getObjectId("_id"))
            ) {
                levels.add(new Document("name", level.getString("name"))
                        .append("min_point", level.getInteger("min_point"))
                        .append("max_point", level.getInteger("max_point"))
                        .append("_id", level.getObjectId("_id"))
                );
                if(updateQuery == null)
                    updateQuery = new BasicDBObject("levels", levels);
                else
                    updateQuery.append("levels", levels);

                userLevel.put("levels", levels);
                new Thread(() -> {
                    Document user = userRepository.findById(userId);
                    double d = ((Number)user.get("coin")).doubleValue() +
                            ((Number)level.get("coin")).doubleValue();
                    user.put("coin", Math.round((d * 100.0)) / 100.0);
                    userRepository.updateOne(userId, set("coin", user.get("coin")));
                    // todo : send notif
                }).start();
            }
        }

        if(updateQuery != null)
            userLevelRepository.updateOne(userId, new BasicDBObject("$set", updateQuery));
    }
}
