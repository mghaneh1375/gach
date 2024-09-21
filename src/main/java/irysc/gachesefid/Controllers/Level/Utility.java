package irysc.gachesefid.Controllers.Level;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.levelRepository;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class Utility {
    public static JSONObject convertToJSON(Document level) {
        return new JSONObject()
                .put("id", level.getObjectId("_id").toString())
                .put("name", level.getString("name"))
                .put("coin", level.get("coin"))
                .put("minPoint", level.getInteger("min_point"))
                .put("maxPoint", level.getInteger("max_point"));
    }

    static String returnFirstLevel() {
        Document level = levelRepository.findOne(eq("min_point", 0),
                new BasicDBObject("name", 1).append("min_point", 1)
                        .append("max_point", 1)
        );
        if(level == null)
            return generateSuccessMsg("data", null);

        return generateSuccessMsg("data",
                new JSONObject()
                        .put("name", level.getString("name"))
                        .put("point", 0)
                        .put("minPoint", level.getInteger("min_point"))
                        .put("maxPoint", level.getInteger("max_point"))
        );
    }
}
