package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.YesOrNo;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
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
@RequestMapping(path = "/api/advisor/manage/")
@Validated
public class AdvisorAPIRoutes extends Router {


    @PostMapping(value = "toggleStdAcceptance")
    @ResponseBody
    public String toggleStdAcceptance(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.toggleStdAcceptance(getAdvisorUser(request));
    }

    @PostMapping(value = "answerToRequest/{reqId}/{answer}")
    @ResponseBody
    public String answerToRequest(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId reqId,
                                  @PathVariable @EnumValidator(enumClazz = YesOrNo.class) String answer
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.answerToRequest(getAdvisorUser(request), reqId, answer);
    }

    @DeleteMapping(value = "removeStudents")
    @ResponseBody
    public String removeStudents(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = {JSONArray.class}
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.removeStudents(getAdvisorUser(request),
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @GetMapping(value = "myRequests")
    @ResponseBody
    public String myRequests(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        return AdvisorController.myStudentRequests(getAdvisorUser(request).getObjectId("_id"));
    }

}
