package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.ConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/admin/config/config")
@Validated
public class ConfigAPIRoutes extends Router {

    @GetMapping(path = "/")
    @ResponseBody
    public String get(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.get();
    }

    //todo : json fields in this section

    @PutMapping(path = "/update")
    @ResponseBody
    public String update(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 paramsType = {}, params = {},
                                 optionals = {"init_coin", "init_money",
                                         "invitation_money", "invitation_coin",
                                         "min_request_money", "advisor_percent",
                                         "namayande_off_percent",
                                         "school_off_percent",
                                         "advisor_off_percent",
                                         "beck_dep", "beck_anx",
                                         "CDI", "MBTI", "standard_raven",
                                         "pro_raven", "child_raven",
                                         "cattell", "hermans", "LASSI",
                                         "gardner", "izenk", "haland",
                                         "neo", "cannor", "GHQ", "RCMAS",
                                         "top_in_quiz_for_cert",
                                         "top_in_quiz_for_gift",
                                         "top_in_quiz_gift_coin",
                                         "top_in_quiz_gift_money",
                                 },
                                 optionalsType = {Number.class,
                                         Integer.class, Integer.class,
                                         Number.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Integer.class, Integer.class,
                                         Integer.class, Number.class, Integer.class

                                 }
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.get();
    }
}
