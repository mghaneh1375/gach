package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
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

@Controller
@RequestMapping(path = "/api/package_content/public")
@Validated
public class StudentContentAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(required = false, value = "tag") String tag,
                         @RequestParam(required = false, value = "title") String title,
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
                tag, title, teacher, visibility, hasCert, minPrice, maxPrice, minDuration, maxDuration
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
                user.getDouble("money"), user.getString("phone"), user.getString("mail")
        );
    }

    @GetMapping(value = "get/{slug}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @NotBlank String slug
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.get(isAdmin, user == null ? null : user.getObjectId("_id"),
                slug
        );
    }

    @GetMapping(value = "distinctTeachers")
    @ResponseBody
    public String distinctTeachers() {
        return StudentContentController.distinctTeachers();
    }

    @GetMapping(value = "distinctTags")
    @ResponseBody
    public String distinctTags() {
        return StudentContentController.distinctTags();
    }

}
