package irysc.gachesefid.Controllers.Advisor;


import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.YesOrNo;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Advisor.Utility.*;
import static irysc.gachesefid.Controllers.UserController.fillJSONWithEducationalHistory;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.SkyRoomUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdvisorController {

    public static File exportPDF(ObjectId id, ObjectId advisorId, ObjectId userId) {

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return null;

        if (advisorId != null && !schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return null;

        if (userId != null && !schedule.getObjectId("user_id").equals(userId))
            return null;

        return PDFUtils.exportSchedule(
                schedule,
                userRepository.findById(schedule.getObjectId("user_id"))
        );

    }

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
                        eq("student_id", studentId),
                        gte("created_at", firstDayOfMonth)
                )
        );

        int advisorMeetingsInLastMonth = advisorMeetingRepository.count(
                and(
                        eq("advisor_id", advisorId),
                        eq("student_id", studentId),
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
                    .put("maxExam", request.getOrDefault("max_exam", "نامحدود"))
                    .put("createdAt", getSolarDate(request.getLong("paid_at")))
                    .put("finishAt", getSolarDate(request.getLong("paid_at") + ONE_DAY_MIL_SEC * 30));
        }


        return generateSuccessMsg("data", output);
    }

    public static String getStudentsDigest(Document advisor) {

        List<Document> students = advisor.getList("students", Document.class);
        JSONArray jsonArray = new JSONArray();

        for (Document student : students) {

            Document studentDoc = userRepository.findById(student.getObjectId("_id"));

            if (studentDoc == null)
                continue;

            jsonArray.put(new JSONObject()
                    .put("name", studentDoc.getString("first_name") + " " + studentDoc.getString("last_name"))
                    .put("id", studentDoc.getObjectId("_id").toString())
            );

        }

        return generateSuccessMsg("data", jsonArray);
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
            return generateErr("تعداد تماس\u200Cهای تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        if (data.has("videoLink") && !isValidURL(data.getString("videoLink")))
            return generateErr("لینک وارد شده نامعتبر است");

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

        if (data.has("videoLink"))
            newDoc.append("video_link", data.getString("videoLink"));

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
            return generateErr("تعداد تماس\u200Cهای تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        if (data.has("videoLink") && !isValidURL(data.getString("videoLink")))
            return generateErr("لینک وارد شده نامعتبر است");

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

        if (data.has("videoLink"))
            doc.put("video_link", data.getString("videoLink"));
        else
            doc.remove("video_link");

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

        if (docs.size() == 0 && fullAccess) {
            Document config = getConfig();
            return generateSuccessMsg("data", new JSONObject()
                    .put("data", jsonArray)
                    .put("maxVideoCalls", config.getInteger("max_video_call_per_month"))
                    .put("minPrice", config.getInteger("min_advice_price"))
            );
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

        return generateSuccessMsg("data", new JSONObject()
                .put("data", jsonArray)
        );
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

        String roomUrl = "consulting-" + curr;

        int roomId = createMeeting("جلسه مشاوره " + name + " - " + studentName, roomUrl, 2, false);
        if (roomId == -1)
            return generateErr("امکان ساخت اتاق جلسه در حال حاضر وجود ندارد");

        String url = SKY_ROOM_PUBLIC_URL + roomUrl;

        Document document = new Document("advisor_id", advisorId)
                .append("student_id", studentId)
                .append("created_at", curr)
                .append("room_id", roomId)
                .append("url", url)
                .append("advisor_sky_id", advisorSkyRoomId)
                .append("student_sky_id", studentSkyRoomId);

        advisorMeetingRepository.insertOne(document);
        addUserToClass(Collections.singletonList(studentSkyRoomId), advisorSkyRoomId, roomId);

        createNotifAndSendSMS(std, url, "createRoom");
        return generateSuccessMsg("url", url);
    }

    public static String getMyCurrentRoomForAdvisor(ObjectId advisorId) {

        long curr = System.currentTimeMillis();
        long timeLimit = curr - ONE_HOUR_MIL_SEC * 5;

        List<Document> docs = advisorMeetingRepository.find(and(
                eq("advisor_id", advisorId),
                gt("created_at", timeLimit),
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


    public static String getMyCurrentRoomForAdvisor(
            ObjectId advisorId, ObjectId studentId
    ) {

        long curr = System.currentTimeMillis();
        long limitTime = curr - ONE_HOUR_MIL_SEC * 5;

        Document doc = advisorMeetingRepository.findOne(and(
                eq("advisor_id", advisorId),
                eq("student_id", studentId),
                gt("created_at", limitTime),
                lt("created_at", curr),
                exists("url")
        ), new BasicDBObject("url", 1).append("student_id", 1).append("created_at", 1));

        if (doc == null)
            return generateSuccessMsg("url", "");

        return generateSuccessMsg("url", doc.getString("url"));
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

    public static String cancel(Document user, ObjectId advisorId) {

        if (!user.containsKey("my_advisors"))
            return JSON_NOT_ACCESS;

        List<ObjectId> myAdvisors = user.getList("my_advisors", ObjectId.class);
        if (!myAdvisors.contains(advisorId))
            return JSON_NOT_VALID_ID;

        Document advisor = userRepository.findById(advisorId);

        if (advisor != null) {

            List<Document> students = advisor.getList("students", Document.class);
            int idx = searchInDocumentsKeyValIdx(students, "_id", user.getObjectId("_id"));

            if (idx > -1) {
                students.remove(idx);
                userRepository.replaceOne(advisorId, advisor);
            }
        }

        myAdvisors.remove(advisorId);
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
                        .put("userMoney", userMoney.intValue())
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

        if (!planId.equals("-1") && !ObjectId.isValid(planId))
            return JSON_NOT_VALID_PARAMS;

        ObjectId planOId = planId.equals("-1") ? null : new ObjectId(planId);

        Document advisor = userRepository.findById(advisorId);
        if (advisor == null)
            return JSON_NOT_VALID_ID;

        if (user.containsKey("my_advisors") &&
                user.getList("my_advisors", ObjectId.class).contains(advisorId))
            return generateErr("مشاور موردنظر هم اکنون به عنوان مشاور شما می باشد");

        if (advisorRequestsRepository.count(and(
                eq("answer", "pending"),
                eq("user_id", user.getObjectId("_id")),
                eq("advisor_id", advisorId)
        )) > 0)
            return generateErr("شما در حال حاضر درخواست تعیین وضعیت نشده ای برای این مشاور دارید");

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
                return generateErr("لطفا یکی از بسته\u200Cهای پیشنهادی را انتخاب نمایید");

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

        createNotifAndSendSMS(advisor,
                user.getString("first_name") + " " + user.getString("last_name"),
                "request"
        );

        JSONObject jsonObject = new JSONObject()
                .put("advisorId", advisorId.toString())
                .put("id", id.toString())
                .put("answer", "pending");

        return generateSuccessMsg("data", jsonObject);

    }

    private static boolean cancelAdvisor(Document student, Document advisor,
                                         boolean needUpdateStudent) {

        if (student.containsKey("my_advisors") &&
                student.getList("my_advisors", ObjectId.class).contains(advisor.getObjectId("_id"))
        ) {

            student.getList("my_advisors", ObjectId.class).remove(advisor.getObjectId("_id"));

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

    public static String notifyStudentForSchedule(ObjectId scheduleId,
                                                  String advisorName,
                                                  ObjectId advisorId
    ) {

        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.containsKey("advisors") ||
                !schedule.getList("advisors", ObjectId.class).contains(advisorId)
        )
            return JSON_NOT_ACCESS;

        Document student = userRepository.findById(schedule.getObjectId("user_id"));
        if (student == null)
            return JSON_NOT_UNKNOWN;

        createNotifAndSendSMS(student, advisorName, "karbarg");
        return JSON_OK;
    }

    public static void setAdvisor(Document student, Document advisor) {

        List<ObjectId> myAdvisors = (List<ObjectId>) student.getOrDefault("my_advisors", new ArrayList<>());
        myAdvisors.add(advisor.getObjectId("_id"));

        student.put("my_advisors", myAdvisors);
        userRepository.replaceOneWithoutClearCache(student.getObjectId("_id"), student);

        List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());

        String studentName = student.getString("first_name") + " " + student.getString("last_name");
        createNotifAndSendSMS(advisor, studentName, "finalize");

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
            req.put("answer", "accept");
            req.put("answer_at", System.currentTimeMillis());
        }

        Document user = userRepository.findById(req.getObjectId("user_id"));
        if (user != null) {
            createNotifAndSendSMS(
                    user,
                    advisor.getString("first_name") + " " + advisor.getString("last_name"),
                    answer.equalsIgnoreCase(YesOrNo.NO.getName()) ? "rejectRequest" : "acceptRequest"
            );
        }

        advisorRequestsRepository.replaceOne(reqId, req);
        return JSON_OK;
    }


    public static String toggleStdAcceptance(Document user) {

        user.put("accept_std", !(boolean) user.getOrDefault("accept_std", true));
        userRepository.replaceOne(user.getObjectId("_id"), user);

        return JSON_OK;

    }

    public static String getAllAdvisors(Integer minAge,
                                        Integer maxAge,
                                        String tag,
                                        Integer minPrice,
                                        Integer maxPrice,
                                        Integer minRate,
                                        Integer maxRate,
                                        Boolean returnFilters,
                                        String sortBy
    ) {

        if (sortBy != null &&
                !sortBy.equalsIgnoreCase("age") &&
                !sortBy.equalsIgnoreCase("rate") &&
                !sortBy.equalsIgnoreCase("student")
        )
            return JSON_NOT_VALID_PARAMS;

        ArrayList<Bson> filters = new ArrayList<>();

        filters.add(eq("accesses", Access.ADVISOR.getName()));
        filters.add(exists("advice"));

        if (tag != null) {
            filters.add(exists("tags"));
            filters.add(eq("tags", tag));
        }

        if (minRate != null)
            filters.add(and(
                    exists("rate"),
                    gte("rate", minRate)
            ));

        if (maxRate != null)
            filters.add(and(
                    exists("rate"),
                    lte("rate", maxRate)
            ));

        if (minAge != null) {
            long age = System.currentTimeMillis() - minAge * ONE_DAY_MIL_SEC * 365;
            filters.add(lte("birth_day", age));
        }

        if (maxAge != null) {
            long age = System.currentTimeMillis() - maxAge * ONE_DAY_MIL_SEC * 365;
            filters.add(gte("birth_day", age));
        }

        List<Document> advisors = userRepository.find(
                and(filters),
                ADVISOR_PUBLIC_DIGEST
        );

        boolean isAllFiltersOff = (returnFilters == null || returnFilters) && maxAge == null && minAge == null &&
                tag == null && maxPrice == null && minPrice == null && minRate == null && maxRate == null;

        Document config = getConfig();
        int defaultPrice = config.getInteger("min_advice_price");

        int minAgeFilter = -1, maxAgeFilter = -1, minPriceFilter = defaultPrice, maxPriceFilter = defaultPrice;
        long curr = System.currentTimeMillis();
        long one_year_ms = ONE_DAY_MIL_SEC * 365;

        List<JSONObject> docs = new ArrayList<>();

        for (Document advisor : advisors) {

            List<Document> plans = advisorFinanceOfferRepository.find(
                    eq("advisor_id", advisor.getObjectId("_id")),
                    new BasicDBObject("price", 1)
            );

//            if(plans.size() == 0)
//                continue;

            if (minPrice != null || maxPrice != null) {
                if (plans.size() == 0 && (
                        (minPrice != null && defaultPrice < minPrice) ||
                                (maxPrice != null && defaultPrice > maxPrice)
                ))
                    continue;

                else if (plans.size() > 0) {
                    boolean passFilter = false;
                    for (Document plan : plans) {

                        int p = (int) plan.getOrDefault("price", defaultPrice);

                        if (minPrice != null && p < minPrice)
                            continue;

                        if (maxPrice != null && p > maxPrice)
                            continue;

                        passFilter = true;
                        break;
                    }

                    if (!passFilter)
                        continue;
                }

            }

            JSONObject jsonObject = convertToJSONDigest(null, advisor);
            jsonObject.put("advisorPriority", advisor.getOrDefault("advisor_priority", 1000));

            int age = -1;
            if (advisor.containsKey("birth_day")) {
                age = (int) ((curr - advisor.getLong("birth_day")) / one_year_ms);
                jsonObject.put("age", age);
            }

            docs.add(jsonObject);

            if (isAllFiltersOff) {

                if (age != -1) {

                    if (minAgeFilter == -1 || minAgeFilter > age)
                        minAgeFilter = age;

                    if (maxAgeFilter == -1 || maxAgeFilter < age)
                        maxAgeFilter = age;

                }

                plans = advisorFinanceOfferRepository.find(
                        eq("advisor_id", advisor.getObjectId("_id")),
                        new BasicDBObject("price", 1)
                );

                for (Document plan : plans) {

                    int price = (int) plan.getOrDefault("price", defaultPrice);

                    if (minPriceFilter == -1 || minPriceFilter > price)
                        minPriceFilter = price;

                    if (maxPriceFilter == -1 || maxPriceFilter < price)
                        maxPriceFilter = price;

                }

            }
        }

        String sortKey = sortBy == null || sortBy.equalsIgnoreCase("rate") ? "rate" :
                sortBy.equalsIgnoreCase("age") ? "age" : "stdCount";

        docs.sort((o1, o2) -> {
            int a = o1.has(sortKey) ? o1.getInt(sortKey) : -1;
            int b = o2.has(sortKey) ? o2.getInt(sortKey) : -1;
            if (a == b) return o1.getInt("advisorPriority") - o2.getInt("advisorPriority");
            return b - a;
        });

        JSONArray jsonArray = new JSONArray();

        for (JSONObject jsonObject : docs)
            jsonArray.put(jsonObject);

        if (isAllFiltersOff)
            return generateSuccessMsg("data", new JSONObject()
                    .put("data", jsonArray)
                    .put("filters", new JSONObject()
                            .put("minPrice", minPriceFilter)
                            .put("maxPrice", maxPriceFilter)
                            .put("minAge", minAgeFilter)
                            .put("maxAge", maxAgeFilter)
                    )
            );

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

        if (jsonObject.has("numberLabel"))
            newDoc.append("number_label", jsonObject.getString("numberLabel"));

        return db.insertOneWithReturn(newDoc);
    }

    public static String editTag(Common db, ObjectId id, JSONObject jsonObject) {

        Document doc = db.findById(id);
        if (doc == null)
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

        if (jsonObject.has("numberLabel"))
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

            if (tag.containsKey("number_label"))
                jsonObject.put("numberLabel", tag.getString("number_label"));

            jsonArray.put(jsonObject);
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    private static void createNewScheduleFromExistProgram(ObjectId userId, ObjectId advisorId,
                                                          String weekStartAt, HashMap<Integer, List<Document>> items) {

        Document schedule = new Document("user_id", userId)
                .append("week_start_at", weekStartAt)
                .append("week_start_at_int", Utility.convertStringToDate(weekStartAt))
                .append("advisors", new ArrayList<>() {{
                    add(advisorId);
                }});

        List<Document> days = new ArrayList<>();

        for (Integer day : items.keySet())
            days.add(new Document("day", day).append("items", items.get(day)));

        schedule.append("days", days);
        scheduleRepository.insertOne(schedule);

    }

    private static void mergeScheduleFromExistProgram(Document schedule, ObjectId advisorId,
                                                      HashMap<Integer, List<Document>> items) {

        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            schedule.getList("advisors", ObjectId.class).add(advisorId);

        List<Document> days = schedule.getList("days", Document.class);

        for (Integer day : items.keySet()) {

            Document dayDoc = searchInDocumentsKeyVal(days, "day", day);
            if (dayDoc == null)
                days.add(new Document("day", day).append("items", items.get(day)));
            else
                dayDoc.getList("items", Document.class).addAll(items.get(day));
        }

        schedule.put("days", days);
        scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
    }

    public static String copy(ObjectId advisorId, ObjectId scheduleId,
                              JSONArray users, int scheduleFor) {

        if (scheduleFor > 4 || scheduleFor < 0)
            return JSON_NOT_VALID_PARAMS;

        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return JSON_NOT_ACCESS;

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        List<ObjectId> students = new ArrayList<>();

        for (int i = 0; i < users.length(); i++) {

            String id = users.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(users.getString(i));

            if (!Authorization.hasAccessToThisStudent(oId, advisorId)) {
                excepts.put(i + 1);
                continue;
            }

            students.add(oId);
            doneIds.put(id);
        }

        if (students.size() == 0)
            return JSON_NOT_VALID_PARAMS;

        long curr = System.currentTimeMillis();
        HashMap<Integer, List<Document>> items = new HashMap<>();

        for (Document day : schedule.getList("days", Document.class)) {

            List<Document> tmp = new ArrayList<>();

            for (Document item : day.getList("items", Document.class)) {

                if (!item.getObjectId("advisor_id").equals(advisorId))
                    continue;

                Document newDoc = Document.parse(item.toJson());
                newDoc.put("created_at", curr);
                newDoc.remove("done_duration");
                newDoc.remove("done_additional");

                tmp.add(newDoc);
            }

            items.put(day.getInteger("day"), tmp);
        }

        if (items.size() == 0)
            return JSON_NOT_ACCESS;

        String weekStartAt;

        if (scheduleFor == 0)
            weekStartAt = getFirstDayOfCurrWeek();
        else
            weekStartAt = getFirstDayOfFutureWeek(scheduleFor);

        for (ObjectId studentId : students) {

            Document sc = scheduleRepository.findOne(
                    and(
                            eq("user_id", studentId),
                            eq("week_start_at", weekStartAt)
                    ), null
            );

            if (sc == null)
                createNewScheduleFromExistProgram(studentId, advisorId, weekStartAt, items);
            else
                mergeScheduleFromExistProgram(sc, advisorId, items);
        }


        return returnAddResponse(excepts, doneIds);
    }

    public static String setScheduleDesc(ObjectId advisorId, ObjectId id, String desc) {

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (!schedule.getList("advisors", ObjectId.class).contains(advisorId))
            return JSON_NOT_ACCESS;

        List<Document> advisorsDesc = (List<Document>) schedule.getOrDefault("advisors_desc", new ArrayList<>());

        Document advisorDesc = searchInDocumentsKeyVal(advisorsDesc, "advisor_id", advisorId);

        if (advisorDesc == null)
            advisorsDesc.add(new Document("advisor_id", advisorId)
                    .append("description", desc));
        else
            advisorDesc.put("description", desc);

        if (!schedule.containsKey("advisors_desc"))
            schedule.put("advisors_desc", advisorsDesc);

        scheduleRepository.replaceOne(id, schedule);

        return JSON_OK;
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

        if (tag.containsKey("number_label") && !data.has("additional"))
            return generateErr("لطفا " + tag.getString("number_label") + " را وارد نمایید");

        ObjectId lessonId = new ObjectId(data.getString("lessonId"));
        Document grade = gradeRepository.findOne(eq("lessons._id", lessonId), null);
        if (grade == null) {

            grade = branchRepository.findOne(eq("lessons._id", lessonId), null);

            if (grade == null)
                return JSON_NOT_VALID;
        }

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

            if (advisorId != null && schedule.containsKey("advisors") && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
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

            if (schedule == null) {
                schedule = new Document("user_id", userId)
                        .append("week_start_at", weekStartAt)
                        .append("week_start_at_int", Utility.convertStringToDate(weekStartAt))
                        .append("days", new ArrayList<>())
                        .append("advisors", new ArrayList<>() {{
                            add(advisorId);
                        }});
            } else {
                schedule = scheduleRepository.findById(schedule.getObjectId("_id"));
                if (advisorId != null && !schedule.getList("advisors", ObjectId.class).contains(advisorId)) {
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

        if (tag.containsKey("number_label")) {
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

    public static String updateScheduleItem(ObjectId advisorId, ObjectId itemId, JSONObject data) {

        int duration = data.getInt("duration");
        if (duration < 15 || duration > 240)
            return generateErr("زمان هر برنامه باید بین 15 الی 240 دقیقه باشد");

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = adviseTagRepository.findById(tagId);
        if (tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        if (tag.containsKey("number_label") && !data.has("additional"))
            return generateErr("لطفا " + tag.getString("number_label") + " را وارد نمایید");

        Document schedule = scheduleRepository.findOne(eq("days.items._id", itemId), null);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        schedule = scheduleRepository.findById(schedule.getObjectId("_id"));

        Document item = null;

        for (Document day : schedule.getList("days", Document.class)) {
            item = searchInDocumentsKeyVal(day.getList("items", Document.class), "_id", itemId);
            if (item != null) break;
        }

        if (item == null)
            return JSON_NOT_UNKNOWN;

        if (!item.getObjectId("advisor_id").equals(advisorId))
            return JSON_NOT_ACCESS;

        item.put("tag", tag.getString("label"));
        item.put("duration", data.getInt("duration"));

        if (data.has("startAt"))
            item.put("start_at", data.getString("startAt"));

        if (data.has("description"))
            item.put("description", data.getString("description"));

        if (tag.containsKey("number_label")) {
            item.put("additional", data.getInt("additional"));
            item.put("additional_label", tag.getString("number_label"));
        }

        scheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);
        return JSON_OK;
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

            if (!additionalLabel.isEmpty()) {
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
            for (String key : tagStats.keySet())
                jsonArray.put(new JSONObject()
                        .put("tag", key)
                        .put("stats", tagStats.get(key).toJSON())
                );

            jsonObject.put("tags", jsonArray);

            return jsonObject;
        }
    }

    private static HashMap<String, LessonStat> fetchLessonStats(Document schedule) {

        HashMap<String, LessonStat> lessonStats = new HashMap<>();

        for (Document day : schedule.getList("days", Document.class)) {

            for (Document item : day.getList("items", Document.class)) {

                if (!item.containsKey("lesson")) continue;

                String lesson = item.getString("lesson");
                String tag = item.getString("tag");

                if (lessonStats.containsKey(lesson)) {

                    LessonStat lessonStat = lessonStats.get(lesson);

                    lessonStat.total += item.getInteger("duration");
                    lessonStat.done += (int) item.getOrDefault("done_duration", 0);

                    if (lessonStat.tagStats.containsKey(tag)) {
                        TagStat tagStat = lessonStat.tagStats.get(tag);
                        tagStat.total += item.getInteger("duration");
                        tagStat.done += (int) item.getOrDefault("done_duration", 0);
                        if (item.containsKey("additional")) {
                            tagStat.additionalTotal += item.getInteger("additional");
                            tagStat.additionalDone += (int) item.getOrDefault("done_additional", 0);
                        }
                    } else {
                        lessonStat.tagStats.put(tag, new TagStat(
                                item.getInteger("duration"),
                                (int) item.getOrDefault("done_duration", 0),
                                (int) item.getOrDefault("additional", 0),
                                (int) item.getOrDefault("done_additional", 0),
                                (String) item.getOrDefault("additional_label", "")
                        ));
                    }

                } else {

                    LessonStat lessonStat = new LessonStat(
                            item.getInteger("duration"),
                            (int) item.getOrDefault("done_duration", 0)
                    );

                    TagStat tagStat = new TagStat(
                            item.getInteger("duration"),
                            (int) item.getOrDefault("done_duration", 0),
                            (int) item.getOrDefault("additional", 0),
                            (int) item.getOrDefault("done_additional", 0),
                            (String) item.getOrDefault("additional_label", "")
                    );

                    lessonStat.tagStats.put(tag, tagStat);
                    lessonStats.put(lesson, lessonStat);
                }

            }
        }

        return lessonStats;
    }

    public static String lessonsInSchedule(ObjectId advisorId, ObjectId scheduleId, boolean isAdvisor) {

        Document schedule = scheduleRepository.findById(scheduleId);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        if (isAdvisor &&
                !Authorization.hasAccessToThisStudent(schedule.getObjectId("user_id"), advisorId))
            return JSON_NOT_ACCESS;

        if (!isAdvisor && !schedule.getObjectId("user_id").equals(advisorId))
            return JSON_NOT_ACCESS;


        JSONArray jsonArray = new JSONArray();
        HashMap<String, LessonStat> lessonStats = fetchLessonStats(schedule);

        for (String key : lessonStats.keySet()) {

            jsonArray.put(new JSONObject()
                    .put("lesson", key)
                    .put("stats", lessonStats.get(key).toJSON())
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static class WeeklyStat {

        HashMap<String, LessonStat> lessonStats;
        String weekStartAt;

        public WeeklyStat(String weekStartAt, HashMap<String, LessonStat> lessonStatHashMap) {
            this.weekStartAt = weekStartAt;
            lessonStats = lessonStatHashMap;
        }
    }

    public static String progress(ObjectId userId, ObjectId lessonId, Long start, Long end) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("user_id", userId));

        if (start != null) {
            String[] splited = getSolarDate(start).split("-")[0].replace(" ", "").split("\\/");

            String date = "";

            String y = splited[0];
            date += y;

            int month = Integer.parseInt(splited[1]);

            if (month < 10)
                date += "0" + month;
            else
                date += month;

            int day = Integer.parseInt(splited[2]);
            if (day < 10)
                date += "0" + day;
            else
                date += day;

            filters.add(gte("week_start_at_int", Integer.parseInt(date)));
        }

        if (end != null) {

            String[] splited = getSolarDate(end).split("-")[0].replace(" ", "").split("\\/");

            String date = "";

            String y = splited[0];
            date += y;

            int month = Integer.parseInt(splited[1]);

            if (month < 10)
                date += "0" + month;
            else
                date += month;

            int day = Integer.parseInt(splited[2]);
            if (day < 10)
                date += "0" + day;
            else
                date += day;

            filters.add(lte("week_start_at_int", Integer.parseInt(date)));
        }

        List<Document> schedules = scheduleRepository.find(and(filters), null);
        List<WeeklyStat> weeklyStats = new ArrayList<>();

        List<Integer> dailyTotalSum = new ArrayList<>();
        List<Integer> dailyDoneSum = new ArrayList<>();
        List<String> daily = new ArrayList<>();

        schedules.sort(Comparator.comparing(o -> o.getString("week_start_at")));

        for (Document schedule : schedules) {

            String weekStartAt = schedule.getString("week_start_at");

            weeklyStats.add(
                    new WeeklyStat(weekStartAt,
                            fetchLessonStats(schedule)
                    ));

            String[] splited = weekStartAt.split("\\/");

            JalaliCalendar jalaliCalendar = new JalaliCalendar(
                    Integer.parseInt(splited[0]), Integer.parseInt(splited[1]), Integer.parseInt(splited[2])
            );

            int added = 0;
            List<Document> days = schedule.getList("days", Document.class);

            for (int k = days.size() - 1; k >= 0; k--) {

                if (daily.size() >= 14)
                    break;

                Document day = days.get(k);

                if (day.getInteger("day").equals(0))
                    daily.add(weekStartAt);
                else {
                    added = day.getInteger("day") - added;
                    jalaliCalendar.add(Calendar.DAY_OF_MONTH, added);
                    daily.add(jalaliCalendar.get(Calendar.YEAR) + "/" + jalaliCalendar.get(Calendar.MONTH) + "/" + jalaliCalendar.get(Calendar.DAY_OF_MONTH));
                }

                int totalSum = 0;
                int doneSum = 0;

                for (Document item : day.getList("items", Document.class)) {

                    doneSum += (int) item.getOrDefault("done_duration", 0);
                    totalSum += (int) item.getOrDefault("duration", 0);

                }

                dailyDoneSum.add(doneSum);
                dailyTotalSum.add(totalSum);

            }

        }

        weeklyStats.sort(Comparator.comparing(o -> o.weekStartAt));

        List<String> allLessons = new ArrayList<>();
        List<String> allTags = new ArrayList<>();
        HashMap<String, List<String>> allLessonTags = new HashMap<>();

        for (WeeklyStat weeklyStat : weeklyStats) {

            HashMap<String, LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            for (String key : lessonStatHashMap.keySet()) {

                if (!allLessons.contains(key))
                    allLessons.add(key);

                List<String> tmp = allLessonTags.containsKey(key) ? allLessonTags.get(key) : new ArrayList<>();

                for (String t : lessonStatHashMap.get(key).tagStats.keySet()) {
                    if (!tmp.contains(t))
                        tmp.add(t);

                    if (!allTags.contains(t))
                        allTags.add(t);
                }

                if (!allLessonTags.containsKey(key))
                    allLessonTags.put(key, tmp);
            }

        }

        for (WeeklyStat weeklyStat : weeklyStats) {

            HashMap<String, LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            for (String key : allLessons) {

                List<String> lessonTags = allLessonTags.get(key);

                if (lessonStatHashMap.containsKey(key)) {

                    LessonStat lessonStat = lessonStatHashMap.get(key);
                    for (String tag : lessonTags) {
                        if (!lessonStat.tagStats.containsKey(tag))
                            lessonStat.tagStats.put(tag, null);
                    }

                } else {

                    LessonStat lessonStat = new LessonStat(0, 0);

                    for (String tag : lessonTags)
                        lessonStat.tagStats.put(tag, null);

                    lessonStatHashMap.put(key, lessonStat);
                }
            }


        }


        HashMap<String, List<Integer>> doneInEachLessonsWeekly = new HashMap<>();
        HashMap<String, List<Integer>> totalInEachLessonsWeekly = new HashMap<>();

        HashMap<String, List<Integer>> doneTagsGeneralStats = new HashMap<>();
        HashMap<String, List<Integer>> totalTagsGeneralStats = new HashMap<>();

        HashMap<String, List<Integer>> doneAdditionalTagsGeneralStats = new HashMap<>();
        HashMap<String, List<Integer>> totalAdditionalTagsGeneralStats = new HashMap<>();

        HashMap<String, HashMap<String, List<Integer>>> doneTagInEachLessonsWeekly = new HashMap<>();
        HashMap<String, HashMap<String, List<Integer>>> totalTagInEachLessonsWeekly = new HashMap<>();

        ArrayList<Integer> sumTotals = new ArrayList<>();
        ArrayList<Integer> sumDones = new ArrayList<>();
        ArrayList<String> weeks = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();

        for (WeeklyStat weeklyStat : weeklyStats) {

            HashMap<String, Integer> doneTagsSum = new HashMap<>();
            HashMap<String, Integer> totalTagsSum = new HashMap<>();

            HashMap<String, Integer> doneAdditionalTagsSum = new HashMap<>();
            HashMap<String, Integer> totalAdditionalTagsSum = new HashMap<>();

            for (String tag : allTags) {
                doneTagsSum.put(tag, 0);
                totalTagsSum.put(tag, 0);
                doneAdditionalTagsSum.put(tag, 0);
                totalAdditionalTagsSum.put(tag, 0);
            }

            weeks.add(weeklyStat.weekStartAt);
            HashMap<String, LessonStat> lessonStatHashMap = weeklyStat.lessonStats;

            int sumTotal = 0;
            int sumDone = 0;

            for (String lesson : lessonStatHashMap.keySet()) {

                LessonStat lessonStat = lessonStatHashMap.get(lesson);

                List<Integer> list = doneInEachLessonsWeekly.containsKey(lesson) ?
                        doneInEachLessonsWeekly.get(lesson) : new ArrayList<>();

                List<Integer> totalList = totalInEachLessonsWeekly.containsKey(lesson) ?
                        totalInEachLessonsWeekly.get(lesson) : new ArrayList<>();

                list.add(lessonStat == null ? 0 : lessonStat.done);
                totalList.add(lessonStat == null ? 0 : lessonStat.total);

                sumTotal += lessonStat == null ? 0 : lessonStat.total;
                sumDone += lessonStat == null ? 0 : lessonStat.done;

                HashMap<String, List<Integer>> tmp;
                HashMap<String, List<Integer>> tmpTotal;

                if (!doneInEachLessonsWeekly.containsKey(lesson)) {

                    doneInEachLessonsWeekly.put(lesson, list);
                    totalInEachLessonsWeekly.put(lesson, totalList);

                    tmp = new HashMap<>();
                    tmpTotal = new HashMap<>();
                } else {
                    tmp = doneTagInEachLessonsWeekly.get(lesson);
                    tmpTotal = totalTagInEachLessonsWeekly.get(lesson);
                }

                for (String tag : lessonStat.tagStats.keySet()) {

                    List<Integer> tagList = tmp.containsKey(tag) ?
                            tmp.get(tag) : new ArrayList<>();

                    List<Integer> totalTagList = tmpTotal.containsKey(tag) ?
                            tmpTotal.get(tag) : new ArrayList<>();

                    TagStat tagStat = lessonStat.tagStats.get(tag);

                    int tagDone = tagStat == null ? 0 : tagStat.done;
                    int tagTotal = tagStat == null ? 0 : tagStat.total;

                    tagList.add(tagDone);
                    totalTagList.add(tagTotal);

                    if (tagTotal > 0) {

                        if (doneTagsSum.containsKey(tag)) {

                            doneTagsSum.put(tag, doneTagsSum.get(tag) + tagDone);
                            totalTagsSum.put(tag, totalTagsSum.get(tag) + tagTotal);

                            if (tagStat != null && tagStat.additionalTotal > 0) {
                                doneAdditionalTagsSum.put(tag, doneAdditionalTagsSum.get(tag) + tagStat.additionalDone);
                                totalAdditionalTagsSum.put(tag, totalAdditionalTagsSum.get(tag) + tagStat.additionalTotal);
                            }

                        } else {

                            doneTagsSum.put(tag, tagDone);
                            totalTagsSum.put(tag, tagTotal);

                            doneAdditionalTagsSum.put(tag, tagStat == null ? 0 : tagStat.additionalDone);
                            totalAdditionalTagsSum.put(tag, tagStat == null ? 0 : tagStat.additionalTotal);

                        }
                    }

                    if (!tmp.containsKey(tag)) {
                        tmp.put(tag, tagList);
                        tmpTotal.put(tag, totalTagList);
                    }
                }

                if (!doneTagInEachLessonsWeekly.containsKey(lesson)) {
                    doneTagInEachLessonsWeekly.put(lesson, tmp);
                    totalTagInEachLessonsWeekly.put(lesson, tmpTotal);
                }

            }

            sumTotals.add(sumTotal);
            sumDones.add(sumDone);

            for (String tag : totalTagsSum.keySet()) {

                List<Integer> doneSum = doneTagsGeneralStats.containsKey(tag) ?
                        doneTagsGeneralStats.get(tag) : new ArrayList<>();

                List<Integer> totalSum = totalTagsGeneralStats.containsKey(tag) ?
                        totalTagsGeneralStats.get(tag) : new ArrayList<>();

                doneSum.add(doneTagsSum.get(tag));
                totalSum.add(totalTagsSum.get(tag));

                if (!doneTagsGeneralStats.containsKey(tag)) {
                    doneTagsGeneralStats.put(tag, doneSum);
                    totalTagsGeneralStats.put(tag, totalSum);
                }

                List<Integer> doneAdditionalSum = doneAdditionalTagsGeneralStats.containsKey(tag) ?
                        doneAdditionalTagsGeneralStats.get(tag) : new ArrayList<>();

                List<Integer> totalAdditionalSum = totalAdditionalTagsGeneralStats.containsKey(tag) ?
                        totalAdditionalTagsGeneralStats.get(tag) : new ArrayList<>();

                doneAdditionalSum.add(doneAdditionalTagsSum.get(tag));
                totalAdditionalSum.add(totalAdditionalTagsSum.get(tag));

                if (!doneAdditionalTagsGeneralStats.containsKey(tag)) {
                    doneAdditionalTagsGeneralStats.put(tag, doneAdditionalSum);
                    totalAdditionalTagsGeneralStats.put(tag, totalAdditionalSum);
                }
            }

        }

        for (String key : doneInEachLessonsWeekly.keySet()) {

            JSONObject jsonObject = new JSONObject()
                    .put("lesson", key)
                    .put("doneStats", doneInEachLessonsWeekly.get(key))
                    .put("totalStats", totalInEachLessonsWeekly.get(key));

            if (doneTagInEachLessonsWeekly.containsKey(key)) {

                HashMap<String, List<Integer>> tmp = doneTagInEachLessonsWeekly.get(key);
                HashMap<String, List<Integer>> tmpTotal = totalTagInEachLessonsWeekly.get(key);

                JSONArray jsonArray1 = new JSONArray();

                for (String tag : tmp.keySet()) {
                    jsonArray1.put(new JSONObject()
                            .put("tag", tag)
                            .put("done", tmp.get(tag))
                            .put("total", tmpTotal.get(tag))
                    );
                }

                jsonObject.put("tags", jsonArray1);

            }

            jsonArray.put(jsonObject);
        }

        JSONArray tagsGeneralStats = new JSONArray();

        for (String tag : doneTagsGeneralStats.keySet()) {

            tagsGeneralStats.put(new JSONObject()
                    .put("tag", tag)
                    .put("done", doneTagsGeneralStats.get(tag))
                    .put("total", totalTagsGeneralStats.get(tag))
            );

        }

        JSONArray additionalTagsGeneralStats = new JSONArray();

        for (String tag : doneAdditionalTagsGeneralStats.keySet()) {

            boolean findNonZero = false;

            for (Integer i : totalAdditionalTagsGeneralStats.get(tag)) {
                if (i > 0) {
                    findNonZero = true;
                    break;
                }
            }

            if (!findNonZero)
                continue;

            additionalTagsGeneralStats.put(new JSONObject()
                    .put("tag", tag)
                    .put("done", doneAdditionalTagsGeneralStats.get(tag))
                    .put("total", totalAdditionalTagsGeneralStats.get(tag))
            );

        }


        return generateSuccessMsg("data", new JSONObject()
                .put("stats", jsonArray)
                .put("generalStats", new JSONObject()
                        .put("doneStats", sumDones)
                        .put("totalStats", sumTotals))
                .put("tagsGeneralStats", tagsGeneralStats)
                .put("additionalTagsGeneralStats", additionalTagsGeneralStats)
                .put("daily", new JSONObject()
                        .put("done", dailyDoneSum)
                        .put("total", dailyTotalSum)
                        .put("labels", daily)
                )
                .put("weeks", weeks)
        );
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
                eq("user_id", studentId)
                , null, Sorts.descending("week_start_at_int")
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

        if (advisorId != null) {
            Document user = userRepository.findById(studentId);
            if (user != null)
                jsonObject.put("student",
                        new JSONObject()
                                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                                .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                );
        }

        return generateSuccessMsg("data", jsonObject);
    }


    public static String getStudentSchedulesDigest(ObjectId studentId) {

        List<Document> schedules = scheduleRepository.find(
                eq("user_id", studentId)
                , null, Sorts.descending("week_start_at_int")
        );

        JSONArray jsonArray = new JSONArray();

        for (Document schedule : schedules)
            jsonArray.put(new JSONObject()
                    .put("id", schedule.getObjectId("_id").toString())
                    .put("item", schedule.getString("week_start_at"))
            );

        return generateSuccessMsg("data", jsonArray);
    }

    public static String removeSchedule(ObjectId advisorId, ObjectId id) {

        Document schedule = scheduleRepository.findById(id);
        if (schedule == null)
            return JSON_NOT_VALID_ID;

        List<Document> days = schedule.getList("days", Document.class);

        for (Document day : days) {

            if (!day.containsKey("items"))
                continue;

            for (Document item : day.getList("items", Document.class)) {
                if (!item.getObjectId("advisor_id").equals(advisorId))
                    return generateErr("شما تنها مشاور این کاربرگ نیستید و امکان حذف این کاربرگ برای شما وجود ندارد");
            }
        }

        scheduleRepository.deleteOne(schedule.getObjectId("_id"));

        return JSON_OK;
    }

    public static String getAdvisorTransactions(
            ObjectId advisorId, Boolean settlementStatus,
            Long startAt, Long endAt, String transactionMode,
            Long fromSettled, Long toSettled
    ) {
        List<Bson> filters;
        Document config = getConfig();
        int iryscAdvicePercent = config.getInteger("irysc_advice_percent");
        int iryscTeachPercent = config.getInteger("irysc_teach_percent");
        List<Document> adviceTransactions = null;
        List<Document> teachTransactions = null;
        List<ObjectId> settlementsRefs = new ArrayList<>();
        List<Document> settlementRequests = null;

        JSONArray jsonArray = new JSONArray();

        if (transactionMode == null || transactionMode.equals("advice")) {
            filters = new ArrayList<>() {{
                add(eq("advisor_id", advisorId));
                add(exists("paid_at"));
            }};
            if (settlementStatus != null)
                filters.add(exists("settled_at", settlementStatus));
            if (startAt != null)
                filters.add(gte("paid_at", startAt));
            if (endAt != null)
                filters.add(lte("paid_at", endAt));
            if(fromSettled != null || toSettled != null) {
                filters.add(exists("settled_at"));
                if(fromSettled != null)
                    filters.add(gte("settled_at", fromSettled));
                if(toSettled != null)
                    filters.add(lte("settled_at", toSettled));
            }

            AggregateIterable<Document> adviceTransactionsIter = advisorRequestsRepository.findWithJoinUser(
                    "user_id", "student",
                    match(and(filters)), project(
                            new BasicDBObject("title", 1)
                                    .append("paid_at", 1)
                                    .append("price", 1)
                                    .append("paid", 1)
                                    .append("paid_at", 1)
                                    .append("_id", 1)
                                    .append("settled_at", 1)
                                    .append("irysc_percent", 1)
                                    .append("student", 1)
                    ),
                    Sorts.descending("created_at"),
                    null, null, project(USER_DIGEST)
            );

            adviceTransactions = new ArrayList<>();
            for(Document transaction : adviceTransactionsIter) {
                if(transaction.containsKey("settled_at"))
                    settlementsRefs.add(transaction.getObjectId("_id"));
                adviceTransactions.add(transaction);
            }
        }

        if (transactionMode == null || transactionMode.equals("teach")) {
            filters = new ArrayList<>() {{
                add(eq("user_id", advisorId));
                add(exists("students.0"));
                add(lte("start_at", System.currentTimeMillis()));
            }};
            if (settlementStatus != null)
                filters.add(exists("settled_at", settlementStatus));
            if (startAt != null)
                filters.add(gte("start_at", startAt));
            if (endAt != null)
                filters.add(lte("start_at", endAt));
            if(fromSettled != null || toSettled != null) {
                filters.add(exists("settled_at"));
                if(fromSettled != null)
                    filters.add(gte("settled_at", fromSettled));
                if(toSettled != null)
                    filters.add(lte("settled_at", toSettled));
            }

            teachTransactions = teachScheduleRepository.find(
                    and(filters),
                    new BasicDBObject("start_at", 1)
                            .append("teach_mode", 1)
                            .append("price", 1)
                            .append("irysc_percent", 1)
                            .append("students", 1)
                            .append("length", 1)
                            .append("title", 1)
                            .append("settled_at", 1)
                            .append("_id", 1),
                    Sorts.descending("start_at")
            );

            for(Document transaction : teachTransactions) {
                if(transaction.containsKey("settled_at"))
                    settlementsRefs.add(transaction.getObjectId("_id"));
            }
        }

        if(settlementsRefs.size() > 0) {
            settlementRequests = settlementRequestRepository.find(
                    and(
                            exists("ref_id"),
                            in("ref_id", settlementsRefs)
                    ),
                    new BasicDBObject("amount", 1).append("ref_id", 1)
            );
        }

        if(adviceTransactions != null) {
            try {
                for (Document transaction : adviceTransactions) {
                    try {
                        Document student = transaction.get("student", Document.class);
                        JSONObject jsonObject = new JSONObject()
                                .put("title", transaction.getString("title"))
                                .put("student", student.getString("first_name") + " " + student.getString("last_name"))
                                .put("paidAt", Utility.getSolarDate(transaction.getLong("paid_at")))
                                .put("settledAt", transaction.containsKey("settled_at") ? Utility.getSolarDate(transaction.getLong("settled_at")) : "تسویه نشده")
                                .put("paid", transaction.get("paid"))
                                .put("iryscPercent", transaction.getOrDefault("irysc_percent", iryscAdvicePercent))
                                .put("price", transaction.get("price"))
                                .put("mode", "advice")
                                .put("id", transaction.getObjectId("_id").toString());

                        if(transaction.containsKey("settled_at") && settlementRequests != null)
                            settlementRequests.stream().filter(
                                    document -> document.getObjectId("ref_id").equals(transaction.getObjectId("_id"))
                            ).findFirst().ifPresent(document -> jsonObject.put("settledAmount", document.get("amount")));

                        jsonArray.put(jsonObject);
                    } catch (Exception ignore) {
                    }
                }
            } catch (Exception ignore) {
            }
        }

        if(teachTransactions != null) {
            for (Document transaction : teachTransactions) {
                JSONObject jsonObject = new JSONObject()
                        .put("title", transaction.getString("title"))
                        .put("studentsCount", transaction.getList("students", Document.class).size())
                        .put("startAt", getSolarDate(transaction.getLong("start_at")))
                        .put("settledAt", transaction.containsKey("settled_at") ? getSolarDate(transaction.getLong("settled_at")) : "تسویه نشده")
                        .put("iryscPercent", transaction.getOrDefault("irysc_percent", iryscTeachPercent))
                        .put("price", transaction.get("price"))
                        .put("length", transaction.get("length"))
                        .put("teachMode", transaction.get("teach_mode"))
                        .put("mode", "teach")
                        .put("id", transaction.getObjectId("_id").toString());

                if(transaction.containsKey("settled_at") && settlementRequests != null)
                    settlementRequests.stream().filter(
                            document -> document.getObjectId("ref_id").equals(transaction.getObjectId("_id"))
                    ).findFirst().ifPresent(document -> jsonObject.put("settledAmount", document.get("amount")));

                jsonArray.put(jsonObject);
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
