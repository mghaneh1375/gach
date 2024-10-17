package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Controllers.Teaching.TeachTagReportController;
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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/teach/admin/")
@Validated
public class AdminTeachAPIRoutes extends Router {

    @GetMapping(value = "getSchedules")
    @ResponseBody
    public String getSchedules(
            @RequestParam(required = false, value = "userId") ObjectId userId,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "teachMode") String teachMode,
            @RequestParam(required = false, value = "activeMode") String activeMode
    ) {
        return TeachController.getSchedules(userId, from, to, activeMode, true, null, teachMode);
    }

    @PutMapping(value = "setAdvisorIRYSCPercent/{advisorId}")
    @ResponseBody
    public String setAdvisorIRYSCPercent(
            @PathVariable @ObjectIdConstraint ObjectId advisorId,
            @RequestParam(value = "teachPercent") @Min(0) @Max(100) Integer teachPercent,
            @RequestParam(value = "advicePercent") @Min(0) @Max(100) Integer advicePercent
    ) {
        return TeachController.setAdvisorIRYSCPercent(advisorId, teachPercent, advicePercent);
    }

    @GetMapping(value = "getAdvisorIRYSCPercent/{advisorId}")
    @ResponseBody
    public String getAdvisorIRYSCPercent(@PathVariable @ObjectIdConstraint ObjectId advisorId) {
        return TeachController.getAdvisorIRYSCPercent(advisorId);
    }

    @GetMapping(value = "getAllTeachersDigest")
    @ResponseBody
    public String getAllTeachersDigest() {
        return TeachController.getAllTeachersDigest();
    }

    @GetMapping(value = "getTransactions")
    @ResponseBody
    public String getTransactions(
            @RequestParam(required = false, value = "teacherId") ObjectId teacherId,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "justSettlements") Boolean justSettlements
    ) {
        return TeachController.getTransactions(teacherId, from, to, justSettlements);
    }

    @PutMapping(value = "setTeachReportAsSeen/{id}")
    @ResponseBody
    public String setTeachReportAsSeen(@PathVariable @ObjectIdConstraint ObjectId id) {
        return TeachController.setTeachReportAsSeen(id);
    }

    @GetMapping(value = "getTeachReports")
    @ResponseBody
    public String getTeachReports(
            @RequestParam(required = false, name = "teachId") ObjectId teachId,
            @RequestParam(required = false, name = "teacherId") ObjectId teacherId,
            @RequestParam(required = false, name = "from") Long from,
            @RequestParam(required = false, name = "to") Long to,
            @RequestParam(required = false, name = "showJustUnSeen") Boolean showJustUnSeen,
            @RequestParam(required = false, name = "justSendFromStudent") Boolean justSendFromStudent,
            @RequestParam(required = false, name = "justSendFromTeacher") Boolean justSendFromTeacher
    ) {
        return TeachController.getTeachReports(from, to, showJustUnSeen, teacherId, justSendFromStudent, justSendFromTeacher, teachId);
    }

    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public String getAllReportTags() {
        return TeachTagReportController.getAllReportTags(null, true);
    }

    @PostMapping(value = "createReportTag")
    @ResponseBody
    public String createReportTag(
            @RequestBody @StrongJSONConstraint(
                    params = {"label", "priority", "mode", "visibility"},
                    paramsType = {String.class, Positive.class, TeachReportTagMode.class, Boolean.class}
            ) @NotBlank String jsonStr
    ) {
        return TeachTagReportController.createTag(new JSONObject(jsonStr));
    }

    @PutMapping(value = "editReportTag/{tagId}")
    @ResponseBody
    public String editReportTag(
            @PathVariable @ObjectIdConstraint ObjectId tagId,
            @RequestBody @StrongJSONConstraint(
                    params = {"label", "priority", "mode", "visibility"},
                    paramsType = {String.class, Integer.class, TeachReportTagMode.class, Boolean.class}
            ) @NotBlank String jsonStr
    ) {
        return TeachTagReportController.editTag(tagId, new JSONObject(jsonStr));
    }

    @DeleteMapping(value = "removeTags")
    @ResponseBody
    public String removeTags(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"}, paramsType = {JSONArray.class}
            ) String jsonStr
    ) {
        return TeachTagReportController.removeTags(new JSONObject(jsonStr).getJSONArray("items"));
    }
}
