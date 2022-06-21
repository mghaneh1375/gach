package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.ContentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Test.Content.ContentTestController;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static irysc.gachesefid.Main.GachesefidApplication.branchRepository;
import static irysc.gachesefid.Main.GachesefidApplication.gradeRepository;
import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.Utility.printException;

@Controller
@RequestMapping(path = "/api/admin/content")
@Validated
public class ContentAPIRoutes extends Router {

    @PostMapping(value = "/test")
    @ResponseBody
    public String test(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (!DEV_MODE)
            return "not allowed in main server";

        getAdminPrivilegeUserVoid(request);
        String msg;

        try {
            new ContentTestController();
            msg = "success";
        } catch (Exception x) {
            msg = x.getMessage();
            printException(x);
        }

        return msg;
    }

    @PostMapping(value = "/addBatch")
    @ResponseBody
    public String addBatch(HttpServletRequest request,
                           @RequestBody @NotNull MultipartFile file)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
//        getAdminPrivilegeUserVoid(request);
        return ContentController.addBatch(file);
    }

    @PostMapping(value = "/addBranch")
    @ResponseBody
    public String addBranch(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"name"},
                                   paramsType = {String.class}
                           ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
//        getAdminPrivilegeUserVoid(request);
        return ContentController.addBranch(new JSONObject(jsonStr).getString("name"));
    }

    @PostMapping(value = "/addGrade")
    @ResponseBody
    public String addGrade(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"name"},
                                   paramsType = {String.class}
                           ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.addGrade(new JSONObject(jsonStr).getString("name"));
    }

    @PostMapping(value = "/addLesson/{gradeId}")
    @ResponseBody
    public String addLesson(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId gradeId,
                            @RequestBody @StrongJSONConstraint(params = {"name"},
                                    paramsType = {String.class},
                                    optionals = {"description"},
                                    optionalsType = {String.class}
                            ) String json)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.addLesson(new JSONObject(json), gradeId);
    }

    @PostMapping(value = "/addSubject/{gradeId}/{lessonId}")
    @ResponseBody
    public String addSubject(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId gradeId,
                          @PathVariable @ObjectIdConstraint ObjectId lessonId,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"name", "midPrice", "easyPrice", "hardPrice"},
                                  paramsType = {
                                          String.class, Positive.class,
                                          Positive.class, Positive.class
                                  },
                                  optionals = {"description"},
                                  optionalsType = {String.class}
                          ) @NotBlank String json)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.addSubject(gradeId, lessonId, new JSONObject(json));
    }

    @PostMapping(value = "/updateGrade/{gradeId}")
    @ResponseBody
    public String updateGrade(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId gradeId,
                              @RequestBody @StrongJSONConstraint(
                                      params = {"name"},
                                      paramsType = {String.class}
                              ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.updateGrade(gradeId, new JSONObject(jsonStr).getString("name"));
    }

    @PostMapping(value = "/updateLesson/{gradeId}/{lessonId}")
    @ResponseBody
    public String updateLesson(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId gradeId,
                               @PathVariable @ObjectIdConstraint ObjectId lessonId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {},
                                       paramsType = {},
                                       optionals = {"name", "description"},
                                       optionalsType = {
                                               String.class, String.class
                                       }
                               ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.updateLesson(gradeId, lessonId, new JSONObject(jsonStr));
    }

    @PostMapping(value = "/updateSubject/{subjectId}")
    @ResponseBody
    public String updateSubject(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId subjectId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {},
                                        paramsType = {},
                                        optionals = {"name", "hardPrice",
                                                "midPrice", "easyPrice",
                                                "gradeId", "lessonId",
                                                "description"
                                        },
                                        optionalsType = {
                                                String.class, Positive.class,
                                                Positive.class, Positive.class,
                                                String.class, String.class,
                                                String.class
                                        }
                                ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.editSubject(subjectId, new JSONObject(jsonStr));
    }

    @DeleteMapping(value = "/deleteGrade/{gradeId}")
    @ResponseBody
    public String deleteGrade(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId gradeId)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.delete(gradeId);
    }

    @DeleteMapping(value = "/deleteLesson/{gradeId}/{lessonId}")
    @ResponseBody
    public String deleteLesson(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId gradeId,
                               @PathVariable @ObjectIdConstraint ObjectId lessonId)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.deleteLesson(gradeId, lessonId);
    }

    @DeleteMapping(value = "/deleteSubject/{subjectId}")
    @ResponseBody
    public String deleteSubject(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId subjectId)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.deleteSubject(subjectId);
    }

    @GetMapping(value = "/grades")
    @ResponseBody
    public String grades() {
        return ContentController.getGradesOrBranches(gradeRepository);
    }

    @GetMapping(value = "/branches")
    @ResponseBody
    public String branches() {
        return ContentController.getGradesOrBranches(branchRepository);
    }

    @GetMapping(value = "/all")
    @ResponseBody
    public String all(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.all();
    }

    @GetMapping(value = "/search")
    @ResponseBody
    public String search(HttpServletRequest request,
                      @RequestParam(value = "key") @NotBlank String key
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.search(key);
    }

    @GetMapping(value = "/allSubjects")
    @ResponseBody
    public String allSubjects(HttpServletRequest request,
                              @RequestParam(required = false, value = "gradeId") ObjectId gradeId,
                              @RequestParam(required = false, value = "lessonId") ObjectId lessonId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.allSubjects(gradeId, lessonId);
    }
}
