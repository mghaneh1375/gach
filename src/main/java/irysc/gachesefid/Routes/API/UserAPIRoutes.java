package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.PhoneValidator;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.activationRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.FileUtils.uploadDir_dev;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.printException;

@Controller
@RequestMapping(path = "/api/user")
@Validated
public class UserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @GetMapping(path = "createExam")
    @ResponseBody
    public String createExam(HttpServletRequest request)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        ArrayList<String> files = new ArrayList<>();
        files.add(uploadDir_dev + "questions/1.jpg");
        files.add(uploadDir_dev + "questions/2.png");
        files.add(uploadDir_dev + "questions/3.png");
        files.add(uploadDir_dev + "questions/4.png");
        files.add(uploadDir_dev + "questions/5.png");
        files.add(uploadDir_dev + "questions/6.png");
        files.add(uploadDir_dev + "questions/7.png");
        files.add(uploadDir_dev + "questions/8.png");
        files.add(uploadDir_dev + "questions/9.png");
        files.add(uploadDir_dev + "questions/10.png");
        files.add(uploadDir_dev + "questions/11.png");
        files.add(uploadDir_dev + "questions/12.png");
        PDFUtils.createExam(files);
        return JSON_OK;
    }

    @GetMapping(path = "getCertificate")
    @ResponseBody
    public String getCertificate(HttpServletRequest request,
                                 @RequestBody(required = false) @JSONConstraint(params = {}, optionals = {
                                         "name", "competition", "date"
                                 }) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        JSONObject jsonObject = (jsonStr != null) ? new JSONObject(jsonStr) : new JSONObject();
//        PDFUtils.getCertificate(jsonObject.getString("name"),
//                jsonObject.getString("competition"),
//                jsonObject.getString("date")
//        );
//        PDFUtils.getCertificate("محمد قانع",
//                "المپیاد آزمایشی زیست",
//                "1399/04/12"
//        );
        return JSON_OK;
    }


    @GetMapping(path = "getCertificate2")
    @ResponseBody
    public String getCertificate2(HttpServletRequest request,
                                  @RequestBody(required = false) @JSONConstraint(params = {}, optionals = {
                                          "course", "hours", "date"
                                  }) String jsonStr)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        JSONObject jsonObject = (jsonStr != null) ? new JSONObject(jsonStr) : new JSONObject();
