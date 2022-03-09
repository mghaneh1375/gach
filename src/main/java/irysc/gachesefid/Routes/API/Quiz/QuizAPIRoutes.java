package irysc.gachesefid.Routes.API.Quiz;

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
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;


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
                                        String.class, Positive.class,
                                        Positive.class
                                },
                                optionals = {
                                        "price",
                                        "description", "startRegistry",
                                        "endRegistry", "start",
                                        "end", "tag", "isOnline",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrect",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperSize", "database",
                                        "descAfter", "descAfterMode"
                                },
                                optionalsType = {
                                        Positive.class,
                                        String.class, Long.class,
                                        Long.class, Long.class,
                                        Long.class, String.class,
                                        Boolean.class, Positive.class,
                                        Boolean.class, Boolean.class,
                                        Boolean.class, Number.class,
                                        Positive.class, Positive.class,
                                        String.class, String.class,
                                        String.class, String.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        if (mode.equals(KindQuiz.REGULAR.getName()))
            return RegularQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        return "sd";
    }

    @PostMapping(value = "/toggleVisibility/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        if (mode.equals(KindQuiz.REGULAR.getName()))
            return RegularQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        return "sd";
    }
}
