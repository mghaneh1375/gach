package irysc.gachesefid.Controllers.Advisor;


import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Utility {

    static JSONObject convertToJSONDigest(Document advisor) {

        JSONObject jsonObject = new JSONObject()
                .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                .put("acceptStd", advisor.getOrDefault("accept_std", true))
                .put("stdCount", ((List<Document>)advisor.getOrDefault("students", new ArrayList<>())).size())
                .put("rate", advisor.getOrDefault("rate", 0))
                .put("bio", advisor.getString("bio"))
                .put("id", advisor.getObjectId("_id").toString())
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic"))
                ;

        return jsonObject;
    }

}
