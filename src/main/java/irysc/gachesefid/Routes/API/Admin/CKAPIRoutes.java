package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.UploadController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/ckeditor")
@Validated
public class CKAPIRoutes extends Router {

    @PostMapping(value = "quiz")
    @ResponseBody
    public String uploadQuizAttach(HttpServletRequest request,
                                   @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException {
        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getSuperAdminPrivilegeUserVoid(request);
        return UploadController.uploadFiles(file, "ck");
    }

}
