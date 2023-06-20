package irysc.gachesefid.Controllers.Advisor;


import com.mongodb.BasicDBObject;
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
import static irysc.gachesefid.Controllers.Advisor.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdvisorController {

    public static String removeOffers(ObjectId advisorId, JSONArray items) {

        List<Document> docs = advisorFinanceOfferRepository.find(eq("advisor_id", advisorId), JUST_ID);
        List<ObjectId> ids = new ArrayList<>();

        for(Document doc : docs)
            ids.add(doc.getObjectId("_id"));

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for(int i = 0; i < items.length(); i++) {

            try {
                String id = items.getString(i);
                if(!ObjectId.isValid(id)) {
                    excepts.put(id);
                    continue;
                }

                ObjectId oId = new ObjectId(id);
                if(!ids.contains(oId)) {
                    excepts.put(id);
                    continue;
                }

                advisorFinanceOfferRepository.deleteOne(oId);
                doneIds.put(id);
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return returnRemoveResponse(excepts, doneIds);
    }

    public static String createNewOffer(ObjectId advisorId, JSONObject data) {

        if(data.getString("title").length() < 3)
            return generateErr("عنوان باید بیش از ۲ کاراکتر باشد");

        Document config = getConfig();

        if(config.getInteger("min_advice_price") > data.getInt("price"))
            return generateErr("قیمت هر بسته باید حداقل " + config.getInteger("min_advice_price") + " باشد");

        if(config.getInteger("max_video_call_per_month") < data.getInt("videoCalls"))
            return generateErr("تعداد تماس های تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        Document newDoc = new Document("advisor_id", advisorId)
                .append("price", data.getInt("price"))
                .append("title", data.getString("title"))
                .append("video_calls", data.getInt("videoCalls"))
                .append("visibility", data.getBoolean("visibility"))
                .append("created_at", System.currentTimeMillis());

        if(data.has("description"))
            newDoc.append("description", data.getString("description"));

        if(data.has("maxKarbarg"))
            newDoc.append("max_karbarg", data.getInt("maxKarbarg"));

        if(data.has("maxExam"))
            newDoc.append("max_exam", data.getInt("maxExam"));

        if(data.has("maxChat"))
            newDoc.append("max_chat", data.getInt("maxChat"));

        advisorFinanceOfferRepository.insertOne(newDoc);

        return generateSuccessMsg("data", convertFinanceOfferToJSONObject(newDoc, true));
    }

    public static String updateOffer(ObjectId id, JSONObject data) {

        if(data.getString("title").length() < 3)
            return generateErr("عنوان باید بیش از ۲ کاراکتر باشد");

        Document doc = advisorFinanceOfferRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        Document config = getConfig();

        if(config.getInteger("min_advice_price") > data.getInt("price"))
            return generateErr("قیمت هر بسته باید حداقل " + config.getInteger("min_advice_price") + " باشد");

        if(config.getInteger("max_video_call_per_month") < data.getInt("videoCalls"))
            return generateErr("تعداد تماس های تصویر می تواند حداکثر  " + config.getInteger("max_video_call_per_month") + " باشد");

        doc.put("title", data.getString("title"));
        doc.put("price", data.getInt("price"));
        doc.put("video_calls", data.getInt("videoCalls"));
        doc.put("visibility", data.getBoolean("visibility"));

        if(data.has("description"))
            doc.put("description", data.getString("description"));
        else
            doc.remove("description");

        if(data.has("maxKarbarg"))
            doc.put("max_karbarg", data.getInt("maxKarbarg"));
        else
            doc.remove("max_karbarg");

        if(data.has("maxExam"))
            doc.put("max_exam", data.getInt("maxExam"));
        else
            doc.remove("max_exam");

        if(data.has("maxChat"))
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

        if(docs.size() > 0) {
            for (Document doc : docs) {
                jsonArray.put(convertFinanceOfferToJSONObject(
                        doc, accessorId != null && accessorId.equals(doc.getObjectId("advisor_id")))
                );
            }
        }
        else if(accessorId == null){

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

        if(roomUrl != null)
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

        for(Document doc : docs) {

            Document advisor = userRepository.findById(doc.getObjectId("advisor_id"));

            if(advisor == null)
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

        for(Document doc : docs) {

            Document student = userRepository.findById(doc.getObjectId("student_id"));

            if(student == null)
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

    public static String cancelRequest(ObjectId userId, ObjectId reqId) {

        Document doc = advisorRequestsRepository.findOneAndDelete(
                and(
                        eq("_id", reqId),
                        eq("user_id", userId),
                        eq("answer", "pending")
                )
        );

        if (doc == null)
            return generateErr("تنها درخواست های پاسخ داده نشده می توانند حذف شوند");

        return JSON_OK;
    }

    public static String hasOpenRequest(ObjectId userId) {

        if (advisorRequestsRepository.count(
                and(
                        eq("user_id", userId),
                        eq("answer", "pending")
                )) > 0
        )
            return generateSuccessMsg("data", "yes");

        return generateSuccessMsg("data", "no");
    }

    private static String returnRequests(String key, List<Document> requests) {

        JSONArray jsonArray = new JSONArray();

        for (Document request : requests) {

            Document advisor = userRepository.findById(request.getObjectId(key));
            if (advisor == null)
                continue;

            jsonArray.put(new JSONObject()
                    .put("id", request.getObjectId("_id").toString())
                    .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                    .put("createdAt", Utility.getSolarDate(request.getLong("created_at")))
                    .put("answerAt", request.containsValue("answer_at") ?
                            Utility.getSolarDate(request.getLong("answer_at")) :
                            ""
                    )
                    .put("status", request.getString("answer"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String myRequests(ObjectId userId) {

        List<Document> requests = advisorRequestsRepository.find(
                eq("user_id", userId), null, Sorts.descending("created_at")
        );

        return returnRequests("advisor_id", requests);
    }


    public static String myStudentRequests(ObjectId advisorId) {

        List<Document> requests = advisorRequestsRepository.find(
                eq("advisor_id", advisorId), null, Sorts.descending("created_at")
        );

        return returnRequests("user_id", requests);
    }

    public static String request(Document user, ObjectId advisorId, ObjectId planId) {

//        if (advisorRequestsRepository.count(
//                and(
//                        eq("user_id", user.getObjectId("_id")),
//                        eq("answer", "pending")
//                )) > 0
//        )
//            return generateErr("شما تنها یک درخواست پاسخ داده نشده می توانید داشته باشید");


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

        advisor.put("students", students);

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

        if (answer.equalsIgnoreCase(YesOrNo.NO.getName())) {
            req.put("answer", "reject");
            req.put("answer_at", System.currentTimeMillis());
        } else {

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
            jsonArray.put(convertToJSONDigest(null, advisor));

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getMyAdvisor(ObjectId userId, ObjectId advisorId) {

        Document advisor = userRepository.findById(advisorId);

        if (advisor == null)
            return JSON_NOT_UNKNOWN;

        return generateSuccessMsg("data", convertToJSONDigest(userId, advisor));
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

        double newRate = Math.round(oldTotalRate / rateCount * 100.0) / 100.0;
        advisor.put("rate", newRate);
        advisor.put("rate_count", rateCount);

        userRepository.replaceOne(advisorId, advisor);
        return generateSuccessMsg("rate", newRate);

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

    public static String myLifeStyle(ObjectId userId) {

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null) {
            schedule = new Document("days", new ArrayList<>() {{
                add(new Document("day", 0).append("items", new ArrayList<>()));
                add(new Document("day", 1).append("items", new ArrayList<>()));
                add(new Document("day", 2).append("items", new ArrayList<>()));
                add(new Document("day", 3).append("items", new ArrayList<>()));
                add(new Document("day", 4).append("items", new ArrayList<>()));
                add(new Document("day", 5).append("items", new ArrayList<>()));
                add(new Document("day", 6).append("items", new ArrayList<>()));
            }}).append("user_id", userId).append("created_at", System.currentTimeMillis());

            lifeScheduleRepository.insertOne(schedule);
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("days", convertLifeScheduleToJSON(schedule))
                .put("exams", schedule.getList("exams", String.class))
        );
    }

    public static String addItemToMyLifeStyle(ObjectId userId, JSONObject data) {

        String day = data.getString("day");
        if(
                !day.equals("شنبه") &&
                        !day.equals("یک شنبه") &&
                        !day.equals("دوشنبه") &&
                        !day.equals("سه شنبه") &&
                        !day.equals("چهار شنبه") &&
                        !day.equals("پنج شنبه") &&
                        !day.equals("جمعه")
        )
            return JSON_NOT_VALID_PARAMS;

        ObjectId tagId = new ObjectId(data.getString("tag"));
        Document tag = lifeStyleTagRepository.findById(tagId);
        if(tag == null || tag.containsKey("deleted_at"))
            return JSON_NOT_VALID_ID;

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        int dayIndex;

        switch (day) {
            case "شنبه":
            default:
                dayIndex = 0;
                break;
            case "یک شنبه":
                dayIndex = 1;
                break;
            case "دوشنبه":
                dayIndex = 2;
                break;
            case "سه شنبه":
                dayIndex = 3;
                break;
            case "چهار شنبه":
                dayIndex = 4;
                break;
            case "پنج شنبه":
                dayIndex = 5;
                break;
            case "جمعه":
                dayIndex = 6;
                break;
        }

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        if(doc != null) {

            List<Document> items = doc.getList("items", Document.class);

            if(Utility.searchInDocumentsKeyValIdx(
                    items, "tag", tag.getString("label")
            ) != -1)
                return generateErr("تگ وارد شده در روز موردنظر موجود است");

            ObjectId newId = new ObjectId();
            Document newDoc = new Document("_id", newId)
                    .append("tag", tag.getString("label"))
                    .append("duration", data.getInt("duration"));

            if(data.has("startAt"))
                newDoc.put("start_at", data.getString("start_at"));

            items.add(newDoc);
            lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

            return generateSuccessMsg("id", newId.toString());

        }


        return JSON_NOT_UNKNOWN;
    }

    public static String removeItemFromMyLifeStyle(ObjectId userId, JSONObject data) {

        String day = data.getString("day");
        if(
                !day.equals("شنبه") &&
                        !day.equals("یک شنبه") &&
                        !day.equals("دوشنبه") &&
                        !day.equals("سه شنبه") &&
                        !day.equals("چهار شنبه") &&
                        !day.equals("پنج شنبه") &&
                        !day.equals("جمعه")
        )
            return JSON_NOT_VALID_PARAMS;

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        int dayIndex;

        switch (day) {
            case "شنبه":
            default:
                dayIndex = 0;
                break;
            case "یک شنبه":
                dayIndex = 1;
                break;
            case "دوشنبه":
                dayIndex = 2;
                break;
            case "سه شنبه":
                dayIndex = 3;
                break;
            case "چهار شنبه":
                dayIndex = 4;
                break;
            case "پنج شنبه":
                dayIndex = 5;
                break;
            case "جمعه":
                dayIndex = 6;
                break;
        }

        List<Document> days = schedule.getList("days", Document.class);
        Document doc = Utility.searchInDocumentsKeyVal(
                days, "day", dayIndex
        );

        if(doc != null) {

            List<Document> items = doc.getList("items", Document.class);
            int idx = Utility.searchInDocumentsKeyValIdx(
                    items, "tag", data.getString("tag")
            );

            if(idx < 0)
                return JSON_NOT_VALID_PARAMS;

            items.remove(idx);
            lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

            return JSON_OK;

        }


        return JSON_NOT_UNKNOWN;
    }

    public static String setMyExamInLifeStyle(ObjectId userId, JSONArray exams) {

        Document schedule = lifeScheduleRepository.findBySecKey(userId);

        if (schedule == null)
            return JSON_NOT_ACCESS;

        List<String> examTags = new ArrayList<>();

        for(int i = 0; i < exams.length(); i++) {

            if(!ObjectId.isValid(exams.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            Document examTag = adviseExamTagRepository.findById(new ObjectId(exams.getString(i)));
            if(examTag == null)
                return JSON_NOT_VALID_PARAMS;

            examTags.add(examTag.getString("label"));
        }

        schedule.put("exams", examTags);
        lifeScheduleRepository.replaceOne(schedule.getObjectId("_id"), schedule);

        return JSON_OK;
    }
}
