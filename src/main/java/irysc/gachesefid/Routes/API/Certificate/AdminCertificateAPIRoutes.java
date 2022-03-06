package irysc.gachesefid.Routes.API.Certificate;

import irysc.gachesefid.Controllers.Certification.AdminCertification;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/certificate/admin")
@Validated
public class AdminCertificateAPIRoutes extends Router {

    @PostMapping(path = "/store")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestBody @StrongJSONConstraint(
                                params = {"title", "text"},
                                paramsType = {String.class, String.class}
                        ) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.store(new JSONObject(jsonStr));
    }

    @GetMapping(path = "/getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(required = false, value = "title") String title)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.getAll(title);
    }


    @GetMapping(path = "/get/{certificateId}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId certificateId)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.get(certificateId);
    }

    @DeleteMapping(path = "/remove/{certificateId}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint Object certificateId)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

    }

    @PostMapping(path = "/addStudents/{certificateId}")
    @ResponseBody
    public String addStudents(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint Object certificateId,
                              @RequestBody MultipartFile file)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);

    }

    @DeleteMapping(path = "/removeStudents/{certificateId}")
    @ResponseBody
    public String removeStudents(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint Object certificateId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"students"},
                                         paramsType = JSONArray.class) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

    }
}
