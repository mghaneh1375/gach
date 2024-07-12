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
            @RequestParam(required = false, value = "justHasStudents") Boolean justHasStudents,
            @RequestParam(required = false, value = "justHasRequests") Boolean justHasRequests,
            @RequestParam(required = false, value = "teachMode") String teachMode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return TeachController.getSchedules(
                userId, from, to,
                justHasStudents, justHasRequests, teachMode
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
