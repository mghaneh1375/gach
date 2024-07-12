package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.StudentTeachController;
import irysc.gachesefid.Controllers.Teaching.TeachTagReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

@Controller
@RequestMapping(path = "/api/teach/student/")
@Validated
public class StudentTeachAPIRoutes extends Router {


    @PutMapping(value = "submitRequest/{scheduleId}")
    @ResponseBody
    public String submitRequest(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentTeachController.submitRequest(
                getStudentUser(request), scheduleId
        );
    }

    @GetMapping(value = "getSchedules/{teacherId}")
    @ResponseBody
    public String getSchedules(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId teacherId
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        getStudentUser(request);
        return StudentTeachController.getSchedules(teacherId);
    }

    @GetMapping(value = "getMySchedules")
    @ResponseBody
    public String getMySchedules(HttpServletRequest request
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentTeachController.getMySchedules(
                getStudentUser(request).getObjectId("_id")
        );
    }

    @PutMapping(value = "rate/{scheduleId}")
    @ResponseBody
    public String rate(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @RequestParam(value = "rate") @Min(1) @Max(5) Integer rate
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentTeachController.rate(
                getStudentUser(request).getObjectId("_id"), scheduleId, rate
        );
    }

    @GetMapping(value = "getTeachers")
    @ResponseBody
    public String getTeachers(
            @RequestParam(required = false, value = "justHasFreeSchedule") Boolean justHasFreeSchedule,
            @RequestParam(required = false, value = "minAge") Integer minAge,
            @RequestParam(required = false, value = "maxAge") Integer maxAge,
            @RequestParam(required = false, value = "tag") String tag,
            @RequestParam(required = false, value = "minRate") Integer minRate,
            @RequestParam(required = false, value = "maxRate") Integer maxRate,
            @RequestParam(required = false, value = "returnFilters") Boolean returnFilters,
            @RequestParam(required = false, value = "sortBy") String sortBy,
            @RequestParam(required = false, value = "branchId") ObjectId branchId,
            @RequestParam(required = false, value = "gradeId") ObjectId gradeId,
            @RequestParam(required = false, value = "lessonId") ObjectId lessonId
    ) {
        return StudentTeachController.getTeachers(
                justHasFreeSchedule, minAge, maxAge,
                minRate, maxRate, sortBy, tag, returnFilters,
                gradeId, branchId, lessonId
        );
    }

    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public String getAllReportTags(){
        return TeachTagReportController.getAllReportTags(
                TeachReportTagMode.USER.getName(), false
        );
    }


    @GetMapping(value = "getDistinctTags")
    @ResponseBody
    public String getDistinctTags() {
        return generateSuccessMsg("data", userRepository.distinctTagsWithFilter(
                and(
                        eq("accesses", "advisor"),
                        exists("teach_tags")
                ), "teach_tags")
        );
    }
}
