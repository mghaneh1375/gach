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
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.convertPersian;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

@Controller
@RequestMapping(path = "/api/teach/student/")
@Validated
public class StudentTeachAPIRoutes extends Router {


    @PostMapping(value = "submitRequest/{scheduleId}")
    @ResponseBody
    public String submitRequest(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentTeachController.submitRequest(
                getStudentUser(request), scheduleId
        );
    }

    @PutMapping(value = "cancelRequest/{scheduleId}")
    @ResponseBody
    public String cancelRequest(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        Document user = getStudentUser(request);
        return StudentTeachController.cancelRequest(
                user.getObjectId("_id"),
                user.getString("first_name") + " " + user.getString("last_name"),
                scheduleId
        );
    }

    @GetMapping(value = "myScheduleRequests")
    @ResponseBody
    public String myScheduleRequests(
            HttpServletRequest request,
            @RequestParam(value = "activeMode", required = false) String activeMode,
            @RequestParam(value = "statusMode", required = false) String statusMode,
            @RequestParam(value = "scheduleActiveMode", required = false) String scheduleActiveMode
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentTeachController.myScheduleRequests(
                getStudentUser(request).getObjectId("_id"),
                activeMode, statusMode, scheduleActiveMode
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

    @PostMapping(value = "payForSchedule/{scheduleId}")
    @ResponseBody
    public String payForSchedule(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId scheduleId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {}, paramsType = {},
                                         optionals = {"code"}, optionalsType = {String.class}
                                 ) String json
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        JSONObject jsonObject = json != null && !json.isEmpty() ? convertPersian(new JSONObject(json)) : new JSONObject();
        return StudentTeachController.payForSchedule(
                getStudentUser(request), scheduleId,
                jsonObject.has("code") ? jsonObject.getString("code") : null
        );
    }

    @GetMapping(value = "getMySchedules")
    @ResponseBody
    public String getMySchedules(
            HttpServletRequest request,
            @RequestParam(value = "activeMode", required = false) String activeMode
    ) throws NotAccessException, NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentTeachController.getMySchedules(
                getStudentUser(request).getObjectId("_id"), activeMode
        );
    }

    @PutMapping(value = "rateToTeacher/{teacherId}")
    @ResponseBody
    public String rateToTeacher(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId teacherId,
            @RequestParam(value = "rate") @Min(1) @Max(5) Integer rate
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentTeachController.rateToTeacher(
                getStudentUser(request).getObjectId("_id"), teacherId, rate
        );
    }

    @PutMapping(value = "rateToSchedule/{scheduleId}")
    @ResponseBody
    public String rateToSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @RequestParam(value = "rate") @Min(1) @Max(5) Integer rate
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentTeachController.rateToSchedule(
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

    @GetMapping(value = "getMyRate/{teacherId}")
    @ResponseBody
    public String getMyRate(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId teacherId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentTeachController.getMyRate(
                getUser(request).getObjectId("_id"), teacherId
        );
    }

    @GetMapping(value = "getMyTeachScheduleReportProblems/{scheduleId}")
    @ResponseBody
    public String getMyTeachScheduleReportProblems(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentTeachController.getMyTeachScheduleReportProblems(
                getUser(request).getObjectId("_id"), scheduleId
        );
    }

    @PutMapping(value = "setMyTeachScheduleReportProblems/{scheduleId}")
    @ResponseBody
    public String setMyTeachScheduleReportProblems(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @RequestBody @StrongJSONConstraint(
                    params = {}, paramsType = {},
                    optionals = {"tagIds", "desc"}, optionalsType = {JSONArray.class, String.class}
            ) String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        JSONObject jsonObject;
        if(jsonStr == null || jsonStr.isEmpty()) jsonObject = new JSONObject();
        else jsonObject = new JSONObject(jsonStr);

        return StudentTeachController.setMyTeachScheduleReportProblems(
                getUser(request).getObjectId("_id"), scheduleId,
                jsonObject.has("tagIds") ? jsonObject.getJSONArray("tagIds") : null,
                jsonObject.has("desc") ? jsonObject.getString("desc") : null
        );
    }


    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public String getAllReportTags() {
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
