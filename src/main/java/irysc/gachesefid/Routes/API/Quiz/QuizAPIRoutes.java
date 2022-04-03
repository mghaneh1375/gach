package irysc.gachesefid.Routes.API.Quiz;

import irysc.gachesefid.Controllers.Quiz.OpenQuizController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Routes.Router;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;


@Controller
@RequestMapping(path = "/api/quiz/admin")
@Validated
public class QuizAPIRoutes extends Router {

    @PostMapping(value = "/store/{mode}")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable @EnumValidator(enumClazz = KindQuiz.class) @NotBlank String mode,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "title", "duration"
                                },
                                paramsType = {
                                        String.class, Positive.class
                                },
                                optionals = {
                                        "price", "permute",
                                        "description", "startRegistry",
                                        "endRegistry", "start",
                                        "end", "tag", "isOnline",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrect",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperTheme", "database",
                                        "descAfter", "descAfterMode"
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
                                        String.class, String.class
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

        return "sd";
    }

    @PostMapping(value = "/toggleVisibility/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.toggleVisibility(iryscQuizRepository, null, quizId);
    }

    @PutMapping(value = "/forceRegistry/{quizId}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"students"},
                                        paramsType = JSONArray.class
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.forceRegistry(iryscQuizRepository, null, quizId,
                new JSONObject(jsonStr).getJSONArray("students"));
    }

    @DeleteMapping(value = "/forceDeportation/{quizId}")
    @ResponseBody
    public String forceDeportation(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"students"},
                                           paramsType = JSONArray.class
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.forceDeportation(iryscQuizRepository, null, quizId,
                new JSONObject(jsonStr).getJSONArray("students"));
    }

    @DeleteMapping(value = "/remove/{quizId}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.remove(iryscQuizRepository, null, quizId);
    }

    @GetMapping(value = "/getParticipants/{quizId}")
    @ResponseBody
    public String getParticipants(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                  @RequestParam(value = "studentId", required = false) ObjectId studentId,
                                  @RequestParam(value = "isResultsNeeded", required = false) Boolean isResultsNeeded,
                                  @RequestParam(value = "isStudentAnswersNeeded", required = false) Boolean isStudentAnswersNeeded,
                                  @RequestParam(value = "justAbsents", required = false) Boolean justAbsents,
                                  @RequestParam(value = "justPresence", required = false) Boolean justPresence,
                                  @RequestParam(value = "justMarked", required = false) Boolean justMarked,
                                  @RequestParam(value = "justNotMarked", required = false) Boolean justNotMarked
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.getParticipants(iryscQuizRepository,null,
                quizId, studentId, isStudentAnswersNeeded,
                isResultsNeeded, justMarked,
                justNotMarked, justAbsents, justPresence
        );
    }
}
