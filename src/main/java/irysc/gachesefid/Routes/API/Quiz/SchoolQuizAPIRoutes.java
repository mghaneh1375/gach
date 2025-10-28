package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.SchoolQuizController;
import irysc.gachesefid.Controllers.Quiz.TashrihiQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.HWAnswerType;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
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
import static irysc.gachesefid.Utility.Utility.convertPersian;
import static irysc.gachesefid.Utility.Utility.generateErr;

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
    ) {

        boolean isAdmin = false;
        ObjectId userId = null;
        try {
            UserTokenInfo userTokenInfo = getUserTokenInfo(request);
            isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());
            userId = userTokenInfo.getId();
        } catch (Exception ignore) {
        }

        return TashrihiQuizController.getMyMarkListForSpecificStudent(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : userId, quizId, studentId
        );
    }


    @GetMapping(path = "getMyMarks/{mode}/{quizId}")
    @ResponseBody
    public String getMyMarks(HttpServletRequest request,
                             @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException {
        return TashrihiQuizController.getMyMarks(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                quizId, getUserId(request)
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
    ) {
        boolean isAdmin = false;
        ObjectId userId = null;
        try {
            UserTokenInfo userTokenInfo = getUserTokenInfo(request);
            isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());
            userId = userTokenInfo.getId();
        } catch (Exception ignore) {
        }

        return TashrihiQuizController.getMyMarkListForSpecificQuestion(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : userId,
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
    ) throws UnAuthException {
        UserTokenInfo userTokenInfo = getUserTokenInfo(request);
        boolean isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());

        return TashrihiQuizController.setMark(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : userTokenInfo.getId(),
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

        Document user = getQuizUser(request);

        return QuizController.finalizeQuiz(quizId,
                user.getObjectId("_id"),
                jsonStr != null && !jsonStr.isEmpty() ?
                        new JSONObject(jsonStr).getString("off") :
                        null, ((Number) user.get("money")).doubleValue()
        );
    }


    @PostMapping(value = "/finalizeHW/{hwId}")
    @ResponseBody
    public String finalizeHW(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId hwId,
                             @RequestBody(required = false) @StrongJSONConstraint(
                                     params = {},
                                     paramsType = {},
                                     optionals = {"off"},
                                     optionalsType = {String.class}
                             ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getSchoolUser(request);

        return SchoolQuizController.finalizeHW(
                hwId, user.getObjectId("_id"),
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
                getQuizUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "/recpHW/{hwId}")
    @ResponseBody
    public String recpHW(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId hwId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return SchoolQuizController.recp(hwId,
                getQuizUser(request).getObjectId("_id")
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
                ((Number) user.get("money")).doubleValue()
        );
    }

    @GetMapping(value = "/getTotalHWPrice/{hwId}")
    @ResponseBody
    public String getTotalHWPrice(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId hwId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);

        return SchoolQuizController.getTotalHWPrice(hwId,
                user.getObjectId("_id"),
                ((Number) user.get("money")).doubleValue()
        );
    }


    @PostMapping(value = "/createHW")
    @ResponseBody
    public String createHW(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "title", "start",
                            "end", "showResultsAfterCorrection",
                            "answerType", "maxUploadSize"
                    },
                    paramsType = {
                            String.class, Long.class,
                            Long.class, Boolean.class,
                            HWAnswerType.class, Positive.class
                    },
                    optionals = {
                            "desc", "descAfter",
                            "delayPenalty", "delayEnd"
                    },
                    optionalsType = {
                            String.class, String.class,
                            Positive.class, Long.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getQuizUser(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));

        if (
                !user.containsKey("students") ||
                        user.getList("students", ObjectId.class).size() == 0
        )
            return generateErr("در حال حاضر دانش آموزی ندارید و امکان ساخت تمرین/آزمون برای شما میسر نمی باشد.");

        return SchoolQuizController.createHW(
                user.getObjectId("_id"),
                jsonObject, Authorization.isAdvisor(user.getList("accesses", String.class))
        );
    }

    @PostMapping(value = "/editHW/{hwId}")
    @ResponseBody
    public String editHW(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId hwId,
                         @RequestBody @StrongJSONConstraint(
                                 params = {
                                         "title", "start",
                                         "end", "showResultsAfterCorrection",
                                         "answerType", "maxUploadSize"
                                 },
                                 paramsType = {
                                         String.class, Long.class,
                                         Long.class, Boolean.class,
                                         HWAnswerType.class, Positive.class
                                 },
                                 optionals = {
                                         "desc", "descAfter",
                                         "delayPenalty", "delayEnd"
                                 },
                                 optionalsType = {
                                         String.class, String.class,
                                         Positive.class, Long.class
                                 }
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);

        return SchoolQuizController.updateHW(
                user.getObjectId("_id"), hwId,
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @PutMapping(value = "setMark/{hwId}/{studentId}")
    @ResponseBody
    public String setMark(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId hwId,
                          @PathVariable @ObjectIdConstraint ObjectId studentId,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"mark"},
                                  paramsType = {Positive.class},
                                  optionals = {"markDesc"},
                                  optionalsType = {String.class}
                          ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        return SchoolQuizController.setMark(
                hwId, getQuizUser(request).getObjectId("_id"),
                studentId, convertPersian(new JSONObject(jsonStr))
        );

    }

}
