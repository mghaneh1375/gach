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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.printException;

@Controller
@RequestMapping(path = "/api/admin/question")
@Validated
public class QuestionAPIRoutes extends Router {

    @GetMapping(value = "/subjectQuestions")
    @ResponseBody
    public String subjectQuestions(HttpServletRequest request,
                                   @RequestParam(required = false) Boolean isQuestionNeeded,
                                   @RequestParam(required = false) Integer criticalThresh,
                                   @RequestParam(required = false) ObjectId subjectId,
                                   @RequestParam(required = false) ObjectId lessonId,
                                   @RequestParam(required = false) ObjectId gradeId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return QuestionController.subjectQuestions(
                isQuestionNeeded, criticalThresh, subjectId, lessonId, gradeId
        );
    }


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
            printException(x);
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


    @PostMapping(value = "/addBatch")
    @ResponseBody
    public String addBatch(HttpServletRequest request,
                           @RequestBody MultipartFile file)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if(file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return QuestionController.addBatch(file);
    }

    @GetMapping
    @ResponseBody
    public String search(HttpServletRequest request,
                         @RequestParam(required = false, value = "isSubjectsNeeded") Boolean isSubjectsNeeded,
                         @RequestParam(required = false, value = "isAuthorsNeeded") Boolean isAuthorsNeeded,
                         @RequestParam(required = false, value = "justUnVisible") Boolean justUnVisible,
                         @RequestParam(required = false, value = "organizationId") String organizationId,
                         @RequestParam(required = false, value = "organizationLike") String organizationLike,
                         @RequestParam(required = false, value = "level") String level,
                         @RequestParam(required = false, value = "kindQuestion") String kindQuestion,
                         @RequestParam(required = false, value = "kindQuestion") String sortBy,
                         @RequestParam(required = false, value = "subjectId") ObjectId subjectId,
                         @RequestParam(required = false, value = "lessonId") ObjectId lessonId,
                         @RequestParam(required = false, value = "questionId") ObjectId questionId,
                         @RequestParam(required = false, value = "quizId") ObjectId quizId,
                         @RequestParam(required = false, value = "authorId") ObjectId authorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);
        return QuestionController.search(true,
                isSubjectsNeeded == null ? true : isSubjectsNeeded,
                isAuthorsNeeded == null ? true : isAuthorsNeeded,
                justUnVisible, organizationId, organizationLike,
                subjectId, lessonId, questionId, quizId, authorId,
                level, kindQuestion, sortBy
        );
    }

}
