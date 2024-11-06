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

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;

@Controller
@RequestMapping(path = "/api/quiz/report")
@Validated
public class ReportAPIRoutes extends Router {

    @GetMapping(value = "/stateReport/{quizMode}/{quizId}")
    @ResponseBody
    public String getStateReport(
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        return AdminReportController.getStateReport(quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                        schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/cityReport/{quizMode}/{quizId}")
    @ResponseBody
    public String cityReport(
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        return AdminReportController.getCityReport(quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                        schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/schoolReport/{quizMode}/{quizId}")
    @ResponseBody
    public String getSchoolReport(
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        return AdminReportController.getSchoolReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/genderReport/{quizMode}/{quizId}")
    @ResponseBody
    public String genderReport(
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        return AdminReportController.getGenderReport(
                quizMode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        quizMode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository, quizId);
    }

    @GetMapping(value = "/authorReport/{quizMode}/{quizId}")
    @ResponseBody
    public String authorReport(
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String quizMode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
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
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @GetMapping(value = "/showRanking/{mode}/{quizId}")
    @ResponseBody
    public String showRanking(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        boolean isAdmin = false;
        ObjectId userId = null;
        try {
            UserTokenInfo userTokenInfo = getUserTokenInfo(request);
            isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());
            userId = userTokenInfo.getId();
        }
        catch (Exception ignore) {}

        if (mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
            return OnlineStandingController.getOnlineQuizRankingTableDetail(quizId, isAdmin);

        if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
            return EscapeQuizController.getRanking(quizId, isAdmin);

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return StudentReportController.getRanking(iryscQuizRepository, isAdmin, null, quizId);

        if (userId == null)
            return JSON_NOT_ACCESS;

        if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return StudentReportController.getRanking(openQuizRepository, isAdmin, userId, quizId);

        return StudentReportController.getRanking(
                schoolQuizRepository,
                isAdmin,
                isAdmin ? null : userId,
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
        ObjectId studentId = null;
        boolean isAdmin = false;
        try {
            UserTokenInfo userTokenInfo = getUserTokenInfo(request);
            isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());
            studentId = userTokenInfo.getId();
        }
        catch (Exception ignore) {}

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return AdminReportController.getStudentStat(
                    iryscQuizRepository, isAdmin ? null : "", quizId, userId, studentId == null
            );

        if (studentId == null)
            return JSON_NOT_ACCESS;

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return AdminReportController.getStudentStatCustomQuiz(quizId, studentId);

        if (mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()))
            return AdminReportController.getStudentStatContentQuiz(quizId, studentId);

        if (!Authorization.hasAccessToThisStudent(userId, studentId))
            return JSON_NOT_ACCESS;

        return AdminReportController.getStudentStat(
                mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository : schoolQuizRepository,
                null, quizId, userId, false
        );
    }


    @GetMapping(value = "/getKarnameReportExcel/{mode}/{quizId}")
    @ResponseBody
    public void getKarnameReportExcel(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {
        try {
            //todo: consideration needed
//            Document user = getPrivilegeUser(request);
//            boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
            boolean isAdmin = true;

            ByteArrayInputStream byteArrayInputStream = AdminReportController.getKarnameReportExcel(
                    mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                    schoolQuizRepository, isAdmin, null, quizId
            ); //user.getObjectId("_id")

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=karname_report.xlsx");
            IOUtils.copy(byteArrayInputStream, response.getOutputStream());
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }
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
