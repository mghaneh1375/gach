package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Quiz.TashrihiQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
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

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return TashrihiQuizController.setCorrectors(iryscQuizRepository, null, quizId,
                    new JSONObject(jsonStr).getJSONArray("correctors")
            );

        return TashrihiQuizController.setCorrectors(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                new JSONObject(jsonStr).getJSONArray("correctors")
        );
    }

    @GetMapping(path = "getMyMarkList/{mode}/{quizId}")
    @ResponseBody
    public String getMyMarkList(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @RequestParam(required = false, value = "justMarked") Boolean justMarked,
                                @RequestParam(required = false, value = "justNotMarked") Boolean justNotMarked
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return TashrihiQuizController.getMyMarkList(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, justMarked, justNotMarked
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

    @GetMapping(path = "getMyTasks")
    @ResponseBody
    public String getMyTasks(HttpServletRequest request,
                             @RequestParam(required = false, value = "mode") @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                             @RequestParam(required = false, value = "pass") Boolean pass,
                             @RequestParam(required = false, value = "curr") Boolean curr,
                             @RequestParam(required = false, value = "future") Boolean future
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TashrihiQuizController.getMyTasks(
                getPrivilegeUser(request).getObjectId("_id"), mode,
                pass == null ? false : pass, curr == null ? true : curr,
                future == null ? true : future
        );
    }

    @PutMapping(value = "/setMark/{mode}/{taskId}/{studentId}/{answerId}")
    @ResponseBody
    public String setMark(HttpServletRequest request,
                          @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                          @PathVariable @ObjectIdConstraint ObjectId taskId,
                          @PathVariable @ObjectIdConstraint ObjectId answerId,
                          @PathVariable @ObjectIdConstraint ObjectId studentId,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"mark"},
                                  paramsType = {Number.class},
                                  optionals = {"description"},
                                  optionalsType = {String.class}
                          ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return TashrihiQuizController.setMark(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                taskId, answerId, studentId, new JSONObject(jsonStr)
        );
    }

}
