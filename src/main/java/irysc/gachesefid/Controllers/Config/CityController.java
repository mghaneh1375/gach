package irysc.gachesefid.Controllers.Config;

import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.cityRepository;
import static irysc.gachesefid.Main.GachesefidApplication.stateRepository;

public class CityController {

    private static JSONArray cachedAll = null;

    public static String getAll() {

        if(cachedAll != null)
            return Utility.generateSuccessMsg("data", cachedAll);

        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> docs = stateRepository.find(null, null);

        for(Document doc : docs) {

            JSONArray citiesJSON = new JSONArray();

            ArrayList<Document> cities = cityRepository.find(
                    eq("state_id", doc.getObjectId("_id")),
                    null
            );

            for(Document city : cities) {
                citiesJSON.put(new JSONObject()
                        .put("id", city.getObjectId("_id").toString())
                        .put("name", city.getString("name"))
                );
            }

            jsonArray.put(new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("name", doc.getString("name"))
                    .put("cities", citiesJSON)
            );
        }

        cachedAll = jsonArray;
        return Utility.generateSuccessMsg("data", cachedAll);
    }

}
