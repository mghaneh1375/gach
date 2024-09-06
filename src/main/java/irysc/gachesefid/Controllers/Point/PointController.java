package irysc.gachesefid.Controllers.Point;

import irysc.gachesefid.Controllers.Level.LevelController;
import irysc.gachesefid.Models.Action;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.pointRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userPointRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class PointController {

    // ####################### ADMIN SECTION #####################

    public static String add(JSONObject jsonObject) {
        try {
            Utility.getActionTranslate(
                    jsonObject.getString("action")
            );
            if(pointRepository.findBySecKey(jsonObject.getString("action").toLowerCase()) != null)
                return generateErr("این متریک قبلا تعریف شده است");
            Document newDoc = new Document("action", jsonObject.getString("action"))
                    .append("point", jsonObject.getInt("point"));
            pointRepository.insertOne(newDoc);
            return generateSuccessMsg("data", Utility.convertToJSON(newDoc));
        } catch (Exception ex) {
            return generateErr(ex.getMessage());
        }
    }

    public static String getAll() {
        List<Document> points = pointRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();
        points.forEach(point -> jsonArray.put(Utility.convertToJSON(point)));
        return generateSuccessMsg("data", jsonArray);
    }

    public static String update(ObjectId pointId, JSONObject jsonObject) {
        Document point = pointRepository.findById(pointId);
        if (point == null)
            return JSON_NOT_VALID_ID;
        try {
            Utility.getActionTranslate(
                    jsonObject.getString("action")
            );
            point.put("point", jsonObject.getInt("point"));
            point.put("action", jsonObject.getString("action"));
            pointRepository.replaceOneWithoutClearCache(pointId, point);
            return generateSuccessMsg("data", Utility.convertToJSON(point));
        } catch (Exception ex) {
            return generateErr(ex.getMessage());
        }
    }

    public static String remove(ObjectId pointId) {
        pointRepository.deleteOne(pointId);
        return JSON_OK;
    }

    public static String getActions() {
        JSONArray jsonArray = new JSONArray();
        Arrays.stream(Action.values()).forEach(action -> jsonArray.put(new JSONObject()
                .put("action", action.getName())
                .put("actionFa", action.getFaTranslate())
        ));
        return generateSuccessMsg("data", jsonArray);
    }

    // ####################### PUBLIC SECTION #####################

    synchronized
    public static void addPointForAction(
            ObjectId userId, Action action,
            Object ref, Object additional
    ) {

        Document point = pointRepository.findBySecKey(action.getName());
        if(point == null) return;

        if(userPointRepository.exist(
                and(
                        eq("user_id", userId),
                        eq("action", action.getName()),
                        eq("ref", ref),
                        eq("additional", additional)
                )
        )) return;

        userPointRepository.insertOne(
                new Document("user_id", userId)
                        .append("action", action.getName())
                        .append("additional", additional)
                        .append("ref", ref)
                        .append("point", point.getInteger("point"))
                        .append("created_at", System.currentTimeMillis())
        );

        LevelController.checkForUpgrade(userId, point.getInteger("point"));
    }
}
