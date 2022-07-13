package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.schoolRepository;

@Controller
@RequestMapping(path = "/api/admin/config/school")
@Validated
public class SchoolAPIRoutes extends Router {

    @GetMapping(value = "/fetchSchools")
    @ResponseBody
    public String fetchSchools(HttpServletRequest request,
                               @RequestParam(value = "grade", required = false) String grade,
                               @RequestParam(value = "kind", required = false) String kind,
                               @RequestParam(value = "city", required = false) String city
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return UserController.fetchSchools(grade, kind, city);
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
}
