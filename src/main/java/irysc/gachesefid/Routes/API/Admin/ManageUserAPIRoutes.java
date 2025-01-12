package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.GradeSchool;
import irysc.gachesefid.Models.PasswordMode;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Digit;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

@Controller
@RequestMapping(path = "/api/admin/user")
@Validated
public class ManageUserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @PutMapping(value = "setPriority/{userId}")
    @ResponseBody
    public String setPriority(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId userId,
            @RequestBody @StrongJSONConstraint(
                    params = {"advisorPriority", "teachPriority"},
                    paramsType = {Positive.class, Positive.class}
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.setPriority(userId, new JSONObject(jsonStr));
    }

//    public String createUser(
//            HttpServletRequest request,
//            @
//    ) {
//
//    }

    @PutMapping(path = "/setCoins/{userId}/{newCoins}")
    @ResponseBody
    public String setCoins(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId userId,
                           @PathVariable @Min(0) int newCoins
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.setCoins(userId, newCoins);
    }

    @PutMapping(value = {"/addAccess/{userId}/{newRole}", "/addAccess/{userId}/{newRole}/{schoolId}"})
    @ResponseBody
    public String addAccess(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId userId,
                            @PathVariable @EnumValidator(enumClazz = Access.class) String newRole,
                            @PathVariable(required = false) String schoolId
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.addAccess(userId, newRole, schoolId);
    }

    @DeleteMapping(value = "/removeUsers")
    @ResponseBody
    public String removeUsers(HttpServletRequest request,
                              @RequestBody @StrongJSONConstraint(
                                      params = {"items"},
                                      paramsType = {JSONArray.class}
                              ) String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.deleteStudents(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @DeleteMapping(value = "/removeAccess/{userId}/{role}")
    @ResponseBody
    public String removeAccess(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId userId,
                               @PathVariable @EnumValidator(enumClazz = Access.class) String role)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.removeAccess(userId, role);
    }

    @PutMapping(value = "/toggleStatus/{userId}")
    @ResponseBody
    public String toggleStatus(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId userId)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return userService.toggleStatus(userId);
    }

    @GetMapping(value = "/fetchTinyUser")
    @ResponseBody
    public String fetchTinyUser(HttpServletRequest request,
                                @RequestParam(value = "level", required = false) String level,
                                @RequestParam(value = "name", required = false) String name,
                                @RequestParam(value = "lastname", required = false) String lastname,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "gradeId", required = false) ObjectId gradeId,
                                @RequestParam(value = "branchId", required = false) ObjectId branchId,
                                @RequestParam(value = "mail", required = false) String mail,
                                @RequestParam(value = "NID", required = false) String NID,
                                @RequestParam(value = "additionalLevel", required = false) String additionalLevel,
                                @RequestParam(value = "justSettled", required = false) Boolean justSettled,
                                @RequestParam(value = "pageIndex", required = false) @Min(0) @Max(10000) Integer pageIndex,
                                @RequestParam(required = false, value = "from") Long from,
                                @RequestParam(required = false, value = "to") Long to
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getEditorPrivilegeUserVoid(request);
        return ManageUserController.fetchTinyUser(
                level, name, lastname,
                phone, mail, NID, gradeId, branchId,
                additionalLevel, justSettled, pageIndex,
                from, to
        );
    }

    @GetMapping(value = "/fetchUser/{unique}")
    @ResponseBody
    public String fetchUser(HttpServletRequest request,
                            @PathVariable @NotBlank String unique)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        return ManageUserController.fetchUser(null, unique,
                Authorization.isAdmin(getPrivilegeUser(request).getList("accesses", String.class))
        );
    }

    @GetMapping(value = "/fetchUserLike")
    @ResponseBody
    public String fetchUserLike(HttpServletRequest request,
                                @RequestParam(value = "nameFa", required = false) String nameFa,
                                @RequestParam(value = "nameEn", required = false) String nameEn,
                                @RequestParam(value = "lastNameFa", required = false) String lastNameFa,
                                @RequestParam(value = "lastNameEn", required = false) String lastNameEn
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);

        if (nameFa == null && nameEn == null && lastNameEn == null && lastNameFa == null)
            return JSON_NOT_VALID_PARAMS;

        return ManageUserController.fetchUserLike(nameEn, lastNameEn, nameFa, lastNameFa);
    }

    @PutMapping(value = "/setAdvisorPercent/{userId}/{percent}")
    @ResponseBody
    public String resetPassword(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId userId,
                                @PathVariable @Min(0) @Max(100) int percent
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.setAdvisorPercent(userId, percent);
    }

    @PostMapping(value = "/signIn/{userId}")
    @ResponseBody
    public String signIn(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        try {

            Document user = userRepository.findById(userId);

            if (user == null || Authorization.isAdmin(user.getList("accesses", String.class)))
                return JSON_NOT_VALID_ID;

            return userService.signIn(user.getString("NID"), "1", false);

        } catch (NotActivateAccountException x) {
            return generateErr("not active account");
        } catch (Exception x) {
            return JSON_NOT_VALID_PARAMS;
        }
    }

    @PostMapping(value = "/checkDuplicate")
    @ResponseBody
    public String checkDuplicate(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {
                                                 "NID", "phone"
                                         },
                                         paramsType = {
                                                 Digit.class, Digit.class
                                         }
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getPrivilegeUser(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        return ManageUserController.checkDuplicate(
                jsonObject.getString("phone"),
                jsonObject.get("NID").toString()
        );
    }

    @PostMapping(value = "/acceptInvite/{reqId}")
    @ResponseBody
    public String acceptInvite(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId reqId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        Document user = getUser(request);
        return ManageUserController.acceptInvite(user, reqId);
    }

    @PostMapping(value = "/addSchool")
    @ResponseBody
    public String addSchool(HttpServletRequest request,
                            @RequestBody @StrongJSONConstraint(
                                    params = {
                                            "NID", "phone",
                                            "name", "tel",
                                            "address", "managerName",
                                            "schoolSex", "kindSchool"
                                    },
                                    paramsType = {
                                            String.class, Digit.class,
                                            String.class, Digit.class,
                                            String.class, String.class,
                                            Sex.class, GradeSchool.class
                                    },
                                    optionals = {
                                            "firstName", "lastName",
                                            "password", "rPassword",
                                            "city"
                                    },
                                    optionalsType = {
                                            String.class, String.class,
                                            String.class, String.class,
                                            ObjectId.class,
                                    }
                            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAgentUser(request);
        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));

        if (jsonObject.has("password")) {
            if (!jsonObject.getString("password").equals(
                    jsonObject.getString("rPassword"))
            )
                return generateErr("رمزعبور و تکرار آن یکسان نیست.");

            jsonObject.put("password", userService.getEncPass(
                    jsonObject.getString("password"))
            );
        }

        return ManageUserController.addSchool(
                jsonObject,
                user.getObjectId("_id"),
                user.getString("first_name") + " " + user.getString("last_name")
        );
    }

    @PostMapping(value = "/addExistSchool")
    @ResponseBody
    public String addExistSchool(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "NID", "phone",
                    },
                    paramsType = {
                            String.class, Digit.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAgentUser(request);
        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));

        return ManageUserController.addExistSchool(
                jsonObject,
                user.getObjectId("_id"),
                user.getString("first_name") + " " + user.getString("last_name")
        );
    }

    @DeleteMapping(value = "/removeSchools")
    @ResponseBody
    public String removeSchools(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "items"
                                        },
                                        paramsType = {
                                                JSONArray.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAgentUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        return ManageUserController.removeSchools(isAdmin ? null : user.getObjectId("_id"),
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @DeleteMapping(value = "/removeStudents")
    @ResponseBody
    public String removeStudents(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {
                                                 "items"
                                         },
                                         paramsType = {
                                                 JSONArray.class
                                         }
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getSchoolUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        boolean isAgent = Authorization.isAgent(user.getList("accesses", String.class));

        ObjectId id = user.getObjectId("_id");

        if (!isAdmin && !isAgent) {
            if (!user.containsKey("form_list"))
                return JSON_NOT_ACCESS;

            Document form = searchInDocumentsKeyVal(
                    user.getList("form_list", Document.class),
                    "role", "school"
            );

            if (form == null || !form.containsKey("school_id"))
                return JSON_NOT_ACCESS;

            id = form.getObjectId("school_id");
        }

        return ManageUserController.removeStudents(
                isAdmin ? null : id,
                isAgent, !isAdmin & !isAgent ? user : null,
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PostMapping(value = "/addStudent")
    @ResponseBody
    public String addStudent(HttpServletRequest request,
                             @RequestBody @StrongJSONConstraint(
                                     params = {
                                             "firstName", "lastName",
                                             "NID",
                                             "password", "rPassword",
                                     },
                                     paramsType = {
                                             String.class, String.class,
                                             Digit.class,
                                             String.class, String.class
                                     },
                                     optionals = {
                                             "phone", "schoolId"
                                     },
                                     optionalsType = {
                                             Digit.class, ObjectId.class
                                     }
                             ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (!jsonObject.getString("password").equals(
                jsonObject.getString("rPassword"))
        )
            return generateErr("رمزعبور و تکرار آن یکسان نیست.");

        if (jsonObject.has("schoolId") && !Authorization.isAgent(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;
        else if (!jsonObject.has("schoolId") && !Authorization.isSchool(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;

        Document school;

        if (jsonObject.has("schoolId")) {

            school = userRepository.findById(
                    new ObjectId(jsonObject.getString("schoolId"))
            );

            if (school == null)
                return JSON_NOT_VALID_ID;

            if (!isAdmin && !Authorization.hasAccessToThisSchool(school, user.getObjectId("_id")))
                return JSON_NOT_ACCESS;

        } else
            school = user;


        jsonObject.put("password", userService.getEncPass(
                jsonObject.getString("password"))
        );

        return ManageUserController.addStudent(
                jsonObject,
                school
        );
    }

    @PostMapping(value = {"/addStudent/{mode}", "/addStudent/{schoolId}/{mode}"})
    @ResponseBody
    public String addStudent(HttpServletRequest request,
                             @PathVariable(required = false) String schoolId,
                             @PathVariable @EnumValidator(enumClazz = PasswordMode.class) @NotBlank String mode,
                             @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (schoolId != null && !ObjectId.isValid(schoolId))
            return JSON_NOT_VALID_PARAMS;

        if (schoolId != null && !Authorization.isAgent(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;
        else if (schoolId == null && !Authorization.isSchool(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;

        Document school;

        if (schoolId != null) {

            school = userRepository.findById(
                    new ObjectId(schoolId)
            );

            if (school == null)
                return JSON_NOT_VALID_ID;

            if (!isAdmin && !Authorization.hasAccessToThisSchool(school, user.getObjectId("_id")))
                return JSON_NOT_ACCESS;

        } else
            school = user;

        return ManageUserController.addBatchStudents(
                school, file, mode
        );
    }

    @GetMapping(value = "/getMySchools")
    @ResponseBody
    public String getMySchools(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getAgentUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        return ManageUserController.getMySchools(isAdmin ? null : user.getObjectId("_id"));
    }

    @GetMapping(value = {"/getStudents", "/getStudents/{schoolId}"})
    @ResponseBody
    public String getStudents(HttpServletRequest request,
                              @PathVariable(required = false) String schoolId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (schoolId != null && !Authorization.isAgent(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;
        else if (schoolId == null &&
                !Authorization.isSchool(user.getList("accesses", String.class)) &&
                !Authorization.isAdvisor(user.getList("accesses", String.class))
        )
            return JSON_NOT_ACCESS;

        Document school;
        if (schoolId != null) {
            if (!ObjectId.isValid(schoolId))
                return JSON_NOT_VALID_PARAMS;

            school = userRepository.findById(new ObjectId(schoolId));
            if (school == null)
                return JSON_NOT_VALID_ID;

            if (!isAdmin &&
                    !Authorization.hasAccessToThisSchool(school, user.getObjectId("_id"))
            )
                return JSON_NOT_ACCESS;

        } else
            school = user;

        if (!isAdmin && Authorization.isAdvisor(user.getList("accesses", String.class)))
            return ManageUserController.getMyStudents(pluckIds((List<Document>) school.getOrDefault("students", new ArrayList<Document>())));

        return ManageUserController.getMyStudents((List<ObjectId>) school.getOrDefault("students", new ArrayList<ObjectId>()));
    }

    @GetMapping(value = "getUsersReport")
    @ResponseBody
    public void getUsersReport(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false, value = "NID") String NID,
            @RequestParam(required = false, value = "phone") String phone,
            @RequestParam(required = false, value = "firstname") String firstname,
            @RequestParam(required = false, value = "lastname") String lastname,
            @RequestParam(required = false, value = "level") String level,
            @RequestParam(required = false, value = "additionalLevel") String additionalLevel,
            @RequestParam(required = false, value = "gradeId") ObjectId gradeId,
            @RequestParam(required = false, value = "branchId") ObjectId branchId,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        try {
            ByteArrayInputStream byteArrayInputStream = UserController.getUsersReport(
                    NID, phone, firstname, lastname,
                    gradeId, branchId, level, additionalLevel,
                    from, to
            );
            if (byteArrayInputStream != null) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
                IOUtils.copy(byteArrayInputStream, response.getOutputStream());
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }
    }
}
