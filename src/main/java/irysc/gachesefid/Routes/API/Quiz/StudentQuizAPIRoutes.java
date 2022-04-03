package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/quiz/irysc/student")
@Validated
public class StudentQuizAPIRoutes extends Router {

    @PostMapping(path = "buy/{quizId}")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return QuizController.buy(getUser(request), quizId);
    }


}
