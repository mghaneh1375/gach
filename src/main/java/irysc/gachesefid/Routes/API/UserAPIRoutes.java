package irysc.gachesefid.Routes.API;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Path;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.uploadDir_dev;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

@Controller
@RequestMapping(path = "/api/user")
@Validated
public class UserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @GetMapping(value = "/test")
    @ResponseBody
    public String test() {

        if(1 == 1) {
//            09026021609
//            sendSMSWithoutTemplate("09214915905-09105559653-09224786125-09191613134", "تست پیامک");

//            String s = "<p>test2</p>";
//            String plainText= Jsoup.parse(s).text();
//
//            if(1 == 1)
//                return plainText;

//            sendSMS("09214915905", "محمد قانع", "المپیاد زیست", "", "regexam");
//            sendSMS("09214915905", "محمد قانع", "المپیاد زیست", "1400/02/04", "newQuiz");
//            sendSMS("09214915905", "محمد قانع", "المپیاد زیست", "", "quizRemember");
//            sendSMS("09214915905", "محمد قانع", "", "", "sysans");
//            sendSMS("09214915905", "محمد قانع", "", "", "gift");
            return "pk";
        }

        JSONArray tags2 = questionRepository.distinctTags("tags");
        for(int i = 0; i < tags2.length(); i++) {
            String tag = tags2.getString(i);
            questionTagRepository.insertOne(
                    new Document("tag", tag)
                        .append("code", Utility.getRandIntForTag())
            );
        }

        if(1 == 1)
            return "s";

        return "s";
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

    @GetMapping(value = {"/fetchUser", "/fetchUser/{userId}"})
    @ResponseBody
    public String fetchUser(HttpServletRequest request,
                            @PathVariable(required = false) String userId)
            throws NotActivateAccountException, UnAuthException, InvalidFieldsException, NotCompleteAccountException {

        Document user = (Document) getUserWithAdminAccess(request, false, false, userId).get("user");

        return generateSuccessMsg("user",
                UserController.isAuth(user)
        );
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
            JSONObject jsonObject =
                    Utility.convertPersian(new JSONObject(jsonStr));

            return userService.signIn(
                    jsonObject.get("username").toString().toLowerCase(),
                    jsonObject.get("password").toString(), !DEV_MODE
            );

        } catch (NotActivateAccountException x) {
            return Utility.generateErr("حساب کاربری شما هنوز فعال نشده است.");
        } catch (Exception x) {
            return Utility.generateErr("نام کاربری و یا رمزعبور اشتباه است.");
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
    public String signUp(@RequestBody @JSONConstraint(params = {
            "password", "username",
            "firstName", "lastName",
            "NID", "authVia"
    }) String json) {

        try {
            JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

//            if (!Utility.isValidPassword(jsonObject.getString("password")))
//                return generateErr("رمزعبور وارد شده ملاحظات امنیتی لازم را ندارد.");

            jsonObject.put("password", userService.getEncPass(jsonObject.getString("password")));

            return UserController.signUp(jsonObject);
        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID;
    }

    @PostMapping(value = {"/updateInfo", "/updateInfo/{userId}"})
    @ResponseBody
    public String updateInfo(HttpServletRequest request,
                             @PathVariable(required = false) String userId,
                             @RequestBody @StrongJSONConstraint(
                                     params = {
                                             "sex", "cityId",
                                             "firstName", "lastName",
                                             "NID"
                                     },
                                     paramsType = {
                                             Sex.class, ObjectId.class,
                                             String.class, String.class,
                                             String.class
                                     },
                                     optionals = {
                                             "branches", "schoolId", "gradeId",
                                     },
                                     optionalsType = {
                                             JSONArray.class, ObjectId.class,
                                             ObjectId.class,
                                     }
                             ) String json
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        Document user = (Document) getUserWithAdminAccess(request, false, false, userId).get("user");
        return UserController.updateInfo(convertPersian(new JSONObject(json)), user);
    }

    @PostMapping(value = "/resendCode")
    @ResponseBody
    public String resendCode(@RequestBody @JSONConstraint(params = {"token", "username"}) String json) {
        return UserController.resend(convertPersian(new JSONObject(json)));
    }

    @PostMapping(value = "/activate")
    @ResponseBody
    public String activate(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code"},
            paramsType = {String.class, String.class, Positive.class}
    ) String json) {

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

        int code = jsonObject.getInt("code");
        if (code < 100000 || code > 999990)
            return JSON_NOT_VALID_PARAMS;

        try {

            String password = UserController.activate(code,
                    jsonObject.getString("token"),
                    jsonObject.getString("NID")
            );

            return userService.signIn(jsonObject.getString("NID"),
                    password, false);

        } catch (Exception e) {
            return generateErr(e.getMessage());
        }
    }

    @GetMapping(value = "/getRoleForms")
    @ResponseBody
    public String getRoleForms() {
        return UserController.getRoleForms();
    }

    @PostMapping(value = {"/sendRoleForm", "/sendRoleForm/{userId}"})
    @ResponseBody
    public String sendRoleForm(HttpServletRequest request,
                               @PathVariable(required = false) String userId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"role"},
                                       paramsType = {String.class},
                                       optionals = {
                                               "name", "tel", "state",
                                               "workYear", "workSchools", "universeField",
                                               "bio", "address", "managerName", "schoolSex",
                                               "kindSchool", "invitationCode", "workLessons"
                                       },
                                       optionalsType = {
                                               String.class, String.class, String.class,
                                               Positive.class, String.class, String.class,
                                               String.class, String.class, String.class,
                                               String.class, String.class, String.class,
                                               String.class

                                       }
                               ) String json
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.setRole(
                (Document) getUserWithAdminAccess(request, false, false, userId).get("user"),
                new JSONObject(json)
        );
    }

    @PutMapping(value = "/setIntroducer")
    @ResponseBody
    public String setIntroducer(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"invitationCode"},
                                        paramsType = {String.class}
                                ) String json
    ) throws UnAuthException, NotActivateAccountException {
        return UserController.setIntroducer(
                getUserWithOutCheckCompleteness(request),
                new JSONObject(json).getString("invitationCode")
        );
    }

    @GetMapping(value = "/whichKindOfAuthIsAvailable")
    @ResponseBody
    public String whichKindOfAuthIsAvailable(@RequestParam @Digits(integer = 10, fraction = 0) String NID) {
        return UserController.whichKindOfAuthIsAvailable(NID);
    }

    @PostMapping(value = "/forgetPassword")
    @ResponseBody
    public String forgetPassword(@RequestBody @JSONConstraint(params = {"NID", "authVia"}) String json) {
        return UserController.forgetPass(convertPersian(new JSONObject(json)));
    }

    @PostMapping(value = "/checkCode")
    @ResponseBody
    public String checkCode(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code"},
            paramsType = {String.class, String.class, Positive.class}
    ) String json) {

        JSONObject jsonObject = convertPersian(new JSONObject(json));

        Document doc = activationRepository.findOne(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.get("NID").toString()),
                        eq("code", jsonObject.getInt("code"))
                ), new BasicDBObject("_id", 1)
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        return JSON_OK;
    }

    @PostMapping(value = "/resetPassword")
    @ResponseBody
    public String resetPassword(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code", "newPass", "rNewPass"},
            paramsType = {String.class, String.class, Positive.class,
                    String.class, String.class}
    ) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        if (!Utility.validationNationalCode(jsonObject.getString("NID")))
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getString("token").length() != 20)
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getInt("code") < 100000 || jsonObject.getInt("code") > 999999)
            return Utility.generateErr("کد وارد شده معتبر نمی باشد.");

        if (!jsonObject.getString("newPass").equals(jsonObject.getString("rNewPass")))
            return Utility.generateErr("رمزجدید و تکرار آن یکسان نیستند.");

