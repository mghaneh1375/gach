package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class ManageUserController {

    // todo: review teacher scenario access
    public static String fetchUser(Document user, String unique, boolean isAdmin) {

        if (user == null) {
            user = userRepository.findByUnique(unique, true);
            if (user == null)
                return generateSuccessMsg("user", "");
        }

        if (!isAdmin && !Authorization.isPureStudent(user.getList("accesses", String.class)))
            return JSON_NOT_ACCESS;

        JSONObject userJson = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("pic", user.containsKey("pic") ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("nameFa", user.getString("name_fa"))
                .put("lastNameFa", user.getString("last_name_fa"))
                .put("address", user.containsKey("address") && isAdmin ? user.getString("address") : "")
                .put("preTel", user.containsKey("pre_tel") && isAdmin ? user.getString("pre_tel") : "")
                .put("tel", user.containsKey("tel") && isAdmin ? user.getString("tel") : "")
                .put("access", user.getList("accesses", String.class))
                .put("city", user.containsKey("city") && isAdmin ? user.getString("city") : "")
                .put("NID", user.getString("NID"))
                .put("birthDay", user.containsKey("birth_day") ?
                        user.getString("birth_day") : "")
                .put("sex", user.getString("sex"))
                .put("mail", user.getString("mail"))
                ;

        return generateSuccessMsg("user", userJson);
    }

    public static String fetchTinyUser(String level, String name,
                                       String lastname, String phone,
                                       String mail, String NID
    ) {

        ArrayList<Bson> filters = new ArrayList<>();

        if(level != null) {
            if(!EnumValidatorImp.isValid(level, Access.class))
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("accesses", level));
        }

        if(NID != null)
            filters.add(eq("NID", NID));

        if(phone != null)
            filters.add(eq("phone", phone));

        if(mail != null)
            filters.add(eq("mail", mail));

        if (name!= null)
            filters.add(regex("first_name", Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE)));

        if (lastname != null)
            filters.add(regex("last_name", Pattern.compile(Pattern.quote(lastname), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> docs = userRepository.find(
                filters.size() == 0 ? null : and(filters), USER_MANAGEMENT_INFO_DIGEST
        );

        try {
//            if (cursor == null || !cursor.iterator().hasNext())
//                return Utility.generateSuccessMsg("user", "");

            JSONArray jsonArray = new JSONArray();

            for (Document user : docs) {

                JSONObject jsonObject = new JSONObject()
                        .put("id", user.getObjectId("_id").toString())
                        .put("name", user.getString("first_name") + user.getString("last_name"))
                        .put("mail", user.getOrDefault("mail", ""))
                        .put("phone", user.getOrDefault("phone", ""))
                        .put("NID", user.getString("NID"))
                        .put("coin", user.get("coin"))
                        .put("status", user.getString("status"))
                        .put("statusFa", user.getString("status").equals("active") ? "فعال" : "غیرفعال")
                        .put("accesses", user.getList("accesses", String.class))
                        .put("school", user.containsKey("school") ?
                                ((Document)user.get("school")).getString("name") : ""
                        );

                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("users", jsonArray);
        }
        catch (Exception x) {
            return generateSuccessMsg("user", "");
        }
    }

    public static String setCoins(ObjectId userId, int newCoins) {

        Document user = userRepository.findOneAndUpdate(
                eq("_id", userId),
                set("coin", newCoins)
        );

        if (user == null)
            return JSON_NOT_VALID_ID;

        user.put("coin", newCoins);
        return JSON_OK;
    }

    public static String addAccess(ObjectId userId, String newAccess) {

        Document user = userRepository.findById(userId);

        if (user == null)
            return JSON_NOT_VALID_ID;

        boolean change = false;

        if (!newAccess.equals(Access.STUDENT.getName()) && !user.getBoolean("level")) {
            user.put("level", true);
            user.put("accesses", new ArrayList<>());
            change = true;
        }
        else if (newAccess.equals(Access.STUDENT.getName()) && user.getBoolean("level")) {
            user.put("level", false);
            user.put("accesses", new ArrayList<>() {
                {add(Access.STUDENT.getName());}
            });
            change = true;
        }

        if (!newAccess.equals(Access.STUDENT.getName())) {

            List<String> accesses;
            if (user.containsKey("accesses"))
                accesses = user.getList("accesses", String.class);
            else
                accesses = new ArrayList<>();

            if (!accesses.contains(newAccess)) {
                accesses.add(newAccess);
                user.put("accesses", accesses);
                change = true;
            }
        }

        if (change)
            userRepository.replaceOne(userId, user);

        return generateSuccessMsg("accesses", user.getList("accesses", String.class));
    }

    public static String removeAccess(ObjectId userId, String role) {

        Document user = userRepository.findById(userId);

        if (user == null || !user.getBoolean("level"))
            return JSON_NOT_VALID_ID;

        List<String> accesses = user.getList("accesses", String.class);

        if (!accesses.contains(role))
            return JSON_NOT_VALID_PARAMS;

        accesses.remove(role);
        if (accesses.size() == 0) {
            user.put("accesses", new ArrayList<>() {
                {add(Access.STUDENT.getName());}
            });
        } else
            user.put("accesses", accesses);

        if(accesses.contains(Access.STUDENT.getName()))
            user.put("level", false);

        userRepository.replaceOne(userId, user);
        return generateSuccessMsg("accesses", user.getList("accesses", String.class));
    }

    public static String fetchUserLike(String nameEn, String lastNameEn,
                                       String nameFa, String lastNameFa) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if (nameEn != null)
            constraints.add(regex("name_en", Pattern.compile(Pattern.quote(nameEn), Pattern.CASE_INSENSITIVE)));

        if (nameFa != null)
            constraints.add(regex("name_fa", Pattern.compile(Pattern.quote(nameFa), Pattern.CASE_INSENSITIVE)));

        if (lastNameFa != null)
            constraints.add(regex("last_name_fa", Pattern.compile(Pattern.quote(lastNameFa), Pattern.CASE_INSENSITIVE)));

        if (lastNameEn != null)
            constraints.add(regex("last_name_en", Pattern.compile(Pattern.quote(lastNameEn), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> users = userRepository.find(and(constraints), USER_DIGEST);
        JSONArray jsonArray = new JSONArray();

        for (Document user : users)
            jsonArray.put(UserRepository.convertUserDigestDocumentToJSON(user));

        return generateSuccessMsg("users", jsonArray);
    }

    public static String setAdvisorPercent(ObjectId advisorId, int percent) {

        Document advisor = userRepository.findById(advisorId);

        if (advisor == null || !advisor.containsKey("accesses") ||
                !advisor.getList("accesses", String.class).contains(Access.ADVISOR.getName()))
            return JSON_NOT_VALID_ID;

        advisor.put("percent", percent);
        userRepository.updateOne(advisorId, set("percent", percent));

        return JSON_OK;
    }

    public static String checkDuplicate(String phone, String NID) {
        return generateSuccessMsg("exist",
                userRepository.exist(or(
                        eq("phone", phone),
                        eq("NID", NID)
                ))
        );
    }

    public static String getMySchools(ObjectId agentId) {

        ArrayList<Document> docs = userRepository.find(and(
                exists("form_list"),
                eq("form_list.role", "school"),
                eq("form_list.agent_id", agentId)
        ), new BasicDBObject("form_list", 1)
                .append("NID", 1)
                .append("phone", 1)
                .append("_id", 1)
                .append("first_name", 1)
                .append("last_name", 1)
                .append("students_no", 1)
        );

        JSONArray jsonArray = new JSONArray();
        for(Document doc : docs) {

            List<Document> formList = doc.getList("form_list", Document.class);
            Document form = searchInDocumentsKeyVal(
                    formList, "role", "school"
            );

            if(form == null)
                continue;

            jsonArray.put(convertSchoolToJSON(doc, form));
        }

        return generateSuccessMsg("data", jsonArray);
    }
//
//    public static String getMySchoolInfo(ObjectId agentId) {
//
//    }

    private static JSONObject convertSchoolToJSON(Document doc, Document form) {
        return new JSONObject()
                .put("phone", doc.getString("phone"))
                .put("NID", doc.getString("NID"))
                .put("id", doc.getObjectId("_id").toString())
                .put("studentsNo", doc.getOrDefault("students_no", 0))
                .put("firstName", doc.getString("first_name"))
                .put("lastName", doc.getString("last_name"))
                .put("name", form.getString("name"))
                .put("schoolSex", form.getString("school_sex"))
                .put("kindSchool", form.getString("kind_school"))
                .put("kindSchoolFa",
                        form.getString("kind_school").equalsIgnoreCase(GradeSchool.DABESTAN.getName()) ?
                                "دبستان" :
                                form.getString("kind_school").equalsIgnoreCase(GradeSchool.MOTEVASETEAVAL.getName()) ?
                                        "متوسطه اول" : "متوسطه دوم"
                )
                .put("schoolSexFa", form.getString("school_sex").equalsIgnoreCase(Sex.MALE.getName()) ? "آقا" : "خانم")
                .put("ManagerName", form.getString("manager_name"));

    }

    public static String addSchool(JSONObject jsonObject,
                                   ObjectId agentId,
                                   String agentName) {

        String phone = jsonObject.getString("phone");
        String NID = jsonObject.getString("NID");

        if(!PhoneValidator.isValid(phone) ||
            !Utility.validationNationalCode(NID)
        )
            return generateErr("کد ملی و یا شماره همراه وارد شده نامعتبر است.");

        ArrayList<Document> users = userRepository.find(
                or(
                        eq("phone", phone),
                        eq("NID", NID)
                ), null
        );

        if(users.size() > 1)
            return generateErr("کد ملی و یا شماره همراه وارد شده نامعتبر است.");

        Document user = users.size() == 1 ? users.get(0) : null;

        long curr = System.currentTimeMillis();

        if(user == null) {

            if(!jsonObject.has("firstName") ||
                    !jsonObject.has("lastName") ||
                    !jsonObject.has("password") ||
                    !jsonObject.has("rPassword") ||
                    !jsonObject.has("city"))
                return JSON_NOT_VALID_PARAMS;

            Document config = Utility.getConfig();
            Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

            user = new Document("NID", NID)
                    .append("phone", phone)
                    .append("status", "active")
                    .append("level", true)
                    .append("money", config.getInteger("init_money"))
                    .append("coin", config.getDouble("init_coin"))
                    .append("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                    .append("events", new ArrayList<>())
                    .append("avatar_id", avatar.getObjectId("_id"))
                    .append("pic", avatar.getString("file"))
                    .append("invitation_code", Utility.randomString(8))
                    .append("created_at", curr)
                    .append("accesses", new ArrayList<>() {
                        {add(Access.SCHOOL.getName());}
                    })
                    .append("password", jsonObject.getString("password"))
                    .append("first_name", jsonObject.getString("firstName"))
                    .append("last_name", jsonObject.getString("lastName"));

            ArrayList<Document> forms = new ArrayList<>();

            Document form = new Document("role", "school")
                    .append("tel", jsonObject.getString("tel"))
                    .append("address", jsonObject.getString("address"))
                    .append("name", jsonObject.getString("name"))
                    .append("manager_name", jsonObject.getString("managerName"))
                    .append("school_sex", jsonObject.getString("schoolSex"))
                    .append("agent_id", agentId)
                    .append("kind_school", jsonObject.getString("kindSchool"));

            forms.add(form);
            user.append("form_list", forms);

            ObjectId oId = userRepository.insertOneWithReturnId(user);
            return generateSuccessMsg("id", oId.toString(),
                    new PairValue("school", convertSchoolToJSON(
                            user, form
                    ))
            );
        }

        ObjectId reqId = accessRequestRepository.insertOneWithReturnId(
                new Document("sender_id", agentId)
                        .append("target_id", user.getObjectId("_id"))
                        .append("phone", phone)
                        .append("role", "school")
                        .append("created_at", curr)
                        .append("tel", jsonObject.getString("tel"))
                        .append("address", jsonObject.getString("address"))
                        .append("name", jsonObject.getString("name"))
                        .append("manager_name", jsonObject.getString("managerName"))
                        .append("school_sex", jsonObject.getString("schoolSex"))
                        .append("kind_school", jsonObject.getString("kindSchool"))
        );

        ArrayList<Document> chats = new ArrayList<>();
        chats.add(new Document("msg",
                "<p>" + agentName + " از شما دعوت کرده است تا زیرمجموعه نمایندگی ایشان قرار گیرید. در صورت تمایل بر روی " +
                "<a href='" + SERVER + "acceptInvite/" + reqId + "'>" + "لینک" + "</a>" +
                        " کلیک کنید. " + "</p>"
                )
                .append("created_at", curr)
                .append("is_for_user", true)
                .append("files", new ArrayList<>())
        );

        ticketRepository.insertOne(
                new Document("title", "درخواست اتصال به نمایندگی")
                        .append("created_at", curr)
                        .append("send_date", curr)
                        .append("answer_date", curr)
                        .append("is_for_teacher", false)
                        .append("chats", chats)
                        .append("status", "finish")
                        .append("priority", TicketPriority.HIGH.getName())
                        .append("section", TicketSection.ACCESS.getName())
                        .append("user_id", user.getObjectId("_id"))
        );

        return generateSuccessMsg("id", -1);
    }

    public static String acceptInvite(Document user,
                                      ObjectId reqId) {

        Document doc = accessRequestRepository.findOne(
                and(
                        eq("_id", reqId),
                        eq("target_id", user.getObjectId("_id"))
                ), null
        );

        if(doc == null)
            return JSON_NOT_VALID_PARAMS;

        if(doc.containsKey("response_at"))
            return generateErr("شما قبلا این دعوت را پذیرفته اید.");

        doc.put("response_at", System.currentTimeMillis());

        if(!user.containsKey("phone"))
            user.put("phone", doc.getString("phone"));

        List<Document> forms = user.containsKey("form_list") ?
                user.getList("form_list", Document.class) :
                new ArrayList<>();

        Document form = new Document("role", doc.getString("role"));

        if(doc.getString("role").equalsIgnoreCase("school")) {
            form = new Document("role", "school")
                    .append("tel", doc.getString("tel"))
                    .append("address", doc.getString("address"))
                    .append("name", doc.getString("name"))
                    .append("manager_name", doc.getString("manager_name"))
                    .append("school_sex", doc.getString("school_sex"))
                    .append("agent_id", doc.getObjectId("sender_id"))
                    .append("kind_school", doc.getString("kind_school"));
        }

        int idx = searchInDocumentsKeyValIdx(forms, "role", doc.getString("role"));
        if(idx == -1)
            forms.add(form);
        else
            forms.set(idx, form);

        user.put("form_list", forms);

        userRepository.replaceOne(user.getObjectId("_id"), user);
        accessRequestRepository.replaceOne(reqId, doc);
        return generateSuccessMsg("msg", "انتقال شما به نمایندگی مورد نظر با موفقیت انجام شد.");
    }
}
