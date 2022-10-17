package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.Controllers.Config.CityController;
import irysc.gachesefid.Controllers.ContentController;
import irysc.gachesefid.Controllers.Finance.Off.OffCodeController;
import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.Finance.TransactionController;
import irysc.gachesefid.Controllers.Question.QuestionController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.ExchangeMode;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.util.Map;


@Controller
@RequestMapping(path = "/api/general")
@Validated
public class GeneralAPIRoutes extends Router {


    @PostMapping(value = "/chargeAccount")
    @ResponseBody
    public String chargeAccount(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"amount"},
                                        paramsType = {Integer.class}
                                ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return PayPing.chargeAccount(
                getUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getInt("amount")
        );
    }


    @PostMapping(value = "/exchange")
    @ResponseBody
    public String exchange(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"amount", "mode"},
                                   paramsType = {Number.class, ExchangeMode.class}
                           ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {

        Document user = getUser(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));

        return PayPing.exchange(
                user.getObjectId("_id"),
                ((Number) user.get("money")).doubleValue(),
                user.getDouble("coin"),
                jsonObject.getNumber("amount").doubleValue(),
                jsonObject.getString("mode")
        );
    }


    @PostMapping(value = "/callBackBank",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
    )
    @ResponseBody
    public ModelAndView callBackBank(
            @RequestParam Map<String, Object> name
    ) {
        String refId = null, resCode = null;
        Long saleOrderId = null, saleReferenceId = null;

        for (String key : name.keySet()) {

            if (key.equalsIgnoreCase("RefId"))
                refId = name.get(key).toString();
            else if (key.equalsIgnoreCase("ResCode"))
                resCode = name.get(key).toString();
            else if (key.equalsIgnoreCase("saleOrderId"))
                saleOrderId = Long.parseLong(name.get(key).toString());
            else if (key.equalsIgnoreCase("saleReferenceId"))
                saleReferenceId = Long.parseLong(name.get(key).toString());
        }

        ModelAndView modelAndView = new ModelAndView();

        String frontEndUrl = "https://e.irysc.com/";

        if (refId == null || resCode == null) {
            modelAndView.addObject("status", "fail");
        } else {

            String[] p = PayPing.checkPay(refId, resCode, saleOrderId, saleReferenceId);
            if (p == null)
                modelAndView.addObject("status", "fail");
            else {

                String referenceId = p[0];

                if (referenceId == null)
                    modelAndView.addObject("status", "fail");
                else {

                    String section = p[1];

                    modelAndView.addObject("status", "success");
                    modelAndView.addObject("refId", referenceId);
                    modelAndView.addObject("section", section);

                    String transactionId = p[2];

                    modelAndView.addObject("financeUrl", frontEndUrl + "financeHistory");
                    modelAndView.addObject("getRecpUrl", frontEndUrl + "invoice/" + transactionId);
                    modelAndView.addObject("myQuizzesUrl",
                            section.equalsIgnoreCase(OffCodeSections.GACH_EXAM.getName()) ?
                                    frontEndUrl + "myIRYSCQuizzes" :
                                    section.equalsIgnoreCase(OffCodeSections.BANK_EXAM.getName()) ?
                                            frontEndUrl + "myCustomQuizzes" : frontEndUrl
                    );
                }
            }
        }

        modelAndView.addObject("homeUrl", frontEndUrl);

        modelAndView.setViewName("transaction");
        return modelAndView;
    }

    @GetMapping(value = "/fetchInvoice/{refId}")
    @ResponseBody
    public String fetchInvoice(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId refId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return TransactionController.fetchInvoice(
                getUser(request).getObjectId("_id"), refId
        );
    }

    @GetMapping(value = "/getMySummary")
    @ResponseBody
    public String getMySummary(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return UserController.getMySummary(
                getUserWithOutCheckCompleteness(request)
        );
    }

    @GetMapping(value = "/getRankingList")
    @ResponseBody
    public String getRankingList(
            @RequestParam(required = false, value = "gradeId") ObjectId gradeId
    ) {
        return UserController.getRankingList(gradeId);
    }

    @GetMapping(value = "/getSiteStats")
    @ResponseBody
    public String getSiteStats() {
        return UserController.getSiteSummary();
    }

    @GetMapping(value = "/getQuestionTagsExcel")
    @ResponseBody
    public void getQuestionTagsExcel(
            HttpServletResponse response
    ) {

        try {
            ByteArrayInputStream byteArrayInputStream = QuestionController.getQuestionTagsExcel();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=tags.xlsx");
            IOUtils.copy(byteArrayInputStream, response.getOutputStream());
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }
    }


    @GetMapping(value = "/getAllFlags")
    @ResponseBody
    public String getAllFlags(HttpServletRequest request
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        getUser(request);
        return QuestionController.getAllFlags();
    }

    @PostMapping(value = "/checkAvailableQuestions")
    @ResponseBody
    public String checkAvailableQuestions(HttpServletRequest request,
                                          @RequestParam(value = "qNo") int qNo,
                                          @RequestParam(required = false, value = "level") String level,
                                          @RequestParam(required = false, value = "gradeId") ObjectId gradeId,
                                          @RequestParam(required = false, value = "lessonId") ObjectId lessonId,
                                          @RequestParam(required = false, value = "subjectId") ObjectId subjectId,
                                          @RequestParam(required = false, value = "author") String author,
                                          @RequestParam(required = false, value = "tag") String tag
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return QuestionController.checkAvailableQuestions(
                getUser(request).getObjectId("_id"),
                tag, gradeId, lessonId, subjectId, qNo, level, author
        );
    }


    @GetMapping(value = "/getTagsKeyVals")
    @ResponseBody
    public String getTagsKeyVals() {
        return QuestionController.getTagsKeyVals();
    }

    @GetMapping(value = "/getSubjectCodesExcel")
    @ResponseBody
    public void getSubjectCodesExcel(
            HttpServletResponse response
    ) {
        try {
            ByteArrayInputStream byteArrayInputStream = ContentController.getSubjectCodesExcel();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=codes.xlsx");
            IOUtils.copy(byteArrayInputStream, response.getOutputStream());
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }
    }

    @GetMapping(value = "/getAuthorCodesExcel")
    @ResponseBody
    public void getAuthorCodesExcel(
            HttpServletResponse response
    ) {
        try {
            ByteArrayInputStream byteArrayInputStream = UserController.getAuthorCodesExcel();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=author_codes.xlsx");
            IOUtils.copy(byteArrayInputStream, response.getOutputStream());
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }
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
