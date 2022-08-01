package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static irysc.gachesefid.Main.GachesefidApplication.configRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;


public class ConfigController {

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
            if(o instanceof JSONArray)
                o = new ArrayList<>(((JSONArray) o).toList());

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
