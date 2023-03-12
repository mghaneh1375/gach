package irysc.gachesefid.Routes.API.QuestionReport;

import irysc.gachesefid.Controllers.QuestionReport.QuestionReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/questionReport/public")
@Validated
public class StudentQuestionReportAPIRoutes extends Router {

    @GetMapping(value = "getAllTags")
    @ResponseBody
    public String getAllTags(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        getUser(request);
        return QuestionReportController.getAllTags(false);
    }

    @PostMapping(value = "storeReport/{tagId}/{questionId}")
    @ResponseBody
    public String storeReport(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId tagId,
                              @PathVariable @ObjectIdConstraint ObjectId questionId,
                              @RequestBody(required = false) @StrongJSONConstraint(
                                      params = {}, paramsType = {},
                                      optionals = {"desc"}, optionalsType = {String.class}
                              ) String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        JSONObject jsonObject = jsonStr == null || jsonStr.isEmpty() ? new JSONObject() : new JSONObject(jsonStr);
        String desc = jsonObject.has("desc") ? jsonObject.getString("desc") : null;
        return QuestionReportController.storeReport(
                getUser(request).getObjectId("_id"), questionId, tagId, desc
        );
    }

}
