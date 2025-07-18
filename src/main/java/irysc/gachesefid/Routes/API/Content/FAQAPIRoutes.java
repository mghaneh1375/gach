package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.ContentConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
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
    public String get(
            HttpServletRequest request,
            @RequestParam(required = false, value = "contentId") ObjectId contentId
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return ContentConfigController.getFAQ(isAdmin, contentId);
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            HttpServletRequest request,
            @RequestParam(required = false, value = "contentId") ObjectId contentId,
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
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.store(
                contentId,
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestParam(required = false, name = "contentId") ObjectId contentId,
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
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.update(id, Utility.convertPersian(new JSONObject(jsonStr)), contentId);
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestParam(required = false, name = "contentId") ObjectId contentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.remove(id, contentId);
    }
}
