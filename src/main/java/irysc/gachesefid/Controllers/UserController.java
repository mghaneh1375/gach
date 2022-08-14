package irysc.gachesefid.Controllers;


import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.Enc;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class UserController {

    private static FormField[] fieldsNeededForSchool = new FormField[]{
            new FormField(true, "tel", "تلفن مدرسه", null, true),
            new FormField(true, "address", "آدرس مدرسه", null, false),
            new FormField(true, "name", "نام مدرسه", null, false),
            new FormField(true, "managerName", "نام مدیر مدرسه", null, false),
            new FormField(true, "schoolSex", "نوع مدرسه", null,
                    new PairValue("female", "دخترانه"),
                    new PairValue("male", "پسرانه")
            ),
            new FormField(true, "kindSchool", "مقطع مدرسه", null,
                    new PairValue(GradeSchool.DABESTAN.getName(), "دبستان"),
                    new PairValue(GradeSchool.MOTEVASETEAVAL.getName(), "متوسطه اول"),
                    new PairValue(GradeSchool.MOTEVASETEDOVOM.getName(), "متوسطه دوم")
            ),
//            new FormField(false, "bio", "بیو", "اختیاری", false),
    };

    private static FormField[] fieldsNeededForAdvisor = new FormField[]{
            new FormField(true, "workYear", "سابقه کار", null, true),
            new FormField(true, "workSchools", "مدارس کارکرده", null, false),
            new FormField(true, "universeField", "رشته تحصیلی", null, false),
            new FormField(false, "bio", "بیو", "اختیاری", false),
    };

    private static FormField[] fieldsNeededForAgent = new FormField[]{
            new FormField(true, "state", "استان", null, false),
            new FormField(false, "bio", "بیو", "اختیاری", false),
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

    private static String translateRole(String role) {

        if (role.equalsIgnoreCase(Access.ADVISOR.getName()))
            return "مشاور";

        if (role.equalsIgnoreCase(Access.SCHOOL.getName()))
            return "مدرسه";

        if (role.equalsIgnoreCase(Access.TEACHER.getName()))
            return "دبیر";

        if (role.equalsIgnoreCase(Access.AGENT.getName()))
            return "نماینده";

        if (role.equalsIgnoreCase(Access.ADMIN.getName()))
            return "ادمین";

        if (role.equalsIgnoreCase(Access.SUPERADMIN.getName()))
            return "سوپر ادمین";

        return "دانش آموز";
    }

    private static FormField[] getWantedList(String role) {

        FormField[] wantedList = null;

        if (role.equals(Access.ADVISOR.getName()))
            wantedList = fieldsNeededForAdvisor;
        else if (role.equals(Access.SCHOOL.getName()))
            wantedList = fieldsNeededForSchool;
        else
            wantedList = fieldsNeededForAgent;

        return wantedList;
    }

    public static String setRole(Document user, JSONObject jsonObject) {

        String role = jsonObject.getString("role");

        if (!EnumValidatorImp.isValid(role, Access.class) ||
                role.equals(Access.ADMIN.getName()) ||
                role.equals(Access.SUPERADMIN.getName())
        )
            return generateErr("سطح دسترسی انتخاب شده معتبر نمی باشد.");

        Set<String> keys = jsonObject.keySet();
        FormField[] wantedList = getWantedList(role);
        if (wantedList == null)
            return JSON_NOT_VALID_PARAMS;

        for (FormField field : wantedList) {
            if (field.isMandatory && !keys.contains(field.key))
                return generateErr("لطفا تمام اطلاعات لازم را پر نمایید.");
            if(keys.contains(field.key) && field.pairValues != null) {

                boolean find = false;
                for(PairValue p : field.pairValues) {
                    if(jsonObject.get(field.key).equals(p.getKey())) {
                        find = true;
                        break;
                    }
                }
                if(!find)
                    return JSON_NOT_VALID_PARAMS;
            }
        }

        Document form = new Document("role", role);

        for (String key : keys)
            form.put(key, jsonObject.get(key));

        List<Document> forms;
        int idx = -1;

        if (user.containsKey("form_list")) {
            forms = user.getList("form_list", Document.class);
            idx = Utility.searchInDocumentsKeyValIdx(forms, "role", role);
        } else
            forms = new ArrayList<>();

        if (idx == -1)
            forms.add(form);
        else
            forms.set(idx, form);

        user.put("form_list", forms);
        userRepository.replaceOne(
                user.getObjectId("_id"),
                user
        );

        long curr = System.currentTimeMillis();

        ArrayList<Document> chats = new ArrayList<>();
        chats.add(new Document("msg", "")
                .append("created_at", curr)
                .append("is_for_user", true)
                .append("files", new ArrayList<>())
        );

        ticketRepository.insertOne(
                new Document("title", "درخواست ارتقای سطح به " + translateRole(role))
                        .append("created_at", curr)
                        .append("send_date", curr)
                        .append("is_for_teacher", false)
                        .append("chats", chats)
                        .append("status", "pending")
                        .append("priority", TicketPriority.HIGH.getName())
                        .append("section", TicketSection.UPGRADELEVEL.getName())
                        .append("user_id", user.getObjectId("_id"))
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
                .append("accesses", new ArrayList<>() {
                    {
                        add("student");
                    }
                })
                .append("password", doc.getString("password"));

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName()))
            newDoc.append("phone", username);
        else
            newDoc.append("mail", username);

        userRepository.insertOne(newDoc);
        return doc.getString("password");
    }

    public static JSONObject convertUser(Document user) {

        ObjectId userId = user.getObjectId("_id");

        Document city = !user.containsKey("city") ? null :
                (Document) user.get("city");

        ObjectId cityId = city != null ? city.getObjectId("_id") : null;

        Document state = cityId != null ? stateRepository.findById(
                cityRepository.findById(cityId).getObjectId("state_id")
        ) : null;

        List<Document> branches = user.containsKey("branches") ?
                user.getList("branches", Document.class) :
                new ArrayList<>();

        JSONArray branchesJSON = new JSONArray();
        for (Document branch : branches) {
            branchesJSON.put(new JSONObject()
                    .put("id", branch.getObjectId("_id").toString())
                    .put("name", branch.getString("name"))
            );
        }

        JSONObject jsonObject = new JSONObject()
                .put("id", userId.toString())
                .put("pic", (user.containsKey("pic")) ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("firstName", user.getString("first_name"))
                .put("NID", user.getString("NID"))
                .put("grade", !user.containsKey("grade") ? "" : new JSONObject().put("id",
                        ((Document) user.get("grade")).getObjectId("_id").toString())
                        .put("name",
                                ((Document) user.get("grade")).getString("name"))
                )
                .put("school", !user.containsKey("school") ? "" : new JSONObject().put("id",
                        ((Document) user.get("school")).getObjectId("_id").toString())
                        .put("name",
                                ((Document) user.get("school")).getString("name"))
                )
                .put("city", city == null ? "" : new JSONObject()
                        .put("id", cityId.toString())
                        .put("name", city.getString("name"))
                )
                .put("state", state == null ? "" : new JSONObject()
                        .put("id", state.getObjectId("_id").toString())
                        .put("name", state.getString("name"))
                )
                .put("branches", branchesJSON)
                .put("lastName", user.getString("last_name"))
                .put("mail", user.getOrDefault("mail", ""))
                .put("sex", user.getOrDefault("sex", ""))
                .put("phone", user.getOrDefault("phone", ""));

        if (user.containsKey("form_list")) {

            JSONArray formsJSON = new JSONArray();
            List<Document> forms = user.getList("form_list", Document.class);

            for (Document form : forms) {
                JSONObject jsonObject1 = new JSONObject();
                String role = form.getString("role");

                FormField[] wantedList = getWantedList(role);
                if (wantedList == null)
                    continue;

                jsonObject1.put("role", role)
                        .put("roleFa", translateRole(role));

                JSONArray data = new JSONArray();

                for (FormField field : wantedList) {

                    JSONObject jsonObject2 = new JSONObject()
                            .put("key", field.key)
                            .put("value", form.getOrDefault(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.key), ""))
                            .put("help", field.help)
                            .put("title", field.title)
                            .put("isJustNum", field.isJustNum);

                    if(field.pairValues != null) {
                        JSONArray jsonArray1 = new JSONArray();
                        for(PairValue p : field.pairValues)
                            jsonArray1.put(
                                    new JSONObject()
                                            .put("id", p.getKey())
                                            .put("item", p.getValue())
                            );

                        jsonObject2.put("keyVals", jsonArray1);
                    }

                    data.put(jsonObject2);
                }

                jsonObject1.put("data", data);
                formsJSON.put(jsonObject1);
            }

            jsonObject.put("forms", formsJSON);
        }


        return jsonObject;

    }

    private static void fillWithFormFields(FormField[] fields, JSONArray jsonArray, String role) {

        JSONObject jsonObject1 = new JSONObject()
                .put("role", role);

        JSONArray data = new JSONArray();

        for (FormField field : fields) {

            JSONObject jsonObject =  new JSONObject()
                    .put("key", field.key)
                    .put("help", field.help)
                    .put("isMandatory", field.isMandatory)
                    .put("title", field.title)
                    .put("isJustNum", field.isJustNum);

            if(field.pairValues != null) {
                JSONArray jsonArray1 = new JSONArray();
                for(PairValue p : field.pairValues)
                    jsonArray1.put(
                            new JSONObject()
                                .put("id", p.getKey())
                                .put("item", p.getValue())
                    );

                jsonObject.put("keyVals", jsonArray1);
            }

            data.put(jsonObject);
        }

        jsonObject1.put("data", data);
        jsonArray.put(jsonObject1);
    }

    public static String getRoleForms() {

        JSONArray formsJSON = new JSONArray();
        fillWithFormFields(fieldsNeededForAdvisor, formsJSON, Access.ADVISOR.getName());
        fillWithFormFields(fieldsNeededForAgent, formsJSON, Access.AGENT.getName());
        fillWithFormFields(fieldsNeededForSchool, formsJSON, Access.SCHOOL.getName());

        return generateSuccessMsg("data", formsJSON);
    }

    public static JSONObject isAuth(Document user) {

        boolean isComplete = user.containsKey("NID") && user.containsKey("pic");
        JSONObject jsonObject = new JSONObject().put("isComplete", isComplete);

        jsonObject.put("user", convertUser(user));
        if (user.containsKey("accesses"))
            return jsonObject.put("accesses", user.getList("accesses", String.class));

        return jsonObject.put("accesses", "student");
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

        return sendSMSOrMail(NID, via);
    }

    public static String forceUpdateUsername(Document user, JSONObject data) {

        convertPersian(data);
        String via = data.getString("mode");

        if (!EnumValidatorImp.isValid(via, AuthVia.class))
            return JSON_NOT_VALID_PARAMS;

        String username = data.getString("username").toLowerCase();

        if (via.equals(AuthVia.SMS.getName()) && !PhoneValidator.isValid(username))
            return generateErr("شماره همراه وارد شده معتبر نمی باشد.");

        if (via.equals(AuthVia.MAIL.getName()) && !Utility.isValidMail(username))
            return generateErr("ایمیل وارد شده معتبر نمی باشد.");

        Bson filter = via.equals(AuthVia.SMS.getName()) ?
                eq("phone", username) : eq("mail", username);

        if (userRepository.exist(filter))
            return generateErr("ایمیل/شماره همراه وارد شده در سیستم موجود است.");

        if (via.equals(AuthVia.SMS.getName()))
            user.put("phone", username);
        else
            user.put("mail", username);

        userRepository.replaceOne(user.getObjectId("_id"), user);
        return JSON_OK;
    }

    public static String updateUsername(JSONObject data) {

        convertPersian(data);
        String via = data.getString("mode");

        if (!EnumValidatorImp.isValid(via, AuthVia.class))
            return JSON_NOT_VALID_PARAMS;

        String username = data.getString("username").toLowerCase();

        if (via.equals(AuthVia.SMS.getName()) && !PhoneValidator.isValid(username))
            return generateErr("شماره همراه وارد شده معتبر نمی باشد.");

        if (via.equals(AuthVia.MAIL.getName()) && !Utility.isValidMail(username))
            return generateErr("ایمیل وارد شده معتبر نمی باشد.");

        Bson filter = via.equals(AuthVia.SMS.getName()) ?
                eq("phone", username) : eq("mail", username);

        if (userRepository.exist(filter))
            return generateErr("ایمیل/شماره همراه وارد شده در سیستم موجود است.");

        return sendSMSOrMail(username, via);
    }

    private static String sendSMSOrMail(String username, String via) {

        PairValue existTokenP = UserRepository.existSMS(username);

        if (existTokenP != null)
            return generateSuccessMsg("token", existTokenP.getKey(),
                    new PairValue("reminder", existTokenP.getValue())
            );

        String token = UserRepository.sendNewSMS(username, via);

        return generateSuccessMsg("token", token,
                new PairValue("reminder", SMS_RESEND_SEC)
        );

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
                return generateSuccessMsg("file", STATICS_SERVER + UserRepository.FOLDER + "/" + avatar.getString("file"));

            Document oldAvatar = avatarRepository.findById(user.getObjectId("avatar_id"));
            if (oldAvatar == null) {
                oldAvatar.put("used", oldAvatar.getInteger("used") - 1);
                avatarRepository.updateOne(oldAvatar.getObjectId("_id"), set("used", oldAvatar.getInteger("used")));
            }

        }

        user.put("avatar_id", avatarId);
        // if admin update avatar, user avatar will not change !!!
        user.put("pic", avatar.getString("file"));

        userRepository.replaceOne(user.getObjectId("_id"), user);

        avatar.put("used", avatar.getInteger("used") + 1);
        avatarRepository.updateOne(avatarId, set("used", avatar.getInteger("used")));

        return generateSuccessMsg("file", STATICS_SERVER + UserRepository.FOLDER + "/" + avatar.getString("file"));
    }

    public static String addSchool(JSONObject data) {

        if (!EnumValidatorImp.isValid(data.getString("kind"), KindSchool.class))
            return JSON_NOT_VALID_PARAMS;

        if (!EnumValidatorImp.isValid(data.getString("grade"), GradeSchool.class))
            return JSON_NOT_VALID_PARAMS;

        String id = data.getString("cityId");
        if (!ObjectId.isValid(id))
            return JSON_NOT_VALID_PARAMS;

        Document city = cityRepository.findById(new ObjectId(id));
        if (city == null)
            return JSON_NOT_VALID_ID;

        Document newDoc = new Document();

        for (String key : data.keySet()) {

            if (key.equals("city"))
                continue;

            newDoc.append(
                    Utility.camel(key, false),
                    data.get(key)
            );
        }

        newDoc.append("city_id", city.getObjectId("_id"));
        newDoc.append("city_name", city.getString("name"));

        return schoolRepository.insertOneWithReturn(newDoc);
    }

    public static String editSchool(ObjectId schoolId, JSONObject data) {

        Document school = schoolRepository.findById(schoolId);
        if (school == null)
            return JSON_NOT_VALID_ID;

        if (data.has("cityId")) {

            String id = data.getString("cityId");
            if (!ObjectId.isValid(id))
                return JSON_NOT_VALID_PARAMS;

            Document city = cityRepository.findById(new ObjectId(id));
            if (city == null)
                return JSON_NOT_VALID_ID;

            school.put("city_id", city.getObjectId("_id"));
            school.put("city_name", city.getString("name"));
        }

        for (String key : data.keySet()) {

            if (key.equals("city"))
                continue;

            school.put(
                    Utility.camel(key, false),
                    data.get(key)
            );
        }

        schoolRepository.replaceOne(schoolId, school);
        return JSON_OK;
    }

    public static String fetchSchoolsDigest(Boolean justUnsets) {

        ArrayList<Document> docs = schoolRepository.find(
                justUnsets == null || !justUnsets ? null :
                        exists("user_id", false),
                new BasicDBObject("_id", 1).append("name", 1)
                        .append("city_name", 1).append("kind", 1)
                        .append("grade", 1)
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            JSONObject jsonObject = new JSONObject().
                    put("id", doc.getObjectId("_id").toString());

            String name = doc.getString("name") + " " + doc.getString("city_name") + " - مقطع ";
            String grade = doc.getString("grade");
            String kind = doc.getString("kind");

            if (grade.equals(GradeSchool.DABESTAN.getName()))
                name += "دبستان";
            else if (grade.equals(GradeSchool.MOTEVASETEAVAL.getName()))
                name += "متوسطه اول";
            else
                name += "متوسطه دوم";

            name += " - ";

            if (kind.equals(KindSchool.SAMPAD.getName()))
                name += "سمپاد";
            else if (kind.equals(KindSchool.GHEYR.getName()))
                name += "غیرانتفاعی";
            else if (kind.equals(KindSchool.DOLATI.getName()))
                name += "دولتی";
            else if (kind.equals(KindSchool.HEYAT.getName()))
                name += "هیت امنایی";
            else if (kind.equals(KindSchool.SHAHED.getName()))
                name += "شاهد";
            else if (kind.equals(KindSchool.NEMONE.getName()))
                name += "نمونه";

            jsonObject.put("name", name);
            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String fetchSchools(String grade, String kind, ObjectId cityId, ObjectId stateId) {

        ArrayList<Bson> filter = new ArrayList<>();

        if (grade != null)
            filter.add(eq("grade", grade));

        if (kind != null)
            filter.add(eq("kind", kind));

        if (cityId != null)
            filter.add(eq("city_id", cityId));

        ArrayList<Document> docs = schoolRepository.find(
                filter.size() == 0 ? null : and(filter),
                new BasicDBObject("_id", 1).append("name", 1)
                        .append("city_name", 1).append("kind", 1)
                        .append("city_id", 1).append("grade", 1)
                        .append("address", 1)
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            Document cityDoc = cityRepository.findById(doc.getObjectId("city_id"));
            if (cityDoc == null)
                continue;

            Document state = stateRepository.findById(cityDoc.getObjectId("state_id"));
            if (state == null)
                continue;

            if (stateId != null && !state.getObjectId("_id").equals(stateId))
                continue;

            grade = doc.getString("grade");
            String gradeStr;
            kind = doc.getString("kind");
            String kindStr;

            JSONObject jsonObject = new JSONObject().
                    put("id", doc.getObjectId("_id").toString())
                    .put("name", doc.getString("name"))
                    .put("city", new JSONObject()
                            .put("name", doc.getString("city_name"))
                            .put("id", doc.getObjectId("city_id").toString())
                    )
                    .put("state", new JSONObject()
                            .put("name", state.getString("name"))
                            .put("id", state.getObjectId("_id").toString())
                    )
                    .put("grade", grade)
                    .put("kind", kind)
                    .put("address", doc.getOrDefault("address", ""));

            if (grade.equals(GradeSchool.DABESTAN.getName()))
                gradeStr = "دبستان";
            else if (grade.equals(GradeSchool.MOTEVASETEAVAL.getName()))
                gradeStr = "متوسطه اول";
            else
                gradeStr = "متوسطه دوم";

            if (kind.equals(KindSchool.SAMPAD.getName()))
                kindStr = "سمپاد";
            else if (kind.equals(KindSchool.GHEYR.getName()))
                kindStr = "غیرانتفاعی";
            else if (kind.equals(KindSchool.DOLATI.getName()))
                kindStr = "دولتی";
            else if (kind.equals(KindSchool.HEYAT.getName()))
                kindStr = "هیت امنایی";
            else if (kind.equals(KindSchool.SHAHED.getName()))
                kindStr = "شاهد";
            else
                kindStr = "نمونه";

            jsonObject.put("kindStr", kindStr);
            jsonObject.put("gradeStr", gradeStr);

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }


    public static String updateInfo(JSONObject jsonObject, Document user) {

        String NID = jsonObject.getString("NID");
        if (!Utility.validationNationalCode(NID))
            return generateErr("کد ملی وارد شده معتبر نمی باشد.");

        String sex = jsonObject.getString("sex");
        if (!EnumValidatorImp.isValid(sex, Sex.class))
            return JSON_NOT_VALID_PARAMS;

        if (userRepository.exist(and(
                eq("NID", NID),
                ne("_id", user.getObjectId("_id"))
        )))
            return generateErr("کد ملی وارد شده در سامانه موجود است.");

        JSONArray branches = jsonObject.getJSONArray("branches");
        List<Document> branchesDoc = new ArrayList<>();

        for (int i = 0; i < branches.length(); i++) {
            if (!ObjectId.isValid(branches.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            Document branch = branchRepository.findById(new ObjectId(branches.getString(i)));
            if (branch == null)
                return JSON_NOT_VALID_PARAMS;

            branchesDoc.add(
                    new Document("_id", branch.getObjectId("_id"))
                            .append("name", branch.getString("name"))
            );
        }

        Document city = cityRepository.findById(
                new ObjectId(jsonObject.getString("cityId"))
        );

        if (city == null)
            return JSON_NOT_VALID_PARAMS;

        Document grade = gradeRepository.findById(
                new ObjectId(jsonObject.getString("gradeId"))
        );

        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        Document school = schoolRepository.findById(
                new ObjectId(jsonObject.getString("schoolId"))
        );

        if (school == null)
            return JSON_NOT_VALID_PARAMS;

        user.put("first_name", jsonObject.getString("firstName"));
        user.put("last_name", jsonObject.getString("lastName"));
        user.put("grade", new Document("_id", grade.getObjectId("_id"))
                .append("name", grade.getString("name"))
        );
        user.put("city", new Document("_id", city.getObjectId("_id"))
                .append("name", city.getString("name"))
        );
        user.put("school", new Document("_id", school.getObjectId("_id"))
                .append("name", school.getString("name"))
        );
        user.put("NID", NID);
        user.put("sex", sex);
        user.put("branches", branchesDoc);

        userRepository.replaceOne(
                user.getObjectId("_id"),
                user
        );

        return JSON_OK;
    }
}
