package irysc.gachesefid.Controllers.Teaching;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.ActiveMode;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.TeachMode;
import irysc.gachesefid.Models.TeachRequestStatus;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class Utility {

    public static Document getSchedule(
            ObjectId userId, ObjectId scheduleId,
            boolean checkStrongAccess, boolean checkRegistryAccess

    ) throws InvalidFieldsException {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            throw new InvalidFieldsException("id is not correct");

        if (!schedule.getBoolean("visibility"))
            throw new InvalidFieldsException("access denied");

        if (checkStrongAccess) {
            if (!schedule.containsKey("students"))
                throw new InvalidFieldsException("access denied");

            int idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    schedule.getList("students", Document.class), "_id", userId
            );

            if (idx == -1)
                throw new InvalidFieldsException("access denied");
        }

        if (checkRegistryAccess) {

            boolean isSemiPrivate =
                    schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.SEMI_PRIVATE.getName());

            if (isSemiPrivate && (
                    !schedule.containsKey("students") ||
                            schedule.getList("students", Document.class).size() < schedule.getInteger("min_cap")
            ))
                throw new InvalidFieldsException("هنوز تعداد نفرات جلسه مدتظر برای پرداخت نهایی به حدنصاب نرسیده است");

            if (schedule.containsKey("students") &&
                    searchInDocumentsKeyValIdx(
                            schedule.getList("students", Document.class),
                            "_id", userId
                    ) != -1
            )
                throw new InvalidFieldsException("شما قبلا در این جلسه ثبت نام شده اید");

            boolean needForRegistryConfirmation = schedule.getBoolean("need_registry_confirmation");

            if (needForRegistryConfirmation) {

                if (!schedule.containsKey("requests"))
                    throw new InvalidFieldsException("access denied");

                Document request = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        schedule.getList("requests", Document.class), "_id", userId
                );

                if (request == null)
                    throw new InvalidFieldsException("access denied");

                if (request.getString("status").equalsIgnoreCase("reject"))
                    throw new InvalidFieldsException("درخواست شما رد شده است");

                if (request.getString("status").equalsIgnoreCase("pending"))
                    throw new InvalidFieldsException("درخواست شما در حال بررسی می باشد");
            }
        }

        return schedule;
    }


    public static double payFromWallet(
            int shouldPay, double money, ObjectId userId
    ) {
        double newUserMoney = money;
        newUserMoney -= Math.min(shouldPay, money);
        Document user = userRepository.findById(userId);
        user.put("money", newUserMoney);
        userRepository.replaceOne(userId, user);
        return newUserMoney;
    }

    static JSONObject convertTeacherToJSONDigest(
            ObjectId stdId, Document teacher,
            HashMap<ObjectId, String> branches,
            HashMap<ObjectId, Document> grades
    ) {

        JSONObject jsonObject = new JSONObject()
                .put("name", teacher.getString("first_name") + " " + teacher.getString("last_name"))
                .put("rate", teacher.getOrDefault("teach_rate", 0))
                .put("bio", teacher.getString("teach_bio"))
                .put("videoLink", teacher.getOrDefault("teach_video_link", ""))
                .put("id", teacher.getObjectId("_id").toString())
                .put("teaches", teacher.getOrDefault("teaches", 0))
                .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + teacher.getString("pic"));

        if (teacher.containsKey("teach_tags"))
            jsonObject.put("tags", teacher.getList("teach_tags", String.class));

        if (teacher.containsKey("form_list")) {
            Document form = searchInDocumentsKeyVal(
                    teacher.getList("form_list", Document.class),
                    "role", "advisor"
            );
            if (form != null) {
                jsonObject.put("form", new JSONObject()
                        .put("workSchools", form.getString("work_schools"))
                );
            }
        }
        if (stdId != null) {
            List<Document> rates = (List<Document>) teacher.getOrDefault("teach_rates", new ArrayList<>());

            Document stdRate = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    rates, "_id", stdId
            );

            if (stdRate != null)
                jsonObject.put("myRate", stdRate.getInteger("rate"));
        }

        if (teacher.containsKey("teach_branches")) {
            Set<String> branchesJSON = new HashSet<>();

            for (ObjectId itr : teacher.getList("teach_branches", ObjectId.class)) {
                if (branches.containsKey(itr))
                    branchesJSON.add(branches.get(itr));
                else {
                    Document branch = gradeRepository.findById(itr);
                    if (branch != null) {
                        branches.put(itr, branch.getString("name"));
                        branchesJSON.add(branch.getString("name"));
                    }
                }
            }
            if (branchesJSON.size() > 0)
                jsonObject.put("branches", branchesJSON);
        }

        if (teacher.containsKey("teach_grades")) {
            Set<String> gradesJSON = new HashSet<>();
            Set<String> lessonsJSON = new HashSet<>();

            List<ObjectId> lessons = null;
            if (teacher.containsKey("teach_lessons"))
                lessons = teacher.getList("teach_lessons", ObjectId.class);

            for (ObjectId itr : teacher.getList("teach_grades", ObjectId.class)) {
                Document grade;

                if (grades.containsKey(itr)) {
                    grade = grades.get(itr);
                    gradesJSON.add(grade.getString("name"));
                } else {
                    grade = branchRepository.findById(itr);
                    if (grade != null) {
                        grades.put(itr, grade);
                        gradesJSON.add(grade.getString("name"));
                    }
                }

                if (lessons != null && grade != null) {

                    List<Document> gradeLessons =
                            grade.getList("lessons", Document.class);

                    for (ObjectId lessonId : lessons) {

                        Document wantedLesson = searchInDocumentsKeyVal(
                                gradeLessons, "_id", lessonId
                        );

                        if (wantedLesson != null)
                            lessonsJSON.add(wantedLesson.getString("name"));
                    }
                }
            }

            if (lessonsJSON.size() > 0)
                jsonObject.put("lessons", lessonsJSON);

            if (gradesJSON.size() > 0)
                jsonObject.put("grades", gradesJSON);
        }

        return jsonObject;
    }

    static JSONObject convertScheduleToJSONDigestForTeacher(
            Document schedule, boolean isUserNeed,
            List<Document> users, boolean isForUpdate
    ) {
        JSONObject jsonObject = new JSONObject()
                .put("title", schedule.getOrDefault("title", ""))
                .put("teachMode", schedule.getString("teach_mode"))
                .put("price", schedule.get("price"))
                .put("visibility", schedule.get("visibility"))
                .put("length", schedule.get("length"))
                .put("needRegistryConfirmation", schedule.getOrDefault("need_registry_confirmation", true))
                .put("minCap", schedule.getOrDefault("min_cap", 1))
                .put("maxCap", schedule.getOrDefault("max_cap", 1));

        if (!isForUpdate) {
            long curr = System.currentTimeMillis();
            boolean isInTeachPeriod = curr - ONE_DAY_MIL_SEC < schedule.getLong("start_at") &&
                    schedule.getLong("start_at") < curr + ONE_DAY_MIL_SEC;

            jsonObject.put("createdAt", getSolarDate(schedule.getLong("created_at")))
                    .put("requestsCount", schedule.containsKey("requests") ?
                            schedule.getList("requests", Document.class).size() : 0)
                    .put("studentsCount", schedule.containsKey("students") ?
                            schedule.getList("students", Document.class).size() : 0)
                    .put("id", schedule.getObjectId("_id").toString())
                    .put("startAt", getSolarDate(schedule.getLong("start_at")))
                    .put("skyRoomUrl", isInTeachPeriod ? schedule.getOrDefault("sky_room_url", "") : "")
                    .put("canBuildSkyRoom",
                            !schedule.containsKey("sky_room_url") &&
                                    schedule.containsKey("students") &&
                                    schedule.getList("students", Document.class).size() > 0 && isInTeachPeriod

                    );
        } else
            jsonObject.put("startAt", schedule.getLong("start_at"));

        if (isUserNeed) {
            users.stream()
                    .filter(document -> document.getObjectId("_id").equals(schedule.getObjectId("user_id")))
                    .findFirst().ifPresent(document -> jsonObject.put("user", document.getString("first_name") + " " + document.getString("last_name")));
        }

        return jsonObject;
    }

    static JSONObject publicConvertScheduleToJSONDigest(
            Document schedule
    ) {
        JSONObject jsonObject = new JSONObject()
                .put("title", schedule.getOrDefault("title", ""))
                .put("teachMode", schedule.getString("teach_mode"))
                .put("price", schedule.get("price"))
                .put("length", schedule.get("length"))
                .put("startAt", getSolarDate(schedule.getLong("start_at")))
                .put("minCap", schedule.getOrDefault("min_cap", 1))
                .put("maxCap", schedule.getOrDefault("max_cap", 1))
                .put("description", schedule.getOrDefault("description", 1))
                .put("needRegistryConfirmation", schedule.getOrDefault("need_registry_confirmation", true))
                .put("id", schedule.getObjectId("_id").toString());

        if (Objects.equals(
                schedule.getString("teach_mode"),
                TeachMode.SEMI_PRIVATE.getName()
        )) {
            jsonObject
                    .put("requestsCount", schedule.containsKey("requests") ?
                            schedule.getList("requests", Document.class).size() : 0);
        }

        return jsonObject;
    }

    static JSONObject convertMySchedule(Document schedule, Document teacher, int rate) {
        long curr = System.currentTimeMillis();
        return new JSONObject()
                .put("title", schedule.getOrDefault("title", ""))
                .put("teachMode", schedule.getString("teach_mode"))
                .put("price", schedule.get("price"))
                .put("length", schedule.get("length"))
                .put("rate", rate)
                .put("startAt", getSolarDate(schedule.getLong("start_at")))
                .put("canRate",
                        schedule.getLong("start_at") < curr &&
                                curr < schedule.getLong("start_at") + 30 * ONE_DAY_MIL_SEC
                )
                .put("id", schedule.getObjectId("_id").toString())
                .put("skyRoomUrl", schedule.getOrDefault("sky_room_url", ""))
                .put("teacher", new JSONObject()
                        .put("name", teacher.getString("first_name") + " " + teacher.getString("last_name"))
                        .put("teachRate", teacher.getOrDefault("teach_rate", 0))
                        .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + teacher.getString("pic"))
                );
    }

    static Document createTransactionDoc(
            int shouldPay, ObjectId userId,
            ObjectId scheduleId,
            Document offDoc, double offAmount
    ) {

        Document doc = new Document("user_id", userId)
                .append("amount", 0)
                .append("account_money", shouldPay > 100 ? shouldPay : 0)
                .append("created_at", System.currentTimeMillis())
                .append("status", "success")
                .append("section", OffCodeSections.CLASSES.getName())
                .append("products", scheduleId);

        if (offDoc != null) {
            doc.append("off_code", offDoc.getObjectId("_id"));
            doc.append("off_amount", (int) offAmount);
        }

        return doc;
    }

    static Document createPrePayTransactionDoc(
            int shouldPay, ObjectId userId,
            ObjectId scheduleId
    ) {
        return new Document("user_id", userId)
                .append("amount", 0)
                .append("account_money", shouldPay)
                .append("created_at", System.currentTimeMillis())
                .append("status", "success")
                .append("section", "prePay")
                .append("products", scheduleId);
    }

    static void completePrePayForSemiPrivateSchedule(
            ObjectId scheduleId, Document schedule,
            ObjectId userId, int paid, int paidFromWallet
    ) {

        if (schedule == null && scheduleId != null)
            schedule = scheduleRepository.findById(scheduleId);

        Document newReqDoc = new Document("_id", userId)
                .append("created_at", System.currentTimeMillis())
                .append("paid", paid)
                .append("wallet_paid", paidFromWallet)
                .append("status", "accept");

        List<Document> requests = schedule.getList("requests", Document.class);
        requests.add(newReqDoc);

        int maxCap = schedule.getInteger("max_cap");
        int minCap = schedule.getInteger("min_cap");
        int acceptStatusCount = 0;

        for (Document req : schedule.getList("requests", Document.class)) {
            if (req.getString("status").equalsIgnoreCase("pending"))
                acceptStatusCount++;
        }

        if (acceptStatusCount >= maxCap)
            schedule.put("can_request", false);

        if (acceptStatusCount >= minCap) {
            if (!schedule.containsKey("send_finalize_pay_sms")) {
                schedule.put("send_finalize_pay_sms", true);
                // todo: send sms
            }
        }

        teachScheduleRepository.replaceOneWithoutClearCache(
                scheduleId, schedule
        );
    }

    static Bson buildMyScheduleRequestsFilters(
            ObjectId userId, String activeMode,
            String statusMode, String scheduleActiveMode
    ) throws InvalidFieldsException {
        List<Bson> requestFilter = new ArrayList<>() {{
            add(eq("_id", userId));
        }};
        Bson scheduleFilter = null;

        if (scheduleActiveMode != null) {
            if (!EnumValidatorImp.isValid(scheduleActiveMode, ActiveMode.class))
                throw new InvalidFieldsException("params is not valid");

            if (scheduleActiveMode.equalsIgnoreCase("active"))
                scheduleFilter = gt("start_at", System.currentTimeMillis());
            else
                scheduleFilter = lt("start_at", System.currentTimeMillis());
        }

        if (activeMode != null) {
            if (!EnumValidatorImp.isValid(activeMode, ActiveMode.class))
                throw new InvalidFieldsException("params is not valid");

            if (activeMode.equalsIgnoreCase("active"))
                requestFilter.add(gt("expire_at", System.currentTimeMillis()));
            else
                requestFilter.add(lt("expire_at", System.currentTimeMillis()));
        }

        if (activeMode != null) {
            if (!EnumValidatorImp.isValid(activeMode, ActiveMode.class))
                throw new InvalidFieldsException("params is not valid");

            if (activeMode.equalsIgnoreCase("active"))
                requestFilter.add(gt("expire_at", System.currentTimeMillis()));
            else
                requestFilter.add(lt("expire_at", System.currentTimeMillis()));
        }

        if (statusMode != null) {
            if (!EnumValidatorImp.isValid(statusMode, TeachRequestStatus.class))
                throw new InvalidFieldsException("params is not valid");

            requestFilter.add(eq("status", statusMode.toLowerCase()));
        }

        List<Bson> filters = new ArrayList<>() {{
            add(elemMatch("requests", and(requestFilter)));
        }};
        if (scheduleFilter != null)
            filters.add(scheduleFilter);

        return and(filters);
    }

    public static Bson register(
            Document schedule, ObjectId userId,
            long curr, Document request,
            boolean isPrivate
    ) {

        List<Document> students = schedule.containsKey("students") ?
                schedule.getList("students", Document.class) :
                new ArrayList<>();

        students.add(new Document("_id", userId)
                .append("created_at", curr)
        );

        List<Document> requests = schedule.containsKey("requests")
                ? schedule.getList("requests", Document.class)
                : new ArrayList<>();

        if (request != null)
            request.put("status", "paid");
        else {
            request = new Document("_id", userId)
                    .append("created_at", System.currentTimeMillis())
                    .append("status", "paid")
                    .append("expire_at", curr + SET_STATUS_TEACH_REQUEST_EXPIRATION_MSEC);

            requests.add(request);
            schedule.put("requests", requests);
        }

        schedule.put("students", students);

        BasicDBObject update = new BasicDBObject("students", students)
                .append("requests", requests);

        if (isPrivate) {
            schedule.put("can_request", false);
            update.append("can_request", false);
        }

        return update;
    }

    static Document getScheduleForCreateSkyRoom(ObjectId scheduleId, ObjectId userId) throws InvalidFieldsException {

        Document schedule = teachScheduleRepository.findById(scheduleId);
        if (schedule == null)
            throw new InvalidFieldsException("not valid id");

        if (!schedule.getObjectId("user_id").equals(userId))
            throw new InvalidFieldsException("not access");

        if (schedule.containsKey("sky_room_url"))
            throw new InvalidFieldsException("یکبار برای این جلسه لینک اتاق جلسه ساخته شده است و امکان ساخت مجدد آن وجود ندارد");

        if (!schedule.containsKey("students") ||
                schedule.getList("students", Document.class).size() == 0
        )
            throw new InvalidFieldsException("این جلسه دانش آموزی ندارد و امکان ساخت لینک اتاق جلسه وجود ندارد");

        long curr = System.currentTimeMillis();
        if (
                (curr < schedule.getLong("start_at") - ONE_DAY_MIL_SEC) ||
                        (curr > schedule.getLong("start_at") + ONE_DAY_MIL_SEC)
        )
            throw new InvalidFieldsException("ساخت لینک جلسه در بازه یک روز قبل و یا بعد از زمان شروع جلسه امکان پذیر است");

        return schedule;
    }
}
