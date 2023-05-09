package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

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

        Document user = getStudentUser(request);

        if (!user.containsKey("advisor_id"))
            return Utility.generateSuccessMsg("data", new JSONObject());

        return AdvisorController.getMyAdvisor(user.getObjectId("_id"), user.getObjectId("advisor_id"));
    }

    @GetMapping(value = "hasOpenRequest")
    @ResponseBody
    public String hasOpenRequest(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.hasOpenRequest(getStudentUser(request).getObjectId("_id"));
    }

    @DeleteMapping(value = "cancelRequest/{reqId}")
    @ResponseBody
    public String cancelRequest(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId reqId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.cancelRequest(getStudentUser(request).getObjectId("_id"), reqId);
    }

    @DeleteMapping(value = "cancel")
    @ResponseBody
    public String cancel(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.cancel(getStudentUser(request));
    }

    @PostMapping(value = "request/{advisorId}")
    @ResponseBody
    public String request(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId advisorId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.request(getStudentUser(request), advisorId);
    }

    @GetMapping(value = "myRequests")
    @ResponseBody
    public String myRequests(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.myRequests(getStudentUser(request).getObjectId("_id"));
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
        if (!user.containsKey("advisor_id"))
            return JSON_NOT_ACCESS;

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        int rate = jsonObject.getInt("rate");
        if (rate < 1 || rate > 5)
            return JSON_NOT_VALID_PARAMS;

        return AdvisorController.rate(
                user.getObjectId("_id"), user.getObjectId("advisor_id"),
                rate
        );
    }
}
