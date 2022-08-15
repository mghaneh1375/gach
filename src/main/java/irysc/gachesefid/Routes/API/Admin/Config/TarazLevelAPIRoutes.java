package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Config.TarazLevelController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GradeSchool;
import irysc.gachesefid.Models.KindSchool;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.schoolRepository;

@Controller
@RequestMapping(path = "/api/admin/config/tarazLevel")
@Validated
public class TarazLevelAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String get(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TarazLevelController.getAll();
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return CommonController.removeAll(schoolRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                null
        );
    }

    @PostMapping(value = "create")
    @ResponseBody
    public String create(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"min", "max", "color", "priority"},
                                 paramsType = {
                                         Positive.class, Positive.class,
                                         String.class, Positive.class
                                 }
                         ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TarazLevelController.add(new JSONObject(str));
    }

//    @PutMapping(value = "edit/{id}")
//    @ResponseBody
//    public String edit(HttpServletRequest request,
//                       @PathVariable @ObjectIdConstraint ObjectId id,
//                       @RequestBody @StrongJSONConstraint(
//                               params = {},
//                               paramsType = {},
//                               optionals = {
//                                       "min", "max", "color"
//                               },
//                               optionalsType = {
//                                       Positive.class, Positive.class, String.class
//                               }
//                       ) @NotBlank String str
//    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
//        getAdminPrivilegeUser(request);
//        return UserController.editSchool(schoolId, new JSONObject(str));
//    }
}
