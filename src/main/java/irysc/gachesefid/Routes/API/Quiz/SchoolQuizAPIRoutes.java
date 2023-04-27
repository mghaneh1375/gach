package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.SchoolQuizController;
import irysc.gachesefid.Controllers.Quiz.TashrihiQuizController;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;

@Controller
@RequestMapping(path = "/api/quiz/school")
@Validated
public class SchoolQuizAPIRoutes extends Router {

    @PutMapping(path = "setCorrectors/{mode}/{quizId}")
    @ResponseBody
    public String setCorrectors(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"correctors"},
                                        paramsType = {JSONArray.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

//        Document user = getPrivilegeUser(request);
//        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
//
//        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
//            return TashrihiQuizController.setCorrectors(iryscQuizRepository, null, quizId,
//                    new JSONObject(jsonStr).getJSONArray("correctors")
//            );

//        return TashrihiQuizController.setCorrectors(schoolQuizRepository,
//                isAdmin ? null : user.getObjectId("_id"), quizId,
//                new JSONObject(jsonStr).getJSONArray("correctors")
//        );

        getAdminPrivilegeUserVoid(request);
        return TashrihiQuizController.setCorrectors(iryscQuizRepository, null, quizId,
                new JSONObject(jsonStr).getJSONArray("correctors")
        );
    }

    @GetMapping(path = "getMyMarkList/{mode}/{quizId}")
    @ResponseBody
    public String getMyMarkList(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @RequestParam(value = "taskMode") String taskMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        return TashrihiQuizController.getMyMarkList(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                user.getObjectId("_id"), quizId, taskMode
        );
    }

    @GetMapping(path = "getMyMarkListForSpecificStudent/{mode}/{quizId}/{studentId}")
    @ResponseBody
    public String getMyMarkListForSpecificStudent(HttpServletRequest request,
                                                  @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                                  @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return TashrihiQuizController.getMyMarkListForSpecificStudent(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId
        );
    }


    @GetMapping(path = "getMyMarks/{mode}/{quizId}")
    @ResponseBody
    public String getMyMarks(HttpServletRequest request,
                             @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return TashrihiQuizController.getMyMarks(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );
    }


    @PostMapping(value = "/addBatchQuestions/{quizId}")
    @ResponseBody
    public String addBatchQuestions(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId quizId,
                                    @RequestBody @NotNull MultipartFile file)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return SchoolQuizController.addBatchQuestions(file, quizId,
                getPrivilegeUser(request).getObjectId("_id")
        );
    }

    @PostMapping(value = "/copy/{quizId}")
    @ResponseBody
    public String copy(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                       @RequestBody @StrongJSONConstraint(
                               params = {"start", "end", "title", "launchMode", "copyStudents"},
                               paramsType = {Long.class, Long.class, String.class, String.class, Boolean.class}
                       ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        return SchoolQuizController.copy(getPrivilegeUser(request).getObjectId("_id"), quizId, new JSONObject(jsonStr));
    }


    @GetMapping(path = "getMyMarkListForSpecificQuestion/{mode}/{quizId}/{questionId}")
    @ResponseBody
    public String getMyMarkListForSpecificQuestion(HttpServletRequest request,
                                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                                   @PathVariable @ObjectIdConstraint ObjectId questionId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return TashrihiQuizController.getMyMarkListForSpecificQuestion(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, questionId
        );
    }

    @GetMapping(path = "getMyTasks")
    @ResponseBody
    public String getMyTasks(HttpServletRequest request,
                             @RequestParam(required = false, value = "mode") String mode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TashrihiQuizController.getMyTasks(
                getPrivilegeUser(request).getObjectId("_id"), mode
        );
    }

    @PutMapping(value = "/setMark/{mode}/{quizId}/{studentId}/{questionId}")
    @ResponseBody
    public String setMark(HttpServletRequest request,
                          @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                          @PathVariable @ObjectIdConstraint ObjectId studentId,
                          @PathVariable @ObjectIdConstraint ObjectId questionId,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"mark"},
                                  paramsType = {Object.class},
                                  optionals = {"description"},
                                  optionalsType = {String.class}
                          ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return TashrihiQuizController.setMark(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, questionId, studentId, Utility.convertPersian(new JSONObject(jsonStr))
        );
    }


    @PostMapping(value = "/finalize/{quizId}")
    @ResponseBody
    public String finalize(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId quizId,
                           @RequestBody(required = false) @StrongJSONConstraint(
                                   params = {},
                                   paramsType = {},
                                   optionals = {"off"},
                                   optionalsType = {String.class}
                           ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getSchoolUser(request);

        return QuizController.finalizeQuiz(quizId,
                user.getObjectId("_id"),
                jsonStr != null && !jsonStr.isEmpty() ?
                        new JSONObject(jsonStr).getString("off") :
                        null, ((Number) user.get("money")).doubleValue()
        );
    }

    @GetMapping(value = "/recp/{quizId}")
    @ResponseBody
    public String recp(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return QuizController.recp(quizId,
                getSchoolUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "/getTotalPrice/{quizId}")
    @ResponseBody
    public String getTotalPrice(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);

        return QuizController.getTotalPrice(quizId,
                user.getObjectId("_id"),
                user.getDouble("money")
        );
    }
}
