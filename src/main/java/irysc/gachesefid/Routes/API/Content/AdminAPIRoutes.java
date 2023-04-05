package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.AdminContentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/package_content/admin")
@Validated
public class AdminAPIRoutes extends Router {

    @GetMapping(value = "buyers/{id}")
    @ResponseBody
    public String buyers(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.buyers(id);
    }


    @PutMapping(value = "force_registry/{id}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items", "paid"},
                                        paramsType = {JSONArray.class, Positive.class},
                                        optionals = {},
                                        optionalsType = {}
                                ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        return AdminContentController.forceRegistry(id, jsonObject.getJSONArray("items"), jsonObject.getInt("paid"));
    }


    @DeleteMapping(value = "forceFire/{id}")
    @ResponseBody
    public String forceFire(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId id,
                            @RequestBody @StrongJSONConstraint(
                                    params = {"items"},
                                    paramsType = {JSONArray.class}
                            ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.forceFire(id, new JSONObject(jsonStr).getJSONArray("items"));
    }
}
