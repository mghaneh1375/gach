package irysc.gachesefid.Controllers;


import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.*;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
//            new FormField(true, "workYear", "سابقه کار", null, true),
            new FormField(true, "workLessons", "درس تخصصی", null, false),
            new FormField(true, "workSchools", "مدارس همکار", null, false)
    };

    private static FormField[] fieldsNeededForStudent = new FormField[]{
            new FormField(true, "invitationCode", "کد معرف", "این فیلد اختیاری است و در صورت نداشتن کد معرف این فیلد را خالی رها کنید.", false),
    };

    private static FormField[] fieldsNeededForAgent = new FormField[]{
            new FormField(true, "name", "نام نمایندگی یا موسسه", null, false),
    };

    private static FormField[] fieldsNeededForTeacher = new FormField[]{
            new FormField(true, "workLessons", "درس تخصصی", null, false),
            new FormField(true, "workSchools", "مدارس همکار", null, false)
    };

    public static String whichKindOfAuthIsAvailable(String NID) {

        Document user = userRepository.findBySecKey(NID);
        if (user == null || !user.getString("status").equals("active"))
            return generateSuccessMsg("via", "none");

        if (user.containsKey("phone") && user.containsKey("mail"))
            return generateSuccessMsg("via", "both");

        if (user.containsKey("phone"))
            return generateSuccessMsg("via", "sms");

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

        if (userRepository.exist(
                eq("NID", jsonObject.getString("NID"))
        ))
            return Utility.generateErr("کد ملی وارد شده در سامانه موجود است.");

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

    public static String resend(JSONObject jsonObject) {

        Document doc = activationRepository.findOne(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.get("username").toString()))
                , null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        long createdAt = doc.getLong("created_at");

        if (System.currentTimeMillis() - createdAt < SMS_RESEND_MSEC)
            return Utility.generateErr("کد قبلی هنوز منقضی نشده است.");

        String username = doc.getString("username");

        if(!doc.containsKey("NID")) {

            if(doc.containsKey("phone_or_mail"))
                username = doc.getString("phone_or_mail");
            else {
                Document user = userRepository.findBySecKey(jsonObject.getString("username"));
                if (user == null)
                    return JSON_NOT_ACCESS;

                if (doc.getString("auth_via").equals(AuthVia.SMS.getName()))
                    username = user.getString("phone");
                else
                    username = user.getString("mail");
            }
        }

        int code = Utility.randInt();

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName()))
            Utility.sendSMS(username, code + "", "", "", "activationCode");
        else
            Utility.sendMail(
                    username, code + "","signUp", doc.getString("first_name") + " " + doc.getString("last_name")
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
        user.put("money", ((Number)user.get("money")).doubleValue() + config.getInteger("invitation_money"));
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

        FormField[] wantedList;

        if (role.equals(Access.ADVISOR.getName()))
            wantedList = fieldsNeededForAdvisor;
        else if (role.equals(Access.SCHOOL.getName()))
            wantedList = fieldsNeededForSchool;
        else if (role.equals(Access.STUDENT.getName()))
            wantedList = fieldsNeededForStudent;
        else if (role.equals(Access.TEACHER.getName()))
            wantedList = fieldsNeededForTeacher;
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

        if (role.equals(Access.STUDENT.getName())) {

            if(user.containsKey("invitor"))
                return generateErr("شما قبلا معرف خود را انتخاب کرده اید.");

            Document invitor = userRepository.findOne(
                    eq("invitation_code", jsonObject.getString("invitationCode")), new BasicDBObject("_id", 1)
            );

            if(invitor == null)
                return generateErr("کد معرف وارد شده معتبر نمی باشد.");

            invitor = userRepository.findById(invitor.getObjectId("_id"));
            Document config = Utility.getConfig();

            if(config.containsKey("invite_coin")) {
                invitor.put("coin",
                        config.getDouble("invite_coin") +
                                invitor.getDouble("coin")
                );
                user.put("coin",
                        config.getDouble("invite_coin") +
                                user.getDouble("coin")
                );
            }

            if(config.containsKey("invite_money")) {
                invitor.put("money",
                        config.getInteger("invite_money") +
                                ((Number)invitor.get("money")).doubleValue()
                );
                user.put("money",
                        config.getInteger("invite_money") +
                                ((Number)user.get("money")).doubleValue()
                );
            }

            user.put("invitor", invitor.getObjectId("_id"));

            userRepository.replaceOne(
                    invitor.getObjectId("_id"), invitor
            );

            userRepository.replaceOne(
                    user.getObjectId("_id"), user
            );

            return JSON_OK;
        }

        Document form = new Document("role", role);

        for (String key : keys)
            form.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    jsonObject.get(key)
            );

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
        avatar.put("used", (int)avatar.getOrDefault("used", 0) + 1);
        avatarRepository.replaceOne(config.getObjectId("default_avatar"), avatar);

        Document newDoc = new Document("status", "active")
                .append("level", false)
                .append("first_name", doc.getString("first_name"))
                .append("last_name", doc.getString("last_name"))
                .append("NID", doc.getString("NID"))
                .append("money", (double)config.getInteger("init_money"))
                .append("coin", config.getDouble("init_coin"))
                .append("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                .append("events", new ArrayList<>())
                .append("avatar_id", avatar.getObjectId("_id"))
                .append("pic", avatar.getString("file"))
                .append("invitation_code", Utility.simpleRandomString(5))
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
        if(doc.getString("auth_via").equals(AuthVia.MAIL.getName()))
            sendMail(
                    username, "", "successSignUp",
                    doc.getString("first_name") + " " + doc.getString("last_name")
            );

        STUDENTS++;

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
                .put("money", user.get("money"))
                .put("coin", user.get("coin"))
                .put("pic", (user.containsKey("pic")) ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("firstName", user.getString("first_name"))
                .put("invitationCode", user.get("invitation_code"))
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
                .put("bio", user.getOrDefault("bio", ""))
                .put("acceptStd", user.getOrDefault("accept_std", true))
                .put("lastName", user.getString("last_name"))
                .put("mail", user.getOrDefault("mail", ""))
                .put("sex", user.getOrDefault("sex", ""))
                .put("phone", user.getOrDefault("phone", ""));

//        jsonObject.put("birthDay", user.containsKey("birth_day") ? getSolarJustDate(user.getLong("birth_day")) : "");
        jsonObject.put("birthDay", user.getOrDefault("birth_day", ""));

        if(user.containsKey("block_notif"))
            jsonObject.put("blockNotif", true);

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

                    if (field.pairValues != null) {
                        JSONArray jsonArray1 = new JSONArray();
                        for (PairValue p : field.pairValues)
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
        fillWithFormFields(fieldsNeededForStudent, formsJSON, Access.STUDENT.getName());
        fillWithFormFields(fieldsNeededForAdvisor, formsJSON, Access.ADVISOR.getName());
        fillWithFormFields(fieldsNeededForAgent, formsJSON, Access.AGENT.getName());
        fillWithFormFields(fieldsNeededForTeacher, formsJSON, Access.TEACHER.getName());
        fillWithFormFields(fieldsNeededForSchool, formsJSON, Access.SCHOOL.getName());

        return generateSuccessMsg("data", formsJSON);
    }

    public static JSONObject isAuth(Document user) {

        boolean isComplete = user.containsKey("school") &&
                user.containsKey("grade") && user.containsKey("city");

        JSONObject jsonObject = new JSONObject().put("isComplete", isComplete);

        jsonObject.put("user", convertUser(user));
        if (user.containsKey("accesses"))
            return jsonObject.put("accesses", user.getList("accesses", String.class));

        return jsonObject.put("accesses", "student");
    }

    public static String forgetPass(JSONObject jsonObject) {

        String NID = jsonObject.getString("NID");
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

        return sendSMSOrMail(
                NID,
                via.equals(AuthVia.SMS.getName()) ? user.getString("phone") : user.getString("mail"),
                via, false
        );
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

    public static String updateUsername(String NID, JSONObject data) {

        convertPersian(data);
        String via = data.getString("mode");

        if (!EnumValidatorImp.isValid(via, AuthVia.class))
            return JSON_NOT_VALID_PARAMS;

        String phoneOrMail = data.getString("username").toLowerCase();

        if (via.equals(AuthVia.SMS.getName()) && !PhoneValidator.isValid(phoneOrMail))
            return generateErr("شماره همراه وارد شده معتبر نمی باشد.");

        if (via.equals(AuthVia.MAIL.getName()) && !Utility.isValidMail(phoneOrMail))
            return generateErr("ایمیل وارد شده معتبر نمی باشد.");

        Bson filter = via.equals(AuthVia.SMS.getName()) ?
                eq("phone", phoneOrMail) : eq("mail", phoneOrMail);

        if (userRepository.exist(filter))
            return generateErr("ایمیل/شماره همراه وارد شده در سیستم موجود است.");

        return sendSMSOrMail(NID, phoneOrMail, via, true);
    }

    private static String sendSMSOrMail(
            String NID, String phoneOrMail,
            String via, boolean savePhoneOrMail
    ) {

        PairValue existTokenP = UserRepository.existSMS(NID);

        if (existTokenP != null)
            return generateSuccessMsg("token", existTokenP.getKey(),
                    new PairValue("reminder", existTokenP.getValue())
            );

        String token = UserRepository.sendNewSMS(NID, phoneOrMail, via, savePhoneOrMail);

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

        if (user.containsKey("pic") && user.getString("pic") != null)
            new Thread(() -> FileUtils.removeFile(user.getString("pic"), UserRepository.FOLDER)).start();

        String filename = FileUtils.uploadFile(file, UserRepository.FOLDER);

        if (filename != null) {

            user.put("pic", filename);

            if (user.containsKey("avatar_id")) {
                Document oldAvatar = avatarRepository.findById(user.getObjectId("avatar_id"));
                if(oldAvatar != null) {
                    oldAvatar.put("used", oldAvatar.getInteger("used") - 1);
                    avatarRepository.updateOne(oldAvatar.getObjectId("_id"), set("used", oldAvatar.getInteger("used")));
                }
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
            if (oldAvatar != null) {
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
                name += "هیئت امنایی";
            else if (kind.equals(KindSchool.SHAHED.getName()))
                name += "شاهد";
            else if (kind.equals(KindSchool.NEMONE.getName()))
                name += "نمونه";

            jsonObject.put("name", name);
            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String fetchSchools(String grade, String kind,
                                      ObjectId cityId, ObjectId stateId, Boolean hasUser,
                                      boolean isAdmin
    ) {

        ArrayList<Bson> filter = new ArrayList<>();

        if (grade != null)
            filter.add(eq("grade", grade));

        if (grade != null)
            filter.add(eq("grade", grade));

        if (kind != null)
            filter.add(eq("kind", kind));

        if (cityId != null)
            filter.add(eq("city_id", cityId));

        if (hasUser != null && isAdmin)
            filter.add(exists("user_id", hasUser));

        ArrayList<Document> docs = schoolRepository.find(
                filter.size() == 0 ? null : and(filter),
                new BasicDBObject("_id", 1).append("name", 1)
                        .append("city_name", 1).append("kind", 1)
                        .append("city_id", 1).append("grade", 1)
                        .append("address", 1).append("user_id", 1)
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

            if(isAdmin && doc.containsKey("user_id")) {

                Document user = userRepository.findById(doc.getObjectId("user_id"));

                jsonObject.put("manager",
                        user.getString("first_name") + " " +
                        user.getString("last_name")
                );
                jsonObject.put("managerPhone", user.getOrDefault("phone", ""));
            }
            else if(isAdmin)
                jsonObject.put("manager", "").put("managerPhone", "");

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
                kindStr = "هیئت امنایی";
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

    public static String setAboutMe(Document user, String bio) {

        if(bio.length() > 150)
            return generateErr("متن درباره من می تواند حداکثر ۱۵۰ کاراکتر باشد");

        user.put("bio", bio);
        userRepository.replaceOne(user.getObjectId("_id"), user);

        return JSON_OK;
    }

    public static String getMySummary(Document user) {

        long curr = System.currentTimeMillis();

        Document rank = tarazRepository.findBySecKey(user.getObjectId("_id"));
        Document config = getConfig();

        double exchangeRate = ((Number)config.get("coin_rate_coef")).doubleValue();
        DecimalFormat decfor = new DecimalFormat("0.000");

        double a = (10000.0 / exchangeRate);
        String roundVal = decfor.format(a);

        JSONObject jsonObject = new JSONObject()
                .put("money", user.get("money"))
                .put("coinToMoneyExchange", exchangeRate)
//                .put("moneyToCoinExchange", config.get("money_rate_coef"))
                .put("moneyToCoinExchange", roundVal)
                .put("rank", rank == null ? "" : rank.getInteger("rank"))
                .put("branchRank", 1)
                .put("gradeRank", rank == null || !rank.containsKey("grade_rank") ? "" : rank.getInteger("grade_rank"))
                .put("registrableQuizzes", iryscQuizRepository.count(
                        and(
                                lt("start_registry", curr),
                                or(
                                        and(
                                                exists("end_registry", false),
                                                gt("end", curr)
                                        ),
                                        and(
                                                exists("end_registry", true),
                                                gt("end_registry", curr)
                                        )
                                )
                        )
                ) + openQuizRepository.count(
                        nin("students._id", user.getObjectId("_id"))
                ))
                .put("activeQuizzes", iryscQuizRepository.count(
                        and(
                            in("students._id", user.getObjectId("_id")),
                            gt("start", curr)
                        )
                ))
                .put("passedQuizzes", iryscQuizRepository.count(
                        and(
                                in("students._id", user.getObjectId("_id")),
                                lt("end", curr)
                        )
                ))
                .put("coin", user.get("coin"));

        int totalQuizzes = iryscQuizRepository.count(
                in("students._id", user.getObjectId("_id"))
        );

        jsonObject.put("totalQuizzes", totalQuizzes);

        return generateSuccessMsg("data", jsonObject);
    }

    public static String getSiteSummary() {
        return generateSuccessMsg("data", new JSONObject()
                .put("schools", SCHOOLS)
                .put("students", STUDENTS)
                .put("questions", QUESTIONS)
        );
    }

    public static String blockNotif(Document user) {

        if(user.containsKey("block_notif"))
            user.remove("block_notif");
        else
            user.put("block_notif", true);

        userRepository.replaceOne(user.getObjectId("_id"), user);
        return JSON_OK;
    }

    public static String updateInfo(JSONObject jsonObject, Document user) {

        String NID = jsonObject.getString("NID");
        if (!Utility.validationNationalCode(NID))
            return generateErr("کد ملی وارد شده معتبر نمی باشد.");

        if(jsonObject.has("birthDay")) {
            long age = (System.currentTimeMillis() - jsonObject.getLong("birthDay")) / (ONE_DAY_MIL_SEC * 365);
            if(age <= 5)
                return generateErr("تاریخ تولد وارد شده معتبر نمی باشد");
        }

        String sex = jsonObject.getString("sex");
        boolean dontCheckNID = user.containsKey("NID") && user.getString("NID").equals(NID);

        if (!dontCheckNID && userRepository.exist(and(
                eq("NID", NID),
                ne("_id", user.getObjectId("_id"))
        )))
            return generateErr("کد ملی وارد شده در سامانه موجود است.");

        List<Document> branchesDoc = null;

        if(jsonObject.has("branches")) {

            JSONArray branches = jsonObject.getJSONArray("branches");
            branchesDoc = new ArrayList<>();

            for (int i = 0; i < branches.length(); i++) {
                if (!ObjectId.isValid(branches.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

//                Document branch = branchRepository.findById(new ObjectId(branches.getString(i)));
                Document branch = gradeRepository.findById(new ObjectId(branches.getString(i)));
                if (branch == null)
                    return JSON_NOT_VALID_PARAMS;

                branchesDoc.add(
                        new Document("_id", branch.getObjectId("_id"))
                                .append("name", branch.getString("name"))
                );
            }
        }

        Document city = cityRepository.findById(
                new ObjectId(jsonObject.getString("cityId"))
        );

        if (city == null)
            return JSON_NOT_VALID_PARAMS;

        Document grade = null;

        if(jsonObject.has("gradeId")) {
//            grade = gradeRepository.findById(
//                    new ObjectId(jsonObject.getString("gradeId"))
//            );
            grade = branchRepository.findById(
                    new ObjectId(jsonObject.getString("gradeId"))
            );

            if (grade == null)
                return JSON_NOT_VALID_PARAMS;
        }

        Document school = null;

        if(jsonObject.has("schoolId") &&
                (
                        !user.containsKey("school") ||
                        !user.get("school", Document.class).getObjectId("_id").toString().equals(jsonObject.getString("schoolId"))
                )
        ) {
           school = schoolRepository.findById(
                    new ObjectId(jsonObject.getString("schoolId"))
            );

            if (school == null)
                return JSON_NOT_VALID_PARAMS;
        }

        user.put("first_name", jsonObject.getString("firstName"));
        user.put("last_name", jsonObject.getString("lastName"));

        if(grade != null)
            user.put("grade", new Document("_id", grade.getObjectId("_id"))
                    .append("name", grade.getString("name"))
            );
        else

            user.remove("grade");

        user.put("city", new Document("_id", city.getObjectId("_id"))
                .append("name", city.getString("name"))
        );

        if(school != null) {

            user.put("school", new Document("_id", school.getObjectId("_id"))
                    .append("name", school.getString("name"))
            );

            if(school.containsKey("user_id")) {

                Document schoolUser = userRepository.findById(school.getObjectId("user_id"));

                if(schoolUser != null) {

                    List<ObjectId> students = (List<ObjectId>) schoolUser.getOrDefault("students", new ArrayList<>());

                    if(!students.contains(user.getObjectId("_id"))) {
                        students.add(user.getObjectId("_id"));
                        userRepository.replaceOne(schoolUser.getObjectId("_id"), schoolUser);
                    }

                }

            }

        }

        else
            user.remove("school");

        user.put("NID", NID);
        user.put("sex", sex);

        if(branchesDoc != null)
            user.put("branches", branchesDoc);
        else
            user.remove("branches");

        if(jsonObject.has("birthDay")) {
            user.put("birth_day", jsonObject.getLong("birthDay"));
        }

        userRepository.replaceOne(
                user.getObjectId("_id"),
                user
        );

        return JSON_OK;
    }

    public static String getRankingList(ObjectId gradeId) {

        ArrayList<Bson> filters = new ArrayList<>();

        filters.add(
                and(
                        ne("user_id", new ObjectId("632bf8f3bd5b8c48dae0a12e")),
                        ne("user_id", new ObjectId("6354d7c7ec057f4ff5cdc88f")),
                        ne("user_id", new ObjectId("63550a10ec057f4ff5cdc8a2")),
                        ne("user_id", new ObjectId("6337334dd975897a8007aa5a")),
                        ne("user_id", new ObjectId("632ec8e2bd5b8c48dae0a130")),
                        ne("user_id", new ObjectId("6332c72257aa3143056e6767")),
                        ne("user_id", new ObjectId("635520c2ec057f4ff5cdc8ad")),
                        ne("user_id", new ObjectId("6342dd6fe282850402604d1c")),
                        ne("user_id", new ObjectId("635e26430c9ba5235788b6f7"))
                )
        );

        if(gradeId != null) {
            filters.add(and(
                    eq("grade_id", gradeId),
                    lt("grade_rank", 50)

            ));
        }
        else
            filters.add(
                    lt("rank", 50)
            );

        ArrayList<Document> docs = tarazRepository.find(and(filters), null,
                gradeId == null ?
                        Sorts.ascending("rank") :
                        Sorts.ascending("grade_rank")
        );

        ArrayList<ObjectId> userIds = new ArrayList<>();
        for(Document doc : docs)
            userIds.add(doc.getObjectId("user_id"));

        ArrayList<Document> users = userRepository.findByIds(userIds, true);
        if(users == null)
            return JSON_NOT_UNKNOWN;

        JSONArray jsonArray = new JSONArray();

        int i = 0;

        int rank = 0;
        int oldSum = -1;
        int skip = 1;

        for(Document user : users) {

            int currSum = docs.get(i).getInteger("cum_sum_last_five");

            if (oldSum != currSum) {
                rank += skip;
                skip = 1;
            } else
                skip++;

            JSONObject jsonObject = new JSONObject()
                    .put("totalQuizzes", docs.get(i).getList("quizzes", Document.class).size())
                    .put("cumSum", currSum);

            user.put("rank", rank);
            oldSum = currSum;

            Utility.fillJSONWithUser(jsonObject, user);
            jsonArray.put(jsonObject);
            i++;
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static ByteArrayInputStream getAuthorCodesExcel() {

        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> docs = authorRepository.find(null, null);

        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("name", doc.getString("name"))
                    .put("code", doc.get("code"))
            );
        }

        return Excel.write(jsonArray);
    }

    private static JSONArray convertQuizzesToJSON(List<Document> quizzes, ObjectId userId) {

        JSONArray jsonArray = new JSONArray();

        for (Document quiz : quizzes) {

            try {

                List<Document> rankingList = quiz.getList("ranking_list", Document.class);

                Document studentDocInQuiz = searchInDocumentsKeyVal(
                        rankingList, "_id", userId
                );

                if (studentDocInQuiz == null || (
                        (boolean)quiz.getOrDefault("pay_by_student", false) && !studentDocInQuiz.containsKey("paid")
                ))
                    continue;

                boolean isTashrihi = quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase("tashrihi");

                double totalQuizMark = 0;
                List<Number> marks = quiz.get("questions", Document.class).getList("marks", Number.class);

                if(isTashrihi) {

                    for (Number mark : marks)
                        totalQuizMark += mark.doubleValue();

                }

                Object[] stats = isTashrihi ? QuizAbstract.decodeFormatGeneralTashrihi(studentDocInQuiz.get("stat", Binary.class).getData()) :
                        QuizAbstract.decodeFormatGeneral(studentDocInQuiz.get("stat", Binary.class).getData());

                JSONObject jsonObject;

                jsonObject = new JSONObject()
                        .put("taraz", stats[0])
                        .put("cityRank", stats[3])
                        .put("stateRank", stats[2])
                        .put("rank", stats[1]);

                if(isTashrihi) {
                    jsonObject.put("mark", stats[4])
                            .put("mode", "tashrihi")
                            .put("totalMark", totalQuizMark);
                }

                jsonObject.put("name", quiz.getString("title"))
                        .put("studentsCount", rankingList.size())
                        .put("questionsCount", marks.size())
                        .put("id", quiz.getObjectId("_id").toString());

                if (quiz.containsKey("start") && quiz.containsKey("end"))
                    jsonObject.put("date", getSolarDate(quiz.getLong("start")) + " تا " + getSolarDate(quiz.getLong("end")));

                else if(quiz.containsKey("students")) {
                    Document tmp = searchInDocumentsKeyVal(quiz.getList("students", Document.class),
                            "_id", userId
                    );

                    if(tmp != null && tmp.containsKey("finish_at") && tmp.get("finish_at") != null)
                        jsonObject.put("date", getSolarDate(tmp.getLong("finish_at")));
                }

                jsonArray.put(jsonObject);
            }
            catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        return jsonArray;
    }

    private static JSONObject convertCustomQuizStats(Document std) {

        JSONObject data = new JSONObject();
        JSONArray lessons = new JSONArray();

        int totalCorrect = 0;
        for (Document lesson : std.getList("lessons", Document.class)) {

            Object[] stats = QuizAbstract.decodeCustomQuiz(lesson.get("stat", Binary.class).getData());
            totalCorrect += (int) stats[2];

            JSONObject jsonObject = new JSONObject()
                    .put("name", lesson.getString("name"))
                    .put("whites", stats[0])
                    .put("corrects", stats[1])
                    .put("incorrects", stats[2])
                    .put("total", (int) stats[0] + (int) stats[1] + (int) stats[2]);

            lessons.put(jsonObject);
        }

        JSONArray subjects = new JSONArray();
        for (Document subject : std.getList("subjects", Document.class)) {

            Object[] stats = QuizAbstract.decodeCustomQuiz(subject.get("stat", Binary.class).getData());

            JSONObject jsonObject = new JSONObject()
                    .put("name", subject.getString("name"))
                    .put("whites", stats[0])
                    .put("corrects", stats[1])
                    .put("incorrects", stats[2])
                    .put("total", (int) stats[0] + (int) stats[1] + (int) stats[2]);

            subjects.put(jsonObject);
        }

        data.put("lessons", lessons);
        data.put("subjects", subjects);
        data.put("date", getSolarDate(std.getLong("start_at")));
        data.put("totalCorrects", totalCorrect);
        data.put("name", std.getString("name"));

        return data;
    }



    public static String getEducationalHistory(ObjectId userId) {

        Document student = userRepository.findById(userId);
        if(student == null)
            return JSON_NOT_UNKNOWN;

        List<Document> iryscQuizzes = iryscQuizRepository.find(and(
                eq("students._id", userId),
                exists("report_status"),
                eq("report_status", "ready")
        ), new BasicDBObject("title", 1).append("_id", 1)
                .append("start", 1).append("end", 1)
                .append("ranking_list", 1).append("mode", 1)
                .append("questions", 1), Sorts.descending("created_at")
        );

        List<Document> openQuizzes = openQuizRepository.find(and(
                        eq("students._id", userId),
                        exists("report_status"),
                        eq("report_status", "ready")
                ), new BasicDBObject("title", 1).append("_id", 1)
                        .append("ranking_list", 1).append("mode", 1)
                        .append("students", 1)
                        .append("questions", 1), Sorts.descending("created_at")
        );

        List<Document> schoolQuizzes = schoolQuizRepository.find(and(
                        eq("students._id", userId),
                        exists("report_status"),
                        eq("report_status", "ready"),
                        exists("pay_by_student", false),
                        eq("status", "finish")
                ), new BasicDBObject("title", 1).append("_id", 1)
                        .append("ranking_list", 1)
                        .append("students", 1)
                        .append("questions", 1), Sorts.descending("created_at")
        );


        List<Document> advisorQuizzes = schoolQuizRepository.find(and(
                        eq("students._id", userId),
                        exists("report_status"),
                        eq("report_status", "ready"),
                        exists("pay_by_student"),
                        or(
                                eq("status", "finish"),
                                eq("status", "semi_finish")
                        )
                ), new BasicDBObject("title", 1).append("_id", 1)
                        .append("students", 1)
                        .append("ranking_list", 1).append("pay_by_student", 1)
                        .append("questions", 1), Sorts.descending("created_at")
        );

        List<Document> customQuizzes = customQuizRepository.find(and(
                        eq("user_id", userId),
                        eq("status", "finished")
                ), new BasicDBObject("name", 1).append("_id", 1)
                        .append("lessons", 1).append("subjects", 1)
                        .append("start_at", 1)
        );

        JSONObject output = new JSONObject();

        output.put("iryscQuizzes", convertQuizzesToJSON(iryscQuizzes, userId));
        output.put("schoolQuizzes", convertQuizzesToJSON(schoolQuizzes, userId));
        output.put("advisorQuizzes", convertQuizzesToJSON(advisorQuizzes, userId));
        output.put("openQuizzes", convertQuizzesToJSON(openQuizzes, userId));

        JSONArray customQuizzesJSON = new JSONArray();
        for (Document customQuiz : customQuizzes) {
            customQuizzesJSON.put(convertCustomQuizStats(customQuiz));
        }

        output.put("customQuizzes", customQuizzesJSON);

        if(student.containsKey("school") && student.get("school") != null)
            output.put("school", student.get("school", Document.class).getString("name"));
        else
            output.put("school", "");

        if(student.containsKey("city")) {
            Document city = (Document) student.get("city");
            if(city != null && city.containsKey("name"))
                output.put("city", city.getString("name"));
            else
                output.put("city", "");
        }
        else
            output.put("city", "");

        if(student.containsKey("grade")) {
            Document grade = (Document) student.get("grade");
            if(grade != null && grade.containsKey("name"))
                output.put("grade", grade.getString("name"));
        }
        else
            output.put("grade", "");

        if(student.containsKey("branches")) {
            List<Document> branches = student.getList("branches", Document.class);
            if(branches.size() > 0) {

                StringBuilder sb = new StringBuilder();

                for (Document branch : branches) {
                    sb.append(branch.getString("name")).append(" - ");
                }

                output.put("branches", sb.substring(0, sb.toString().length() - 3));
            }
            else output.put("branches", "");
        }
        else
            output.put("branches", "");

        if(student.containsKey("rank"))
            output.put("rank", student.get("rank"));
        else {
            Document rank = tarazRepository.findOne(eq("user_id", student.getObjectId("_id")), JUST_RANK);
            output.put("rank", rank == null ? -1 : rank.get("rank"));
        }

        if(student.containsKey("advisor_id")) {
            Document advisor = userRepository.findById(student.getObjectId("advisor_id"));
            if(advisor != null)
                output.put("advisor", irysc.gachesefid.Controllers.Advisor.Utility.convertToJSONDigest(
                        null, advisor
                ));
        }

        output.put("name", student.getString("first_name") + " " + student.getString("last_name"))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + student.getString("pic"));

        return generateSuccessMsg("data", output);
    }
}