//        PDFUtils.getCertificate(jsonObject.getString("name"),
//                jsonObject.getString("competition"),
//                jsonObject.getString("date")
//        );
        PDFUtils.getCertificate2("آمادگی مرحله اول المپیاد شیمی",
                60,
                "1399/04/12"
        );
        return JSON_OK;
    }

    @PostMapping(value = "clearCache/{table}")
    @ResponseBody
    public String clearCache(HttpServletRequest request,
                             @PathVariable @NotBlank String table)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        Repository.clearCache(table);
        return JSON_OK;
    }

    @PostMapping(value = "/isAuth")
    @ResponseBody
    public String isAuth(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {
        return UserController.isAuth(getUserWithOutCheckCompleteness(request));
    }

    @GetMapping(value = "/getInfo")
    @ResponseBody
    public String getInfo(HttpServletRequest request)
            throws NotCompleteAccountException, NotActivateAccountException, UnAuthException {
        return ManageUserController.fetchUser(getUser(request), null, false);
    }

    @PostMapping(value = "/signIn")
    @ResponseBody
    public String signIn(@RequestBody @JSONConstraint(
            params = {"username", "password"}
    ) @NotBlank String jsonStr) {
        try {

            JSONObject jsonObject = new JSONObject(jsonStr);

            Utility.convertPersian(jsonObject);
            String token = userService.signIn(
                    jsonObject.getString("username").toLowerCase(),
                    jsonObject.getString("password"), true
            );

            return Utility.generateSuccessMsg("token", token);

        } catch (NotActivateAccountException x) {
            return Utility.generateErr("حساب کاربری شما هنوز فعال نشده است.");
        } catch (Exception x) {
            return JSON_NOT_VALID_PARAMS;
        }
    }

    @PostMapping(value = "/logout")
    @ResponseBody
    public String logout(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        getUserWithOutCheckCompleteness(request);

        try {
            String token = request.getHeader("Authorization");
            userService.logout(token);
            JwtTokenFilter.removeTokenFromCache(token.replace("Bearer ", ""));
            return JSON_OK;
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return JSON_NOT_VALID_TOKEN;
    }

    @PostMapping(value = "/signUp")
    @ResponseBody
    public String signUp(@RequestBody @JSONConstraint(params = {"password", "phone"}, optionals = {"introduced"}) String json) {

        try {
            JSONObject jsonObject = new JSONObject(json);
            Utility.convertPersian(jsonObject);

            if (!Utility.isValidPassword(jsonObject.getString("password")))
                return new JSONObject().put("status", "nok").put("msg", "password security is low").toString();

            if (!PhoneValidator.isValid(jsonObject.getString("phone")))
                return new JSONObject().put("status", "nok").put("msg", "phone is invalid").toString();

            jsonObject.put("password", userService.getEncPass(jsonObject.getString("password")));
            return UserController.signUp(jsonObject);
        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID;
    }

    @PostMapping(value = "/resendCode")
    @ResponseBody
    public String resendCode(@RequestBody @JSONConstraint(params = {"token", "phone"}) String json) {
        return UserController.resend(new JSONObject(json));
    }

    @PostMapping(value = "/activate")
    @ResponseBody
    public String activate(@RequestBody @StrongJSONConstraint(
            params = {"token", "phone", "code"},
            paramsType = {String.class, String.class, Integer.class}
    ) String json) {

        JSONObject jsonObject = new JSONObject(json);

        Utility.convertPersian(jsonObject);

        int code = jsonObject.getInt("code");
        if (code < 10000 || code > 99999)
            return JSON_NOT_VALID_PARAMS;

        return UserController.activate(code, jsonObject.getString("token"), jsonObject.getString("phone"));
    }

    @PostMapping(value = "/forgetPassword")
    @ResponseBody
    public String forgetPassword(@RequestBody @JSONConstraint(params = {"unique", "via"}) String json) {
        return UserController.forgetPass(new JSONObject(json));
    }

    @PostMapping(value = "/resetPassword")
    @ResponseBody
    public String resetPassword(@RequestBody @StrongJSONConstraint(
            params = {"token", "unique", "code", "newPass", "rNewPass"},
            paramsType = {String.class, String.class, Integer.class,
                    String.class, String.class}
    ) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        jsonObject.put("unique", jsonObject.getString("unique").toLowerCase());

        if (jsonObject.getString("token").length() != 20)
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getInt("code") < 10000 || jsonObject.getInt("code") > 99999)
            return Utility.generateErr("کد وارد شده معتبر نمی باشد.");

        if (!jsonObject.getString("newPass").equals(jsonObject.getString("rNewPass")))
            return Utility.generateErr("رمزجدید و تکرار آن یکسان نیستند.");

        if (!Utility.isValidPassword(jsonObject.getString("newPass")))
            return Utility.generateErr("رمزجدید انتخاب شده قوی نیست.");

        Document doc = activationRepository.findOneAndDelete(
                and(
                        eq("token", jsonObject.getString("token")),
                        or(
                                eq("mail", jsonObject.getString("unique")),
                                eq("phone", jsonObject.getString("unique"))
                        ),
                        eq("code", jsonObject.getInt("code"))
                )
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return Utility.generateErr("توکن موردنظر منقضی شده است.");

        userRepository.updateOne(eq("username", doc.getString("username")),
                set("password", userService.getEncPass(doc.getString("username"),
                        jsonObject.getString("newPass"))));

        return JSON_OK;
    }

    @PostMapping(value = "/changeMail")
    @ResponseBody
    public String changeMail(HttpServletRequest request,
                             @RequestParam(value = "newMail") @NotBlank String newMail)
            throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if (!Utility.isValidMail(newMail))
            return new JSONObject().put("status", "nok").put("msg", "mail is incorrect").toString();

        return UserController.changeMail(getUser(request), newMail);
    }

    @GetMapping(value = "/doChangeMail/{link}")
    @ResponseBody
    public String doChangeMail(HttpServletRequest request,
                               @PathVariable @NotBlank String link)
            throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return UserController.doChangeMail(getUser(request), link);
    }

    @PostMapping(value = "/confirmForgetPassword")
    @ResponseBody
    public String confirmForgetPassword(@RequestBody @JSONConstraint(params = {"token", "mail", "code"}) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        jsonObject.put("mail", jsonObject.getString("mail").toLowerCase());

        if (jsonObject.getString("token").length() != 20)
            return new JSONObject().put("status", "nok").put("msg", "invalid token").toString();

        if (jsonObject.getInt("code") < 10000 || jsonObject.getInt("code") > 99999)
            return new JSONObject().put("status", "nok").put("msg", "invalid code").toString();

        if (!Utility.isValidMail(jsonObject.getString("mail")))
            return new JSONObject().put("status", "nok").put("msg", "mail is incorrect").toString();

        Document doc = activationRepository.findOne(and(
                eq("token", jsonObject.getString("token")),
                eq("mail", jsonObject.getString("mail")),
                eq("code", jsonObject.getInt("code")))
                , null);

        if (doc == null)
            return JSON_NOT_VALID_TOKEN;

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return new JSONObject().put("status", "nok").put("msg", "token has been expired").toString();

        return JSON_OK;
    }

    @PostMapping(value = "/changePassword")
    @ResponseBody
    public String changePassword(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(params = {
                                         "oldPass", "newPass", "confirmNewPass"
                                 },
                                         paramsType = {
                                                 String.class, String.class, String.class
                                         }) String jsonStr
    ) throws NotActivateAccountException, UnAuthException {

        Document user = getUserWithOutCheckCompleteness(request);

        try {
            JSONObject jsonObject = new JSONObject(jsonStr);

            String newPass = jsonObject.getString("newPass");
            String confirmNewPass = jsonObject.getString("confirmNewPass");
            String oldPassword = jsonObject.getString("oldPass");

            if (!newPass.equals(confirmNewPass))
                return Utility.generateErr("رمزعبور جدید و تکرار آن یکسان نیستند.");

            if (!Utility.isValidPassword(newPass))
                return Utility.generateErr("رمزعبور جدید قوی نیست.");

            newPass = Utility.convertPersianDigits(newPass);
            oldPassword = Utility.convertPersianDigits(oldPassword);

            if (!userService.isOldPassCorrect(oldPassword, user.getString("password")))
                return Utility.generateErr("رمزعبور وارد شده صحیح نیست.");

            newPass = userService.getEncPass(user.getString("username"), newPass);

            user.put("password", newPass);
            userRepository.updateOne(user.getObjectId("_id"), set("password", newPass));

            JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
            return JSON_OK;

        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID_PARAMS;
    }


    @PostMapping(value = "/setPic")
    @ResponseBody
    public String setPic(HttpServletRequest request,
                         @RequestBody MultipartFile file)
            throws UnAuthException, NotActivateAccountException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getPrivilegeUser(request);
        if (UserController.setPic(file, user))
            return JSON_OK;

        return JSON_NOT_UNKNOWN;
    }

    @PostMapping(value = "/setAvatar/{avatarId}")
    @ResponseBody
    public String setAvatar(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId avatarId)
            throws UnAuthException, NotActivateAccountException {

        return UserController.setAvatar(getUserWithOutCheckCompleteness(request), avatarId);
    }


    @GetMapping(value = "/myTransactions")
    @ResponseBody
    public String myTransactions(HttpServletRequest request,
                                 @RequestParam(required = false, value = "usedFor") String usedFor,
                                 @RequestParam(required = false, value = "useOffCode") Boolean useOffCode
    ) throws UnAuthException, NotActivateAccountException, NotAccessException, NotCompleteAccountException {
        return PayPing.myTransactions(getStudentUser(request).getObjectId("_id"), usedFor, useOffCode);
    }

    @GetMapping(value = "/myTransaction/{referenceId}")
    @ResponseBody
    public String myTransaction(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId referenceId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException, NotCompleteAccountException {
        return PayPing.myTransaction(getStudentUser(request).getObjectId("_id"), referenceId);
    }

}
