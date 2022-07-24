package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;

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

    @PostMapping(path = "buy/{quizId}")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return QuizController.buy(getUser(request), quizId);
    }


}
