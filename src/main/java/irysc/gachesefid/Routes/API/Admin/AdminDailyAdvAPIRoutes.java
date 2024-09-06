package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.DailyAdv.DailyAdvController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Controller
@RequestMapping(path = "/api/daily_adv/admin")
@Validated
public class AdminDailyAdvAPIRoutes extends Router {

    @GetMapping("getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return DailyAdvController.getAll();
    }

    @PostMapping("add")
    @ResponseBody
    public String add(
            HttpServletRequest request,
            @RequestParam(value = "expireAt") Long expireAt,
            @RequestParam(value = "title") @NotBlank @Size(min = 2, max = 100) String title,
            @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return DailyAdvController.add(file, title, expireAt);
    }

    @DeleteMapping("remove/{id}")
    @ResponseBody
    public String delete(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return DailyAdvController.delete(id);
    }

}
