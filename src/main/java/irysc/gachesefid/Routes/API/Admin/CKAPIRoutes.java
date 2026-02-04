package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.UploadController;
import irysc.gachesefid.Routes.Router;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@RestController
@RequestMapping(path = "/ckeditor")
@Validated
public class CKAPIRoutes extends Router {

    @PostMapping(value = "quiz")
    @ResponseBody
    public String uploadQuizAttach(
            @RequestBody MultipartFile file
    ) {
        if (file == null)
            return JSON_NOT_VALID_PARAMS;
        
        return UploadController.uploadFiles(file, "ck");
    }

}
