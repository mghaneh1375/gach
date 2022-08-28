package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.StudentReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;

@Controller
@RequestMapping(path = "/api/quiz/public/")
@Validated
public class StudentQuizAPIRoutes extends Router {

    @GetMapping(value = "get/{mode}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                      @RequestParam(required = false) String tag,
                      @RequestParam(required = false) Boolean finishedIsNeeded
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.getRegistrable(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin, tag, finishedIsNeeded
        );
    }

    @GetMapping(value = "getRecpForQuiz/{mode}/{quizId}")
    @ResponseBody
    public String getRecpForQuiz(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return QuizController.getMyRecp(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );

    }


    @GetMapping(value = "reviewQuiz/{mode}/{quizId}")
    @ResponseBody
    public String reviewQuiz(HttpServletRequest request,
                             @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        Document user = getUser(request);
        boolean isStudent = Authorization.isPureStudent(user.getList("accesses", String.class));

        return QuizController.reviewQuiz(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                quizId, user.getObjectId("_id"), isStudent
        );

    }

    @GetMapping(value = "myQuizzes")
    @ResponseBody
    public String myQuizzes(HttpServletRequest request,
                            @RequestParam(required = false) String mode,
                            @RequestParam(required = false) String status
    ) throws UnAuthException, NotActivateAccountException {
        return StudentReportController.myQuizzes(
                getUserWithOutCheckCompleteness(request).getObjectId("_id"),
                mode, status
        );
    }

    @GetMapping(value = "/getMyAnswerSheet/{mode}/{quizId}")
    @ResponseBody
    public String getMyAnswerSheet(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getStudentAnswerSheet(
                    iryscQuizRepository,
                    null,
                    quizId, user.getObjectId("_id")
            );

        return QuizController.getStudentAnswerSheet(
                schoolQuizRepository,
                null,
                quizId, user.getObjectId("_id")
        );
    }

    @PostMapping(path = "buy")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @RequestBody @StrongJSONConstraint(
                              params = {"ids"},
                              paramsType = {JSONArray.class},
                              optionals = {"packageId", "code"},
                              optionalsType = {ObjectId.class, String.class}
                      )
                      @NotBlank String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        Document user = getUser(request);

        JSONObject jsonObject = Utility.convertPersian(
                new JSONObject(jsonStr)
        );

        return QuizController.buy(
                user.getObjectId("_id"),
                jsonObject.has("packageId") ?
                        new ObjectId(jsonObject.getString("packageId")) : null,
                jsonObject.getJSONArray("ids"),
                user.getInteger("money"),
                user.getString("phone"),
                user.getString("mail"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }


}
