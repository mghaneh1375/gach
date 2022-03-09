package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Quiz.AdminReportController;
import irysc.gachesefid.Controllers.Quiz.StudentReportController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
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

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_TOKEN;

@Controller
@RequestMapping(path="/api/report")
@Validated
public class ReportAPIRoutes extends Router {

    @GetMapping(value = "admin/{quizMode}/A{reportNo}/{quizId}")
    @ResponseBody
    public String generalQuizReport(HttpServletRequest request,
                     @PathVariable @EnumValidator(enumClazz = KindQuiz.class) String quizMode,
                     @PathVariable @Min(1) @Max(9) int reportNo,
                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        Document user = getUser(request);
        if(user == null)
            return JSON_NOT_VALID_TOKEN;

        return AdminReportController.generalQuizReport(user.getString("access"), reportNo, user.getObjectId("_id"), quizId, quizMode);
    }

    @GetMapping(value = "{quizMode}/karname/{mode}/{quizId}")
    @ResponseBody
    public String karname(HttpServletRequest request,
                          @PathVariable @EnumValidator(enumClazz = KindQuiz.class) String quizMode,
                          @PathVariable @EnumValidator(enumClazz = KindKarname.class) String mode,
                          @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        Document user = getUser(request);
        if(user == null)
            return JSON_NOT_VALID_TOKEN;

        return StudentReportController.generalQuizReport(mode, user.getObjectId("_id"), quizId, quizMode);
    }

}
