package irysc.gachesefid.Routes.API.Quiz;

import irysc.gachesefid.Controllers.Quiz.OpenQuizController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Controllers.Quiz.TashrihiQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
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

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;


@Controller
@RequestMapping(path = "/api/quiz/manage")
@Validated
public class QuizAPIRoutes extends Router {

    @PostMapping(value = "/store/{mode}")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable @EnumValidator(enumClazz = KindQuiz.class) @NotBlank String mode,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "title"
                                },
                                paramsType = {
                                        String.class
                                },
                                optionals = {
                                        "price", "permute",
                                        "description", "startRegistry",
                                        "endRegistry", "start",
                                        "end", "tags", "isOnline",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrection",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperTheme", "database",
                                        "descAfter", "desc",
                                        "duration" // duration is in min format
                                },
                                optionalsType = {
                                        Positive.class, Boolean.class,
                                        String.class, Long.class,
                                        Long.class, Long.class,
                                        Long.class, String.class,
                                        Boolean.class, Positive.class,
                                        Boolean.class, Boolean.class,
                                        Boolean.class, Number.class,
                                        Positive.class, Positive.class,
                                        String.class, Boolean.class,
                                        String.class, String.class,
                                        Positive.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        if (mode.equals(KindQuiz.REGULAR.getName()))
            return RegularQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        if (mode.equals(KindQuiz.OPEN.getName()))
            return OpenQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        if (mode.equals(KindQuiz.TASHRIHI.getName()))
            return TashrihiQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        return "sd";
    }

    @PostMapping(value = "/edit/{quizId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                       @RequestBody @StrongJSONConstraint(
                               params = {},
                               paramsType = {},
                               optionals = {
                                       "title", "price", "permute",
                                       "description", "startRegistry",
                                       "endRegistry", "start",
                                       "end", "tags", "isOnline",
                                       "capacity", "minusMark",
                                       "backEn", "showResultsAfterCorrection",
                                       "topStudentsGiftCoin",
                                       "topStudentsGiftMoney",
                                       "topStudentsCount",
                                       "paperTheme", "database",
                                       "descAfter", "desc",
                                       "duration", // duration is in min format
                                       "visibility"
                               },
                               optionalsType = {
                                       String.class, Positive.class, Boolean.class,
                                       String.class, Long.class,
                                       Long.class, Long.class,
                                       Long.class, String.class,
                                       Boolean.class, Positive.class,
                                       Boolean.class, Boolean.class,
                                       Boolean.class, Number.class,
                                       Positive.class, Positive.class,
                                       String.class, Boolean.class,
                                       String.class, String.class,
                                       Positive.class, Boolean.class
                               }
                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.update(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId,
                new JSONObject(jsonStr)
        );
    }

    @GetMapping(value = "getAll/{mode}")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = KindQuiz.class
                         ) @NotBlank String mode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin)
            return QuizController.getAll(iryscQuizRepository, null, mode);

        return QuizController.getAll(schoolQuizRepository, user.getObjectId("_id"), mode);
    }

    @GetMapping(value = "/get/{mode}/{quizId}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.get(iryscQuizRepository, null, quizId);

        return QuizController.get(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @PostMapping(value = "/toggleVisibility/{mode}/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.toggleVisibility(iryscQuizRepository, null, quizId);

        return QuizController.toggleVisibility(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @PutMapping(value = "/forceRegistry/{mode}/{quizId}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"students"},
                                        paramsType = JSONArray.class
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.forceRegistry(iryscQuizRepository, null, quizId,
                    new JSONObject(jsonStr).getJSONArray("students"));

        return QuizController.forceRegistry(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                new JSONObject(jsonStr).getJSONArray("students")
        );
    }

    @DeleteMapping(value = "/forceDeportation/{mode}/{quizId}")
    @ResponseBody
    public String forceDeportation(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"students"},
                                           paramsType = JSONArray.class
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.forceDeportation(iryscQuizRepository, null, quizId,
                    new JSONObject(jsonStr).getJSONArray("students"));

        return QuizController.forceDeportation(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                new JSONObject(jsonStr).getJSONArray("students")
        );
    }

    @DeleteMapping(value = "/remove/{mode}/{quizId}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.remove(iryscQuizRepository, null, quizId);

        return QuizController.remove(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @GetMapping(value = "/getParticipants/{mode}/{quizId}")
    @ResponseBody
    public String getParticipants(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                  @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                  @RequestParam(value = "studentId", required = false) ObjectId studentId,
                                  @RequestParam(value = "isResultsNeeded", required = false) Boolean isResultsNeeded,
                                  @RequestParam(value = "isStudentAnswersNeeded", required = false) Boolean isStudentAnswersNeeded,
                                  @RequestParam(value = "justAbsents", required = false) Boolean justAbsents,
                                  @RequestParam(value = "justPresence", required = false) Boolean justPresence,
                                  @RequestParam(value = "justMarked", required = false) Boolean justMarked,
                                  @RequestParam(value = "justNotMarked", required = false) Boolean justNotMarked
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getParticipants(iryscQuizRepository, null,
                    quizId, studentId, isStudentAnswersNeeded,
                    isResultsNeeded, justMarked,
                    justNotMarked, justAbsents, justPresence
            );

        return QuizController.getParticipants(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId, isStudentAnswersNeeded,
                isResultsNeeded, justMarked,
                justNotMarked, justAbsents, justPresence
        );
    }

    @PutMapping(path = "addAttach/{mode}/{quizId}")
    @ResponseBody
    public String addAttach(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId quizId,
                            @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                            @RequestBody(required = false) MultipartFile file,
                            @RequestBody @NotBlank String title,
                            @RequestBody(required = false) String link
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addAttach(iryscQuizRepository, null, quizId,
                    file, title, link);

        return QuizController.addAttach(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                file, title, link);
    }

    @DeleteMapping(path = "removeAttach/{mode}/{quizId}/{attachId}")
    @ResponseBody
    public String removeAttach(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId attachId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.removeAttach(iryscQuizRepository, null,
                    quizId, attachId);

        return QuizController.removeAttach(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, attachId);
    }

    @PutMapping(value = "/extend/{mode}/{quizId}")
    @ResponseBody
    public String extend(HttpServletRequest request,
                         @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId,
                         @RequestParam(value = "start", required = false) Long start,
                         @RequestParam(value = "end", required = false) Long end
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.extend(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, start, end);
    }

    @PostMapping(value = "/arrangeQuestions/{mode}/{quizId}")
    @ResponseBody
    public String arrangeQuestions(HttpServletRequest request,
                                   @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(params = {"questionIds"},
                                           paramsType = JSONArray.class) @NotBlank String jsonStr)
            throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.arrangeQuestions(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, new JSONObject(jsonStr)
        );
    }
}

