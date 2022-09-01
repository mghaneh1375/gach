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
import org.springframework.http.MediaType;
import java.util.Map;


@Controller
@RequestMapping(path = "/api/general")
@Validated
public class GeneralAPIRoutes extends Router {


	public class A {
		public String RefId;
		public String ResCode;
		public Long SaleOrderId;
		public Long SaleReferenceId;

		public void print() {

			System.out.println(RefId);
			System.out.println(ResCode);
			System.out.println(SaleOrderId);
			System.out.println(SaleReferenceId);
		};
	}

    @GetMapping(value = "/preparePay")
    @ResponseBody
    public String preparePay() {
        return PayPing.pay();
    }


    @PostMapping(value = "/callBackBank",
		consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
    )
    @ResponseBody
    public String callBackBank(
	@RequestParam Map<String, Object> name
    ) {
	String refId = null, resCode = null;
	Long saleOrderId = null, saleReferenceId = null;

	for(String key : name.keySet()) {

		if(key.equalsIgnoreCase("RefId"))
			refId = name.get(key).toString();
		else if(key.equalsIgnoreCase("ResCode"))
			resCode = name.get(key).toString();
		else if(key.equalsIgnoreCase("saleOrderId"))
			saleOrderId = Long.parseLong(name.get(key).toString());
		else if(key.equalsIgnoreCase("saleReferenceId"))
			saleReferenceId = Long.parseLong(name.get(key).toString());


	}
	System.out.println("Salam");
	System.out.println(refId);
	System.out.println(resCode);
	System.out.println(saleOrderId);
	System.out.println(name.keySet().size());

        PayPing.checkPay(refId, resCode, saleOrderId, saleReferenceId);

	return "ok";
    }

/*
    @PostMapping(value = "/callBackBank")
    public void callBackBank(@RequestBody Map<String, Object> paramMap) {
	System.out.println("Salam");
	System.out.println(paramMap.keySet().size());

//        PayPing.checkPay(a.refId, a.refCode, a.saleOrderId, a.saleReferenceId);
    }

*/

/*
    @PostMapping(value = "/callBackBank")
    public void callBackBank(@RequestBody A a) {
	System.out.println("Salam");
	a.print();
        PayPing.checkPay(a.refId, a.refCode, a.saleOrderId, a.saleReferenceId);
    }
*/


/*
    @PostMapping(value = "/callBackBank")
    public void callBackBank(
				@RequestBody String RefId,
				@RequestBody String ResCode,
				@RequestBody Long SaleOrderId,
				@RequestBody(required=false) Long SaleReferenceId
    ) {
	System.out.println("Salam");
        PayPing.checkPay(RefId, ResCode, SaleOrderId, SaleReferenceId);
    }
*/

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
