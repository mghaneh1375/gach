package irysc.gachesefid.Controllers.Advisor;


import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.adviseTagRepository;
import static irysc.gachesefid.Main.GachesefidApplication.lifeStyleTagRepository;
import static irysc.gachesefid.Utility.Utility.generateErr;

public class AdvisorController {

    public static String createTag(Common db, JSONObject jsonObject) {

        if (db.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        return db.insertOneWithReturn(
                new Document("label", jsonObject.getString("label"))
        );
    }

    public static String remove(Common db, JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if(!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = db.findOneAndUpdate(
                        new ObjectId(id),
                        set("deleted_at", System.currentTimeMillis())
                );

                if(tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String getAllTags(Common db) {

        ArrayList<Document> tags = db.find(
                exists("deleted_at", false), null
        );
        JSONArray jsonArray = new JSONArray();

        for (Document tag : tags)
            jsonArray.put(new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"))
            );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

}
