package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.StudentTeachController;
import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Controllers.Teaching.TeachTagReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;

import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
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
                                                    Long.class, Positive.class,
                                                    Boolean.class, TeachMode.class
                                            },
                                            optionals = {
                                                    "description", "price",
                                                    "minCap", "maxCap", "title",
                                                    "needRegistryConfirmation"
                                            },
                                            optionalsType = {
                                                    String.class, Positive.class,
                                                    Positive.class, Positive.class,
                                                    String.class, Boolean.class
                                            }
                                    ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.createNewSchedule(
                getAdvisorUser(request), convertPersian(new JSONObject(jsonStr))
        );
    }

    @PostMapping(value = "copySchedule/{scheduleId}")
    @ResponseBody
    public String copySchedule(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId scheduleId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"start"},
                                       paramsType = {Long.class}
                               ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.copySchedule(
                getAdvisorUser(request).getObjectId("_id"),
                scheduleId, convertPersian(new JSONObject(jsonStr))
        );
    }

    @PutMapping(value = "updateSchedule/{id}")
    @ResponseBody
    public String updateSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "start", "length",
                            "visibility", "teachMode",
                            "price"
                    },
                    paramsType = {
                            Long.class, Positive.class,
                            Boolean.class, TeachMode.class,
                            Positive.class
                    },
                    optionals = {
                            "description", "title",
                            "minCap", "maxCap",
                            "needRegistryConfirmation"
                    },
                    optionalsType = {
                            String.class, String.class,
                            Positive.class, Positive.class,
                            Boolean.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.updateSchedule(
                getAdvisorUser(request).getObjectId("_id"), id,
                convertPersian(new JSONObject(jsonStr))
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
            @RequestParam(required = false, value = "teachMode") String teachMode,
            @RequestParam(required = false, value = "activeMode") String activeMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getSchedules(
                getAdvisorUser(request).getObjectId("_id"), from, to,
                activeMode, justHasStudents, justHasRequests, teachMode
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

    @GetMapping(value = "getScheduleStudents/{scheduleId}")
    @ResponseBody
    public String getScheduleStudents(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAdvisorUser(request);
        return TeachController.getScheduleStudents(
                Authorization.isAdmin(user.getList("accesses", String.class)) ?
                        null : user.getObjectId("_id"), scheduleId
        );
    }

    @PutMapping(value = "createMeetingRoom/{scheduleId}")
    @ResponseBody
    public String createMeetingRoom(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAdvisorUser(request);
        return TeachController.createMeetingRoom(
                user.getObjectId("_id"),
                user.getString("first_name") + " " + user.getString("last_name"),
                user.getString("NID"), scheduleId
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
        Document user = getAdvisorUser(request);
        return TeachController.setRequestStatus(
                user.getObjectId("_id"),
                user.getString("first_name") + " " + user.getString("last_name"),
                scheduleId, studentId, status
        );
    }

    @GetMapping(value = "getRequests")
    @ResponseBody
    public String getRequests(
            HttpServletRequest request,
            @RequestParam(required = false, value = "statusMode") String statusMode,
            @RequestParam(required = false, value = "expireMode") String expireMode,
            @RequestParam(required = false, value = "teachMode") String teachMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getRequests(
                expireMode, statusMode, teachMode,
                getAdvisorUser(request).getObjectId("_id")
        );
    }


    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public String getAllReportTags() {
        return TeachTagReportController.getAllReportTags(
                TeachReportTagMode.TEACHER.getName(), false
        );
    }

    @GetMapping(value = "getMyTeachScheduleReportProblems/{scheduleId}")
    @ResponseBody
    public String getMyTeachScheduleReportProblems(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        return StudentTeachController.getMyTeachScheduleReportProblems(
                getAdvisorUser(request).getObjectId("_id"), scheduleId, false
        );
    }

    @PutMapping(value = "setTeachScheduleReportProblems/{scheduleId}/{studentId}")
    @ResponseBody
    public String setTeachScheduleReportProblems(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @PathVariable @ObjectIdConstraint ObjectId studentId,
            @RequestBody @StrongJSONConstraint(
                    params = {}, paramsType = {},
                    optionals = {"tagIds", "desc"}, optionalsType = {JSONArray.class, String.class}
            ) String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        JSONObject jsonObject;
        if(jsonStr == null || jsonStr.isEmpty()) jsonObject = new JSONObject();
        else jsonObject = new JSONObject(jsonStr);

        return TeachController.setTeachScheduleReportProblems(
                getAdvisorUser(request).getObjectId("_id"), scheduleId, studentId,
                jsonObject.has("tagIds") ? jsonObject.getJSONArray("tagIds") : null,
                jsonObject.has("desc") ? jsonObject.getString("desc") : null
        );
    }

    @GetMapping(value = "getMyTransactions")
    @ResponseBody
    public String getMyTransactions(
            HttpServletRequest request,
            @RequestParam(value = "needCanRequestSettlement") boolean needCanRequestSettlement,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "justSettlements") Boolean justSettlements
    ) throws UnAuthException, NotActivateAccountException {
        return TeachController.getMyTransactions(
                getUserId(request),
                from, to, justSettlements, needCanRequestSettlement
        );
    }

    @PostMapping(value = "settlementRequest")
    @ResponseBody
    public String settlementRequest(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.settlementRequest(
                getAdvisorUser(request).getObjectId("_id")
        );
    }

    @DeleteMapping(value = "cancelSettlementRequest")
    @ResponseBody
    public String cancelSettlementRequest(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.cancelSettlementRequest(
                getAdvisorUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "getMySettledRequests")
    @ResponseBody
    public String getMySettledRequests(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "createdFrom", required = false) Long createdFrom,
            @RequestParam(value = "createdTo", required = false) Long createdTo,
            @RequestParam(value = "answerFrom", required = false) Long answerFrom,
            @RequestParam(value = "answerTo", required = false) Long answerTo
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.getMySettledRequests(
                getAdvisorUser(request).getObjectId("_id"),
                status, createdFrom, createdTo, answerFrom, answerTo
        );
    }

    @PutMapping(value = "rateToSchedule/{scheduleId}/{studentId}")
    @ResponseBody
    public String rateToSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId,
            @PathVariable @ObjectIdConstraint ObjectId studentId,
            @RequestParam(value = "rate") @Min(1) @Max(5) Integer rate
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return TeachController.rateToSchedule(
                getAdvisorUser(request).getObjectId("_id"), scheduleId,
                studentId, rate
        );
    }
}
