package irysc.gachesefid.Routes.API.Certificate;

import irysc.gachesefid.Controllers.Certification.AdminCertification;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
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
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/certificate/admin")
@Validated
public class AdminCertificateAPIRoutes extends Router {

    @PostMapping(path = "/store")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "title", "isLandscape", "params",
                                        "qrSize", "qrX", "qrY"
                                },
                                paramsType = {
                                        String.class, Boolean.class,
                                        JSONArray.class, Positive.class,
                                        Positive.class, Positive.class
                                }
                        ) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.store(Utility.convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(path = "/remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {
                                         "items",
                                 },
                                 paramsType = {
                                         JSONArray.class,
                                 }
                         ) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.remove(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(path = "/update/{certId}")
    @ResponseBody
    public String update(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId certId,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"title", "isLandscape", "params",
                                         "qrX", "qrY", "qrSize"},
                                 paramsType = {
                                         String.class, Boolean.class,
                                         JSONArray.class, Positive.class,
                                         Positive.class, Positive.class,

                                 },
                                 optionals = {"visibility"},
                                 optionalsType = {Boolean.class}
                         ) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.update(certId, new JSONObject(jsonStr));
    }

    @PostMapping(path = "/setImg/{certId}")
    @ResponseBody
    public String setImg(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId certId,
                         @RequestBody MultipartFile file)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return AdminCertification.setImg(certId, file);
    }

    @PutMapping(path = "/addUserToCert/{certId}/{NID}")
    @ResponseBody
    public String addUserToCert(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId certId,
                                @PathVariable @NotBlank String NID,
                                @RequestBody @StrongJSONConstraint(
                                        params = "params",
                                        paramsType = JSONArray.class
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.addUserToCert(null, certId, NID,
                new JSONObject(jsonStr).getJSONArray("params")
        );
    }

    @PutMapping(path = "/editUserInCert/{certId}/{NID}")
    @ResponseBody
    public String editUserInCert(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId certId,
                                 @PathVariable @NotBlank String NID,
                                 @RequestBody @StrongJSONConstraint(
                                         params = "params",
                                         paramsType = JSONArray.class
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.editUserInCert(certId, NID,
                new JSONObject(jsonStr).getJSONArray("params")
        );
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
        return "as";
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
        return "as";
    }

    @DeleteMapping(path = "/removeStudents/{certificateId}")
    @ResponseBody
    public String removeStudents(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId certificateId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = JSONArray.class) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminCertification.removeUsersFromCert(
                certificateId,
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }
}
