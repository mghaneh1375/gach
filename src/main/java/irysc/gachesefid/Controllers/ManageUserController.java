package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Service.UserService.getEncPassStatic;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class ManageUserController {

    private final static int PAGE_SIZE = 10;

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
                .put("mail", user.getString("mail"));

        return generateSuccessMsg("user", userJson);
    }

    public static String fetchTinyUser(
            String level, String name,
            String lastname, String phone,
            String mail, String NID,
            ObjectId gradeId, ObjectId branchId,
            String additionalLevel, Boolean justSettled,
            Integer pageIndex,
            Long from, Long to
    ) {
        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(exists("remove_at", false));

        if (level != null) {
            if (!EnumValidatorImp.isValid(level, Access.class) &&
                    !level.equalsIgnoreCase("all")
            )
                return JSON_NOT_VALID_PARAMS;

            if (!level.equalsIgnoreCase("all"))
                filters.add(eq("accesses", level));

            if (additionalLevel != null && additionalLevel.equals("teach"))
                filters.add(eq("teach", true));
            else if (additionalLevel != null && additionalLevel.equals("advice"))
                filters.add(eq("advice", true));
        }

        if (NID != null)
            filters.add(eq("NID", NID));

        if (phone != null)
            filters.add(eq("phone", phone));

        if (mail != null)
            filters.add(eq("mail", mail));

        if (name != null)
            filters.add(regex("first_name", Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE)));

        if (lastname != null)
            filters.add(regex("last_name", Pattern.compile(Pattern.quote(lastname), Pattern.CASE_INSENSITIVE)));

        if (gradeId != null)
            filters.add(and(
                    exists("branches"),
                    eq("branches._id", gradeId)
            ));

        if (branchId != null)
            filters.add(and(
                    exists("grade"),
                    eq("grade._id", branchId)
            ));

        if (from != null)
            filters.add(gte("created_at", from));

        if (to != null)
            filters.add(lte("created_at", to));

        List<Document> docs = pageIndex == null ?
                userRepository.find(
                        and(filters), USER_MANAGEMENT_INFO_DIGEST
                ) : level == null || !level.equalsIgnoreCase(Access.ADVISOR.getName()) ?
                userRepository.findLimited(
                        and(filters), USER_MANAGEMENT_INFO_DIGEST,
                        Sorts.ascending("created_at"),
                        (pageIndex - 1) * PAGE_SIZE, PAGE_SIZE
                ) :
                userRepository.findUsersWithSettlementStatus(
                        and(filters),
                        Sorts.ascending("created_at"),
                        (pageIndex - 1) * PAGE_SIZE, PAGE_SIZE,
                        justSettled
                );
        int count = level == null || !level.equalsIgnoreCase(Access.ADVISOR.getName()) ?
                userRepository.count(and(filters)) : userRepository.countUsersWithSettlementStatus(and(filters), justSettled);

        try {
            JSONArray jsonArray = new JSONArray();
            for (Document user : docs) {

                StringBuilder branchBuilder = new StringBuilder();
                String branch = "";
                List<Document> branches = user.containsKey("branches") ?
                        user.getList("branches", Document.class) : null;

                if (branches != null && branches.size() > 0) {

                    for (Document itr : branches)
                        branchBuilder.append(itr.getString("name")).append("-");

                    branch = branchBuilder.substring(0, branchBuilder.length() - 1);
                }

                JSONObject jsonObject = new JSONObject()
                        .put("id", user.getObjectId("_id").toString())
                        .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                        .put("mail", user.getOrDefault("mail", ""))
                        .put("phone", user.getOrDefault("phone", ""))
                        .put("NID", user.getString("NID"))
                        .put("coin", user.get("coin"))
                        .put("money", user.get("money"))
                        .put("createdAt", getSolarDate(user.getLong("created_at")))
                        .put("sex", user.containsKey("sex") ?
                                user.getString("sex").equalsIgnoreCase("male") ? "آقا" : "خانم" :
                                ""
                        )
                        .put("status", user.getString("status"))
                        .put("statusFa", user.getString("status").equals("active") ? "فعال" : "غیرفعال")
                        .put("accesses", user.getList("accesses", String.class))
                        .put("school", user.containsKey("school") ?
                                ((Document) user.get("school")).getString("name") : ""
                        )
                        .put("grade", user.containsKey("grade") ?
                                ((Document) user.get("grade")).getString("name") : ""
                        )
                        .put("city", user.containsKey("city") ?
                                ((Document) user.get("city")).getString("name") : ""
                        )
                        .put("branch", branch);

                if (level != null && level.equalsIgnoreCase(Access.ADVISOR.getName())) {
                    jsonObject
                            .put("notSettledCounts", user.getInteger("settlementsCount")
//                                    teachScheduleRepository.count(
//                                            and(
//                                                    exists("settled_at", false),
//                                                    eq("user_id", user.getObjectId("_id")),
//                                                    exists("students.0")
//                                            )
//                                    ) + advisorRequestsRepository.count(
//                                            and(
//                                                    exists("settled_at", false),
//                                                    eq("advisor_id", user.getObjectId("_id")),
//                                                    exists("paid_at")
//                                            )
//                                    )
                            )
                            .put("teachPriority", user.getOrDefault("teach_priority", 1000))
                            .put("advisorPriority", user.getOrDefault("advisor_priority", 1000));
                }

                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("data", new JSONObject()
                    .put("users", jsonArray)
                    .put("totalCount", count)
            );
        } catch (Exception x) {
            return generateSuccessMsg("data", new JSONObject()
                    .put("users", new JSONArray())
                    .put("totalCount", 0)
            );
        }
    }

    public static String setPriority(ObjectId userId, JSONObject jsonObject) {

        Document user = userRepository.findById(userId);
        if (user == null)
            return JSON_NOT_VALID_ID;

        user.put("advisor_priority", jsonObject.getInt("advisorPriority"));
        user.put("teach_priority", jsonObject.getInt("teachPriority"));

        userRepository.updateOne(
                eq("_id", user.getObjectId("_id")),
                new BasicDBObject("$set",
                        new BasicDBObject("advisor_priority", user.getInteger("advisor_priority"))
                                .append("teach_priority", user.getInteger("teach_priority"))
                )
        );

        return JSON_OK;
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

    public static String addAccess(ObjectId userId, String newAccess, String schoolId) {

        Document user = userRepository.findById(userId);

        if (user == null)
            return JSON_NOT_VALID_ID;

        Document school = null;

        if (newAccess.equalsIgnoreCase(Access.SCHOOL.getName())) {

            if (schoolId == null || !ObjectId.isValid(schoolId))
                return JSON_NOT_VALID_PARAMS;

            school = schoolRepository.findById(new ObjectId(schoolId));
            if (school == null)
                return JSON_NOT_VALID_ID;

            if (school.containsKey("user_id"))
                return generateErr("متولی این مدرسه شخص دیگری است.");
        }

        Document schoolRoleForm = null;

        if (school != null) {

            if (!user.containsKey("form_list"))
                return generateErr("لطفا ابتدا اطلاعات مربوط به فرم مدرسه این اکانت را پر نمایید.");

            schoolRoleForm = Utility.searchInDocumentsKeyVal(
                    user.getList("form_list", Document.class),
                    "role", "school"
            );

            if (schoolRoleForm == null)
                return generateErr("لطفا ابتدا اطلاعات مربوط به فرم مدرسه این اکانت را پر نمایید.");
        }

        boolean change = false;

        if (!newAccess.equals(Access.STUDENT.getName()) && !user.getBoolean("level")) {
            user.put("level", true);
            user.put("accesses", new ArrayList<>());
            change = true;
        } else if (newAccess.equals(Access.STUDENT.getName()) && user.getBoolean("level")) {
            user.put("level", false);
            user.put("accesses", new ArrayList<>() {
                {
                    add(Access.STUDENT.getName());
                }
            });
            change = true;
        }

        if (!newAccess.equals(Access.STUDENT.getName())) {

            List<String> accesses;
            if (user.containsKey("accesses"))
                accesses = user.getList("accesses", String.class);
            else
                accesses = new ArrayList<>();

            accesses.remove(Access.STUDENT.getName());

            if (schoolRoleForm != null) {

                schoolRoleForm.put("school_id", school.getObjectId("_id"));
                if (!accesses.contains(newAccess))
                    accesses.add(newAccess);

                user.put("accesses", accesses);
                ArrayList<ObjectId> students = new ArrayList<>();
                ArrayList<Document> stds = userRepository.find(and(
                                eq("school._id", school.getObjectId("_id")),
                                eq("level", false)
                        ), new BasicDBObject("_id", 1)
                );

                for (Document std : stds)
                    students.add(std.getObjectId("_id"));

                user.put("students", students);
                school.put("user_id", userId);

                schoolRepository.updateOne(school.getObjectId("_id"), set("user_id", userId));

                change = true;
            } else if (!accesses.contains(newAccess)) {
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

        if (role.equalsIgnoreCase(Access.SCHOOL.getName())) {

            user.remove("students");

            if (user.containsKey("form_list")) {
                List<Document> forms = user.getList("form_list", Document.class);
                Document form = Utility.searchInDocumentsKeyVal(
                        forms, "role", "school"
                );
                if (form != null && form.containsKey("school_id")) {

                    ObjectId schoolId = form.getObjectId("school_id");
                    form.remove("school_id");
                    form.remove("agent_id");

                    Document school = schoolRepository.findById(schoolId);

                    if (school != null) {
                        school.remove("user_id");
                        schoolRepository.replaceOne(schoolId, school);
                    }
                }
            }

        }

        accesses.remove(role);
        if (accesses.size() == 0) {
            user.put("accesses", new ArrayList<>() {
                {
                    add(Access.STUDENT.getName());
                }
            });
        } else
            user.put("accesses", accesses);

        if (accesses.contains(Access.STUDENT.getName()))
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

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(in("accesses", Access.SCHOOL.getName()));
        filters.add(exists("form_list"));
        filters.add(eq("form_list.role", "school"));
        if (agentId != null)
            filters.add(eq("form_list.agent_id", agentId));

        ArrayList<Document> docs = userRepository.find(and(filters), new BasicDBObject("form_list", 1)
                .append("NID", 1)
                .append("phone", 1)
                .append("_id", 1)
                .append("first_name", 1)
                .append("last_name", 1)
                .append("students", 1)
        );

        JSONArray jsonArray = new JSONArray();
        for (Document doc : docs) {

            List<Document> formList = doc.getList("form_list", Document.class);
            Document form = searchInDocumentsKeyVal(
                    formList, "role", "school"
            );

            if (form == null)
                continue;

            jsonArray.put(convertSchoolToJSON(doc, form, agentId == null));
        }

        return generateSuccessMsg("data", jsonArray);
    }
//
//    public static String getMySchoolInfo(ObjectId agentId) {
//
//    }


    public static String getMyStudents(List<ObjectId> students) {

        List<Document> studentsInfo = userRepository.findPreserveOrderWitNull("_id", students);

        JSONArray data = new JSONArray();

        for (Document itr : studentsInfo) {
            if (itr == null)
                continue;

            data.put(Utility.fillJSONWithUser(itr));
        }

        return generateSuccessMsg("data", data);
    }

    private static JSONObject convertSchoolToJSON(Document doc, Document form, boolean isAgentInfoNeeded) {

        String schoolName = "";
        if (form.containsKey("school_id")) {
            Document school = schoolRepository.findById(form.getObjectId("school_id"));
            if (school != null)
                schoolName = school.getString("name") + " " + school.getString("city_name");
        }

        String agentName = "";

        if (isAgentInfoNeeded && form.containsKey("agent_id")) {
            Document user = userRepository.findById(form.getObjectId("agent_id"));
            if (user != null)
                agentName = user.getString("first_name") + " " + user.getString("last_name");
        }

        JSONObject jsonObject = new JSONObject()
                .put("phone", doc.getString("phone"))
                .put("NID", doc.getString("NID"))
                .put("id", doc.getObjectId("_id").toString())
                .put("studentsNo", doc.containsKey("students") ? doc.getList("students", ObjectId.class).size() : 0)
                .put("firstName", doc.getString("first_name"))
                .put("lastName", doc.getString("last_name"))
                .put("schoolName", schoolName)
                .put("schoolSex", form.getString("school_sex"))
                .put("kindSchool", form.getString("kind_school"))
                .put("kindSchoolFa",
                        form.getString("kind_school").equalsIgnoreCase(GradeSchool.DABESTAN.getName()) ?
                                "دبستان" :
                                form.getString("kind_school").equalsIgnoreCase(GradeSchool.MOTEVASETEAVAL.getName()) ?
                                        "متوسطه اول" : "متوسطه دوم"
                )
                .put("schoolSexFa", form.getString("school_sex").equalsIgnoreCase(Sex.MALE.getName()) ? "آقا" : "خانم")
                .put("managerName", form.getString("manager_name"));

        if (isAgentInfoNeeded)
            jsonObject.put("agent", agentName);

        return jsonObject;

    }

    public static String addSchool(JSONObject jsonObject,
                                   ObjectId agentId,
                                   String agentName) {

        String phone = jsonObject.getString("phone");
        String NID = jsonObject.getString("NID");

        if (!PhoneValidator.isValid(phone) ||
                !Utility.validationNationalCode(NID)
        )
            return generateErr("کد ملی و یا شماره همراه وارد شده نامعتبر است.");

        ArrayList<Document> users = userRepository.find(
                or(
                        eq("phone", phone),
                        eq("NID", NID)
                ), null
        );

        if (users.size() > 1)
            return generateErr("کد ملی و یا شماره همراه وارد شده نامعتبر است.");

        Document user = users.size() == 1 ? users.get(0) : null;

        long curr = System.currentTimeMillis();

        if (user == null) {

            if (!jsonObject.has("firstName") ||
                    !jsonObject.has("lastName") ||
                    !jsonObject.has("password") ||
                    !jsonObject.has("rPassword") ||
                    !jsonObject.has("city"))
                return JSON_NOT_VALID_PARAMS;

            Document config = Utility.getConfig();
            Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

            if (avatar == null)
                avatar = avatarRepository.findOne(null, null);

            avatar.put("used", (int) avatar.getOrDefault("used", 0) + 1);
            avatarRepository.replaceOne(config.getObjectId("default_avatar"), avatar);

            user = new Document("NID", NID)
                    .append("phone", phone)
                    .append("status", "active")
                    .append("level", false)
                    .append("money", (double) config.getInteger("init_money"))
                    .append("coin", ((Number) config.get("init_coin")).doubleValue())
                    .append("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                    .append("events", new ArrayList<>())
                    .append("avatar_id", avatar.getObjectId("_id"))
                    .append("pic", avatar.getString("file"))
                    .append("invitation_code", Utility.randomString(8))
                    .append("created_at", curr)
                    .append("accesses", new ArrayList<>() {
                        {
                            add(Access.STUDENT.getName());
                        }
                    })
                    .append("password", jsonObject.getString("password"))
                    .append("first_name", jsonObject.getString("firstName"))
                    .append("last_name", jsonObject.getString("lastName"));

            ArrayList<Document> forms = new ArrayList<>();

            Document form = new Document("role", "school")
                    .append("tel", jsonObject.get("tel").toString())
                    .append("address", jsonObject.getString("address"))
                    .append("name", jsonObject.getString("name"))
                    .append("manager_name", jsonObject.getString("managerName"))
                    .append("school_sex", jsonObject.getString("schoolSex"))
                    .append("agent_id", agentId)
                    .append("kind_school", jsonObject.getString("kindSchool"));

            forms.add(form);
            user.append("form_list", forms);

            ObjectId oId = userRepository.insertOneWithReturnId(user);
            ticketUpgradeRequest(oId);

            return JSON_OK;
//            return generateSuccessMsg("id", oId.toString(),
//                    new PairValue("school", convertSchoolToJSON(
//                            user, form
//                    ))
//            );
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

        return JSON_OK;
    }

    public static String removeStudents(ObjectId wantedId, boolean isAgent,
                                        Document schoolUser, JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        HashMap<ObjectId, List<ObjectId>> checked = isAgent ? new HashMap<>() : null;
        HashMap<ObjectId, ObjectId> schoolUserIds = isAgent ? new HashMap<>() : null;

        ArrayList<Bson> filter = schoolUser == null ? new ArrayList<>() : null;
        List<ObjectId> students = schoolUser != null ? schoolUser.getList("students", ObjectId.class) : null;

        if (filter != null) {
            filter.add(eq("level", true));
            filter.add(exists("form_list"));
            filter.add(eq("form_list.role", "school"));
            filter.add(exists("form_list.school_id"));
            filter.add(exists("students"));
        }

        if (isAgent && filter != null) {
            filter.add(exists("form_list.agent_id"));
            filter.add(eq("form_list.agent_id", wantedId));
        }

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            Document user = userRepository.findById(new ObjectId(id));
            if (user == null) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId schoolId = user.get("school", Document.class)
                    .getObjectId("_id");

            if (!isAgent && wantedId != null && !schoolId.equals(wantedId)) {
                excepts.put(i + 1);
                continue;
            }

            if (students != null && !students.contains(user.getObjectId("_id"))) {
                excepts.put(i + 1);
                continue;
            }

            if (students != null) {
                students.remove(user.getObjectId("_id"));
                user.remove("school");
                userRepository.replaceOne(user.getObjectId("_id"), user);
                doneIds.put(user.getObjectId("_id"));
                continue;
            }

            if (isAgent && checked.containsKey(schoolId) && checked.get(schoolId) == null) {
                excepts.put(i + 1);
                continue;
            }
            if (checked != null && checked.containsKey(schoolId) && checked.get(schoolId) != null) {
                checked.get(schoolId).remove(user.getObjectId("_id"));
                user.remove("school");
                userRepository.replaceOne(user.getObjectId("_id"), user);
                doneIds.put(user.getObjectId("_id"));
                continue;
            }

            if (checked != null && filter != null && !checked.containsKey(schoolId)) {

                Document schoolAccount = userRepository.findOne(
                        and(and(filter), eq("form_list.school_id", schoolId)), null
                );

                if (schoolAccount == null) {
                    checked.put(schoolId, null);
                    excepts.put(i + 1);
                    continue;
                }

                List<ObjectId> tmp =
                        userRepository.findById(schoolAccount.getObjectId("_id"))
                                .getList("students", ObjectId.class);

                checked.put(schoolId, tmp);
                schoolUserIds.put(schoolId, schoolAccount.getObjectId("_id"));

                checked.get(schoolId).remove(user.getObjectId("_id"));
                user.remove("school");
                userRepository.replaceOne(user.getObjectId("_id"), user);
                doneIds.put(user.getObjectId("_id"));
            }
        }

        if (doneIds.length() > 0) {

            if (students != null)
                userRepository.replaceOne(wantedId, schoolUser);
            else if (checked != null) {
                for (ObjectId oId : schoolUserIds.keySet()) {
                    userRepository.updateOne(schoolUserIds.get(oId),
                            set("students", checked.get(oId)));
                }
            }
        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String removeSchools(ObjectId agentId, JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i);
                continue;
            }

            Document user = userRepository.findById(new ObjectId(id));
            if (user == null || !user.containsKey("form_list")) {
                excepts.put(i);
                continue;
            }

            Document form = searchInDocumentsKeyVal(
                    user.getList("form_list", Document.class),
                    "role", "school"
            );

            if (form == null || !form.containsKey("agent_id")) {
                excepts.put(i);
                continue;
            }

            if (agentId != null && !form.getObjectId("agent_id").equals(agentId)) {
                excepts.put(i);
                continue;
            }

            form.remove("agent_id");
            doneIds.put(user.getObjectId("_id"));
            userRepository.replaceOne(user.getObjectId("_id"), user);
        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    private static void ticketUpgradeRequest(ObjectId oId) {

        long curr = System.currentTimeMillis();

        ArrayList<Document> chats = new ArrayList<>();
        chats.add(new Document("msg", "")
                .append("created_at", curr)
                .append("is_for_user", true)
                .append("files", new ArrayList<>())
        );

        ticketRepository.insertOne(
                new Document("title", "درخواست ارتقای سطح به مدرسه")
                        .append("created_at", curr)
                        .append("send_date", curr)
                        .append("is_for_teacher", false)
                        .append("chats", chats)
                        .append("status", "pending")
                        .append("priority", TicketPriority.HIGH.getName())
                        .append("section", TicketSection.UPGRADELEVEL.getName())
                        .append("user_id", oId)
        );

    }

    private static Document validateSchoolForAddStudent(
            Document school
    ) throws InvalidFieldsException {

        Document form = Utility.searchInDocumentsKeyVal(
                school.getList("form_list", Document.class),
                "role", "school"
        );

        if (form == null)
            throw new InvalidFieldsException("not access");

        if (!form.containsKey("school_id"))
            throw new InvalidFieldsException("unknown1 err");

        Document validSchool = schoolRepository.findById(form.getObjectId("school_id"));
        if (validSchool == null)
            throw new InvalidFieldsException("unknown2 err");

        if (!school.containsKey("city"))
            throw new InvalidFieldsException("ابتدا شهر خود را در پروفایل انتخاب کنید");

        return validSchool;
    }

    public static String addStudent(JSONObject jsonObject,
                                    Document school) {

        try {

            Document validSchool = validateSchoolForAddStudent(school);

            Document config = Utility.getConfig();
            Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));
            avatar.put("used", (int) avatar.getOrDefault("used", 0) + 1);
            avatarRepository.replaceOne(config.getObjectId("default_avatar"), avatar);

            ObjectId oId = doAddStudent(jsonObject, avatar, System.currentTimeMillis(),
                    config.getInteger("init_money"),
                    ((Number) config.get("init_coin")).doubleValue(),
                    validSchool, school.get("city", Document.class)
            );

            List<ObjectId> students = school.containsKey("students") ?
                    school.getList("students", ObjectId.class) : new ArrayList<>();

            students.add(oId);
            userRepository.updateOne(school.getObjectId("_id"), set("students", students));

            return generateSuccessMsg("id", oId.toString());
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    private static ObjectId doAddStudent(
            JSONObject jsonObject, Document avatar, long curr,
            int initMoney, double initCoin, Document school, Document city
    ) throws InvalidFieldsException {

        String NID = jsonObject.getString("NID");

        if (!Utility.validationNationalCode(NID))
            throw new InvalidFieldsException("کد ملی وارد شده نامعتبر است.");

        String phone = null;
        if (jsonObject.has("phone")) {
            phone = jsonObject.getString("phone");
            if (!PhoneValidator.isValid(phone))
                throw new InvalidFieldsException("شماره همراه وارد شده نامعتبر است.");
        }

        boolean isExist = phone != null ? userRepository.exist(
                or(
                        eq("phone", phone),
                        eq("NID", NID)
                )
        ) : userRepository.exist(eq("NID", NID));

        if (isExist)
            throw new InvalidFieldsException("دانش آموزی با کدملی/شماره همراه وارد شده در سامانه موجود است.");

        Document student = new Document("NID", NID)
                .append("status", "active")
                .append("level", false)
                .append("money", initMoney)
                .append("coin", initCoin)
                .append("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")))
                .append("events", new ArrayList<>())
                .append("avatar_id", avatar.getObjectId("_id"))
                .append("pic", avatar.getString("file"))
                .append("invitation_code", Utility.randomString(8))
                .append("created_at", curr)
                .append("accesses", new ArrayList<>() {
                    {
                        add(Access.STUDENT.getName());
                    }
                })
                .append("password", jsonObject.getString("password"))
                .append("first_name", jsonObject.getString("firstName"))
                .append("last_name", jsonObject.getString("lastName"))
                .append("school",
                        new Document("_id", school.getObjectId("_id"))
                                .append("name", school.getString("name"))
                );

        if (city != null)
            student.put("city", new Document("_id", city.getObjectId("_id"))
                    .append("name", city.getString("name"))
            );

        if (phone != null)
            student.append("phone", phone);

        return userRepository.insertOneWithReturnId(student);
    }


    public static String addBatchStudents(Document school,
                                          MultipartFile file,
                                          String passwordPolicy) {

        Document validSchool;
        try {
            validSchool = validateSchoolForAddStudent(school);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            return generateErr("File is not valid");

        rows.remove(0);

        JSONArray excepts = new JSONArray();

        Document config = Utility.getConfig();
        Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

        long curr = System.currentTimeMillis();
        int minNeededCols = passwordPolicy.equalsIgnoreCase(
                PasswordMode.CUSTOM.getName()
        ) ? 4 : 3;

        List<ObjectId> myStudents = school.getList("students", ObjectId.class);
        int dones = 0;

        int rowIdx = 0;
        JSONArray passwords = new JSONArray();

        for (Row row : rows) {

            rowIdx++;

            try {

                if (row.getCell(1) == null)
                    break;

                if (row.getLastCellNum() < minNeededCols) {
                    excepts.put(rowIdx);
                    continue;
                }

                String NID = Excel.getCellValue(row.getCell(3)).toString();
                String firstName = row.getCell(1).getStringCellValue();
                String lastName = row.getCell(2).getStringCellValue();

                JSONObject jsonObject1 =
                        new JSONObject()
                                .put("NID", NID)
                                .put("firstName", firstName)
                                .put("lastName", lastName);

                if (row.getLastCellNum() >= 4 && row.getCell(4) != null)
                    jsonObject1.put("phone", Excel.getCellValue(row.getCell(4)).toString());

                String password =
                        passwordPolicy.equalsIgnoreCase(PasswordMode.SIMPLE.getName()) ?
                                "123456" :
                                passwordPolicy.equalsIgnoreCase(PasswordMode.NID.getName()) ?
                                        NID :
                                        passwordPolicy.equalsIgnoreCase(PasswordMode.LAST_4_DIGIT_NID.getName()) ?
                                                NID.substring(6) :
                                                passwordPolicy.equalsIgnoreCase(PasswordMode.RANDOM.getName()) ?
                                                        Utility.randomPhone(6) :
                                                        row.getCell(row.getLastCellNum()).getStringCellValue();

                jsonObject1.put("password", getEncPassStatic(password));

                ObjectId stdId = doAddStudent(jsonObject1,
                        avatar, curr,
                        config.getInteger("init_money"),
                        ((Number) config.get("init_coin")).doubleValue(),
                        validSchool, school.get("city", Document.class)
                );

                dones++;
                myStudents.add(stdId);

                if (passwordPolicy.equalsIgnoreCase(PasswordMode.RANDOM.getName()))
                    passwords.put(new JSONObject()
                            .put("name", firstName + " " + lastName)
                            .put("password", password)
                    );

            } catch (Exception x) {
                System.out.println(x.getMessage());
                x.printStackTrace();
                excepts.put(rowIdx);
            }

        }

        if (dones > 0) {
            avatar.put("used", (int) avatar.getOrDefault("used", 0) + dones);
            avatarRepository.replaceOne(config.getObjectId("default_avatar"), avatar);
            userRepository.replaceOne(school.getObjectId("_id"), school);
        }
//        return generateSuccessMsg("password", passwords);
        return returnBatchResponse(excepts, null, "اضافه");
    }

    public static String acceptInvite(Document user,
                                      ObjectId reqId) {

        Document doc = accessRequestRepository.findOne(
                and(
                        eq("_id", reqId),
                        eq("target_id", user.getObjectId("_id"))
                ), null
        );

        if (doc == null)
            return JSON_NOT_VALID_PARAMS;

        if (doc.containsKey("response_at"))
            return generateErr("شما قبلا این دعوت را پذیرفته اید.");

        doc.put("response_at", System.currentTimeMillis());

        if (!user.containsKey("phone"))
            user.put("phone", doc.getString("phone"));

        List<Document> forms = user.containsKey("form_list") ?
                user.getList("form_list", Document.class) :
                new ArrayList<>();

        Document form = new Document("role", doc.getString("role"));

        if (doc.getString("role").equalsIgnoreCase("school")) {
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
        if (idx == -1)
            forms.add(form);
        else
            forms.set(idx, form);

        user.put("accesses", new ArrayList<String>().add(Access.STUDENT.getName()));
        user.put("level", false);
        user.put("form_list", forms);

        userRepository.replaceOne(user.getObjectId("_id"), user);
        accessRequestRepository.replaceOne(reqId, doc);
        ticketUpgradeRequest(user.getObjectId("_id"));

        return generateSuccessMsg("msg", "درخواست شما برای تایید ادمین ارسال گردید.");
    }

    public static String deleteStudents(JSONArray list) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        long curr = System.currentTimeMillis();
        List<WriteModel<Document>> writes = new ArrayList<>();

        for (int i = 0; i < list.length(); i++) {

            String id = list.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            Document user = userRepository.findById(new ObjectId(id));
            if (user == null || Authorization.isAdmin(user.getList("accesses", String.class))) {
                excepts.put(i + 1);
                continue;
            }

            user.put("remove_at", curr);
            doneIds.put(id);

            writes.add(new UpdateOneModel<>(
                    eq("_id", user.getObjectId("_id")),
                    new BasicDBObject("$set",
                            new BasicDBObject("remove_at", curr)
                    )
            ));
        }

        userRepository.bulkWrite(writes);
        return Utility.returnRemoveResponse(excepts, doneIds);
    }

}
