package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Controllers.QuestionReport.QuestionReportController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
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

import static irysc.gachesefid.Main.GachesefidApplication.adviseTagRepository;

@Controller
@RequestMapping(path = "/api/questionReport/manage")
@Validated
public class QuestionReportAPIRoutes extends Router {

    @GetMapping(value = "getAllTags")
    @ResponseBody
    public String getAllTags(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return QuestionReportController.getAllTags();
    }

    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return QuestionReportController.remove(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(value = "addTag")
    @ResponseBody
    public String addTag(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"label", "priority", "canHasDesc"},
                                 paramsType = {
                                         String.class, Positive.class, Boolean.class
                                 }
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return QuestionReportController.create(new JSONObject(jsonStr));
    }

    @PostMapping(value = "editTag/{id}")
    @ResponseBody
    public String editTag(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId id,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"label"},
                                  paramsType = {
                                          String.class
                                  }
                          ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.createTag(adviseTagRepository, new JSONObject(jsonStr));
    }

}
