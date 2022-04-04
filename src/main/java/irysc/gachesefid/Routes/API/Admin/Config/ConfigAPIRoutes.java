package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.ConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.json.JSONObject;
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
                                 optionals = {"initCoin", "initMoney",
                                         "invitationMoney", "invitationCoin",
                                         "minRequestMoney", "advisorPercent",
                                         "namayandeOffPercent",
                                         "schoolOffPercent",
                                         "advisorOffPercent",
                                         "beckDep", "beckAnx",
                                         "CDI", "MBTI", "standardRaven",
                                         "proRaven", "childRaven",
                                         "cattell", "hermans", "LASSI",
                                         "gardner", "izenk", "haland",
                                         "neo", "cannor", "GHQ", "RCMAS",
                                         "topInQuizForCert",
                                         "topInQuizForGift",
                                         "topInQuizGiftCoin",
                                         "topInQuizGiftMoney",
                                         "schoolQuizAttachesMax",
                                         "schoolQuizAttachesJustLink",
                                 },
                                 optionalsType = {
                                         Number.class, Positive.class, Positive.class,
                                         Number.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Number.class, Positive.class,
                                         Positive.class, Boolean.class
                                 }
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.update(new JSONObject(jsonStr));
    }
}
