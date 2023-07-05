package irysc.gachesefid.Controllers.Advisor;


import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.YesOrNo;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Advisor.Utility.*;
import static irysc.gachesefid.Controllers.UserController.fillJSONWithEducationalHistory;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdvisorController {

    public static String getStudentDigest(ObjectId advisorId, ObjectId studentId) {

        Document student = userRepository.findById(studentId);
        JSONObject output = new JSONObject();

        int firstDayOfMonth = getFirstDayOfMonth();
        int firstDayOfLastMonth = getFirstDayOfLastMonth();

        int schedulesInCurrMonth = scheduleRepository.count(
                and(
                        in("advisors", advisorId),
                        eq("user_id", studentId),
                        gte("week_start_at_int", firstDayOfMonth)
                )
        );

        int schedulesInLastMonth = scheduleRepository.count(
                and(
                        in("advisors", advisorId),
                        eq("user_id", studentId),
                        gte("week_start_at_int", firstDayOfMonth),
                        lte("week_start_at_int", firstDayOfLastMonth)
                )
        );

        int quizzesInMonth = schoolQuizRepository.count(
                and(
                        eq("created_by", advisorId),
                        eq("students._id", studentId),
                        gte("created_at", firstDayOfMonth)
                )
        );

        int quizzesInLastMonth = schoolQuizRepository.count(
                and(
                        eq("created_by", advisorId),
                        eq("students._id", studentId),
                        gte("created_at", firstDayOfMonth),
                        lte("created_at", firstDayOfLastMonth)
                )
        );

        int advisorMeetingsInMonth = advisorMeetingRepository.count(
                and(
                        eq("advisor_id", advisorId),
                        eq("user_id", studentId),
                        gte("created_at", firstDayOfMonth)
                )
        );

        int advisorMeetingsInLastMonth = advisorMeetingRepository.count(
                and(
                        eq("advisor_id", advisorId),
                        eq("user_id", studentId),
                        gte("created_at", firstDayOfMonth),
                        lte("created_at", firstDayOfLastMonth)
                )
        );

        List<Document> requests = advisorRequestsRepository.find(
                and(
                        eq("advisor_id", advisorId),
                        eq("user_id", studentId),
                        eq("answer", "accept"),
                        exists("paid")
                ), null, Sorts.descending("created_at")
        );

        Document request = requests.size() == 0 ? null : requests.get(0);

        fillJSONWithEducationalHistory(student, output, false);
        output.put("schedulesInCurrMonth", schedulesInCurrMonth);
        output.put("schedulesInLastMonth", schedulesInLastMonth);
        output.put("quizzesInMonth", quizzesInMonth);
        output.put("quizzesInLastMonth", quizzesInLastMonth);
        output.put("advisorMeetingsInMonth", advisorMeetingsInMonth);
        output.put("advisorMeetingsInLastMonth", advisorMeetingsInLastMonth);

        if (request != null) {
            output.put("maxKarbarg", request.getOrDefault("max_karbarg", "نامحدود"))
                    .put("maxVideoCalls", request.getInteger("video_calls"))
                    .put("planTitle", request.getString("title"))
                    .put("maxChats", request.getOrDefault("max_chat", "نامحدود"))
                    .put("maxExam", request.getOrDefault("max_exam", "نامحدود"));
        }


        return generateSuccessMsg("data", output);
    }

    public static String removeOffers(ObjectId advisorId, JSONArray items) {

        List<Document> docs = advisorFinanceOfferRepository.find(eq("advisor_id", advisorId), JUST_ID);
        List<ObjectId> ids = new ArrayList<>();

        for (Document doc : docs)
            ids.add(doc.getObjectId("_id"));

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < items.length(); i++) {

            try {
                String id = items.getString(i);
                if (!ObjectId.isValid(id)) {
                    excepts.put(id);
                    continue;
                }

                ObjectId oId = new ObjectId(id);
                if (!ids.contains(oId)) {
                    excepts.put(id);
                    continue;
                }

                advisorFinanceOfferRepository.deleteOne(oId);
                doneIds.put(id);
            } catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return returnRemoveResponse(excepts, doneIds);
    }

    public static String createNewOffer(ObjectId advisorId, JSONObject data) {

        if (data.getString("title").length() < 3)
            return generateErr("عنوان باید بیش از ۲ کاراکتر باشد");

        Document config = getConfig();

        if (config.getInteger("min_advice_price") > data.getInt("price"))
            return generateErr("قیمت هر بسته باید حداقل " + config.getInteger("min_advice_price") + " باشد");

        if (config.getInteger("max_video_call_per_month") < data.getInt("videoCalls"))
            return generateErr("تعداد تماس های تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        Document newDoc = new Document("advisor_id", advisorId)
                .append("price", data.getInt("price"))
                .append("title", data.getString("title"))
                .append("video_calls", data.getInt("videoCalls"))
                .append("visibility", data.getBoolean("visibility"))
                .append("created_at", System.currentTimeMillis());

        if (data.has("description"))
            newDoc.append("description", data.getString("description"));

        if (data.has("maxKarbarg"))
            newDoc.append("max_karbarg", data.getInt("maxKarbarg"));

        if (data.has("maxExam"))
            newDoc.append("max_exam", data.getInt("maxExam"));

        if (data.has("maxChat"))
            newDoc.append("max_chat", data.getInt("maxChat"));

        advisorFinanceOfferRepository.insertOne(newDoc);

        return generateSuccessMsg("data", convertFinanceOfferToJSONObject(newDoc, true));
    }

    public static String updateOffer(ObjectId id, JSONObject data) {

        if (data.getString("title").length() < 3)
            return generateErr("عنوان باید بیش از ۲ کاراکتر باشد");

        Document doc = advisorFinanceOfferRepository.findById(id);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        Document config = getConfig();

        if (config.getInteger("min_advice_price") > data.getInt("price"))
            return generateErr("قیمت هر بسته باید حداقل " + config.getInteger("min_advice_price") + " باشد");

        if (config.getInteger("max_video_call_per_month") < data.getInt("videoCalls"))
            return generateErr("تعداد تماس های تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        doc.put("title", data.getString("title"));
        doc.put("price", data.getInt("price"));
        doc.put("video_calls", data.getInt("videoCalls"));
        doc.put("visibility", data.getBoolean("visibility"));

        if (data.has("description"))
            doc.put("description", data.getString("description"));
        else
            doc.remove("description");

        if (data.has("maxKarbarg"))
            doc.put("max_karbarg", data.getInt("maxKarbarg"));
        else
            doc.remove("max_karbarg");

        if (data.has("maxExam"))
            doc.put("max_exam", data.getInt("maxExam"));
        else
            doc.remove("max_exam");

        if (data.has("maxChat"))
            doc.put("max_chat", data.getInt("maxChat"));
        else
            doc.remove("max_chat");

        advisorFinanceOfferRepository.replaceOne(doc.getObjectId("_id"), doc);

        return generateSuccessMsg("data", convertFinanceOfferToJSONObject(doc, true));
    }


    public static String getOffers(ObjectId accessorId, ObjectId advisorId) {

        List<Document> docs = advisorFinanceOfferRepository.find(
                or(
                        and(
                                eq("visibility", true),
                                eq("advisor_id", advisorId)
                        ),
                        eq("advisor_id", accessorId)
                ), null, Sorts.descending("created_at")
        );

        JSONArray jsonArray = new JSONArray();

        boolean fullAccess = accessorId != null &&
                (docs.size() == 0 || accessorId.equals(docs.get(0).getObjectId("advisor_id")));

        if (docs.size() > 0) {

            for (Document doc : docs) {
                jsonArray.put(convertFinanceOfferToJSONObject(
                        doc, fullAccess)
                );
            }

            if (fullAccess) {
                Document config = getConfig();

                return generateSuccessMsg("data", new JSONObject()
                        .put("data", jsonArray)
                        .put("maxVideoCalls", config.getInteger("max_video_call_per_month"))
                        .put("minPrice", config.getInteger("min_advice_price"))
                );
            }
        }

        if (docs.size() == 0 && accessorId == null) {

            Document config = getConfig();

            jsonArray.put(convertFinanceOfferToJSONObject(
                            new Document("price", config.getInteger("min_advice_price"))
                                    .append("video_calls", config.getInteger("max_video_call_per_month"))
                                    .append("title", "پیش فرض"), false
                    )
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String requestMeeting(ObjectId advisorId,
                                        String NID,
                                        String name,
                                        ObjectId studentId) {

        Document std = userRepository.findById(studentId);
        if (std == null)
            return JSON_NOT_VALID_ID;

        int advisorSkyRoomId = createUser(NID, name);

        if (advisorSkyRoomId == -1)
            return generateErr("امکان ایجاد کاربر در سایت اسکای روم در حال حاضر وجود ندارد");

        String studentName = std.getString("first_name") + " " + std.getString("last_name");

        int studentSkyRoomId = createUser(std.getString("NID"), studentName);

        if (studentSkyRoomId == -1)
            return generateErr("امکان ایجاد کاربر در سایت اسکای روم در حال حاضر وجود ندارد");

        Document config = getConfig();
        int maxMeetingPerAdvisorInMonth = (int) config.getOrDefault("max_meeting_per_advisor", 2);

        long curr = System.currentTimeMillis();
        long monthAgo = curr - 30 * ONE_DAY_MIL_SEC;

        int advisorMeetingsCount = advisorMeetingRepository.count(
                and(
                        eq("advisor_id", advisorId),
                        gt("created_at", monthAgo)
                )
        );

        if (advisorMeetingsCount >= maxMeetingPerAdvisorInMonth)
            return generateErr("شما در هر ماه می توانید حداکثر " + maxMeetingPerAdvisorInMonth + " جلسه ملاقات بسازید");

        int roomId = irysc.gachesefid.Controllers.Advisor.Utility.createMeeting("جلسه مشاوره " + name + " - " + studentName);
        if (roomId == -1)
            return generateErr("امکان ساخت اتاق جلسه در حال حاضر وجود ندارد");

        String roomUrl = irysc.gachesefid.Controllers.Advisor.Utility.roomUrl(roomId);

        Document document = new Document("advisor_id", advisorId)
                .append("student_id", studentId)
                .append("created_at", curr)
                .append("room_id", roomId)
                .append("advisor_sky_id", advisorSkyRoomId)
                .append("student_sky_id", studentSkyRoomId);

        if (roomUrl != null)
            document.append("url", roomUrl);

        advisorMeetingRepository.insertOne(document);
        addUserToClass(studentSkyRoomId, advisorSkyRoomId, roomId);

        return generateSuccessMsg("url", roomUrl);
    }

    public static String getMyCurrentRoom(ObjectId studentId) {

        long curr = System.currentTimeMillis();
        long yesterday = curr - ONE_DAY_MIL_SEC;

        List<Document> docs = advisorMeetingRepository.find(and(
                eq("student_id", studentId),
                gt("created_at", yesterday),
                lt("created_at", curr),
                exists("url")
        ), new BasicDBObject("url", 1).append("advisor_id", 1).append("created_at", 1));

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            Document advisor = userRepository.findById(doc.getObjectId("advisor_id"));

            if (advisor == null)
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("advisor", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                            .put("url", doc.getString("url"))
                            .put("createdAt", getSolarDate(doc.getLong("created_at")))
            );

        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getMyCurrentRoomForAdvisor(ObjectId advisorId) {

        long curr = System.currentTimeMillis();
        long yesterday = curr - ONE_DAY_MIL_SEC;

        List<Document> docs = advisorMeetingRepository.find(and(
                eq("advisor_id", advisorId),
                gt("created_at", yesterday),
                lt("created_at", curr),
                exists("url")
        ), new BasicDBObject("url", 1).append("student_id", 1).append("created_at", 1));

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            Document student = userRepository.findById(doc.getObjectId("student_id"));

            if (student == null)
                continue;

            jsonArray.put(
                    new JSONObject()
                            .put("student", student.getString("first_name") + " " + student.getString("last_name"))
                            .put("url", doc.getString("url"))
                            .put("createdAt", getSolarDate(doc.getLong("created_at")))
            );

        }

        return generateSuccessMsg("data", jsonArray);
    }

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

    public static String cancel(Document user) {

        if (!user.containsKey("advisor_id"))
            return JSON_NOT_ACCESS;

        Document advisor = userRepository.findById(user.getObjectId("advisor_id"));
        if (advisor != null) {

            List<Document> students = advisor.getList("students", Document.class);
            int idx = searchInDocumentsKeyValIdx(students, "_id", user.getObjectId("_id"));

            if (idx > -1) {

                students.remove(idx);
                userRepository.replaceOne(advisor.getObjectId("_id"), advisor);

            }
        }

        user.remove("advisor_id");
        userRepository.replaceOne(user.getObjectId("_id"), user);

        return JSON_OK;
    }


    public static String hasOpenRequest(ObjectId userId, Number userMoney) {

        List<Document> docs = advisorRequestsRepository.find(
                and(
                        eq("user_id", userId),
                        or(
                                eq("answer", "pending"),
                                and(
                                        eq("answer", "accept"),
                                        exists("paid", false)
                                )
                        )
                ), new BasicDBObject("advisor_id", 1)
                        .append("answer", 1)
                        .append("paid", 1)
                        .append("price", 1)
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("advisorId", doc.getObjectId("advisor_id").toString())
                    .put("id", doc.getObjectId("_id").toString())
                    .put("answer", doc.getString("answer"));

            if (
                    doc.getString("answer").equalsIgnoreCase("accept") &&
                            !doc.containsKey("paid") && doc.containsKey("price")
            )
                jsonObject.put("price", doc.getInteger("price"))
                        .put("shouldPay", Math.max(doc.getInteger("price") - userMoney.intValue(), 0));

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String returnRequests(String key, List<Document> requests) {

        JSONArray jsonArray = new JSONArray();

        for (Document request : requests) {

            Document advisorOrStudent = userRepository.findById(request.getObjectId(key));
            if (advisorOrStudent == null)
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("id", request.getObjectId("_id").toString())
                    .put("name", advisorOrStudent.getString("first_name") + " " + advisorOrStudent.getString("last_name"))
                    .put("createdAt", Utility.getSolarDate(request.getLong("created_at")))
                    .put("answerAt", request.containsKey("answer_at") ?
                            Utility.getSolarDate(request.getLong("answer_at")) :
                            ""
                    )
                    .put("status", request.getString("answer"));

            if (key.equalsIgnoreCase("user_id")) {

                jsonObject
                        .put("maxKarbarg", request.getOrDefault("max_karbarg", -1))
                        .put("maxExam", request.getOrDefault("max_exam", -1))
                        .put("maxChat", request.getOrDefault("max_chat", -1))
                        .put("videoCalls", request.getInteger("video_calls"))
                        .put("price", request.getInteger("price"))
                        .put("userId", request.getObjectId("user_id").toString())
                        .put("title", request.getString("title"));

                if (request.containsKey("paid"))
                    jsonObject.put("paid", request.getInteger("paid"))
                            .put("paidAt", Utility.getSolarDate(System.currentTimeMillis()));

            }

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }


    public static String myStudentRequests(ObjectId advisorId) {

        List<Document> requests = advisorRequestsRepository.find(
                eq("advisor_id", advisorId), null, Sorts.descending("created_at")
        );

        return returnRequests("user_id", requests);
    }

    public static String request(Document user, ObjectId advisorId, String planId) {

//        if (advisorRequestsRepository.count(
//                and(
//                        eq("user_id", user.getObjectId("_id")),
//                        eq("answer", "pending")
//                )) > 0
//        )
//            return generateErr("شما تنها یک درخواست پاسخ داده نشده می توانید داشته باشید");

        if (!planId.equals("-1") && !ObjectId.isValid(planId))
            return JSON_NOT_VALID_PARAMS;

        ObjectId planOId = planId.equals("-1") ? null : new ObjectId(planId);

        Document advisor = userRepository.findById(advisorId);
        if (advisor == null)
            return JSON_NOT_VALID_ID;

        if (user.containsKey("advisor_id") &&
                user.getObjectId("advisor_id").equals(advisorId))
            return generateErr("مشاور موردنظر هم اکنون به عنوان مشاور شما می باشد");

        if (!(boolean) advisor.getOrDefault("accept_std", true))
            return JSON_NOT_ACCESS;

        Document plan;

        if (planOId == null) {

            if (advisorFinanceOfferRepository.count(
                    and(
                            eq("advisor_id", advisorId),
                            eq("visibility", true)
                    )
            ) > 0)
                return generateErr("لطفا یکی از بسته های پیشنهادی را انتخاب نمایید");

            Document config = getConfig();

            plan = new Document("price", config.getInteger("min_advice_price"))
                    .append("title", "پیش فرض")
                    .append("video_calls", config.getInteger("max_video_call_per_month"));

        } else {

            plan = advisorFinanceOfferRepository.findById(planOId);

            if (plan == null || !plan.getObjectId("advisor_id").equals(advisorId))
                return JSON_NOT_VALID_PARAMS;
        }

        Document newReq = new Document("advisor_id", advisorId)
                .append("user_id", user.getObjectId("_id"))
                .append("created_at", System.currentTimeMillis())
                .append("answer", "pending")
                .append("title", plan.getString("title"))
                .append("video_calls", plan.getInteger("video_calls"))
                .append("price", plan.getInteger("price"));

        if (plan.containsKey("max_exam"))
            newReq.append("max_exam", plan.getInteger("max_exam"));

        if (plan.containsKey("max_chat"))
            newReq.append("max_chat", plan.getInteger("max_chat"));

        if (plan.containsKey("max_karbarg"))
            newReq.append("max_karbarg", plan.getInteger("max_karbarg"));

        ObjectId id = advisorRequestsRepository.insertOneWithReturnId(newReq);

        JSONObject jsonObject = new JSONObject()
                .put("advisorId", advisorId.toString())
                .put("id", id.toString())
                .put("answer", "pending");

        return generateSuccessMsg("data", jsonObject);

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

    public static void setAdvisor(Document student, Document advisor) {

//        cancelAdvisor(student, advisor, false);

        student.put("advisor_id", advisor.getObjectId("_id"));
        userRepository.replaceOneWithoutClearCache(student.getObjectId("_id"), student);

        List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());

        int idx = Utility.searchInDocumentsKeyValIdx(students, "_id", student.getObjectId("_id"));

        if (idx == -1) {
            students.add(new Document("_id", student.getObjectId("_id"))
                    .append("created_at", System.currentTimeMillis())
            );
        }
        //todo : extend advisor
//        else {
//            students.get(idx).put("created_at", );
//        }

        advisor.put("students", students);
        userRepository.replaceOneWithoutClearCache(advisor.getObjectId("_id"), advisor);
    }


    public static String answerToRequest(Document advisor, ObjectId reqId, String answer) {

        Document req = advisorRequestsRepository.findById(reqId);
        if (req == null)
            return JSON_NOT_VALID_ID;

        if (!req.getObjectId("advisor_id").equals(advisor.getObjectId("_id")) ||
                !req.getString("answer").equalsIgnoreCase("pending")
        )
            return JSON_NOT_ACCESS;

        if (answer.equalsIgnoreCase(YesOrNo.NO.getName())) {
            req.put("answer", "reject");
            req.put("answer_at", System.currentTimeMillis());
        } else {

//            Document student = userRepository.findById(req.getObjectId("user_id"));
//            if (student == null)
//                return JSON_NOT_UNKNOWN;
//
//            setAdvisor(student, advisor);

            req.put("answer", "accept");
            req.put("answer_at", System.currentTimeMillis());

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
            jsonArray.put(convertToJSONDigest(null, advisor));

        return generateSuccessMsg("data", jsonArray);
    }

    public static String createTag(Common db, JSONObject jsonObject) {

        if (db.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        Document newDoc = new Document("label", jsonObject.getString("label"));

        if(jsonObject.has("numberLabel"))
            newDoc.append("number_label", jsonObject.getString("numberLabel"));

        return db.insertOneWithReturn(newDoc);
    }

    public static String editTag(Common db, ObjectId id, JSONObject jsonObject) {

        Document doc = db.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        if (!doc.getString("label").equals(jsonObject.getString("label")) &&
                db.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        doc.put("label", jsonObject.getString("label"));

        if(jsonObject.has("numberLabel"))
            doc.put("number_label", jsonObject.getString("numberLabel"));
        else
            doc.remove("number_label");

        db.replaceOne(id, doc);
        return JSON_OK;
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

        for (Document tag : tags) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"));

            if(tag.containsKey("number_label"))
                jsonObject.put("numberLabel", tag.getString("number_label"));

            jsonArray.put(jsonObject);
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    public static String addItemToSchedule(ObjectId advisorId,
                                           ObjectId userId,
                                           JSONObject data) {

        String day = data.getString("day");
        int dayIndex;

        try {
            dayIndex = validateDay(day);
        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

        int duration = data.getInt("duration");
        if (duration < 15 || duration > 240)
            return generateErr("زمان هر برنامه باید بین 15 الی 240 دقیقه باشد");

        if (!Authorization.hasAccessToThisStudent(userId, advisorId))
            return JSON_NOT_ACCESS;

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = adviseTagRepository.findById(tagId);
        if (tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        if(tag.containsKey("number_label") && !data.has("additional"))
            return generateErr("لطفا " + tag.getString("number_label") + " را وارد نمایید");

        ObjectId lessonId = new ObjectId(data.getString("lessonId"));
        Document grade = gradeRepository.findOne(eq("lessons._id", lessonId), null);
        if (grade == null)
            return JSON_NOT_VALID;

        Document lesson = searchInDocumentsKeyVal(
                grade.getList("lessons", Document.class),
                "_id", lessonId
        );

        Document schedule;

        if (data.has("id")) {

            if (!ObjectId.isValid(data.getString("id")))
                return JSON_NOT_VALID_PARAMS;

            ObjectId oId = new ObjectId(data.getString("id"));
            schedule = scheduleRepository.findById(oId);

            if (schedule == null ||
                    !schedule.getObjectId("user_id").equals(userId)
            )
                return JSON_NOT_VALID_PARAMS;

            if(advisorId != null && schedule.containsKey("advisors") && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
                schedule.getList("advisors", ObjectId.class).add(advisorId);
            }


        } else {

            if (!data.has("scheduleFor"))
                return JSON_NOT_VALID_PARAMS;

            int scheduleFor = data.getInt("scheduleFor");

            if (scheduleFor < 0 || scheduleFor > 4)
                return JSON_NOT_VALID_PARAMS;

            String weekStartAt;

            if (scheduleFor == 0)
                weekStartAt = getFirstDayOfCurrWeek();
            else
                weekStartAt = getFirstDayOfFutureWeek(scheduleFor);

            schedule = scheduleRepository.findOne(
                    and(
                            eq("user_id", userId),
                            eq("week_start_at", weekStartAt)
                    ), null
            );

            if(schedule == null) {
                schedule = new Document("user_id", userId)
                        .append("week_start_at", weekStartAt)
                        .append("week_start_at_int", Utility.convertStringToDate(weekStartAt))
                        .append("days", new ArrayList<>())
                        .append("advisors", new ArrayList<>(){{add(advisorId);}});
            }
            else {
                schedule = scheduleRepository.findById(schedule.getObjectId("_id"));
                if(advisorId != null && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
                    schedule.getList("advisors", ObjectId.class).add(advisorId);
                }
            }
        }

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        List<Document> items = doc == null ? new ArrayList<>() :
                doc.getList("items", Document.class);

        ObjectId newId = new ObjectId();
        Document newDoc = new Document("_id", newId)
                .append("tag", tag.getString("label"))
                .append("advisor_id", advisorId)
                .append("created_at", System.currentTimeMillis())
                .append("lesson", lesson.getString("name"))
                .append("duration", data.getInt("duration"));

        if (data.has("startAt"))
            newDoc.put("start_at", data.getString("startAt"));

        if (data.has("description"))
            newDoc.put("description", data.getString("description"));

        if(tag.containsKey("number_label")) {
            newDoc.put("additional", data.getInt("additional"));
            newDoc.put("additional_label", tag.getString("number_label"));
        }

        items.add(newDoc);

        if (doc == null)
            days.add(new Document("day", dayIndex)
                    .append("items", items)
            );

        schedule.put("days", days);

        if (data.has("id"))
            scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
        else {
            ObjectId scheduleId = scheduleRepository.insertOneWithReturnId(schedule);
            return generateSuccessMsg("data", new JSONObject()
                    .put("scheduleId", scheduleId.toString())
                    .put("id", newId.toString())
            );
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("id", newId.toString())
        );
    }

    private static PairValue checkUpdatable(ObjectId advisorId, ObjectId userId,
                                            ObjectId id, boolean delete) throws InvalidFieldsException {

        Document doc = scheduleRepository.findOne(
                and(
                        eq("user_id", userId),
                        eq("days.items._id", id)
                ), null
        );

        if (doc == null)
            throw new InvalidFieldsException("not access");

        String firstDayOfWeek = getFirstDayOfCurrWeek();

        if (!doc.getString("week_start_at").equals(firstDayOfWeek)) {

            int d = Utility.convertStringToDate(doc.getString("week_start_at"));
            int today = Utility.getToday();

            if (today > d)
                throw new InvalidFieldsException("زمان ویرایش/حذف به اتمام رسیده است");
        }

        List<Document> days = doc.getList("days", Document.class);
        for (Document day : days) {

            if (!day.containsKey("items"))
                continue;

            List<Document> items = day.getList("items", Document.class);
            int idx = searchInDocumentsKeyValIdx(
                    items, "_id", id
            );

            if (idx == -1)
                continue;

            if (!items.get(idx).getObjectId("advisor_id").equals(advisorId))
                throw new InvalidFieldsException("not access");

            if (delete)
                items.remove(idx);

            return new PairValue(doc, delete ? null : items.get(idx));
        }

        throw new InvalidFieldsException("unknown err");
    }

    public static String removeItemFromSchedule(ObjectId advisorId,
                                                ObjectId userId,
                                                ObjectId id) {
        try {

            PairValue p = checkUpdatable(advisorId, userId, id, true);
            Document doc = (Document) p.getKey();

            scheduleRepository.replaceOne(
                    doc.getObjectId("_id"), doc
            );

            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    static class TagStat {

        int total;
        int done;
        int additionalTotal;
        int additionalDone;
        String additionalLabel;

        public TagStat(int total, int done, int additionalTotal, int additionalDone,
                       String additionalLabel) {
            this.total = total;
            this.done = done;
            this.additionalTotal = additionalTotal;
            this.additionalDone = additionalDone;
            this.additionalLabel = additionalLabel;
        }

        public JSONObject toJSON() {

            JSONObject jsonObject = new JSONObject()
                    .put("total", total)
                    .put("done", done);

            if(!additionalLabel.isEmpty()) {
                jsonObject.put("additionalLabel", additionalLabel)
                        .put("additionalTotal", additionalTotal)
                        .put("additionalDone", additionalDone);
            }

            return jsonObject;
        }

    }

    static class LessonStat {

        int total;
        int done;
        HashMap<String, TagStat> tagStats;

        public LessonStat(int total, int done) {
            this.total = total;
            this.done = done;
            tagStats = new HashMap<>();
        }

        public JSONObject toJSON() {

            JSONObject jsonObject = new JSONObject()
                    .put("total", total)
                    .put("done", done);

            JSONArray jsonArray = new JSONArray();
            for(String key : tagStats.keySet())
                jsonArray.put(new JSONObject()
                        .put("tag", key)
                        .put("stats", tagStats.get(key).toJSON())
                );

            jsonObject.put("tags", jsonArray);

            return jsonObject;
        }
    }

    public static String lessonsInSchedule(ObjectId advisorId, ObjectId scheduleId) {

        Document schedule = scheduleRepository.findById(scheduleId);
        if(schedule == null)
            return JSON_NOT_VALID_ID;

        if(!Authorization.hasAccessToThisStudent(schedule.getObjectId("user_id"), advisorId))
            return JSON_NOT_ACCESS;

        HashMap<String, LessonStat> lessonStats = new HashMap<>();

        for(Document day : schedule.getList("days", Document.class)) {

            for(Document item : day.getList("items", Document.class)) {

                if(!item.containsKey("lesson")) continue;

                String lesson = item.getString("lesson");
                String tag = item.getString("tag");

                if(lessonStats.containsKey(lesson)) {

                    LessonStat lessonStat = lessonStats.get(lesson);

                    lessonStat.total += item.getInteger("duration");
                    lessonStat.done += (int)item.getOrDefault("done_duration", 0);

                    if(lessonStat.tagStats.containsKey(tag)) {
                        TagStat tagStat = lessonStat.tagStats.get(tag);
                        tagStat.total += item.getInteger("duration");
                        tagStat.done += (int) item.getOrDefault("done_duration", 0);
                        if(item.containsKey("additional")) {
                            tagStat.additionalTotal += item.getInteger("additional");
                            tagStat.additionalDone += item.getInteger("done_additional");
                        }
                    }
                    else {
                        lessonStat.tagStats.put(tag, new TagStat(
                                item.getInteger("duration"),
                                (int) item.getOrDefault("done_duration", 0),
                                (int) item.getOrDefault("additional", 0),
                                (int) item.getOrDefault("done_additional", 0),
                                (String) item.getOrDefault("additional_label", "")
                        ));
                    }

                }
                else {

                    LessonStat lessonStat = new LessonStat(
                            item.getInteger("duration"),
                            (int)item.getOrDefault("done_duration", 0)
                    );

                    TagStat tagStat = new TagStat(
                            item.getInteger("duration"),
                            (int)item.getOrDefault("done_duration", 0),
                            (int)item.getOrDefault("additional", 0),
                            (int)item.getOrDefault("done_additional", 0),
                            (String) item.getOrDefault("additional_label", "")
                    );

                    lessonStat.tagStats.put(tag, tagStat);
                    lessonStats.put(lesson, lessonStat);
                }

            }

        }

        JSONArray jsonArray = new JSONArray();

        for(String key : lessonStats.keySet()) {

            jsonArray.put(new JSONObject()
                    .put("lesson", key)
                    .put("stats", lessonStats.get(key).toJSON())
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

//    public static String updateItem(ObjectId advisorId,
//                                    ObjectId userId,
//                                    ObjectId id,
//                                    JSONObject data) {
//
//        int duration = data.getInt("duration");
//        if (duration < 15 || duration > 240)
//            return generateErr("زمان هر برنامه باید بین 15 الی 240 دقیقه باشد");
//
//        ObjectId tagId = new ObjectId(data.getString("tag"));
//        Document tag = adviseTagRepository.findById(tagId);
//        if (tag == null || tag.containsKey("deleted_at"))
//            return JSON_NOT_VALID_ID;
//
//        ObjectId subjectId = new ObjectId(data.getString("subjectId"));
//        Document subject = subjectRepository.findById(subjectId);
//        if (subject == null)
//            return JSON_NOT_VALID;
//
//        try {
//
//            PairValue p = checkUpdatable(advisorId, userId, id, false);
//
//            Document doc = (Document) p.getKey();
//            Document item = (Document) p.getValue();
//
//            item.put("tag", tag.getString("label"));
//            item.put("subject", subject.getString("name"));
//            item.put("duration", data.getInt("duration"));
//
//            if (data.has("startAt"))
//                item.put("start_at", data.getString("start_at"));
//
//            if (data.has("description"))
//                item.put("description", data.getString("description"));
//
//            scheduleRepository.replaceOne(doc.getObjectId("_id"), doc);
//
//        } catch (InvalidFieldsException e) {
//            return generateErr(e.getMessage());
//        }
//
//        return JSON_OK;
//    }

    public static String getStudentSchedules(ObjectId advisorId,
                                             ObjectId studentId,
                                             Boolean notReturnPassed) {

        int today = getToday();

        List<Document> schedules = scheduleRepository.find(
                and(
                        eq("user_id", studentId)
                ), null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document schedule : schedules) {

            if (notReturnPassed != null) {

                int d = convertStringToDate(schedule.getString("week_start_at"));

                if (notReturnPassed && today - d > 7)
                    continue;

                if (!notReturnPassed && today - d < 7)
                    continue;
            }

            jsonArray.put(convertSchedulesToJSONObject(schedule, advisorId));
        }

        JSONObject jsonObject = new JSONObject()
                .put("items", jsonArray);

        if(advisorId != null) {
            Document user = userRepository.findById(studentId);
            if(user != null)
                jsonObject.put("student",
                        new JSONObject()
                                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                                .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                );
        }

        return generateSuccessMsg("data", jsonObject);
    }

    public static String removeSchedule(ObjectId advisorId, ObjectId id) {

        Document schedule = scheduleRepository.findById(id);
        if(schedule == null)
            return JSON_NOT_VALID_ID;

        List<Document> days = schedule.getList("days", Document.class);

        for (Document day : days) {

            if(!day.containsKey("items"))
                continue;

            for(Document item : day.getList("items", Document.class)) {
                if(!item.getObjectId("advisor_id").equals(advisorId))
                    return generateErr("شما تنها مشاور این کاربرگ نیستید و امکان حذف این کاربرگ برای شما وجود ندارد");
            }
        }

        scheduleRepository.deleteOne(schedule.getObjectId("_id"));

        return JSON_OK;
    }
}
