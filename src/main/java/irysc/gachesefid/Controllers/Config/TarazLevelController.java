package irysc.gachesefid.Controllers.Config;

import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.configRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class TarazLevelController {

    public static String add(JSONObject jsonObject) {

        int min = jsonObject.getInt("min");
        int max = jsonObject.getInt("max");
        if(min < 0 || min > max)
            return JSON_NOT_VALID_PARAMS;

        ObjectId newId = new ObjectId();

        Document newDoc = new Document("min", min)
                .append("max", max)
                .append("_id", newId)
                .append("color", jsonObject.getString("color"))
                .append("priority", jsonObject.getInt("priority"));

        Document config = Utility.getConfig();
        List<Document> levels = config.containsKey("taraz_levels") ?
                config.getList("taraz_levels", Document.class) :
                new ArrayList<>();

        levels.add(newDoc);
        config.put("taraz_levels", levels);

        configRepository.replaceOne(config.getObjectId("_id"), config);

        return generateSuccessMsg(
                "_id", newId
        );
    }

    public static String getAll() {

        Document config = Utility.getConfig();

        List<Document> levels = config.containsKey("taraz_levels") ?
                config.getList("taraz_levels", Document.class) :
                new ArrayList<>();

        JSONArray data = new JSONArray();
        for(Document level : levels) {

            data.put(new JSONObject()
                    .put("min", level.getInteger("min"))
                    .put("max", level.getInteger("max"))
                    .put("color", level.getString("color"))
                    .put("priority", level.getInteger("priority"))
            );

        }

        return generateSuccessMsg("data", data);
    }

}
