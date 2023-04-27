package irysc.gachesefid.Controllers.Advisor;


import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.YesOrNo;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Advisor.Utility.convertToJSONDigest;
import static irysc.gachesefid.Main.GachesefidApplication.advisorRequestsRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdvisorController {


    public static String removeStudents(Document advisor, JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i++);
                    continue;
                }

                ObjectId oId = new ObjectId(id);
                Document student = userRepository.findById(oId);

                if (student == null) {
                    excepts.put(i++);
                    continue;
                }

                if (!cancelAdvisor(student, advisor, true)) {
                    excepts.put(i++);
                    continue;
                }

                doneIds.put(id);

            } catch (Exception x) {
                excepts.put(i++);
            }

        }

        if (doneIds.length() > 0)
            userRepository.replaceOne(advisor.getObjectId("_id"), advisor);

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String cancelRequest(ObjectId userId, ObjectId reqId) {

        Document doc = advisorRequestsRepository.findOneAndDelete(
                and(
                        eq("_id", reqId),
                        eq("user_id", userId),
                        eq("answer", "pending")
                )
        );

        if(doc == null)
            return generateErr("تنها درخواست های پاسخ داده نشده می توانند حذف شوند");

        return JSON_OK;
    }

    public static String request(Document user, ObjectId advisorId) {

        if (advisorRequestsRepository.count(
                and(
                        eq("user_id", user.getObjectId("_id")),
                        eq("answer", "pending")
                )) > 0
        )
            return generateErr("شما تنها یک درخواست پاسخ داده نشده می توانید داشته باشید");


        Document advisor = userRepository.findById(advisorId);
        if (advisor == null)
            return JSON_NOT_VALID_ID;

        if (user.containsKey("advisor_id") &&
                user.getObjectId("advisor_id").equals(advisorId))
            return generateErr("مشاور موردنظر هم اکنون به عنوان مشاور شما می باشد");

        if (!(boolean) advisor.getOrDefault("accept_std", true))
            return JSON_NOT_ACCESS;

        ObjectId id = advisorRequestsRepository.insertOneWithReturnId(new Document("advisor_id", advisorId)
                .append("user_id", user.getObjectId("_id"))
                .append("created_at", System.currentTimeMillis())
                .append("answer", "pending")
        );

        return generateSuccessMsg("data", id);
    }

    private static boolean cancelAdvisor(Document student, Document advisor,
                                         boolean needUpdateStudent) {

        if (student.containsKey("advisor_id") &&
                student.getObjectId("advisor_id").equals(advisor.getObjectId("_id"))
        ) {

            student.remove("advisor_id");

            if (needUpdateStudent)
                userRepository.replaceOne(student.getObjectId("_id"), student);

            List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());
            int idx = Utility.searchInDocumentsKeyValIdx(students, "_id", student.getObjectId("_id"));
            if (idx == -1)
                return false;

            students.remove(idx);
            return true;
        }

        return false;
    }

    private static void setAdvisor(Document student, Document advisor) {

        cancelAdvisor(student, advisor, false);

        student.put("advisor_id", advisor.getObjectId("_id"));
        List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());
        students.add(new Document("_id", student.getObjectId("_id"))
                .append("created_at", System.currentTimeMillis())
        );

        userRepository.replaceOne(student.getObjectId("_id"), student);
        userRepository.replaceOne(advisor.getObjectId("_id"), advisor);

    }

    public static String answerToRequest(Document advisor, ObjectId reqId, String answer) {

        Document req = advisorRequestsRepository.findById(reqId);
        if (req == null)
            return JSON_NOT_VALID_ID;

        if (!req.getObjectId("advisor_id").equals(advisor.getObjectId("_id")) ||
                !req.getString("answer").equalsIgnoreCase("pending")
        )
            return JSON_NOT_ACCESS;

        if (answer.equalsIgnoreCase(YesOrNo.NO.getName()))
            req.put("answer", "reject");
        else {

            Document student = userRepository.findById(req.getObjectId("user_id"));
            if (student == null)
                return JSON_NOT_UNKNOWN;

            req.put("answer", "accept");
            req.put("answer_at", System.currentTimeMillis());
            setAdvisor(student, advisor);
        }

        advisorRequestsRepository.replaceOne(reqId, req);
        return JSON_OK;
    }


    public static String toggleStdAcceptance(Document user) {

        user.put("accept_std", !(boolean) user.getOrDefault("accept_std", true));
        userRepository.replaceOne(user.getObjectId("_id"), user);

        return JSON_OK;

    }

    public static String getAllAdvisors() {

        List<Document> advisors = userRepository.find(
                eq("accesses", Access.ADVISOR.getName()),
                ADVISOR_PUBLIC_DIGEST, Sorts.descending("rate")
        );

        JSONArray jsonArray = new JSONArray();

        for (Document advisor : advisors)
            jsonArray.put(convertToJSONDigest(advisor));

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getMyAdvisor(ObjectId advisorId) {

        Document advisor = userRepository.findById(advisorId);

        if (advisor == null)
            return JSON_NOT_UNKNOWN;

        return generateSuccessMsg("data", convertToJSONDigest(advisor));
    }

    public static String rate(ObjectId userId, ObjectId advisorId, int rate) {

        Document advisor = userRepository.findById(advisorId);
        if (advisor == null || !advisor.containsKey("students"))
            return JSON_NOT_UNKNOWN;

        List<Document> students = advisor.getList("students", Document.class);

        Document stdDoc = searchInDocumentsKeyVal(
                students, "_id", userId
        );

        if (stdDoc == null)
            return JSON_NOT_UNKNOWN;

        int oldRate = (int) stdDoc.getOrDefault("rate", 0);
        stdDoc.put("rate", rate);
        stdDoc.put("rate_at", System.currentTimeMillis());

        double oldTotalRate = (double) advisor.getOrDefault("rate", (double) 0);
        int rateCount = (int) advisor.getOrDefault("rate_count", 0);

        oldTotalRate *= rateCount;

        if (oldRate == 0)
            rateCount++;

        oldTotalRate -= oldRate;
        oldTotalRate += rate;

        advisor.put("rate", Math.round(oldTotalRate / rateCount * 100.0) / 100.0);
        advisor.put("rate_count", rateCount);

        userRepository.replaceOne(advisorId, advisor);
        return JSON_OK;

    }

    public static String createTag(Common db, JSONObject jsonObject) {

        if (db.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        return db.insertOneWithReturn(
                new Document("label", jsonObject.getString("label"))
        );
    }

    public static String removeTags(Common db, JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = db.findOneAndUpdate(
                        new ObjectId(id),
                        set("deleted_at", System.currentTimeMillis())
                );

                if (tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            } catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String getAllTags(Common db) {

        ArrayList<Document> tags = db.find(
                exists("deleted_at", false), null
        );
        JSONArray jsonArray = new JSONArray();

        for (Document tag : tags)
            jsonArray.put(new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"))
            );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

}
