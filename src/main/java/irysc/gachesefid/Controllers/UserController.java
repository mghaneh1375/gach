package irysc.gachesefid.Controllers;


import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Enc;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;

public class UserController {

    public static String signUp(JSONObject jsonObject) {

        Utility.convertPersian(jsonObject);

        if(jsonObject.has("introduced") && !userRepository.exist(
                eq("invitation_code", jsonObject.getString("introduced")))
        )
            return Utility.generateErr("کد معرف وارد شده معتبر نمی باشد.");

        if (userRepository.exist(
                eq("username", jsonObject.getString("phone"))
        ))
            return Utility.generateErr("شماره همراه وارد شده در سامانه موجود است.");

        PairValue existTokenP = UserRepository.existSMS(jsonObject.getString("phone"));

        if (existTokenP != null)
            return new JSONObject().put("status", "ok").put("token", existTokenP.getKey())
                    .put("reminder", existTokenP.getValue()).toString();

        String existToken = UserRepository.sendNewSMSSignUp(
                jsonObject.getString("phone"),
                jsonObject.getString("password"),
                jsonObject.has("introduced") ? jsonObject.getString("introduced") : ""
        );

        return new JSONObject()
                .put("status", "ok")
                .put("token", existToken)
                .put("reminder", 300)
                .toString();
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
                        eq("phone", jsonObject.getString("phone")))
                , null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        long createdAt = doc.getLong("created_at");
        if (System.currentTimeMillis() - createdAt < SMS_RESEND_MSEC)
            return Utility.generateErr("کد قبلی هنوز منقضی نشده است.");

        int code = Utility.randInt();
        Utility.sendSMS(code, doc.getString("phone"));
        doc.put("code", code);

        activationRepository.updateOne(
                doc.getObjectId("_id"),
                new BasicDBObject("$set", new BasicDBObject("code", code)
                        .append("created_at", System.currentTimeMillis()))
        );

        return JSON_OK;
    }

    public static String activate(int code, String token, String phone) {

        Document doc = activationRepository.findOneAndDelete(and(
                eq("token", token),
                eq("phone", phone),
                eq("code", code))
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return new JSONObject().put("status", "nok").put("msg", "زمان توکن شما منقضی شده است.").toString();

        Document config = Utility.getConfig();
        Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

        Document newDoc = new Document("status", "active")
                .append("level", false)
                .append("username", doc.getString("phone"))
                .append("money", config.getInteger("init_money"))
                .append("coin", config.getInteger("init_coin"))
                .append("studentId", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                .append("events", new ArrayList<>())
                .append("avatar_id", avatar.getObjectId("_id"))
                .append("pic", avatar.getString("file"))
                .append("invitation_code", Utility.randomString(8))
                .append("created_at", System.currentTimeMillis())
                .append("password", doc.getString("password"));

        if(doc.containsKey("introduced") && !doc.getString("introduced").isEmpty()) {
            newDoc.put("introduced", doc.getString("introduced"));
            newDoc.put("money", newDoc.getInteger("money") + config.getInteger("invitation_money"));
            newDoc.put("coin", newDoc.getInteger("coin") + config.getInteger("invitation_coin"));
        }

        userRepository.insertOne(newDoc);
        return JSON_OK;
    }

    public static String isAuth(Document user) {

        boolean isComplete = user.containsKey("NID") && user.containsKey("pic");
        JSONObject jsonObject = new JSONObject().put("status", "ok")
                .put("isComplete", isComplete);

        jsonObject.put("user", UserRepository.convertUser(user));
        if (user.containsKey("access"))
            return jsonObject.put("access", user.getString("access"))
                    .toString();

        return jsonObject.put("access", "student").toString();

    }

    public static String forgetPass(JSONObject jsonObject) {

        String unique = Utility.convertPersianDigits(jsonObject.getString("unique").toLowerCase());
        String via = jsonObject.getString("via");

        if (!via.equals("sms") && !via.equals("email") && !via.equals("both"))
            return JSON_NOT_VALID_PARAMS;

        Document user = userRepository.findOne(or(
                eq("mail", unique),
                eq("username", unique),
                eq("NID", unique)
        ), new BasicDBObject("username", 1).append("mail", 1));

        if (user == null)
            return JSON_NOT_VALID_PARAMS;

        String username = user.getString("username");
        PairValue existTokenP = UserRepository.existSMS(username);

        if (existTokenP != null)
            return new JSONObject().put("status", "ok").put("token", existTokenP.getKey())
                    .put("reminder", existTokenP.getValue()).toString();

        String existToken = UserRepository.sendNewSMS(username, via, user.getString("mail"));

        return new JSONObject().put("status", "ok").put("token", existToken)
                .put("reminder", SMS_RESEND_SEC).toString();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
