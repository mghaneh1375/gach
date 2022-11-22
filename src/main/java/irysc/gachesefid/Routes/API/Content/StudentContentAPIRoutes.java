package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/package_content/public")
@Validated
public class StudentContentAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(required = false) String tag,
                         @RequestParam(required = false) String title,
                         @RequestParam(required = false) String teacher,
                         @RequestParam(required = false) Boolean visibility,
                         @RequestParam(required = false) Boolean hasCert,
                         @RequestParam(required = false) Integer minPrice,
                         @RequestParam(required = false) Integer maxPrice
    ) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.getAll(user == null ? null : user.getObjectId("_id"), isAdmin,
                tag, title, teacher, visibility, hasCert, minPrice, maxPrice
        );
    }

    @GetMapping(value = "get/{id}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId id
    ) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return StudentContentController.get(isAdmin, user == null ? null : user.getObjectId("_id"),
                id
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
