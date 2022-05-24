package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Question.QuestionController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Test.Question.QuestionTestController;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;

@Controller
@RequestMapping(path = "/api/admin/question")
@Validated
public class QuestionAPIRoutes extends Router {


    @PostMapping(value = "/test")
    @ResponseBody
    public String test(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (!DEV_MODE)
            return "not allowed in main server";

        getAdminPrivilegeUserVoid(request);
        String msg;

        try {
            new QuestionTestController();
            msg = "success";
        } catch (Exception x) {
            msg = x.getMessage();
            x.printStackTrace();
        }

        return msg;
    }


    @PostMapping(value = "/add/{subjectId}")
    @ResponseBody
    public String add(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId subjectId,
                      @RequestBody @StrongJSONConstraint(
                              params = {
                                      "level", "authorId",
                                      "neededTime", "answer",
                                      "organizationId", "kindQuestion"
                              },
                              paramsType = {
                                      QuestionLevel.class, ObjectId.class,
                                      Positive.class, Object.class,
                                      String.class, QuestionType.class
                              },
                              optionals = {
                                      "sentencesCount", "telorance",
                                      "choicesCount", "neededLines"
                              },
                              optionalsType = {
                                      Positive.class, Number.class,
                                      Positive.class, Positive.class
                              }
                      ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
//        getAdminPrivilegeUserVoid(request);
        return QuestionController.addQuestion(subjectId, new JSONObject(jsonStr));
    }


}
