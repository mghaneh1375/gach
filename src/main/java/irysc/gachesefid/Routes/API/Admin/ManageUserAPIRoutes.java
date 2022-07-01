package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;

@Controller
@RequestMapping(path = "/api/admin/user")
@Validated
public class ManageUserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @PutMapping(path = "/setCoins/{userId}/{newCoins}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId userId,
                       @PathVariable @Min(0) int newCoins
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.setCoins(userId, newCoins);
    }

    @PutMapping(value = "/addAccess/{userId}/{newRole}")
    @ResponseBody
    public String addAccess(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId userId,
                            @PathVariable @EnumValidator(enumClazz = Access.class) String newRole)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.addAccess(userId, newRole);
    }

    @DeleteMapping(value = "/removeAccess/{userId}/{role}")
    @ResponseBody
    public String removeAccess(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId userId,
                               @PathVariable @EnumValidator(enumClazz = Access.class) String role)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.removeAccess(userId, role);
    }

    @PutMapping(value = "/toggleStatus/{userId}")
    @ResponseBody
    public String toggleStatus(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId userId)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        userService.toggleStatus(userId);
        return JSON_OK;
    }

    @GetMapping(value = "/fetchTinyUser")
    @ResponseBody
    public String fetchTinyUser(HttpServletRequest request,
                                @RequestParam(value = "name", required = false) String name,
                                @RequestParam(value = "lastname", required = false) String lastname,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "mail", required = false) String mail,
                                @RequestParam(value = "NID", required = false) String NID
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.fetchTinyUser(name, lastname, phone, mail, NID);
    }

    @GetMapping(value = "/fetchUser/{unique}")
    @ResponseBody
    public String fetchUser(HttpServletRequest request,
                            @PathVariable @NotBlank String unique)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        return ManageUserController.fetchUser(null, unique,
                Authorization.isAdmin(getPrivilegeUser(request).getList("accesses", String.class))
        );
    }

    @GetMapping(value = "/fetchUserLike")
    @ResponseBody
    public String fetchUserLike(HttpServletRequest request,
                                @RequestParam(value = "nameFa", required = false) String nameFa,
                                @RequestParam(value = "nameEn", required = false) String nameEn,
                                @RequestParam(value = "lastNameFa", required = false) String lastNameFa,
                                @RequestParam(value = "lastNameEn", required = false) String lastNameEn
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);

        if (nameFa == null && nameEn == null && lastNameEn == null && lastNameFa == null)
            return JSON_NOT_VALID_PARAMS;

        return ManageUserController.fetchUserLike(nameEn, lastNameEn, nameFa, lastNameFa);
    }

    @PostMapping(value = "/resetPassword/{userId}")
    @ResponseBody
    public String resetPassword(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId userId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"newPass", "rNewPass"},
                                        paramsType = {String.class, String.class}
                                ) String json
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        if (!jsonObject.getString("newPass").equals(jsonObject.getString("rNewPass")))
            return generateErr("رمزجدید و تکرار آن یکسان نیستند.");

        if (!Utility.isValidPassword(jsonObject.getString("newPass")))
            return generateErr("رمزجدید انتخاب شده قوی نیست.");

        Document user = userRepository.findById(userId);
        if (user == null)
            return JSON_NOT_VALID_ID;

        userRepository.updateOne(userId,
                set("password", userService.getEncPass(user.getString("username"),
                        jsonObject.getString("newPass"))
                )
        );

        return JSON_OK;
    }

    @PutMapping(value = "/setAdvisorPercent/{userId}/{percent}")
    @ResponseBody
    public String resetPassword(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId userId,
                                @PathVariable @Min(0) @Max(100) int percent
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ManageUserController.setAdvisorPercent(userId, percent);
    }

    @PostMapping(value = "/signIn/{userId}")
    @ResponseBody
    public String signIn(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        try {

            Document user = userRepository.findById(userId);

            if (user == null || Authorization.isAdmin(user.getList("accesses", String.class)))
                return JSON_NOT_VALID_ID;

            return userService.signIn(user.getString("username"), "1", false);

        } catch (NotActivateAccountException x) {
            return generateErr("not active account");
        } catch (Exception x) {
            return JSON_NOT_VALID_PARAMS;
        }
    }
}
