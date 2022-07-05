package irysc.gachesefid.Controllers.Ticket;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.TicketRepository;
import irysc.gachesefid.Models.NewAlert;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Ticket.Utilities.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.uploadImageOrPdfFile;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class TicketController {

    public static String rejectRequests(ObjectId userId, JSONArray jsonArray) {

        Document doc;

        BasicDBObject update = new BasicDBObject("status", "finish")
                .append("finisher", userId)
                .append("answer_date", System.currentTimeMillis());

        JSONArray excepts = new JSONArray();
        JSONArray closedItems = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if(!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            try {
                ObjectId oId = new ObjectId(id);

                doc = ticketRepository.findOneAndUpdate(
                        eq("_id", oId),
                        new BasicDBObject("$set", update)
                );

                if (doc != null) {
                    ticketRepository.cleanReject(doc);
                    closedItems.put(oId);
                }
                else
                    excepts.put(i + 1);

            } catch (Exception ignore) {
                excepts.put(i + 1);
            }
        }
        return returnBatchResponse(excepts, closedItems, "closedIds", "بسته");
    }

    public static String getRequests(Boolean searchInArchive,
                                     Boolean answered,
                                     Boolean finished,
                                     ObjectId finisherIdFilter,
                                     ObjectId idFilter,
                                     ObjectId studentIdFilter,
                                     String sendDate, String answerDate,
                                     String sendDateEndLimit, String answerDateEndLimit,
                                     Boolean isForTeacher, Boolean startByAdmin,
                                     String section, String priority) {

        ArrayList<Bson> constraints = new ArrayList<>();

        fillConstraintArr(false, studentIdFilter, idFilter,
                searchInArchive, sendDate, false, answerDate,
                sendDateEndLimit, answerDateEndLimit,
                finisherIdFilter, finished, answered, constraints
        );

        if (startByAdmin != null)
            constraints.add(exists("start_by_admin", startByAdmin));

        if (isForTeacher != null)
            constraints.add(eq("is_for_teacher", isForTeacher));

        if (section != null)
            constraints.add(eq("section", section));

        if (priority != null)
            constraints.add(eq("priority", priority.toUpperCase()));

        AggregateIterable<Document> docs =
                ticketRepository.findWithJoinUser("user_id", "student",
                        match(and(constraints)),
                        project(new BasicDBObject("finisher", 1).append("student", 1)
                                .append("answer_date", 1).append("send_date", 1)
                                .append("status", 1).append("_id", 1)
                                .append("priority", 1).append("section", 1)
                                .append("title", 1).append("start_by_admin", 1)
                                .append("chats", 1)
                        ),
                        Sorts.descending("send_date"));

        JSONArray jsonArray = new JSONArray();

        for (Document request : docs) {
            try {
                jsonArray.put(fillJSON(request, true,
                        true, false)
                );
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }


    public static String getRequest(ObjectId ticketId) {

        try {
            AggregateIterable<Document> docs =
                    ticketRepository.findWithJoinUser("user_id", "student",
                            match(eq("_id", ticketId)), null, null
                    );

            if (!docs.iterator().hasNext())
                return JSON_NOT_VALID_ID;

            Document request = docs.iterator().next();

            return generateSuccessMsg("data", fillJSON(request, true,
                    true, true)
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }


    public static String setRequestAnswer(ObjectId requestId, ObjectId userId, JSONObject jsonObject) {

        Document doc = ticketRepository.findOne(and(
                eq("_id", requestId),
                eq("status", "pending")
        ), null);

        if (doc == null)
            return JSON_NOT_ACCESS;

        return handleSendAns(doc, userId, jsonObject.getString("answer"));
    }

    private static String handleSendAns(Document doc, ObjectId userId, String answer) {

        ObjectId requestId = doc.getObjectId("_id");

        List<Document> chats = doc.getList("chats", Document.class);
        boolean isFirstAdminAns = chats.size() == 1;

        Document lastChat = chats.get(chats.size() - 1);

        if (lastChat.getBoolean("is_for_user"))
            chats.add(new Document("created_at", System.currentTimeMillis())
                    .append("msg", answer)
                    .append("is_for_user", false)
                    .append("user_id", userId)
                    .append("files", new ArrayList<>())
            );

        else if (!lastChat.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        else
            lastChat.put("msg", answer);

        new Thread(() -> ticketRepository.updateOne(eq("_id", requestId),
                new BasicDBObject("$set", new BasicDBObject("chats", chats)
                        .append("status", "answer")
                        .append("answer_date", System.currentTimeMillis())
                )
        )).start();

        if (isFirstAdminAns)
            newThingsCache.put(NewAlert.NEW_TICKETS.getName(),
                    newThingsCache.get(NewAlert.NEW_TICKETS.getName()) - 1);
        else
            newThingsCache.put(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName(),
                    newThingsCache.get(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName()) - 1);

        return JSON_OK;
    }

    // STUDENT SECTION

    // tickets strategy: 1- admin to any user 2- student or any other access to just admin
    public static String insert(List<String> accesses, ObjectId userId, JSONObject jsonObject) {

        boolean isAdmin = Authorization.isAdmin(accesses);

        if (
                (isAdmin && !jsonObject.has("userId")) ||
                        (!isAdmin && jsonObject.has("userId"))
        )
            return JSON_NOT_VALID_PARAMS;

        ObjectId ticketId;

        if (jsonObject.has("userId")) {

            ObjectId studentId = new ObjectId(jsonObject.getString("userId"));
            Document user = userRepository.findById(studentId);

            JSONObject res = new JSONObject(insert(
                    user.getList("accesses", String.class),
                    studentId,
                    new JSONObject()
                            .put("title", jsonObject.getString("title"))
                            .put("description", "")
                            .put("section", jsonObject.has("section") ? jsonObject.getString("section") : "public")
                            .put("priority", jsonObject.has("priority") ? jsonObject.getString("priority") : "avg")
            ));

            if (!res.getString("status").equals("ok"))
                return res.getString("msg");

            ticketId = new ObjectId(res.getString("id"));

            Document ticket = ticketRepository.findById(ticketId);
            ticket.put("start_by_admin", true);
            ticketRepository.replaceOne(ticketId, ticket);

            sendRequest(studentId, ticketId);
            handleSendAns(ticket, userId, jsonObject.getString("description"));

            new Thread(() -> Utility.sendMail(
                    user.getString("mail"), "", "New ticket",
                    "ticket", user.getString("name_en") + " " + user.getString("last_name_en")
            )).start();

            return Utility.generateSuccessMsg("id", ticketId);
        }

        if (jsonObject.has("priority") &&
                !jsonObject.getString("priority").toLowerCase().equals("high") &&
                !jsonObject.getString("priority").toLowerCase().equals("avg") &&
                !jsonObject.getString("priority").toLowerCase().equals("low")
        )
            return JSON_NOT_VALID_PARAMS;

        //todo: validate section

        ArrayList<Document> chats = new ArrayList<>();
        chats.add(new Document("msg", jsonObject.getString("description"))
                .append("created_at", System.currentTimeMillis())
                .append("is_for_user", true)
                .append("files", new ArrayList<>())
        );

        return ticketRepository.insertOneWithReturn(
                new Document("title", jsonObject.getString("title"))
                        .append("created_at", System.currentTimeMillis())
                        .append("is_for_teacher", Authorization.isTeacher(accesses))
                        .append("chats", chats)
                        .append("status", "init")
                        .append("priority", (jsonObject.has("priority")) ?
                                jsonObject.getString("priority").toUpperCase() : "avg")
                        .append("section", (jsonObject.has("section")) ?
                                jsonObject.getString("section") : "public")
                        .append("user_id", userId)
        );
    }

    public static String setRequestAnswerUser(ObjectId requestId,
                                              ObjectId userId,
                                              String answer) {

        Document request = ticketRepository.findOne(
                and(
                        eq("_id", requestId),
                        eq("user_id", userId),
                        eq("status", "answer")
                ), null
        );

        if (request == null)
            return JSON_NOT_ACCESS;

        List<Document> chats = request.getList("chats", Document.class);

        Document lastChat = chats.get(chats.size() - 1);
        if (lastChat.getBoolean("is_for_user"))
            lastChat.put("msg", answer);
        else
            chats.add(new Document("created_at", System.currentTimeMillis())
                    .append("msg", answer)
                    .append("files", new ArrayList<>())
                    .append("is_for_user", true)
            );

        new Thread(() -> ticketRepository.updateOne(eq("_id", requestId),
                new BasicDBObject("$set", new BasicDBObject("chats", chats)
                        .append("status", "pending").append("send_date", System.currentTimeMillis()))
        )).start();

        newThingsCache.put(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName(),
                newThingsCache.get(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName()) + 1);

        return JSON_OK;
    }

    public static String sendRequest(ObjectId userId, ObjectId requestId) {

        Document request = ticketRepository.findOne(
                and(
                        eq("_id", requestId),
                        eq("user_id", userId),
                        or(
                                eq("status", "init"),
                                eq("status", "answer")
                        )
                ), null
        );

        if (request == null)
            return JSON_NOT_ACCESS;

        new Thread(() -> {

            request.put("send_date", System.currentTimeMillis());
            request.put("status", "pending");

            ticketRepository.replaceOne(eq("_id", requestId), request);

        }).start();

        if (request.getString("status").equals("init"))
            newThingsCache.put(NewAlert.NEW_TICKETS.getName(),
                    newThingsCache.get(NewAlert.NEW_TICKETS.getName()) + 1);
        else
            newThingsCache.put(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName(),
                    newThingsCache.get(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName()) + 1);

        return JSON_OK;
    }

    public static String addFileToRequest(ArrayList<String> accesses, ObjectId userId,
                                          ObjectId requestId, MultipartFile file) {

        Document request;
        boolean isAdmin = Authorization.isAdmin(accesses);

        if (!isAdmin)
            request = ticketRepository.findOne(
                    and(
                            eq("_id", requestId),
                            eq("user_id", userId),
                            or(
                                    eq("status", "init"),
                                    eq("status", "answer")
                            )
                    ), null
            );
        else
            request = ticketRepository.findOne(
                    and(
                            eq("_id", requestId),
                            eq("status", "pending")
                    ), null
            );

        if (request == null)
            return JSON_NOT_ACCESS;

        List<Document> chats = request.getList("chats", Document.class);
        int total = 0;
        ObjectId adminId = null;

        for (Document chat : chats) {

            if (chat.containsKey("files"))
                total += chat.getList("files", String.class).size();

            if (chat.containsKey("user_id"))
                adminId = chat.getObjectId("user_id");
        }

        if (adminId != null &&
                isAdmin &&
                !adminId.equals(userId)
        )
            return JSON_NOT_ACCESS;

        if (total >= 10)
            return generateErr("شما حداکثر 10 فایل می توانید بارگذاری کنید.");

        if (file.getSize() > MAX_TICKET_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز، " + MAX_TICKET_FILE_SIZE + " مگ است.");

        String fileType = uploadImageOrPdfFile(file);

        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        String filename = FileUtils.uploadFile(file, TicketRepository.FOLDER);
        if (filename == null)
            return generateErr("فایل موردنظر معتبر نمی باشد.");

        boolean createNew = false;

        Document lastChat = chats.get(chats.size() - 1);
        if (
                (
                        lastChat.getBoolean("is_for_user") &&
                                isAdmin
                ) ||
                        (
                                !lastChat.getBoolean("is_for_user") &&
                                        !isAdmin
                        )
        )
            createNew = true;

        if (createNew) {

            ArrayList<String> files = new ArrayList<>();
            files.add(filename);

            Document newDoc = new Document("created_at", System.currentTimeMillis())
                    .append("is_for_user", !isAdmin)
                    .append("files", files)
                    .append("msg", "");

            if (isAdmin)
                newDoc.append("user_id", userId);

            chats.add(newDoc);

        } else
            lastChat.getList("files", String.class).add(filename);

        new Thread(() -> ticketRepository.updateOne(
                requestId, set("chats", chats)
        )).start();

        return generateSuccessMsg("filename", filename);
    }

    public static String getMyRequests(ObjectId id,
                                       Boolean answered,
                                       Boolean finished,
                                       ObjectId userId,
                                       String sendDate, String answerDate,
                                       String sendDateEndLimit, String answerDateEndLimit) {

        ArrayList<Bson> constraints = new ArrayList<>();

        fillConstraintArr(true, userId, id,
                null, sendDate, false, answerDate, sendDateEndLimit, answerDateEndLimit,
                null, finished, answered, constraints
        );

        ArrayList<Document> requests = ticketRepository.find(
                and(constraints), null,
                Sorts.descending("send_date")
        );

        JSONArray jsonArray = new JSONArray();

        for (Document request : requests) {
            try {
                jsonArray.put(fillJSON(request, false,
                        false, true)
                );
            } catch (Exception ignore) {
            }
        }

        return generateSuccessMsg("requests", jsonArray);

    }

    public static String deleteRequestFile(List<String> accesses,
                                           ObjectId userId,
                                           ObjectId requestId,
                                           String filename) {

        Document request;
        boolean isAdmin = Authorization.isAdmin(accesses);

        if (!isAdmin)
            request = ticketRepository.findOne(
                    and(
                            eq("_id", requestId),
                            eq("user_id", userId),
                            or(
                                    eq("status", "init"),
                                    eq("status", "answer")
                            )
                    ), null
            );
        else
            request = ticketRepository.findOne(
                    and(
                            eq("_id", requestId),
                            eq("status", "pending")
                    ), null
            );

        if (request == null)
            return JSON_NOT_ACCESS;

        List<Document> chats = request.getList("chats", Document.class);
        boolean deleted = false;

        int i = chats.size() - 1;

        if (
                (
                        !isAdmin &&
                                !chats.get(i).getBoolean("is_for_user")
                ) ||
                        (
                                isAdmin &&
                                        chats.get(i).getBoolean("is_for_user")
                        )
        )
            return JSON_NOT_ACCESS;

        if (chats.get(i).getList("files", String.class).contains(filename)) {

            if (
                    !isAdmin || chats.get(i).getObjectId("user_id").equals(userId)
            ) {
                FileUtils.removeFile(filename, TicketRepository.FOLDER);
                chats.get(i).getList("files", String.class).remove(filename);
                deleted = true;
            }
        }

        if (!deleted)
            return JSON_NOT_ACCESS;

        new Thread(() -> ticketRepository.updateOne(
                request.getObjectId("_id"),
                set("chats", chats)
        )).start();

        return JSON_OK;
    }
}
