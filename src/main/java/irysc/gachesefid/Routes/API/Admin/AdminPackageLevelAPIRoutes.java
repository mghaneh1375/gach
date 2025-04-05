package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Config.PackageLevelController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Controller
@Validated
@RequestMapping(value = "api/admin/package_level")
public class AdminPackageLevelAPIRoutes extends Router {

    @PostMapping(
            value = "store",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public String store(
            HttpServletRequest request,
            @RequestPart(name = "file") @NotNull MultipartFile file,
            @RequestPart(name = "data") @StrongJSONConstraint(
                    params = {"title"},
                    paramsType = {String.class}
            ) @NotNull @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageLevelController.store(new JSONObject(jsonStr), file);
    }

    @PostMapping(
            value = "update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public String update(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestPart(name = "data") @StrongJSONConstraint(
                    params = {"title"},
                    paramsType = {String.class}
            ) @NotNull @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageLevelController.update(id, new JSONObject(jsonStr), file);
    }

    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(
            HttpServletRequest request,
            @RequestBody @NotNull @NotBlank @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageLevelController.remove(
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @GetMapping(value = "list")
    @ResponseBody
    public String list(
            HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageLevelController.list();
    }

}
