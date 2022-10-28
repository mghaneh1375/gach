package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.AdminReportController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.StudentQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.AllKindQuiz;
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

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/quiz/public/")
@Validated
public class StudentQuizAPIRoutes extends Router {

    @GetMapping(value = "get/{mode}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                      @RequestParam(required = false) String tag,
                      @RequestParam(required = false) Boolean finishedIsNeeded,
                      @RequestParam(required = false) Boolean justRegistrable
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) || mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()))
            return QuizController.getRegistrable(
                    mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                    isAdmin, tag, finishedIsNeeded
            );

//        if(mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
//            return OpenQuizController.getAll(isAdmin, user != null ? user.getObjectId("_id") : null);

        return JSON_NOT_VALID_PARAMS;
    }

    @GetMapping(value = "getFinishedQuizzes")
    @ResponseBody
    public String getFinishedQuizzes() {
        return QuizController.getFinishedQuizzes();
    }

    @GetMapping(value = "getRecpForQuiz/{mode}/{quizId}")
    @ResponseBody
    public String getRecpForQuiz(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.getMyRecpForCustomQuiz(
                    quizId, getUser(request).getObjectId("_id")
            );

        return StudentQuizController.getMyRecp(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );
    }


    @GetMapping(value = "reviewQuiz/{mode}/{quizId}")
    @ResponseBody
    public String reviewQuiz(HttpServletRequest request,
                             @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);
        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.reviewCustomQuiz(
                    quizId, user.getObjectId("_id")
            );


        boolean isStudent = Authorization.isPureStudent(user.getList("accesses", String.class));
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return StudentQuizController.reviewQuiz(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id"), isStudent
        );

    }

    @GetMapping(value = "launch/{mode}/{quizId}")
    @ResponseBody
    public String launch(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.launchCustom(
                    quizId, getUser(request).getObjectId("_id")
            );

        return StudentQuizController.launch(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "myQuizzes")
    @ResponseBody
    public String myQuizzes(HttpServletRequest request,
                            @RequestParam(required = false) String mode,
                            @RequestParam(required = false) String status
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.myQuizzes(
                user, mode, status
        );
    }

    @GetMapping(value = "myCustomQuizzes")
    @ResponseBody
    public String myCustomQuizzes(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentQuizController.myCustomQuizzes(
                getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "/getMyAnswerSheet/{mode}/{quizId}")
    @ResponseBody
    public String getMyAnswerSheet(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.toString()))
            return AdminReportController.getStudentAnswerSheetCustomQuiz(
                    quizId, user.getObjectId("_id")
            );

        return AdminReportController.getStudentAnswerSheet(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
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

        return StudentQuizController.buy(
                user.getObjectId("_id"),
                jsonObject.has("packageId") ?
                        new ObjectId(jsonObject.getString("packageId")) : null,
                jsonObject.getJSONArray("ids"), null,
                ((Number) user.get("money")).doubleValue(),
                user.getString("phone"),
                user.getString("mail"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }


    @PostMapping(path = "groupBuy")
    @ResponseBody
    public String groupBuy(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"ids", "studentIds"},
                                   paramsType = {JSONArray.class, JSONArray.class},
                                   optionals = {"packageId", "code"},
                                   optionalsType = {ObjectId.class, String.class}
                           )
                           @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getSchoolUser(request);

        JSONObject jsonObject = Utility.convertPersian(
                new JSONObject(jsonStr)
        );

        return StudentQuizController.buy(
                user.getObjectId("_id"),
                jsonObject.has("packageId") ?
                        new ObjectId(jsonObject.getString("packageId")) : null,
                jsonObject.getJSONArray("ids"),
                jsonObject.getJSONArray("studentIds"),
                ((Number) user.get("money")).doubleValue(),
                user.getString("phone"),
                user.getString("mail"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }

    @PutMapping(value = "/storeAnswers/{mode}/{quizId}")
    @ResponseBody
    public String storeAnswers(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"answers"},
                                       paramsType = {JSONArray.class}
                               ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.storeCustomAnswers(
                    quizId, user.getObjectId("_id"),
                    new JSONObject(jsonStr).getJSONArray("answers")
            );

        return StudentQuizController.storeAnswers(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId,
                user.getObjectId("_id"), new JSONObject(jsonStr).getJSONArray("answers")
        );
    }


    @PostMapping(path = "prepareCustomQuiz")
    @ResponseBody
    public String prepareCustomQuiz(HttpServletRequest request,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {"filters", "name"},
                                            paramsType = {JSONArray.class, String.class}
                                    )
                                    @NotBlank String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        JSONObject jsonObject = new JSONObject(jsonStr);

        return StudentQuizController.prepareCustomQuiz(
                getUser(request).getObjectId("_id"),
                jsonObject.getJSONArray("filters"),
                jsonObject.getString("name")
        );
    }

    @PostMapping(path = "payCustomQuiz/{id}")
    @ResponseBody
    public String payCustomQuiz(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody(required = false) @StrongJSONConstraint(
                                        params = {}, paramsType = {},
                                        optionals = {"offcode"},
                                        optionalsType = {String.class}
                                ) String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return StudentQuizController.payCustomQuiz(
                getUser(request),
                id,
                (jsonStr == null || jsonStr.isEmpty()) ?
                        new JSONObject() :
                        new JSONObject(jsonStr)
        );
    }

}
