package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Quiz.AdminReportController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.StudentReportController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Models.KindKarname;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_TOKEN;

@Controller
@RequestMapping(path="/api/report")
@Validated
public class ReportAPIRoutes extends Router {


    @GetMapping(value = "/stateReport/{quizId}")
    @ResponseBody
    public String getStateReport(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getStateReport(quizId);
    }

    @GetMapping(value = "/cityReport/{quizId}")
    @ResponseBody
    public String cityReport(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getCityReport(quizId);
    }

    @GetMapping(value = "/schoolReport/{quizId}")
    @ResponseBody
    public String getSchoolReport(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getSchoolReport(quizId);
    }

    @GetMapping(value = "/genderReport/{quizId}")
    @ResponseBody
    public String genderReport(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getGenderReport(quizId);
    }

    @GetMapping(value = "/authorReport/{quizId}")
    @ResponseBody
    public String authorReport(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getAuthorReport(quizId);
    }



    @GetMapping(value = "/showRanking/{mode}/{quizId}")
    @ResponseBody
    public String showRanking(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return StudentReportController.getRanking(iryscQuizRepository, isAdmin, null, quizId);

        if (user == null)
            return JSON_NOT_ACCESS;

        return StudentReportController.getRanking(
                schoolQuizRepository,
                isAdmin,
                isAdmin ? null : user.getObjectId("_id"),
                quizId
        );
    }

    @GetMapping(value = "/getStudentStat/{mode}/{quizId}/{userId}")
    @ResponseBody
    public String getStudentStat(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId,
                                 @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {

        Document user = getUser(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return AdminReportController.getStudentStat(iryscQuizRepository, isAdmin ? null : "", quizId, userId);

        if (user == null)
            return JSON_NOT_ACCESS;

        return AdminReportController.getStudentStat(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId,
                userId
        );
    }
}
