package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.StudentTeachController;
import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.TeachMode;
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
    ) throws UnAuthException {
        return TeachController.copySchedule(
                getUserId(request),
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
    ) throws UnAuthException {
        return TeachController.updateSchedule(
                getUserId(request), id,
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
    ) throws UnAuthException {
        return TeachController.getSchedules(
                getUserId(request), from, to,
                activeMode, justHasStudents, justHasRequests, teachMode
        );
    }

    @GetMapping(value = "getSchedule/{scheduleId}")
    @ResponseBody
    public String getSchedule(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws UnAuthException {
        return TeachController.getSchedule(
                getUserId(request), scheduleId
        );
    }

    @GetMapping(value = "getScheduleStudents/{scheduleId}")
    @ResponseBody
    public String getScheduleStudents(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws UnAuthException {
        UserTokenInfo userTokenInfo = getUserTokenInfo(request);
        return TeachController.getScheduleStudents(
                Authorization.isAdmin(userTokenInfo.getAccesses()) ?
                        null : userTokenInfo.getId(), scheduleId
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
    ) throws UnAuthException {
        return TeachController.removeSchedule(
                getUserId(request), scheduleId
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
    ) throws UnAuthException {
        return TeachController.getRequests(
                expireMode, statusMode, teachMode,
                getUserId(request)
        );
    }


    //todo: not used in UI
//    @GetMapping(value = "getAllReportTags")
//    @ResponseBody
//    public String getAllReportTags() {
//        return TeachTagReportController.getAllReportTags(
//                TeachReportTagMode.TEACHER.getName(), false
//        );
//    }

    @GetMapping(value = "getMyTeachScheduleReportProblems/{scheduleId}")
    @ResponseBody
    public String getMyTeachScheduleReportProblems(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId scheduleId
    ) throws UnAuthException {
        return StudentTeachController.getMyTeachScheduleReportProblems(
                getUserId(request), scheduleId, false
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
    ) throws UnAuthException {
        JSONObject jsonObject;
        if(jsonStr == null || jsonStr.isEmpty()) jsonObject = new JSONObject();
        else jsonObject = new JSONObject(jsonStr);

        return TeachController.setTeachScheduleReportProblems(
                getUserId(request), scheduleId, studentId,
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
    ) throws UnAuthException {
        return TeachController.getMyTransactions(
                getUserId(request),
                from, to, justSettlements, needCanRequestSettlement
        );
    }

    @PostMapping(value = "settlementRequest")
    @ResponseBody
    public String settlementRequest(HttpServletRequest request
    ) throws UnAuthException {
        return TeachController.settlementRequest(getUserId(request));
    }

    @DeleteMapping(value = "cancelSettlementRequest")
    @ResponseBody
    public String cancelSettlementRequest(HttpServletRequest request
    ) throws UnAuthException {
        return TeachController.cancelSettlementRequest(getUserId(request));
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
    ) throws UnAuthException {
        return TeachController.getMySettledRequests(
                getUserId(request),
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
    ) throws UnAuthException {
        return TeachController.rateToSchedule(
                getUserId(request), scheduleId,
                studentId, rate
        );
    }
}
