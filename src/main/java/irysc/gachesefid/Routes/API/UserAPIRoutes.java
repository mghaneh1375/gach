package irysc.gachesefid.Routes.API;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Badge.BadgeController;
import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.activationRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;


@Controller
@RequestMapping(path = "/api/user")
@Validated
public class UserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @Value("${shop.security.token}")
    private String token;

    @GetMapping(value = "ttt")
    @ResponseBody
    public String v(HttpServletRequest request) throws UnAuthException {
        UserTokenInfo userTokenInfo = getUserTokenInfo(request);
        return userTokenInfo.getAccesses().toString();
    }

    @PostMapping(value = "/createOpenCardOff")
    @ResponseBody
    public String createOpenCardOff(HttpServletRequest request,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {"amount", "mode"},
                                            paramsType = {Number.class, String.class}
                                    ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUser(request);
        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));
        String mode = jsonObject.getString("mode");

        if (!mode.equalsIgnoreCase("charge") && !mode.equalsIgnoreCase("coin"))
            return JSON_NOT_VALID_PARAMS;

        int amount;

        if (mode.equalsIgnoreCase("charge")) {
            double mainMoney = ((Number) (user.get("money"))).doubleValue();
            int money = (int) mainMoney;
            amount = jsonObject.getInt("amount");

            if (amount > money)
                return generateErr("حداکثر مقدار قابل تبدیل برای شما " + money + " می باشد.");
        } else {

            double coin = ((Number) (user.get("coin"))).doubleValue();

            if (jsonObject.getNumber("amount").doubleValue() > coin)
                return generateErr("حداکثر مقدار قابل تبدیل برای شما " + coin + " می باشد.");

            Document doc = Utility.getConfig();
            amount = (int) (doc.getInteger("coin_rate_coef") * jsonObject.getNumber("amount").doubleValue());
        }

        if (amount > 500000 || amount < 40000)
            return generateErr("حداکثر تخفیف ۵۰۰۰۰۰ و حداقل ۴۰۰۰۰ تومان می باشد.");

        while (true) {
            String code = "ir-" + Utility.simpleRandomString(3).replace("_", "") + Utility.getRandIntForGift(1000);
            JSONObject data = new JSONObject()
                    .put("name", System.currentTimeMillis() + "_" + user.getObjectId("_id").toString())
                    .put("code", code)
                    .put("discount", amount)
                    .put("type", "F")
                    .put("token", token);

            try {

                HttpResponse<String> jsonResponse = Unirest.post("https://shop.irysc.com/index.php?route=extension/total/coupon/add_coupon_api")
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .body(data).asString();

                if (jsonResponse.getStatus() == 200) {
                    String res = jsonResponse.getBody();
                    if (res.equals("ok")) {

                        if (mode.equalsIgnoreCase("charge")) {
                            double mainMoney = ((Number) (user.get("money"))).doubleValue();
                            user.put("money", ((mainMoney - jsonObject.getInt("amount")) * 100.0) / 100.0);
                        } else {
                            double coin = ((Number) (user.get("coin"))).doubleValue();
                            user.put("coin", ((coin - jsonObject.getNumber("amount").doubleValue()) * 100.0) / 100.0);
                        }

                        userRepository.replaceOne(user.getObjectId("_id"), user);
                        return generateSuccessMsg("data", code);
                    }

                    if (!res.equals("nok4")) {
                        System.out.println(res);
                        return JSON_NOT_UNKNOWN;
                    }
                }

            } catch (UnirestException e) {
                return generateErr(e.getMessage());
            }
        }


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
            throws NotActivateAccountException, UnAuthException, InvalidFieldsException {
        Document user = (Document) getUserWithSchoolAccess(request, false, false, userId).get("user");
        return generateSuccessMsg("user",
                UserController.isAuth(user)
        );
    }

    @GetMapping(value = "/getInfo")
    @ResponseBody
    public String getInfo(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {
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
    public String logout(HttpServletRequest request
    ) throws NotActivateAccountException, UnAuthException {
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

    @PutMapping(value = "/aboutMe")
    @ResponseBody
    public String aboutMe(HttpServletRequest request,
                          @RequestBody @StrongJSONConstraint(
                                  params = {
                                          "wantToAdvice", "wantToTeach"
                                  },
                                  paramsType = {
                                          Boolean.class, Boolean.class
                                  },
                                  optionals = {
                                          "teachVideoLink", "adviceVideoLink",
                                          "defaultTeachPrice", "teachAboutMe",
                                          "adviceAboutMe"
                                  },
                                  optionalsType = {
                                          String.class, String.class,
                                          Positive.class, String.class,
                                          String.class
                                  }
                          ) @NotBlank String json
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return UserController.setAboutMe(getAdvisorUser(request), convertPersian(new JSONObject(json)));
    }

    @GetMapping(value = "getMyFields")
    @ResponseBody
    public String getMyFields(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return UserController.getMyFields(getAdvisorUser(request));
    }

    @PutMapping(value = "setMyFields")
    @ResponseBody
    public String setMyFields(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {},
                    paramsType = {},
                    optionals = {
                            "lessons", "grades",
                            "branches"
                    },
                    optionalsType = {
                            JSONArray.class, JSONArray.class,
                            JSONArray.class
                    }
            ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {
        JSONObject jsonObject = new JSONObject(jsonStr);
        return UserController.setMyFields(
                getUser(request),
                jsonObject.has("lessons") ? jsonObject.getJSONArray("lessons") : null,
                jsonObject.has("grades") ? jsonObject.getJSONArray("grades") : null,
                jsonObject.has("branches") ? jsonObject.getJSONArray("branches") : null
        );
    }


    @PostMapping(value = "/signUp")
    @ResponseBody
    public String signUp(@RequestBody @StrongJSONConstraint(
            params = {
                    "password", "username",
                    "firstName", "lastName",
                    "NID", "authVia"
            },
            paramsType = {
                    String.class, String.class,
                    String.class, String.class,
                    String.class, AuthVia.class
            }
    ) @NotBlank String json) {
        try {
            JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

            if (!Utility.isValidPassword(jsonObject.getString("password")))
                return generateErr("رمزعبور باید حداقل 6 کاراکتر باشد.");

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
                                             "branches", "schoolId", "gradeId", "birthDay"
                                     },
                                     optionalsType = {
                                             JSONArray.class, ObjectId.class,
                                             ObjectId.class, Long.class
                                     }
                             ) String json
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
        return UserController.updateInfo(
                convertPersian(new JSONObject(json)),
                (Document) getUserWithSchoolAccess(request, false, false, userId).get("user"),
                userId != null
        );
    }


    @PutMapping(value = {"/blockNotif", "/blockNotif/{studentId}"})
    @ResponseBody
    public String blockNotif(HttpServletRequest request,
                             @PathVariable(required = false) String studentId
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
        return UserController.blockNotif((Document) getUserWithSchoolAccess(request, false, false, studentId).get("user"));
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
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
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
        if (
                user.containsKey("birth_day") && user.containsKey("branches") &&
                        user.containsKey("school") && user.containsKey("grade") &&
                        user.containsKey("mail") && user.containsKey("phone")
        ) {
            new Thread(() -> {
                BadgeController.checkForUpgrade(user.getObjectId("_id"), Action.COMPLETE_PROFILE);
                PointController.addPointForAction(user.getObjectId("_id"), Action.COMPLETE_PROFILE, 1, null);
            });
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
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
        Document doc = getUserWithSchoolAccess(request, false, false, userId);

        if (doc.getBoolean("isAdmin"))
            return UserController.forceUpdateUsername(
                    (Document) doc.get("user"), convertPersian(new JSONObject(json))
            );

        return UserController.updateUsername(
                ((Document) doc.get("user")).getString("NID"),
                convertPersian(new JSONObject(json))
        );
    }

    @GetMapping(value = "/doChangeMail/{link}")
    @ResponseBody
    public String doChangeMail(HttpServletRequest request,
                               @PathVariable @NotBlank String link)
            throws UnAuthException, NotActivateAccountException {
        return UserController.doChangeMail(getUser(request), link);
    }

    @PostMapping(value = "/confirmForgetPassword")
    @ResponseBody
    public String confirmForgetPassword(@RequestBody @JSONConstraint(params = {"token", "mail", "code"}) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        jsonObject.put("mail", jsonObject.getString("mail").toLowerCase());

        if (jsonObject.getString("token").length() != 20)
            return generateErr("invalid token");

        if (jsonObject.getInt("code") < 10000 || jsonObject.getInt("code") > 99999)
            return generateErr("invalid code");

        if (!Utility.isValidMail(jsonObject.getString("mail")))
            return generateErr("mail is incorrect");

        Document doc = activationRepository.findOne(and(
                        eq("token", jsonObject.getString("token")),
                        eq("mail", jsonObject.getString("mail")),
                        eq("code", jsonObject.getInt("code")))
                , null);

        if (doc == null)
            return JSON_NOT_VALID_TOKEN;

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return generateErr("token has been expired");

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
    ) throws NotActivateAccountException, UnAuthException, InvalidFieldsException {

        Document doc = getUserWithSchoolAccess(request, false, false, userId);
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
            if (!isAdmin) {
                oldPassword = Utility.convertPersianDigits(oldPassword);

                if (!userService.isOldPassCorrect(oldPassword, user.getString("password")))
                    return Utility.generateErr("رمزعبور وارد شده صحیح نیست.");
            }

            userService.deleteFromCache(user.getString("NID"));

            if (user.containsKey("phone") && !user.getString("phone").isEmpty())
                userService.deleteFromCache(user.getString("phone"));

            if (user.containsKey("mail") && !user.getString("mail").isEmpty())
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
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = (Document) getUserWithAdminAccess(request, false, true, userId).get("user");

        if (UserController.setPic(file, user))
            return generateSuccessMsg("file", "");

        return JSON_NOT_UNKNOWN;
    }

    @PutMapping(value = {"/setAvatar/{avatarId}", "/setAvatar/{avatarId}/{userId}"})
    @ResponseBody
    public String setAvatar(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId avatarId,
                            @PathVariable(required = false) String userId
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
        return UserController.setAvatar((
                        Document) getUserWithSchoolAccess(request, false, false, userId).get("user"),
                avatarId
        );
    }


    @GetMapping(value = "/myTransactions")
    @ResponseBody
    public String myTransactions(HttpServletRequest request
    ) throws UnAuthException {
        return PayPing.myTransactions(getUserId(request));
    }

    @GetMapping(value = "/getEducationalHistory/{userId}")
    @ResponseBody
    public String getEducationalHistory(HttpServletRequest request,
                                        @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getPrivilegeUser(request);
        if (!Authorization.hasWeakAccessToThisStudent(userId, user.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        return UserController.getEducationalHistory(userId);
    }


    @GetMapping(value = "getTeacherProfile/{teacherId}")
    @ResponseBody
    public String getTeacherProfile(
            @PathVariable @ObjectIdConstraint ObjectId teacherId
    ) {
        return UserController.getTeacherProfile(teacherId);
    }
}
