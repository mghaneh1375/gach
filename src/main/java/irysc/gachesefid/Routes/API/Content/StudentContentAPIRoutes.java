package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Config.PackageLevelController;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/package_content/public")
@Validated
public class StudentContentAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(required = false, value = "tag") String tag,
                         @RequestParam(required = false, value = "title") String title,
                         @RequestParam(required = false, value = "level") String level,
                         @RequestParam(required = false, value = "teacher") String teacher,
                         @RequestParam(required = false, value = "visibility") Boolean visibility,
                         @RequestParam(required = false, value = "hasCert") Boolean hasCert,
                         @RequestParam(required = false, value = "minPrice") Integer minPrice,
                         @RequestParam(required = false, value = "maxPrice") Integer maxPrice,
                         @RequestParam(required = false, value = "minDuration") Integer minDuration,
                         @RequestParam(required = false, value = "maxDuration") Integer maxDuration
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.getAll(user == null ? null : user.getObjectId("_id"), isAdmin,
                tag, title, teacher, visibility, hasCert, minPrice, maxPrice, minDuration, maxDuration, level
        );
    }

    @GetMapping(value = "getMy")
    @ResponseBody
    public String getMy(HttpServletRequest request
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.getMy(getUser(request).getObjectId("_id"));
    }

    @PostMapping(value = "buy/{id}")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId id,
                      @RequestBody @StrongJSONConstraint(
                              params = {},
                              paramsType = {},
                              optionals = {
                                      "off"
                              },
                              optionalsType = {
                                      String.class
                              }
                      ) String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        Document user = getUser(request);
        return StudentContentController.buy(id, new JSONObject(jsonStr), user.getObjectId("_id"),
                ((Number) user.get("money")).doubleValue(), user.getString("phone"), user.getString("mail")
        );
    }


    @PostMapping(value = "startFinalQuiz/{id}")
    @ResponseBody
    public String startFinalQuiz(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.startFinalQuiz(id, getUser(request).getObjectId("_id"));
    }

    @PostMapping(value = "startSessionQuiz/{id}/{sessionId}")
    @ResponseBody
    public String startSessionQuiz(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId id,
                                   @PathVariable @ObjectIdConstraint ObjectId sessionId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.startSessionQuiz(id, sessionId, getUser(request).getObjectId("_id"));
    }


    @PostMapping(value = "reviewFinalQuiz/{id}")
    @ResponseBody
    public String reviewFinalQuiz(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.reviewFinalQuiz(id, getUser(request).getObjectId("_id"));
    }

    @PostMapping(value = "reviewSessionQuiz/{id}/{sessionId}")
    @ResponseBody
    public String reviewSessionQuiz(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId id,
                                    @PathVariable @ObjectIdConstraint ObjectId sessionId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.reviewSessionQuiz(id, sessionId, getUser(request).getObjectId("_id"));
    }

    @GetMapping(value = "get/{slug}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @NotBlank String slug
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.get(isAdmin, user, slug);
    }

    @GetMapping(value = "getListOfPackageLevels")
    @ResponseBody
    public String getListOfPackageLevels() {
        return PackageLevelController.list();
    }

    @PutMapping(value = "rate/{id}")
    @ResponseBody
    public String rate(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId id,
                       @RequestBody @StrongJSONConstraint(params = {"rate"}, paramsType = {Positive.class}) String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentContentController.rate(id, getUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getInt("rate")
        );
    }


    @GetMapping(value = "teacherPackages")
    @ResponseBody
    public String teacherPackages(@RequestParam(value = "teacher") @NotBlank String teacher) {
        return StudentContentController.teacherPackages(teacher);
    }

    @GetMapping(value = "getTeacherContents/{teacherId}")
    @ResponseBody
    public String getTeacherContents(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId teacherId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        getUser(request);
        return StudentContentController.getTeacherContents(teacherId);
    }

    @GetMapping(value = "rates/{id}")
    @ResponseBody
    public String rates(HttpServletRequest request,
                        @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return StudentContentController.rates(id);
    }


    @GetMapping(value = "getSessions/{slug}/{sessionId}")
    @ResponseBody
    public String getSessions(HttpServletRequest request,
                              @PathVariable @NotBlank String slug,
                              @PathVariable @ObjectIdConstraint ObjectId sessionId
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.getSessions(isAdmin, user == null ? null : user.getObjectId("_id"),
                slug, sessionId
        );
    }

    @GetMapping(value = "distinctTeachers")
    @ResponseBody
    public String distinctTeachers() {
        return StudentContentController.distinctTeachers();
    }

    @GetMapping(value = "distinctTeachersForAdmin")
    @ResponseBody
    public String distinctTeachersForAdmin(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return StudentContentController.distinctTeachersForAdmin();
    }

    @PostMapping(value = "getTeacherBio")
    @ResponseBody
    public String getTeacherBio(
            @RequestBody @StrongJSONConstraint(
                    params = {"teacher"},
                    paramsType = {String.class},
                    optionals = {"contentName"},
                    optionalsType = {String.class}
            ) @NotBlank String jsonStr) {
        return StudentContentController.getTeacherBio(
                new JSONObject(jsonStr)
        );
    }

    @GetMapping(value = "distinctTags")
    @ResponseBody
    public String distinctTags() {
        return StudentContentController.distinctTags();
    }


    @GetMapping(value = "chapters/{id}")
    @ResponseBody
    public String chapters(@PathVariable @ObjectIdConstraint ObjectId id) {
        return StudentContentController.chapters(id);
    }

    @PostMapping(value = "changeTeacherName")
    @ResponseBody
    public String changeTeacherName(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "oldName", "newName"
                    },
                    paramsType = {
                            String.class, String.class
                    },
                    optionals = {
                            "NID", "bio"
                    },
                    optionalsType = {
                            String.class, String.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return StudentContentController.changeTeacherName(
                convertPersian(new JSONObject(jsonStr))
        );
    }
}
