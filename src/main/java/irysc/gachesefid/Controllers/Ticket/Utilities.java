package irysc.gachesefid.Controllers.Ticket;

import irysc.gachesefid.DB.TicketRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;

public class Utilities {

    static JSONArray createChatJSONArr(List<Document> chats, boolean startByAdmin) {

        HashMap<ObjectId, Document> fetchedResponders = new HashMap<>();
        JSONArray chatsJSONArr = new JSONArray();
        boolean shouldSkip = startByAdmin;

        for (Document chat : chats) {

            if(shouldSkip) {
                shouldSkip = false;
                continue;
            }

            JSONObject chatJSONObj = new JSONObject()
                    .put("isForUser", chat.getBoolean("is_for_user"))
                    .put("msg", chat.getString("msg"))
                    .put("createdAt", Utility.getSolarDate(chat.getLong("created_at")));

            if (!chat.getBoolean("is_for_user")) {

                Document responder;

                if (fetchedResponders.containsKey(chat.getObjectId("user_id")))
                    responder = fetchedResponders.get(chat.getObjectId("user_id"));
                else {
                    responder = userRepository.findById(chat.getObjectId("user_id"));
                    fetchedResponders.put(chat.getObjectId("user_id"), responder);
                }

                if (responder != null) {
                    chatJSONObj.put("responder", new JSONObject()
                            .put("name", responder.getString("name_fa"))
                            .put("id", responder.getObjectId("_id").toString())
                            .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + responder.getString("pic"))
                            .put("last_name", responder.getString("last_name_fa"))
                    );
                }

            }

            List<String> filenames = chat.getList("files", String.class);
            ArrayList<String> files = new ArrayList<>();

            for(String filename : filenames)
                files.add(StaticValues.STATICS_SERVER + TicketRepository.FOLDER + "/" + filename);

            chatsJSONArr.put(chatJSONObj.put("files", files));
        }

        return chatsJSONArr;
    }

    static String getStatusFa(String status) {

        switch (status) {
            case "init":
                return "در حال تکمیل اطلاعات";
            case "pending":
                return "در حال بررسی درخواست";
            case "finish":
                return "خاتمه یافته";
            case "answer":
                return "پاسخ داده شده";
        }

        return "نامشخص";
    }

    static void fillConstraintArr(boolean needInit,
                                  ObjectId studentIdFilter,
                                  ObjectId id,
                                  Boolean searchInArchive,
                                  String sendDate,
                                  boolean isCreatedAt,
                                  String answerDate,
                                  String sendDateEndLimit, String answerDateEndLimit,
                                  ObjectId finisherIdFilter,
                                  String status,
                                  ArrayList<Bson> constraints) {

        if (!needInit)
            constraints.add(ne("status", "init"));

        if (id != null)
            constraints.add(eq("_id", id));

        if (sendDate != null) {
            long ts = Utility.getTimestamp(sendDate);
            if (ts != -1)
                constraints.add(gte(isCreatedAt ? "created_at" : "send_date", ts));
        }

        if (answerDate != null) {
            long ts = Utility.getTimestamp(answerDate);
            if (ts != -1)
                constraints.add(gte("answer_date", ts));
        }

        if (sendDateEndLimit != null) {
            long ts = Utility.getTimestamp(sendDateEndLimit);
            if (ts != -1)
                constraints.add(lte(isCreatedAt ? "created_at" : "send_date", ts));
        }

        if (answerDateEndLimit != null) {
            long ts = Utility.getTimestamp(answerDateEndLimit);
            if (ts != -1)
                constraints.add(lte("answer_date", ts));
        }

        if (studentIdFilter != null)
            constraints.add(eq("user_id", studentIdFilter));

        if (searchInArchive == null || !searchInArchive) {
            constraints.add(or(
                    ne("status", "finish"),
                    and(
                            eq("status", "finish"),
                            gte("answer_date", Utility.getTimestamp(Utility.getPastMilady(30)))
                    )
            ));
        }

        if (finisherIdFilter != null)
            constraints.add(eq("finisher", finisherIdFilter));

        if (status != null)
            constraints.add(eq("status", status));
    }

    static JSONObject fillJSON(Document request, boolean isStudentNeeded,
                               boolean isFinisherNeeded, boolean isChatNeeded
    ) throws InvalidFieldsException {

        JSONObject result = new JSONObject();

        if(isStudentNeeded) {
            Document student = (Document) request.get("student");
            if (student == null)
                throw new InvalidFieldsException("unknown exception");

            Utility.fillJSONWithUser(result, student);
        }

        if (isFinisherNeeded && request.containsKey("finisher")) {
            Document finisher = userRepository.findById(request.getObjectId("finisher"));
            result.put("finisher", finisher.getString("name_fa") + " " + finisher.getString("last_name_fa"));
        }

        List<Document> chats = request.getList("chats", Document.class);

        result.put("status", request.getString("status"));
        result.put("statusFa", getStatusFa(request.getString("status")));

        result.put("startByAdmin", request.containsKey("start_by_admin"));

        if(!request.getString("status").equals("finish") &&
                request.containsKey("start_by_admin") && chats.size() == 2)
            result.put("statusFa", "نیازمند پاسخ");

        if(request.getString("status").equals("init"))
            result.put("sendDate", Utility.getSolarDate(request.getLong("created_at")));

        if(request.containsKey("send_date"))
            result.put("sendDate", Utility.getSolarDate(request.getLong("send_date")));

        if (request.containsKey("answer_date"))
            result.put("answeredDate", Utility.getSolarDate(request.getLong("answer_date")));
        else
            result.put("answeredDate", "");

        result.put("title", request.getString("title"));

        result.put("priority", request.getString("priority").toLowerCase());
        result.put("section", request.getString("section"));

        if(isChatNeeded)
            result.put("chats", createChatJSONArr(chats, request.containsKey("start_by_admin")));

        result.put("id", request.getObjectId("_id").toString());

        return result;
    }
}
