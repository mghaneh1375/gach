package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.AvatarController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/admin/config/avatar")
@Validated
public class AvatarAPIRoutes extends Router {

    @GetMapping(path = "/getAll")
    @ResponseBody
    public String getAll() {
        return AvatarController.get();
    }

    @PostMapping(path = "/add")
    @ResponseBody
    public String store(
            @RequestBody MultipartFile file
    ) {
        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        return AvatarController.store(file);
    }

    @PostMapping(path = "/edit/{avatarId}")
    @ResponseBody
    public String edit(
            @PathVariable @ObjectIdConstraint ObjectId avatarId,
            @RequestBody MultipartFile file
    ) {
        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        return AvatarController.edit(avatarId, file);
    }

    @PostMapping(path = "/setDefault/{avatarId}")
    @ResponseBody
    public String setDefault(
            @PathVariable @ObjectIdConstraint ObjectId avatarId
    ) {
        return AvatarController.setDefault(avatarId);
    }

    @DeleteMapping(path = "/delete/{avatarId}")
    @ResponseBody
    public String delete(
            @PathVariable @ObjectIdConstraint ObjectId avatarId
    ) {
        return AvatarController.delete(avatarId);
    }
}
