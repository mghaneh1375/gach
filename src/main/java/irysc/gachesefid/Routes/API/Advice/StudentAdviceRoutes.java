package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/advisor/public/")
@Validated
public class StudentAdviceRoutes extends Router {

    @GetMapping(value = "getAllAdvisors")
    @ResponseBody
    public String getAllAdvisors() {
        return AdvisorController.getAllAdvisors();
    }

    @GetMapping(value = "getMyAdvisor")
    @ResponseBody
    public String getMyAdvisor(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.getMyAdvisor(getStudentUser(request));
    }

    @PutMapping(value = "rate")
    @ResponseBody
    public String rate(HttpServletRequest request,
                       @RequestBody @StrongJSONConstraint(
                               params = {"rate"},
                               paramsType = {Positive.class}
                       ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {

        Document user = getStudentUser(request);
        if(!user.containsKey("advisor_id"))
            return JSON_NOT_ACCESS;

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        int rate = jsonObject.getInt("rate");
        if(rate < 1 || rate > 5)
            return JSON_NOT_VALID_PARAMS;

        return AdvisorController.rate(
                user.getObjectId("_id"), user.getObjectId("advisor_id"),
                rate
        );
    }
}
