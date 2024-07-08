package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.TeachTagReportController;
import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
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

import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/teach/manage/")
@Validated
public class TeacherAPIRoutes extends Router {


    @PostMapping(value = "createNewSchedule")
    @ResponseBody
    public String createNewSchedule(HttpServletRequest request,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {
                                                    "start", "length",
                                                    "visibility", "teachMode"
                                            },
                                            paramsType = {
                                                    String.class, Positive.class,
                                                    Boolean.class, TeachMode.class
                                            },
                                            optionals = {
                                                    "description", "price",
                                                    "minCap", "maxCap", "title",
                                                    "time", "needRegistryConfirmation"
                                            },
                                            optionalsType = {
                                                    String.class, Positive.class,
                                                    Positive.class, Positive.class,
                                                    String.class, String.class,
                                                    Boolean.class
                                            }
                                    ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.createNewSchedule(
                getAdvisorUser(request), convertPersian(new JSONObject(jsonStr))
        );
    }

    @GetMapping(value = "getSchedules")
    @ResponseBody
    public String getSchedules(
            HttpServletRequest request,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "justHasStudents") Boolean justHasStudents,
            @RequestParam(required = false, value = "justHasRequests") Boolean justHasRequests,
            @RequestParam(required = false, value = "teachMode") String teachMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getSchedules(
                getAdvisorUser(request).getObjectId("_id"), from, to,
                justHasStudents, justHasRequests, teachMode
        );
    }

    @GetMapping(value = "getSchedule/{scheduleId}")
    @ResponseBody
    public String getSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getSchedule(
                getAdvisorUser(request).getObjectId("_id"),
                scheduleId
        );
    }

    @DeleteMapping(value = "removeSchedule/{scheduleId}")
    @ResponseBody
    public String removeSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.removeSchedule(
                getAdvisorUser(request).getObjectId("_id"), scheduleId
        );
    }

    @PutMapping(value = "setRequestStatus/{scheduleId}/{studentId}")
    @ResponseBody
    public String setRequestStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @PathVariable @ObjectIdConstraint ObjectId studentId,
            @RequestParam(value = "status") Boolean status
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.setRequestStatus(
                getAdvisorUser(request).getObjectId("_id"), scheduleId, studentId, status
        );
    }

    @GetMapping(value = "getRequests")
    @ResponseBody
    public String getRequests(
            HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getRequests(getAdvisorUser(request).getObjectId("_id"));
    }


    @GetMapping(value = "getAllTags")
    @ResponseBody
    public String getAllTags(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachTagReportController.getAllTags(TeachReportTagMode.TEACHER.getName());
    }
}
