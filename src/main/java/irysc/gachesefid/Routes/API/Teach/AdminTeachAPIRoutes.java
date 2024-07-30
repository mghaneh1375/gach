package irysc.gachesefid.Routes.API.Teach;

import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Controllers.Teaching.TeachTagReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
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
            HttpServletRequest request,
            @RequestParam(required = false, value = "userId") ObjectId userId,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "teachMode") String teachMode,
            @RequestParam(required = false, value = "activeMode") String activeMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachController.getSchedules(
                userId, from, to, activeMode, true, null, teachMode
        );
    }

    @PutMapping(value = "setAdvisorIRYSCPercent/{advisorId}")
    @ResponseBody
    public String setAdvisorIRYSCPercent(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId advisorId,
            @RequestParam(value = "teachPercent") @Min(0) @Max(100) Integer teachPercent,
            @RequestParam(value = "advicePercent") @Min(0) @Max(100) Integer advicePercent
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.setAdvisorIRYSCPercent(advisorId, teachPercent, advicePercent);
    }

    @GetMapping(value = "getAdvisorIRYSCPercent/{advisorId}")
    @ResponseBody
    public String getAdvisorIRYSCPercent(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId advisorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.getAdvisorIRYSCPercent(advisorId);
    }

    @GetMapping(value = "getAllTeachersDigest")
    @ResponseBody
    public String getAllTeachersDigest(
            HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.getAllTeachersDigest();
    }

    @GetMapping(value = "getTransactions")
    @ResponseBody
    public String getTransactions(
            HttpServletRequest request,
            @RequestParam(required = false, value = "teacherId") ObjectId teacherId,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "justSettlements") Boolean justSettlements
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.getTransactions(teacherId, from, to, justSettlements);
    }

    @PutMapping(value = "setTeachReportAsSeen/{id}")
    @ResponseBody
    public String setTeachReportAsSeen(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.setTeachReportAsSeen(id);
    }

    @GetMapping(value = "getTeachReports")
    @ResponseBody
    public String getTeachReports(
            HttpServletRequest request,
            @RequestParam(required = false, name = "teachId") ObjectId teachId,
            @RequestParam(required = false, name = "teacherId") ObjectId teacherId,
            @RequestParam(required = false, name = "from") Long from,
            @RequestParam(required = false, name = "to") Long to,
            @RequestParam(required = false, name = "showJustUnSeen") Boolean showJustUnSeen,
            @RequestParam(required = false, name = "justSendFromStudent") Boolean justSendFromStudent,
            @RequestParam(required = false, name = "justSendFromTeacher") Boolean justSendFromTeacher
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TeachController.getTeachReports(
                from, to, showJustUnSeen,
                teacherId, justSendFromStudent,
                justSendFromTeacher, teachId
        );
    }

    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public String getAllReportTags(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachTagReportController.getAllReportTags(null, true);
    }

    @PostMapping(value = "createReportTag")
    @ResponseBody
    public String createReportTag(HttpServletRequest request,
                                  @RequestBody @StrongJSONConstraint(
                                          params = {
                                                  "label", "priority",
                                                  "mode", "visibility"
                                          },
                                          paramsType = {
                                                  String.class, Positive.class,
                                                  TeachReportTagMode.class,
                                                  Boolean.class
                                          }
                                  ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachTagReportController.createTag(new JSONObject(jsonStr));
    }

    @PutMapping(value = "editReportTag/{tagId}")
    @ResponseBody
    public String editReportTag(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId tagId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "label", "priority",
                                                "mode", "visibility"
                                        },
                                        paramsType = {
                                                String.class, Integer.class,
                                                TeachReportTagMode.class,
                                                Boolean.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachTagReportController.editTag(tagId, new JSONObject(jsonStr));
    }

    @DeleteMapping(value = "removeTags")
    @ResponseBody
    public String removeTags(HttpServletRequest request,
                             @RequestBody @StrongJSONConstraint(
                                     params = {"items"},
                                     paramsType = {JSONArray.class}
                             ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachTagReportController.removeTags(new JSONObject(jsonStr).getJSONArray("items"));
    }
}
