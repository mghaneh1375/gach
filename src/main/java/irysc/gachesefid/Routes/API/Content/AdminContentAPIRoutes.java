package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.AdminContentController;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/package_content/manage")
@Validated
public class AdminContentAPIRoutes extends Router {

    @PostMapping(value = "store")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "title", "description", "teacher",
                                        "price", "sessionsCount", "visibility"

                                },
                                paramsType = {
                                        String.class, String.class, String.class,
                                        Positive.class, Positive.class, Boolean.class,
                                },
                                optionals = {
                                        "teacherBio", "certId", "preReq",
                                        "duration", "finalExamId", "finalExamMinMark",
                                        "tags"
                                },
                                optionalsType = {
                                        String.class, ObjectId.class, String.class,
                                        Positive.class, ObjectId.class, Positive.class,
                                        JSONArray.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.store(convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "setImg/{id}")
    @ResponseBody
    public String setImg(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id,
                         @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);

        return AdminContentController.setImg(id, file);
    }

    @PutMapping(value = "addSession/{id}")
    @ResponseBody
    public String addSession(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId id,
                             @RequestBody @StrongJSONConstraint(
                                     params = {
                                             "title", "duration", "description",
                                             "visibility", "priority"

                                     },
                                     paramsType = {
                                             String.class, Positive.class, String.class,
                                             Boolean.class, Positive.class
                                     },
                                     optionals = {
                                             "price", "examId", "minMark",
                                     },
                                     optionalsType = {
                                             Positive.class, ObjectId.class, Positive.class,
                                     }
                             ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.addSession(id, convertPersian(new JSONObject(jsonStr)));
    }


    @PutMapping(value = "addٰVideoToSession/{id}/{sessionId}")
    @ResponseBody
    public String addٰVideoToSession(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId id,
                                     @PathVariable @ObjectIdConstraint ObjectId sessionId,
                                     @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AdminContentController.addٰVideoToSession(id, sessionId, file);
    }


    @PutMapping(value = "addٰAttachToSession/{id}/{sessionId}")
    @ResponseBody
    public String addٰAttachToSession(HttpServletRequest request,
                                      @PathVariable @ObjectIdConstraint ObjectId id,
                                      @PathVariable @ObjectIdConstraint ObjectId sessionId,
                                      @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AdminContentController.addٰAttachToSession(id, sessionId, file);
    }

    @DeleteMapping(value = "removeSession/{id}")
    @ResponseBody
    public String removeSession(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "items"

                                        },
                                        paramsType = {
                                                JSONArray.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.removeSession(
                id, new JSONObject(jsonStr).getJSONArray("items")
        );
    }

}
