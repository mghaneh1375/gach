package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.ProfileConfigController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/profile/public")
@Validated
public class ProfileAPIRoutes extends Router {

    @GetMapping(value = "getMyConfig")
    @ResponseBody
    public String getMyConfig(HttpServletRequest request
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return ProfileConfigController.getMyConfig(getUser(request).getObjectId("_id"));
    }

    @PutMapping(value = "setMyConfig")
    @ResponseBody
    public String setMyConfig(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "showContentPackages", "showQuizzes",
                            "showMyAdvisor", "showTeachers",
                            "showMyComments", "showMyRate",
                            "showGrade", "showBranch",
                            "showSchool", "showCity"
                    },
                    paramsType = {
                            Boolean.class, Boolean.class,
                            Boolean.class, Boolean.class,
                            Boolean.class, Boolean.class,
                            Boolean.class, Boolean.class,
                            Boolean.class, Boolean.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return ProfileConfigController.setMyConfig(
                getUser(request).getObjectId("_id"),
                new JSONObject(jsonStr)
        );
    }

    @GetMapping("getUserProfile/{userId}")
    @ResponseBody
    public String getUserProfile(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserProfile(userId);
    }

    @GetMapping("getUserComments/{userId}")
    @ResponseBody
    public String getUserComments(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserComments(userId);
    }

    @GetMapping("getUserAdvisors/{userId}")
    @ResponseBody
    public String getUserAdvisors(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserAdvisors(userId);
    }

    @GetMapping("getUserTeachers/{userId}")
    @ResponseBody
    public String getUserTeachers(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserTeachers(userId);
    }

    @GetMapping("getUserContents/{userId}")
    @ResponseBody
    public String getUserContents(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserContents(userId);
    }

    @GetMapping("getUserQuizzes/{userId}")
    @ResponseBody
    public String getUserQuizzes(
            @PathVariable @ObjectIdConstraint ObjectId userId
    ) {
        return ProfileConfigController.getUserQuizzes(userId);
    }
}
