package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Quiz.AdminReportController;
import irysc.gachesefid.Controllers.Quiz.EscapeQuizController;
import irysc.gachesefid.Controllers.Quiz.OnlineStandingController;
import irysc.gachesefid.Controllers.Quiz.StudentReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.openQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;

@Controller
@RequestMapping(path = "/api/quiz/report")
@Validated
public class ReportAPIRoutes extends Router {


    @GetMapping(value = "/stateReport/{quizMode}/{quizId}")
    @ResponseBody
    public String getStateReport(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getStateReport(quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                        schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/cityReport/{quizMode}/{quizId}")
    @ResponseBody
    public String cityReport(HttpServletRequest request,
                             @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getCityReport(quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                        schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/schoolReport/{quizMode}/{quizId}")
    @ResponseBody
    public String getSchoolReport(HttpServletRequest request,
                                  @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getSchoolReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/genderReport/{quizMode}/{quizId}")
    @ResponseBody
    public String genderReport(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getGenderReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/authorReport/{quizMode}/{quizId}")
    @ResponseBody
    public String authorReport(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return AdminReportController.getAuthorReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                        schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/participantReport/{quizMode}/{quizId}")
    @ResponseBody
    public String participantReport(HttpServletRequest request,
                                    @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
                                    @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getPrivilegeUser(request);
        return AdminReportController.getParticipantReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, quizId, user
        );
    }

    @GetMapping(value = "/A1/{mode}/{quizId}")
    @ResponseBody
    public String A1(HttpServletRequest request,
                     @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return AdminReportController.A1(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @GetMapping(value = "/showRanking/{mode}/{quizId}")
    @ResponseBody
    public String showRanking(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if(mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
            return OnlineStandingController.getOnlineQuizRankingTableDetail(quizId, isAdmin);

        if(mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
            return EscapeQuizController.getRanking(quizId, isAdmin);

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return StudentReportController.getRanking(iryscQuizRepository, isAdmin, null, quizId);

        if (user == null)
            return JSON_NOT_ACCESS;

        if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return StudentReportController.getRanking(openQuizRepository, isAdmin, user.getObjectId("_id"), quizId);

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
                                 @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId,
                                 @PathVariable @ObjectIdConstraint ObjectId userId
    ) {

        Document user = getUserIfLogin(request);

        if(mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName())) {

            if(user == null)
                return JSON_NOT_ACCESS;

            return AdminReportController.getStudentStatCustomQuiz(quizId, user.getObjectId("_id"));
        }

        if(mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())) {

            if(user == null)
                return JSON_NOT_ACCESS;

            return AdminReportController.getStudentStatContentQuiz(quizId, user.getObjectId("_id"));
        }

        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return AdminReportController.getStudentStat(
                    iryscQuizRepository, isAdmin ? null : "", quizId, userId, user == null
            );

        if (user == null)
            return JSON_NOT_ACCESS;

        if(!Authorization.hasAccessToThisStudent(userId, user.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return AdminReportController.getStudentStat(
                    openQuizRepository, null, quizId, userId, false
            );

        return AdminReportController.getStudentStat(
                schoolQuizRepository, null, quizId, userId, false
        );
    }


    @GetMapping(value = "/karnameReport/{mode}/{quizId}")
    @ResponseBody
    public String karnameReport(
            HttpServletRequest request,
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return AdminReportController.getKarnameReport(
                mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, isAdmin, user.getObjectId("_id"), quizId
        );
    }
}
