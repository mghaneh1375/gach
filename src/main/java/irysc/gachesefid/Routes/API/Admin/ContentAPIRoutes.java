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
import org.json.JSONArray;
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
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;
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
        getAdminPrivilegeUserVoid(request);
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

    @PostMapping(value = "/addLesson/{mode}/{gradeId}")
    @ResponseBody
    public String addLesson(HttpServletRequest request,
                            @PathVariable @NotBlank String mode,
                            @PathVariable @ObjectIdConstraint ObjectId gradeId,
                            @RequestBody @StrongJSONConstraint(params = {"name"},
                                    paramsType = {String.class},
                                    optionals = {"description"},
                                    optionalsType = {String.class}
                            ) String json)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (!mode.equalsIgnoreCase("grade") && !mode.equalsIgnoreCase("branch"))
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);

        return ContentController.addLesson(
                mode.equalsIgnoreCase("grade") ? gradeRepository : branchRepository,
                new JSONObject(json), gradeId
        );
    }

    @PostMapping(value = "/addSubject/{gradeId}/{lessonId}")
    @ResponseBody
    public String addSubject(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId gradeId,
                             @PathVariable @ObjectIdConstraint ObjectId lessonId,
                             @RequestBody @StrongJSONConstraint(
                                     params = {
                                             "name", "midPrice",
                                             "easyPrice", "hardPrice",
                                             "schoolMidPrice", "schoolEasyPrice",
                                             "schoolHardPrice"
                                     },
                                     paramsType = {
                                             String.class, Positive.class,
                                             Positive.class, Positive.class,
                                             Positive.class, Positive.class,
                                             Positive.class
                                     },
                                     optionals = {"description"},
                                     optionalsType = {String.class}
                             ) @NotBlank String json)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.addSubject(gradeId, lessonId, convertPersian(new JSONObject(json)));
    }

    @PutMapping(value = "/updateBranch/{branchId}")
    @ResponseBody
    public String updateBranch(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId branchId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"name", "isOlympiad"},
                                       paramsType = {String.class, Boolean.class}
                               ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        JSONObject jsonObject = new JSONObject(jsonStr);
        return ContentController.updateBranch(
                branchId,
                jsonObject.getString("name"),
                jsonObject.getBoolean("isOlympiad")
        );
    }

    @PutMapping(value = "/updateGrade/{gradeId}")
    @ResponseBody
    public String updateGrade(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId gradeId,
                              @RequestBody @StrongJSONConstraint(
                                      params = {"name", "isOlympiad"},
                                      paramsType = {String.class, Boolean.class}
                              ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        JSONObject jsonObject = new JSONObject(jsonStr);
        return ContentController.updateGrade(
                gradeId,
                jsonObject.getString("name"),
                jsonObject.getBoolean("isOlympiad")
        );
    }

    @PutMapping(value = "/updateLesson/{mode}/{gradeId}/{lessonId}")
    @ResponseBody
    public String updateLesson(HttpServletRequest request,
                               @PathVariable @NotBlank String mode,
                               @PathVariable @ObjectIdConstraint ObjectId gradeId,
                               @PathVariable @ObjectIdConstraint ObjectId lessonId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {},
                                       paramsType = {},
                                       optionals = {"name", "description", "gradeId"},
                                       optionalsType = {
                                               String.class, String.class, ObjectId.class
                                       }
                               ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (!mode.equalsIgnoreCase("grade") && !mode.equalsIgnoreCase("branch"))
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return ContentController.updateLesson(
                mode.equalsIgnoreCase("grade") ? gradeRepository : branchRepository,
                gradeId, lessonId, new JSONObject(jsonStr)
        );
    }

    @PutMapping(value = "/updateSubject/{subjectId}")
    @ResponseBody
    public String updateSubject(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId subjectId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {},
                                        paramsType = {},
                                        optionals = {
                                                "name", "hardPrice",
                                                "midPrice", "easyPrice",
                                                "schoolHardPrice", "schoolMidPrice",
                                                "schoolEasyPrice", "gradeId",
                                                "lessonId", "description"
                                        },
                                        optionalsType = {
                                                String.class, Positive.class,
                                                Positive.class, Positive.class,
                                                Positive.class, Positive.class,
                                                Positive.class, String.class,
                                                String.class, String.class
                                        }
                                ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.editSubject(subjectId, convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "/deleteGrades")
    @ResponseBody
    public String deleteGrades(HttpServletRequest request,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"items"},
                                       paramsType = {JSONArray.class}
                               ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.deleteGrade(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @DeleteMapping(value = "/deleteLessons/{mode}")
    @ResponseBody
    public String deleteLessons(HttpServletRequest request,
                                @PathVariable @NotBlank String mode,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items"},
                                        paramsType = {JSONArray.class}) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (!mode.equalsIgnoreCase("grade") && !mode.equalsIgnoreCase("branch"))
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return ContentController.deleteLessons(
                mode.equalsIgnoreCase("grade") ? gradeRepository : branchRepository,
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @DeleteMapping(value = "/deleteSubjects")
    @ResponseBody
    public String deleteSubject(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items"},
                                        paramsType = {JSONArray.class}
                                ) @NotBlank String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.deleteSubjects(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @GetMapping(value = "/gradesAndBranches")
    @ResponseBody
    public String gradesAndBranches() {
        return ContentController.getGradesAndBranches();
    }

    @GetMapping(value = "/grades")
    @ResponseBody
    public String grades() {
        return ContentController.getGradesOrBranches(gradeRepository, false);
    }

    @GetMapping(value = "/branches")
    @ResponseBody
    public String branches() {
        return ContentController.getGradesOrBranches(branchRepository, true);
    }

    @GetMapping(value = "/lessonsInGrade")
    @ResponseBody
    public String lessonsInGrade() {
        return ContentController.getLessons(gradeRepository);
    }

    @GetMapping(value = "/lessonsInBranch")
    @ResponseBody
    public String lessonsInBranch() {
        return ContentController.getLessons(branchRepository);
    }

    @GetMapping(value = "/getLessonsDigest")
    @ResponseBody
    public String getLessonsDigest(@RequestParam(required = false, value = "searchInBranches") Boolean searchInBranches,
                                   @RequestParam(required = false, value = "parentId") ObjectId parentId
    ) {
        return ContentController.getLessonsDigest(
                searchInBranches == null || !searchInBranches ? gradeRepository : branchRepository,
                parentId
        );
    }


    @GetMapping(value = "/gradeLessons")
    @ResponseBody
    public String gradeLessons() {
        return ContentController.gradeLessons();
    }


    @GetMapping(value = "/gradeLessonsInGradesAndBranches")
    @ResponseBody
    public String gradeLessonsInGradesAndBranches() {
        return ContentController.gradeLessonsInGradesAndBranches();
    }

    @GetMapping(value = "/all")
    @ResponseBody
    public String all(
            @RequestParam(required = false) ObjectId lessonId,
            @RequestParam(required = false) ObjectId gradeId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String code
    ) {
        return ContentController.all(lessonId, gradeId, subject, code);
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

    @GetMapping(value = "/getSubjectsKeyVals")
    @ResponseBody
    public String getSubjectsKeyVals(
            @RequestParam(required = false, value = "lessonId") ObjectId lessonId
    ) {
        return ContentController.getSubjectsKeyVals(lessonId);
    }

    @PutMapping(value = "updateBatchSubjects")
    @ResponseBody
    public String updateBatchSubjects(HttpServletRequest request,
                                      @RequestBody @StrongJSONConstraint(
                                              params = {
                                                      "midPrice", "easyPrice",
                                                      "hardPrice", "schoolMidPrice",
                                                      "schoolEasyPrice", "schoolHardPrice"
                                              },
                                              paramsType = {
                                                      Positive.class, Positive.class,
                                                      Positive.class, Positive.class,
                                                      Positive.class, Positive.class
                                              }
                                      ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ContentController.updateBatchSubjects(new JSONObject(jsonStr));
    }
}
