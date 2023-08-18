package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.certificateRepository;
import static irysc.gachesefid.Main.GachesefidApplication.configRepository;
import static irysc.gachesefid.Utility.StaticValues.*;


public class ConfigController {

    public static String getShop() {

        Document config = Utility.getConfig();
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("createShopOffVisibility", config.getOrDefault("create_shop_off_visibility", true));
        jsonObject.put("minBuyAmountForShop", config.getOrDefault("min_buy_amount_for_shop", "").toString());
        jsonObject.put("percentOfShopBuy", config.getOrDefault("percent_of_shop_buy", "").toString());

        return Utility.generateSuccessMsg(
                "data", jsonObject
        );
    }

    public static String getCert() {

        Document config = Utility.getConfig();
        JSONObject jsonObject = new JSONObject();

        ArrayList<Document> certifications = certificateRepository.find(
                null,
                new BasicDBObject("_id", 1)
                        .append("title", 1)
        );

        JSONArray all = new JSONArray();

        for(Document cert : certifications) {

            JSONObject jsonObject1 = new JSONObject()
                    .put("id", cert.getObjectId("_id").toString())
                    .put("item", cert.getString("title"));

            all.put(jsonObject1);
        }

        jsonObject.put("all", all);
        jsonObject.put("first", config.getOrDefault("first_rank_cert_id", "").toString());
        jsonObject.put("second", config.getOrDefault("second_rank_cert_id", "").toString());
        jsonObject.put("third", config.getOrDefault("third_rank_cert_id", "").toString());
        jsonObject.put("forth", config.getOrDefault("forth_rank_cert_id", "").toString());
        jsonObject.put("fifth", config.getOrDefault("fifth_rank_cert_id", "").toString());

        return Utility.generateSuccessMsg(
                "data", jsonObject
        );
    }

    public static String get() {

        Document config = Utility.getConfig();
        JSONObject jsonObject = new JSONObject();

        for(String key : config.keySet()) {

            if(key.equals("_id") || key.equalsIgnoreCase("maxWebGiftSlot") ||
                    key.equalsIgnoreCase("maxAppGiftSlot") ||
                    key.equalsIgnoreCase("appGiftDays") ||
                    key.equalsIgnoreCase("webGiftDays")
            )
                continue;

            boolean hasLittleChar = false;
            for(int i = 0; i < key.length(); i++) {
                if(!Character.isUpperCase(key.charAt(i))) {
                    hasLittleChar = true;
                    break;
                }
            }

            if(hasLittleChar)
                jsonObject.put(Utility.camel(key, false), config.get(key));
            else
                jsonObject.put(key, config.get(key));

        }

        return Utility.generateSuccessMsg("data", jsonObject);
    }

    public static String update(JSONObject data) {

        Document config = Utility.getConfig();

        for(String key : data.keySet()) {

            Object o = data.get(key);

            if (key.contains("CertId")) {
                if (!certificateRepository.exist(
                        eq("_id", new ObjectId(o.toString()))
                ))
                    return JSON_NOT_VALID_PARAMS;
            }
        }

        for(String key : data.keySet()) {

            Object o = data.get(key);
            if(o instanceof JSONArray)
                o = new ArrayList<>(((JSONArray) o).toList());

            if (key.contains("CertId"))
                o = new ObjectId(o.toString());

            boolean hasLittleChar = false;
            for(int i = 0; i < key.length(); i++) {
                if(!Character.isUpperCase(key.charAt(i))) {
                    hasLittleChar = true;
                    break;
                }
            }
            if(hasLittleChar)
                config.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), o);
            else
                config.put(key, o);
        }

        configRepository.replaceOne(config.getObjectId("_id"), config);
        return JSON_OK;
    }
}
