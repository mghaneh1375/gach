package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Utility.Utility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/api/general")
@Validated
public class GeneralAPIRoutes {

    @GetMapping(value = "/fetchStates")
    @ResponseBody
    public String fetchStates() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject()
                .put("name", "یزد")
                .put("id", "213")
                .put("cities", new JSONArray()
                        .put(new JSONObject().put("name", "یزد").put("id", "2"))
                        .put(new JSONObject().put("name", "اردکان").put("id", "3"))
                )
        );
        jsonArray.put(new JSONObject()
                .put("name", "تهران")
                .put("id", "235")
                .put("cities", new JSONArray()
                        .put(new JSONObject().put("name", "تهران").put("id", "2"))
                        .put(new JSONObject().put("name", "ری").put("id", "3"))
                )
        );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

}
