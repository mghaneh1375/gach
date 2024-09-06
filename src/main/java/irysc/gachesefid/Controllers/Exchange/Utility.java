package irysc.gachesefid.Controllers.Exchange;

import org.bson.Document;
import org.json.JSONObject;

public class Utility {

    public static JSONObject convertToJSON(Document exchange) {
        JSONObject jsonObject = new JSONObject()
                .put("id", exchange.getObjectId("_id").toString())
                .put("neededCoin", exchange.getDouble("needed_coin"))
                .put("section", exchange.getString("section"))
                .put("rewardAmount", exchange.getInteger("reward_amount"));
        if(exchange.containsKey("is_percent"))
            jsonObject.put("isPercent", exchange.getBoolean("is_percent"));

        return jsonObject;
    }
}
