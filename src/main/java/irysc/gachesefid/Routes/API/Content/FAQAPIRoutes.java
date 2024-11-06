package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.ContentConfigController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/package_content/faq")
@Validated
public class FAQAPIRoutes extends Router {


    @GetMapping(value = "get")
    @ResponseBody
    public String get(HttpServletRequest request
    ) {
        boolean isAdmin = false;
        try {
            isAdmin = Authorization.isAdmin(getUserTokenInfo(request).getAccesses());
        } catch (Exception ignore) {
        }
        return ContentConfigController.getFAQ(isAdmin);
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "question", "answer",
                            "visibility", "priority"
                    },
                    paramsType = {
                            String.class, String.class,
                            Boolean.class, Positive.class
                    }
            ) String jsonStr
    ) {
        return ContentConfigController.store(
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "question", "answer",
                            "visibility", "priority"
                    },
                    paramsType = {
                            String.class, String.class,
                            Boolean.class, Positive.class
                    }
            ) String jsonStr
    ) {
        return ContentConfigController.update(
                id, Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return ContentConfigController.remove(id);
    }
}
