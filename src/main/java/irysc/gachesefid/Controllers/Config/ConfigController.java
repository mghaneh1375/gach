package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.configRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;


public class ConfigController {

    public static String get() {

        Document config = Utility.getConfig();
        JSONObject jsonObject = new JSONObject();

        for(String key : config.keySet()) {

            if(key.equals("_id"))
                continue;

            jsonObject.put(Utility.camel(key, false), config.get(key));

        }

        return Utility.generateSuccessMsg("data", jsonObject);
    }

    public static String update(JSONObject data) {

        Document config = Utility.getConfig();

        for(String key : data.keySet())
            config.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), data.get(key));

        configRepository.replaceOne(config.getObjectId("_id"), config);
        return JSON_OK;
    }
}
