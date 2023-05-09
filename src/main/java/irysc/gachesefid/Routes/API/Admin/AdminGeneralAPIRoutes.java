package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.ContentController;
import irysc.gachesefid.Controllers.UploadController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.UploadSection;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/admin/general")
@Validated
public class AdminGeneralAPIRoutes extends Router {

    @PostMapping(value = "uploadFiles/{section}")
    @ResponseBody
    public String uploadFiles(HttpServletRequest request,
                              @PathVariable @NotBlank @EnumValidator(enumClazz = UploadSection.class) String section,
                              @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getSuperAdminPrivilegeUser(request);
        return UploadController.uploadFiles(file, section);
    }

    @GetMapping(value = "refreshSubjectQNo")
    @ResponseBody
    public String refreshSubjectQNo(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);
        return ContentController.refreshSubjectQNo();
    }

}
