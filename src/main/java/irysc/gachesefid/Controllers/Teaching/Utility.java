package irysc.gachesefid.Controllers.Teaching;

import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.TeachMode;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.Utility.*;

public class Utility {

    public static Document getSchedule(
            ObjectId userId, ObjectId scheduleId,
            boolean checkStrongAccess

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
            HashMap<ObjectId, Document> branches,
            HashMap<ObjectId, String> grades
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

        if(teacher.containsKey("form_list")) {
            Document form = searchInDocumentsKeyVal(
                    teacher.getList("form_list", Document.class),
                    "role", "advisor"
            );
            if(form != null) {
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
            JSONArray branchesJSON = new JSONArray();
            JSONArray lessonsJSON = new JSONArray();

            List<ObjectId> lessons = null;
            if (teacher.containsKey("teach_lessons"))
                lessons = teacher.getList("teach_lessons", ObjectId.class);

            for (ObjectId itr : teacher.getList("teach_branches", ObjectId.class)) {
                Document branch;
                if (branches.containsKey(itr)) {
                    branch = branches.get(itr);
                    branchesJSON.put(branch.getString("name"));
                } else {
                    branch = branchRepository.findById(itr);
                    if (branch != null) {
                        branches.put(itr, branch);
                        branchesJSON.put(branch.getString("name"));
                    }
                }

                if (lessons != null && branch != null) {

                    List<Document> branchLessons =
                            branch.getList("lessons", Document.class);

                    for (ObjectId lessonId : lessons) {

                        Document wantedLesson = searchInDocumentsKeyVal(
                                branchLessons, "_id", lessonId
                        );

                        if (wantedLesson != null)
                            lessonsJSON.put(wantedLesson.getString("name"));
                    }
                }
            }
            if (branchesJSON.length() > 0)
                jsonObject.put("branches", branchesJSON);
        }

        if (teacher.containsKey("teach_grades")) {
            JSONArray gradesJSON = new JSONArray();
            for (ObjectId itr : teacher.getList("teach_grades", ObjectId.class)) {
                if (grades.containsKey(itr))
                    gradesJSON.put(grades.get(itr));
                else {
                    Document grade = gradeRepository.findById(itr);
                    if (grade != null) {
                        grades.put(itr, grade.getString("name"));
                        gradesJSON.put(grade.getString("name"));
                    }
                }
            }

            if (gradesJSON.length() > 0)
                jsonObject.put("grades", gradesJSON);
        }

        return jsonObject;
    }

    static JSONObject convertScheduleToJSONDigest(
            Document schedule, boolean isUserNeed,
            List<Document> users
    ) {
        JSONObject jsonObject = new JSONObject()
                .put("title", schedule.getOrDefault("title", ""))
                .put("teachMode", schedule.getString("teach_mode"))
                .put("price", schedule.get("price"))
                .put("canRequest", schedule.get("can_request"))
                .put("visibility", schedule.get("visibility"))
                .put("length", schedule.get("length"))
                .put("startAt", getSolarDate(schedule.getLong("start_at")))
                .put("createdAt", getSolarDate(schedule.getLong("created_at")))
                .put("minCap", schedule.getOrDefault("min_cap", 1))
                .put("maxCap", schedule.getOrDefault("max_cap", 1))
                .put("requestsCount", schedule.containsKey("requests") ?
                        schedule.getList("requests", Document.class).size() : 0)
                .put("studentsCount", schedule.containsKey("students") ?
                        schedule.getList("students", Document.class).size() : 0)
                .put("id", schedule.getObjectId("_id").toString());

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

    static JSONObject publicConvertSchedule(Document schedule) {
        return new JSONObject()
                .put("title", schedule.getOrDefault("title", ""))
                .put("teachMode", schedule.getString("teach_mode"))
                .put("price", schedule.get("price"))
                .put("length", schedule.get("length"))
                .put("startAt", getSolarDate(schedule.getLong("start_at")))
                .put("id", schedule.getObjectId("_id").toString());
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
}
