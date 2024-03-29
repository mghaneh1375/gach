package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GradeSchool;
import irysc.gachesefid.Models.KindSchool;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.schoolRepository;

@Controller
@RequestMapping(path = "/api/admin/config/school")
@Validated
public class SchoolAPIRoutes extends Router {

    @GetMapping(value = "/fetchSchools")
    @ResponseBody
    public String fetchSchools(HttpServletRequest request,
                               @RequestParam(value = "state", required = false) ObjectId stateId,
                               @RequestParam(value = "city", required = false) ObjectId cityId,
                               @RequestParam(value = "grade", required = false) String grade,
                               @RequestParam(value = "hasUser", required = false) Boolean hasUser,
                               @RequestParam(value = "kind", required = false) String kind
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return UserController.fetchSchools(grade, kind, cityId, stateId, hasUser, isAdmin);
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

    @PostMapping(value = "add")
    @ResponseBody
    public String addSchool(HttpServletRequest request,
                            @RequestBody @JSONConstraint(
                                    params = {"name", "cityId", "grade", "kind"},
                                    optionals = {"address", "tel", "bio", "site"}
                            ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return UserController.addSchool(new JSONObject(str));
    }

    @PutMapping(value = "edit/{schoolId}")
    @ResponseBody
    public String editSchool(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId schoolId,
                            @RequestBody @StrongJSONConstraint(
                                    params = {},
                                    paramsType = {},
                                    optionals = {
                                            "name", "cityId", "grade", "kind",
                                            "address", "tel", "bio", "site"
                                    },
                                    optionalsType = {
                                            String.class, ObjectId.class, GradeSchool.class,
                                            KindSchool.class, String.class, Positive.class,
                                            String.class, String.class
                                    }
                            ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return UserController.editSchool(schoolId,  new JSONObject(str));
    }
}
