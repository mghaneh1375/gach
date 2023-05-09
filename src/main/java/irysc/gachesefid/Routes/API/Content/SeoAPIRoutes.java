package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.ContentConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
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
@RequestMapping(path = "/api/package_content/seo")
@Validated
public class SeoAPIRoutes extends Router {

    @GetMapping(value = {"get/{packageId}", "get"})
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable(required = false) ObjectId packageId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.getSeo(packageId);
    }

    @PostMapping(value = {"store/{packageId}", "store"})
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable(required = false) ObjectId packageId,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "key", "value"
                                },
                                paramsType = {
                                        String.class, Object.class,
                                }
                        ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.storeSeo(packageId, Utility.convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id,
                         @RequestBody @StrongJSONConstraint(
                                 params = {
                                         "key"
                                 },
                                 paramsType = {
                                         String.class
                                 }
                         ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentConfigController.removeSeo(
                id, new JSONObject(jsonStr).getString("key")
        );
    }

}
