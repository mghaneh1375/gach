package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.ConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
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

    @GetMapping(path = "/getAll")
    @ResponseBody
    public String get(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.get();
    }

    @GetMapping(path = "/getCert")
    @ResponseBody
    public String getCert(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.getCert();
    }

    @GetMapping(path = "/getShop")
    @ResponseBody
    public String getShop(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.getShop();
    }

    //todo : json fields in this section

    @PutMapping(path = "/update")
    @ResponseBody
    public String update(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 paramsType = {}, params = {},
                                 optionals = {"initCoin", "initMoney",
                                         "inviteMoney", "inviteCoin",
                                         "completeInfoMoney", "completeInfoCoin",
                                         "quizMoney", "quizCoin",
                                         "minRequestMoney", "advisorPercent",
                                         "agentOffPercent", "coinRateCoef",
                                         "schoolOffPercent",
                                         "advisorOffPercent",
                                         "beckDep", "beckAnx",
                                         "CDI", "MBTI", "standardRaven",
                                         "proRaven", "childRaven",
                                         "cattell", "hermans", "LASSI",
                                         "gardner", "izenk", "haland",
                                         "neo", "cannor", "GHQ", "RCMAS",
                                         "topInQuizForCert",
                                         "schoolQuizAttachesMax",
                                         "schoolQuizAttachesJustLink",
                                         "maxStudentQuizPerDay",
                                         "quizPerStudentPrice",
                                         "giftPeriod", "maxAppGiftSlot",
                                         "maxWebGiftSlot", "appGiftDays",
                                         "webGiftDays", "firstRankCertId",
                                         "secondRankCertId", "thirdRankCertId",
                                         "forthRankCertId", "fifthRankCertId",
                                         "minQuestionForCustomQuiz", "moneyRateCoef",
                                         "maxQuestionPerQuiz", "hwPerStudentPrice",
                                         "minAdvicePrice", "maxVideoCallPerMonth",
                                         "minBuyAmountForShop", "percentOfShopBuy",
                                         "createShopOffVisibility"
                                 },
                                 optionalsType = {
                                         Number.class, Positive.class, Positive.class,
                                         Number.class, Positive.class, Number.class,
                                         Positive.class, Number.class, Positive.class,
                                         Positive.class, Positive.class, Number.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class, Positive.class,
                                         Boolean.class, Positive.class,
                                         Positive.class, Positive.class,
                                         Positive.class, Positive.class,
                                         JSONArray.class, JSONArray.class,
                                         ObjectId.class, ObjectId.class, ObjectId.class,
                                         ObjectId.class, ObjectId.class, Integer.class,
                                         Number.class, Positive.class, Positive.class,
                                         Positive.class, Positive.class,
                                         Positive.class, Positive.class, Boolean.class
                                 }
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ConfigController.update(Utility.convertPersian(new JSONObject(jsonStr)));
    }
}
