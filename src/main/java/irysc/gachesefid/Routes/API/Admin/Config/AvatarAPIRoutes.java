package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.AvatarController;
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

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/admin/config/avatar")
@Validated
public class AvatarAPIRoutes extends Router {

    @GetMapping(path = "/getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        getUserWithOutCheckCompletenessVoid(request);
        return AvatarController.get();
    }

    @PostMapping(path = "/")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestBody MultipartFile file
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AvatarController.store(file);
    }

    @PutMapping(path = "/edit/{avatarId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId avatarId,
                       @RequestBody MultipartFile file
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AvatarController.edit(avatarId, file);
    }

    @PostMapping(path = "/setDefault/{avatarId}")
    @ResponseBody
    public String setDefault(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId avatarId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return AvatarController.setDefault(avatarId);
    }

    @DeleteMapping(path = "/{avatarId}")
    @ResponseBody
    public String delete(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId avatarId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return AvatarController.delete(avatarId);
    }
}
