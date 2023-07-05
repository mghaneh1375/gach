package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
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

import static irysc.gachesefid.Main.GachesefidApplication.*;

@Controller
@RequestMapping(path = "/api/advisor/tag")
@Validated
public class AdminAdvisorAPIRoutes extends Router {

    @GetMapping(value = "getAllTags")
    @ResponseBody
    public String getAllTags(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdvisorUser(request);
        return AdvisorController.getAllTags(adviseTagRepository);
    }

    @GetMapping(value = "getAllExamTags")
    @ResponseBody
    public String getAllExamTags(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        getUserWithOutCheckCompletenessVoid(request);
        return AdvisorController.getAllTags(adviseExamTagRepository);
    }

    @GetMapping(value = "getAllLifeTags")
    @ResponseBody
    public String getAllLifeTags(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        getUserWithOutCheckCompletenessVoid(request);
        return AdvisorController.getAllTags(lifeStyleTagRepository);
    }

    @DeleteMapping(value = "removeTags")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.removeTags(adviseTagRepository, new JSONObject(jsonStr).getJSONArray("items"));
    }

    @DeleteMapping(value = "removeExamTags")
    @ResponseBody
    public String removeExamTags(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = {JSONArray.class}
                                 ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.removeTags(adviseExamTagRepository, new JSONObject(jsonStr).getJSONArray("items"));
    }

    @DeleteMapping(value = "removeLifeTags")
    @ResponseBody
    public String removeLifeTags(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = {JSONArray.class}
                                 ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.removeTags(lifeStyleTagRepository, new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(value = "createTag")
    @ResponseBody
    public String createTag(HttpServletRequest request,
                            @RequestBody @StrongJSONConstraint(
                                    params = {"label"},
                                    paramsType = {String.class},
                                    optionals = {"numberLabel"},
                                    optionalsType = {String.class}
                            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.createTag(adviseTagRepository, new JSONObject(jsonStr));
    }

    @PostMapping(value = "editTag/{id}")
    @ResponseBody
    public String editTag(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId id,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"label"},
                                  paramsType = {String.class},
                                  optionals = {"numberLabel"},
                                  optionalsType = {String.class}
                          ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.editTag(adviseTagRepository, id, new JSONObject(jsonStr));
    }

    @PostMapping(value = "createExamTag")
    @ResponseBody
    public String createExamTag(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"label"},
                                        paramsType = {
                                                String.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.createTag(adviseExamTagRepository, new JSONObject(jsonStr));
    }

    @PostMapping(value = "createLifeTag")
    @ResponseBody
    public String createLifeTag(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"label"},
                                        paramsType = {
                                                String.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AdvisorController.createTag(lifeStyleTagRepository, new JSONObject(jsonStr));
    }

}
