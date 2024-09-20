package irysc.gachesefid.Controllers.Exchange;

import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.exchangeRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class ExchangeController {

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

    public static String getAll() {
        JSONArray jsonArray = new JSONArray();
        exchangeRepository.find(null, null)
                .forEach(document -> jsonArray.put(Utility.convertToJSON(document)));
        return generateSuccessMsg("data", jsonArray);
    }
}