//        if (!Utility.isValidPassword(jsonObject.getString("newPass")))
//            return Utility.generateErr("رمزجدید انتخاب شده قوی نیست.");

        Document doc = activationRepository.findOneAndDelete(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.getString("NID")),
                        eq("code", jsonObject.getInt("code"))
                )
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC_LONG)
            return Utility.generateErr("توکن موردنظر منقضی شده است.");

        userRepository.updateOne(
                eq("NID", jsonObject.getString("NID")),
                set("password", userService.getEncPass(doc.getString("username"),
                        jsonObject.getString("newPass")))
        );

        return JSON_OK;
    }

    @PostMapping(value = "/setNewUsername")
    @ResponseBody
    public String setNewUsername(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"token", "code"},
                                         paramsType = {String.class, Positive.class},
                                         optionals = {"NID"},
                                         optionalsType = {String.class}
                                 ) String json
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

        if (jsonObject.getString("token").length() != 20)
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getInt("code") < 100000 || jsonObject.getInt("code") > 999999)
            return Utility.generateErr("کد وارد شده معتبر نمی باشد.");

        Document doc = activationRepository.findOneAndDelete(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", user.getString("NID")),
                        eq("code", jsonObject.getInt("code"))
                )
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return Utility.generateErr("توکن موردنظر منقضی شده است.");

        String phoneOrMail = doc.getString("phone_or_mail");

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName())) {

            if (user.containsKey("phone"))
                userService.deleteFromCache(user.getString("phone"));

            user.put("phone", phoneOrMail);
        } else {

            if (user.containsKey("mail"))
                userService.deleteFromCache(user.getString("mail"));

            user.put("mail", phoneOrMail);
        }

        userRepository.replaceOne(user.getObjectId("_id"), user);
