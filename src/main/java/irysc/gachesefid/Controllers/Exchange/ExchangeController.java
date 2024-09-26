package irysc.gachesefid.Controllers.Exchange;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Finance.Off.OffCodeController;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.exchangeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class ExchangeController {

    // ######################### ADMIN SECTION ####################

    public static String store(JSONObject jsonObject) {
        Document newDoc = new Document("needed_coin", ((Number)jsonObject.get("neededCoin")).doubleValue());
        if (jsonObject.has("money"))
            newDoc.append("section", "money")
                    .append("reward_amount", jsonObject.getInt("money"));
        else {
            if (!jsonObject.has("section") ||
                    !EnumValidatorImp.isValid(jsonObject.getString("section"), OffCodeSections.class)
            )
                return JSON_NOT_VALID_PARAMS;

            newDoc
                    .append("section", jsonObject.getString("section"))
                    .append("reward_amount", jsonObject.getInt("offCodeAmount"))
                    .append("is_percent", jsonObject.getBoolean("isPercent"));
        }

        exchangeRepository.insertOne(newDoc);
        return generateSuccessMsg("data", Utility.convertToJSON(newDoc));
    }

    public static String remove(ObjectId id) {
        exchangeRepository.deleteOne(id);
        return JSON_OK;
    }

    // ################################## PUBLIC SECTION ##########################

    public static String getAll() {
        JSONArray jsonArray = new JSONArray();
        exchangeRepository.find(null, null)
                .forEach(document -> jsonArray.put(Utility.convertToJSON(document)));
        return generateSuccessMsg("data", jsonArray);
    }

    public static String getReward(Document user, ObjectId exchangeId) {

        Document exchange = exchangeRepository.findById(exchangeId);
        if(exchange == null)
            return JSON_NOT_VALID_ID;

        if(exchange.getDouble("needed_coin") > ((Number)user.get("coin")).doubleValue())
            return generateErr("برای دریافت جایزه مدنظر باید حداقل " + exchange.getDouble("needed_coin") + " ایکس پول داشته باشید");

        BasicDBObject update = new BasicDBObject();
        if(exchange.getString("section").equals("money")) {
            user.put("money",
                    Math.round((((Number)user.get("money")).doubleValue() + exchange.getInteger("reward_amount")) * 100.0) / 100.0
            );
            update.append("money", user.get("money"));
        }
        else {
            JSONObject res = new JSONObject(OffCodeController.store(new JSONObject()
                    .put("expireAt",
                            System.currentTimeMillis() + ONE_DAY_MIL_SEC * 120
                    )
                    .put("type", (Boolean) exchange.getOrDefault("is_percent", true) ? "percent" : "value")
                    .put("amount", exchange.getInteger("reward_amount"))
                    .put("section", exchange.getString("section"))
                    .put("items", new JSONArray().put(user.getString("NID")))
            ));

            if(!res.getString("status").equalsIgnoreCase("ok"))
                return JSON_NOT_UNKNOWN;
        }

        user.put("coin",
                Math.round((((Number)user.get("coin")).doubleValue() - exchange.getDouble("needed_coin")) * 100.0) / 100.0
        );
        update.append("coin", user.get("coin"));

        userRepository.updateOne(
                user.getObjectId("_id"),
                new BasicDBObject("$set", update)
        );

        return JSON_OK;
    }
}
