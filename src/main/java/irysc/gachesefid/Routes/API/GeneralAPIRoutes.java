package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.Controllers.Config.CityController;
import irysc.gachesefid.Controllers.Finance.Off.OffCodeController;
import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.Finance.TransactionController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/general")
@Validated
public class GeneralAPIRoutes extends Router {

    @GetMapping(value = "/preparePay")
    @ResponseBody
    public String preparePay() {
        return PayPing.pay();
    }

    @PostMapping(value = "/callBackBank")
    public void callBackBank(@RequestPart @NotBlank String refId,
                               @RequestPart @NotBlank String refCode,
                               @RequestPart @NotBlank Long saleOrderId,
                               @RequestPart @NotBlank Long saleRefId
                               ) {
        PayPing.checkPay(refId, refCode, saleOrderId, saleRefId);
    }

    @GetMapping(value = "/getMySummary")
    @ResponseBody
    public String getMySummary(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return UserController.getMySummary(
                getUserWithOutCheckCompleteness(request)
        );
    }

    @GetMapping(value = "/fetchStates")
    @ResponseBody
    public String fetchStates() {
        return CityController.getAll();
    }

    @GetMapping(value = "/fetchSchoolsDigest")
    @ResponseBody
    public String fetchSchoolsDigest(
            @RequestParam(required = false) Boolean justUnsets
    ) {
        return UserController.fetchSchoolsDigest(justUnsets);
    }

    @GetMapping(value = "/getNewAlerts")
    @ResponseBody
    public String getNewAlerts(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AlertController.newAlerts();
    }

    @PostMapping(value = "/checkOffCode")
    @ResponseBody
    public String checkOffCode(HttpServletRequest request,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"code", "for"},
                                       paramsType = {String.class, OffCodeSections.class}
                               ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        JSONObject jsonObject = new JSONObject(jsonStr);
        return OffCodeController.check(
                getUser(request).getObjectId("_id"),
                jsonObject.getString("code"),
                jsonObject.getString("for")
        );
    }

    @GetMapping(value = "/getRecp")
    @ResponseBody
    public String getRecp(HttpServletRequest request,
                          @RequestParam(required = false) @NotBlank @EnumValidator(enumClazz = OffCodeSections.class) String payFor,
                          @RequestParam(required = false) @NotBlank String refId
                          ) {
        return "Ad";
    }

    @GetMapping(value = "/myOffs")
    @ResponseBody
    public String myOffs(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return OffCodeController.offs(
                getUserWithOutCheckCompleteness(request).getObjectId("_id"),
                null, false, false,
                null, null,
                null, null,
                null, null,
                null, null, null, null,
                null, null
        );
    }
}
