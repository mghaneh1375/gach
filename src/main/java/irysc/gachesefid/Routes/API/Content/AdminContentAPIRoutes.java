package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.AdminContentController;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Controllers.ContentController;
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


    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id,
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
        return AdminContentController.update(id, convertPersian(new JSONObject(jsonStr)));
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


    @DeleteMapping(value = "removeImg/{id}")
    @ResponseBody
    public String removeImg(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.removeImg(id);
    }


    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
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
        return AdminContentController.remove(
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }


    @GetMapping(value = "fetchSessions/{id}")
    @ResponseBody
    public String fetchSessions(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.fetchSessions(id);
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

    @PutMapping(value = "updateSession/{id}/{sessionId}")
    @ResponseBody
    public String updateSession(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId id,
                                @PathVariable @ObjectIdConstraint ObjectId sessionId,
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
        return AdminContentController.updateSession(id, sessionId, convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "removeVideoFromSession/{id}/{sessionId}")
    @ResponseBody
    public String removeVideoFromSession(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @PathVariable @ObjectIdConstraint ObjectId sessionId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminContentController.removeVideoFromSession(id, sessionId);
    }


    @PutMapping(value = "setSessionVideo/{id}/{sessionId}")
    @ResponseBody
    public String setSessionVideo(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId id,
                                     @PathVariable @ObjectIdConstraint ObjectId sessionId,
                                     @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AdminContentController.setSessionVideo(id, sessionId, file);
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
