package irysc.gachesefid.Controllers;


import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Utility.Enc;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class UserController {

    private static String[] fieldsNeededForSchool = new String[]{
            "a", "b", "c"
    };
    private static String[] fieldsNeededForAdvisor = new String[]{
            "schoolName", "schoolPhone"
    };
    private static String[] fieldsNeededForAgent = new String[]{
            "stateName"
    };

    public static String whichKindOfAuthIsAvailable(String NID) {

        Document user = userRepository.findBySecKey(NID);
        if (user == null || !user.getString("status").equals("active"))
            return generateSuccessMsg("via", "none");

        if (user.containsKey("phone") && user.containsKey("mail"))
            return generateSuccessMsg("via", "both");

        if (user.containsKey("phone"))
            return generateSuccessMsg("via", "phone");

        return generateSuccessMsg("via", "mail");
    }

    public static String signUp(JSONObject jsonObject) {

        if (!EnumValidatorImp.isValid(jsonObject.getString("authVia"), AuthVia.class))
            return JSON_NOT_VALID_PARAMS;

        String authVia = jsonObject.getString("authVia");

        if (authVia.equals(AuthVia.SMS.getName()) &&
                !PhoneValidator.isValid(jsonObject.getString("username"))
        )
            return generateErr("شماره همراه وارد شده نامعتبر است.");

        if (authVia.equals(AuthVia.MAIL.getName()) &&
                !Utility.isValidMail(jsonObject.getString("username"))
        )
            return generateErr("ایمیل وارد شده نامعتبر است.");

        if (!Utility.validationNationalCode(jsonObject.getString("NID")))
            return generateErr("کد ملی وارد شده نامعتبر است.");

        if (userRepository.exist(
                or(
                        eq("mail", jsonObject.getString("username")),
                        eq("phone", jsonObject.getString("username"))
                )
        ))
            return Utility.generateErr("شماره همراه/ایمیل وارد شده در سامانه موجود است.");

        PairValue existTokenP = UserRepository.existSMS(jsonObject.getString("username"));

        if (existTokenP != null)
            return generateSuccessMsg("token", existTokenP.getKey(),
                    new PairValue("reminder", existTokenP.getValue())
            );

        String existToken = UserRepository.sendNewSMSSignUp(
                jsonObject.getString("username"),
                jsonObject.getString("password"),
                jsonObject.getString("firstName"),
                jsonObject.getString("lastName"),
                jsonObject.getString("NID"), authVia
        );

        return generateSuccessMsg("token", existToken,
                new PairValue("reminder", SMS_RESEND_SEC)
        );
    }

    public static String setInfo(JSONObject jsonObject, Document user) {

        Utility.convertPersian(jsonObject);
        boolean isComplete = (user.containsKey("NID") || user.containsKey("passport_no"));

        int exist = userRepository.isExistByNID(
                (jsonObject.has("NID")) ? jsonObject.getString("NID") :
                        jsonObject.getString("passport_no"),
                user.getObjectId("_id")
        );

        if (exist < 0)
            return new JSONObject().put("status", "nok")
                    .put("msg", (exist == -4) ? "Duplicate National Code" : "Duplicate passport no").toString();

        BasicDBObject updateList = new BasicDBObject();

        if (jsonObject.has("phone") &&
                !jsonObject.getString("phone").equals(user.getString("username"))) {

            if (userRepository.exist(and(
                    eq("username", jsonObject.getString("phone")),
                    ne("_id", user.getObjectId("_id"))
                    )
            )
            )
                return new JSONObject().put("status", "nok")
                        .put("msg", "This phone already exist").toString();

            updateList.put("username", jsonObject.getString("phone"));
            user.put("username", jsonObject.getString("phone"));
            jsonObject.remove("phone");
        }

        for (String key : jsonObject.keySet()) {
            user.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), jsonObject.get(key));
            updateList.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), jsonObject.get(key));
        }

        if (user.containsKey("n_i_d")) {
            user.put("NID", user.get("n_i_d"));
            user.remove("n_i_d");

            updateList.put("NID", updateList.get("n_i_d"));
            updateList.remove("n_i_d");
        }

        new Thread(() -> {

            userRepository.updateOne(user.getObjectId("_id"),
                    new BasicDBObject("$set", updateList));

            userRepository.checkCache(user);

        }).start();

        return JSON_OK;
    }

    public static String resend(JSONObject jsonObject) {

        Utility.convertPersian(jsonObject);

        Document doc = activationRepository.findOne(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.getString("username")))
                , null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        long createdAt = doc.getLong("created_at");
        if (System.currentTimeMillis() - createdAt < SMS_RESEND_MSEC)
            return Utility.generateErr("کد قبلی هنوز منقضی نشده است.");

        int code = Utility.randInt();

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName()))
            Utility.sendSMS(code, doc.getString("username"));
        else
            Utility.sendMail(
                    doc.getString("username"), code + "", "کد اعتبارسنجی",
                    "signUp", doc.getString("first_name") + " " + doc.getString("last_name")
            );

        doc.put("code", code);

        activationRepository.updateOne(
                doc.getObjectId("_id"),
                new BasicDBObject("$set", new BasicDBObject("code", code)
                        .append("created_at", System.currentTimeMillis()))
        );

        return generateSuccessMsg("reminder", SMS_RESEND_SEC);
    }

    public static String setIntroducer(Document user, String invitationCode) {

        if (
                !userRepository.exist(eq("invitation_code", invitationCode))
        )
            return Utility.generateErr("کد معرف وارد شده معتبر نمی باشد.");

        Document config = Utility.getConfig();

        user.put("introduced", invitationCode);
        user.put("money", user.getInteger("money") + config.getInteger("invitation_money"));
        user.put("coin", user.getInteger("coin") + config.getInteger("invitation_coin"));

        return JSON_OK;
    }

    public static String setRole(Document user, JSONObject jsonObject) {

        String role = jsonObject.getString("role");

        if (!EnumValidatorImp.isValid(role, Access.class) ||
                role.equals(Access.ADMIN.getName()) ||
                role.equals(Access.SUPERADMIN.getName())
        )
            return generateErr("سطح دسترسی انتخاب شده معتبر نمی باشد.");

        Set<String> keys = jsonObject.keySet();
        String[] wantedList;
        if (role.equals(Access.ADVISOR.getName()))
            wantedList = fieldsNeededForAdvisor;
        else if (role.equals(Access.SCHOOL.getName()))
            wantedList = fieldsNeededForSchool;
        else
            wantedList = fieldsNeededForAgent;

        for (String key : wantedList) {
            if (!keys.contains(key))
                generateErr("لطفا تمام اطلاعات لازم را پر نمایید.");
        }

        for (String key : keys)
            user.put(key, jsonObject.get(key));

        userRepository.replaceOne(
                user.getObjectId("_id"),
                user
        );

        return JSON_OK;
    }

    public static String activate(int code, String token, String username
    ) throws InvalidFieldsException {

        Document doc = activationRepository.findOneAndDelete(and(
                eq("token", token),
                eq("username", username),
                eq("code", code))
        );

        if (doc == null)
            throw new InvalidFieldsException("کد وارد شده معتبر نیست");

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            throw new InvalidFieldsException("زمان توکن شما منقضی شده است.");

        Document config = Utility.getConfig();
        Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

        Document newDoc = new Document("status", "active")
                .append("level", false)
                .append("first_name", doc.getString("first_name"))
                .append("last_name", doc.getString("last_name"))
                .append("NID", doc.getString("NID"))
                .append("money", config.getInteger("init_money"))
                .append("coin", config.getDouble("init_coin"))
                .append("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                .append("events", new ArrayList<>())
                .append("avatar_id", avatar.getObjectId("_id"))
                .append("pic", avatar.getString("file"))
                .append("invitation_code", Utility.randomString(8))
                .append("created_at", System.currentTimeMillis())
                .append("password", doc.getString("password"));

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName()))
            newDoc.append("phone", username);
        else
            newDoc.append("mail", username);

        userRepository.insertOne(newDoc);
        return doc.getString("password");
    }

    public static String isAuth(Document user) {

        boolean isComplete = user.containsKey("NID") && user.containsKey("pic");
        JSONObject jsonObject = new JSONObject().put("status", "ok")
                .put("isComplete", isComplete);

        jsonObject.put("user", UserRepository.convertUser(user));
        if (user.containsKey("access"))
            return jsonObject.put("access", user.getList("accesses", String.class))
                    .toString();

        return jsonObject.put("access", "student").toString();

    }

    public static String forgetPass(JSONObject jsonObject) {

        String NID = Utility.convertPersianDigits(jsonObject.getString("NID"));
        if (!Utility.validationNationalCode(NID))
            return JSON_NOT_VALID_PARAMS;

        String via = jsonObject.getString("authVia");

        if (!EnumValidatorImp.isValid(via, AuthVia.class))
            return JSON_NOT_VALID_PARAMS;

        Document user = userRepository.findOne(
                eq("NID", NID),
                new BasicDBObject("phone", 1).append("mail", 1)
        );

        if (user == null)
            return JSON_NOT_VALID_PARAMS;

        if (via.equals(AuthVia.SMS.getName()) && !user.containsKey("phone"))
            return JSON_NOT_VALID_PARAMS;

        if (via.equals(AuthVia.MAIL.getName()) && !user.containsKey("mail"))
            return JSON_NOT_VALID_PARAMS;

        PairValue existTokenP = UserRepository.existSMS(NID);

        if (existTokenP != null)
            return generateSuccessMsg("token", existTokenP.getKey(),
                    new PairValue("reminder", existTokenP.getValue())
            );

        String existToken = UserRepository.sendNewSMS(NID, via);

        return generateSuccessMsg("token", existToken,
                new PairValue("reminder", SMS_RESEND_SEC)
        );
    }

    public static String changeMail(Document user, String newMail) {

        newMail = newMail.toLowerCase();
        if (userRepository.exist(eq("mail", newMail)))
            return new JSONObject()
                    .put("status", "nok")
                    .put("msg", "selected mail exist in system")
                    .toString();

        Enc.Ticket t = new Enc.Ticket(newMail, user.getString("username"), user.getObjectId("_id"));
        try {

            String link = Enc.encryptObject(t);
            link = SERVER + "submit-email/" + link.replace("/", "**^^$$");

            String finalNewMail = newMail;
            String finalLink = link;

            new Thread(() -> Utility.sendMail(finalNewMail, finalLink, "Change Mail",
                    "changeMail", user.getString("name_en") + " " + user.getString("last_name_en")
            )).start();

            return JSON_OK;
        } catch (Exception e) {
            printException(e);
        }

        return JSON_NOT_UNKNOWN;
    }

    public static String doChangeMail(Document user, String link) {

        try {
            Enc.Ticket t = Enc.decryptObject(link.replace("**^^$$", "/"));

            if (
                    !t.username.equals(user.getString("username")) ||
                            !t.userId.equals(user.getObjectId("_id"))
            )
                return JSON_NOT_VALID_TOKEN;

            if ((System.currentTimeMillis() - t.time) / 1000 > 300)
                return new JSONObject()
                        .put("status", "nok")
                        .put("msg", "Token has been expired")
                        .toString();

            user.put("mail", t.newMail);

            new Thread(() -> {

                userRepository.checkCache(user);
                userRepository.updateOne(eq("_id", user.getObjectId("_id")),
                        set("mail", t.newMail));

            }).start();

        } catch (Exception e) {
            printException(e);
        }

        return JSON_OK;
    }

    public static boolean setPic(MultipartFile file, Document user) {

        if (user.getString("pic") != null)
            new Thread(() -> FileUtils.removeFile(user.getString("pic"), UserRepository.FOLDER)).start();

        String filename = FileUtils.uploadFile(file, UserRepository.FOLDER);
        if (filename != null) {

            user.put("pic", filename);

            if (user.containsKey("avatar_id")) {
                Document oldAvatar = avatarRepository.findById(user.getObjectId("avatar_id"));
                oldAvatar.put("used", oldAvatar.getInteger("used") - 1);
                avatarRepository.updateOne(oldAvatar.getObjectId("_id"), set("used", oldAvatar.getInteger("used")));
                user.remove("avatar_id");
            }

            userRepository.replaceOne(user.getObjectId("_id"), user);
            userRepository.checkCache(user);

            return true;
        }

        return false;
    }

    public static String setAvatar(Document user, ObjectId avatarId) {

        Document avatar = avatarRepository.findById(avatarId);
        if (avatar == null)
            return JSON_NOT_VALID_ID;

        if (user.containsKey("pic") && !user.containsKey("avatar_id"))
            new Thread(() -> FileUtils.removeFile(user.getString("pic"), UserRepository.FOLDER)).start();

        if (user.containsKey("avatar_id")) {

            if (user.getObjectId("avatar_id").equals(avatarId))
                return JSON_OK;

            Document oldAvatar = avatarRepository.findById(user.getObjectId("avatar_id"));
            oldAvatar.put("used", oldAvatar.getInteger("used") - 1);
            avatarRepository.updateOne(oldAvatar.getObjectId("_id"), set("used", oldAvatar.getInteger("used")));

        }

        user.put("avatar_id", avatarId);
        // if admin update avatar, user avatar will not change !!!
        user.put("pic", avatar.getString("file"));

        userRepository.replaceOne(user.getObjectId("_id"), user);

        avatar.put("used", avatar.getInteger("used") + 1);
        avatarRepository.updateOne(avatarId, set("used", avatar.getInteger("used")));

        return JSON_OK;
    }

}
