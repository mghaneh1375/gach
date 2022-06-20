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
                .put("state", "یزد")
                .put("cities", new JSONArray()
                        .put("یزد")
                        .put("اردکان")
                )
        );
        jsonArray.put(new JSONObject()
                .put("state", "تهران")
                .put("cities", new JSONArray()
                        .put("تهران")
                        .put("ری")
                )
        );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

}