//        logout(request);

        return JSON_OK;
    }

    @PostMapping(value = {"/updateUsername", "/updateUsername/{userId}"})
    @ResponseBody
    public String updateUsername(HttpServletRequest request,
                                 @PathVariable(required = false) String userId,
                                 @RequestBody @JSONConstraint(params = {"mode", "username"})
                                 @NotBlank String json

    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        Document doc = getUserWithAdminAccess(request, false, false, userId);

        if(doc.getBoolean("isAdmin"))
            return UserController.forceUpdateUsername(
                    (Document)doc.get("user"), convertPersian(new JSONObject(json))
            );

        return UserController.updateUsername(
                ((Document)doc.get("user")).getString("NID"),
                convertPersian(new JSONObject(json))
        );
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

    @PostMapping(value = {"/changePassword", "/changePassword/{userId}"})
    @ResponseBody
    public String changePassword(HttpServletRequest request,
                                 @PathVariable(required = false) String userId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {
                                                 "oldPass", "newPass", "confirmNewPass"
                                         },
                                         paramsType = {
                                                 String.class, String.class, String.class
                                         }) String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotCompleteAccountException, InvalidFieldsException {

        Document doc = getUserWithAdminAccess(request, false, false, userId);
        Document user = (Document) doc.get("user");
        boolean isAdmin = doc.getBoolean("isAdmin");

        try {
            JSONObject jsonObject = new JSONObject(jsonStr);

            String newPass = jsonObject.get("newPass").toString();
            String confirmNewPass = jsonObject.get("confirmNewPass").toString();
            String oldPassword = jsonObject.get("oldPass").toString();

            if (!newPass.equals(confirmNewPass))
                return generateErr("رمزعبور جدید و تکرار آن یکسان نیستند.");

//            if (!Utility.isValidPassword(newPass))
//                return generateErr("رمزعبور جدید قوی نیست.");

            newPass = Utility.convertPersianDigits(newPass);
            if(!isAdmin) {
                oldPassword = Utility.convertPersianDigits(oldPassword);

                if (!userService.isOldPassCorrect(oldPassword, user.getString("password")))
                    return Utility.generateErr("رمزعبور وارد شده صحیح نیست.");
            }

            userService.deleteFromCache(user.getString("NID"));

            if(user.containsKey("phone") && !user.getString("phone").isEmpty())
                userService.deleteFromCache(user.getString("phone"));

            if(user.containsKey("mail") && !user.getString("mail").isEmpty())
                userService.deleteFromCache(user.getString("mail"));

            newPass = userService.getEncPass(newPass);
            user.put("password", newPass);

            userRepository.updateOne(user.getObjectId("_id"), set("password", newPass));
            logout(request);

            return JSON_OK;

        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID_PARAMS;
    }


    @PostMapping(value = {"/setPic", "/setPic/{userId}"})
    @ResponseBody
    public String setPic(HttpServletRequest request,
                         @RequestBody MultipartFile file,
                         @PathVariable(required = false) String userId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getUserWithAdminAccess(request, false, true, userId);

        if (UserController.setPic(file, user))
            return JSON_OK;

        return JSON_NOT_UNKNOWN;
    }

    @PutMapping(value = {"/setAvatar/{avatarId}", "/setAvatar/{avatarId}/{userId}"})
    @ResponseBody
    public String setAvatar(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId avatarId,
                            @PathVariable(required = false) String userId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.setAvatar((
                Document) getUserWithAdminAccess(request, false, false, userId).get("user"),
                avatarId
        );
    }


    @GetMapping(value = "/myTransactions")
    @ResponseBody
    public String myTransactions(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotAccessException, NotCompleteAccountException {
        return PayPing.myTransactions(getStudentUser(request).getObjectId("_id"));
    }

//    @GetMapping(value = "/myTransaction/{referenceId}")
//    @ResponseBody
//    public String myTransaction(HttpServletRequest request,
//                                @PathVariable @ObjectIdConstraint ObjectId referenceId
//    ) throws UnAuthException, NotActivateAccountException, NotAccessException, NotCompleteAccountException {
//        return PayPing.myTransaction(getStudentUser(request).getObjectId("_id"), referenceId);
//    }

}
