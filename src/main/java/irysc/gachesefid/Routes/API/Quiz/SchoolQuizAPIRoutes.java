package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;

@Controller
@RequestMapping(path = "/api/quiz/school")
@Validated
public class SchoolQuizAPIRoutes extends Router {


    @PostMapping(value = "/toggleVisibility/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return QuizController.toggleVisibility(schoolQuizRepository,
                getPrivilegeUser(request).getObjectId("_id"), quizId);
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
        return QuizController.forceRegistry(schoolQuizRepository,
                getPrivilegeUser(request).getObjectId("_id"), quizId,
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
        return QuizController.forceDeportation(schoolQuizRepository,
                getPrivilegeUser(request).getObjectId("_id"), quizId,
                new JSONObject(jsonStr).getJSONArray("students"));
    }

    @DeleteMapping(value = "/remove/{quizId}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return QuizController.remove(schoolQuizRepository,
                getPrivilegeUser(request).getObjectId("_id"), quizId);
    }
}
