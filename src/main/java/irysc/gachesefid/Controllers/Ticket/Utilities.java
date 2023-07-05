package irysc.gachesefid.Controllers.Ticket;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.TicketRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.TicketSection;
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
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;

public class Utilities {

    static JSONArray createChatJSONArr(List<Document> chats, boolean shouldSkip) {

        HashMap<ObjectId, Document> fetchedResponders = new HashMap<>();
        JSONArray chatsJSONArr = new JSONArray();

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
                            .put("id", responder.getObjectId("_id").toString())
                            .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + responder.getString("pic"))
                            .put("name", responder.getString("first_name") + " " + responder.getString("last_name"))
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

    static String getPriorityFa(String priority) {

        switch (priority.toLowerCase()) {
            case "high":
                return "بالا";
            case "avg":
                return "متوسط";
            case "low":
            default:
                return "کم";
        }
    }

    static String getSectionFa(String section) {

        if(section.equalsIgnoreCase(TicketSection.QUIZ.getName()))
            return "آزمون";

        if(section.equalsIgnoreCase(TicketSection.CLASS.getName()))
            return "کلاس";

        if(section.equalsIgnoreCase(TicketSection.UPGRADELEVEL.getName()))
            return "ارتقای سطح";

        if(section.equalsIgnoreCase(TicketSection.REQUESTMOENY.getName()))
            return "تسویه مالی";

        return "آزمون";
    }

    static void fillConstraintArr(boolean needInit,
                                  ObjectId studentIdFilter,
                                  ObjectId id,
                                  Boolean searchInArchive,
                                  Long sendDate,
                                  boolean isCreatedAt,
                                  Long answerDate,
                                  Long sendDateEndLimit,
                                  Long answerDateEndLimit,
                                  ObjectId finisherIdFilter,
                                  String status,
                                  ArrayList<Bson> constraints) {

        if (!needInit)
            constraints.add(ne("status", "init"));

        if (id != null)
            constraints.add(eq("_id", id));

        if (sendDate != null)
            constraints.add(gte(isCreatedAt ? "created_at" : "send_date", sendDate));

        if (answerDate != null)
            constraints.add(gte("answer_date", answerDate));

        if (sendDateEndLimit != null)
            constraints.add(lte(isCreatedAt ? "created_at" : "send_date", sendDateEndLimit));

        if (answerDateEndLimit != null)
            constraints.add(lte("answer_date", answerDateEndLimit));

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
                               boolean isFinisherNeeded, boolean isChatNeeded,
                               boolean isRefNeeded
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

        if(isRefNeeded && request.containsKey("ref_id")) {

            if(request.getOrDefault("section", "").toString().equalsIgnoreCase(TicketSection.CLASS.getName())) {
                Document content = contentRepository.findById(request.getObjectId("ref_id"));
                if(content != null)
                    result.put("ref", content.getString("title"));
            }
            else if(request.containsKey("ref_id") && request.getString("section").equalsIgnoreCase(TicketSection.QUIZ.getName()) &&
                    request.containsKey("additional")
            ) {

                String additional = request.getString("additional");
                Document quiz = null;
                String prefix = "";

                if (additional.equalsIgnoreCase("irysc")) {
                    quiz = iryscQuizRepository.findById(request.getObjectId("ref_id"));
                    prefix = "آزمون آیریسک: ";
                }
                else if (additional.equalsIgnoreCase("open")) {
                    quiz = openQuizRepository.findById(request.getObjectId("ref_id"));
                    prefix = "آزمون باز: ";
                }
                else if (additional.equalsIgnoreCase("custom"))
                    quiz = customQuizRepository.findById(request.getObjectId("ref_id"));

                if(quiz != null)
                    result.put("ref", prefix + quiz.getString("title"));
            }
        }

        result.put("status", request.getString("status"));
        result.put("statusFa", getStatusFa(request.getString("status")));

        result.put("startByAdmin", request.containsKey("start_by_admin"));

        if(!request.getString("status").equals("finish") &&
                request.containsKey("start_by_admin") && chats.size() == 2) {
            result.put("statusFa", "نیازمند پاسخ دانش آموز");
        }

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
        result.put("priorityFa", getPriorityFa(request.getString("priority")));

        result.put("section", request.getString("section").toLowerCase());
        result.put("sectionFa", getSectionFa(request.getString("section")));

        if(isChatNeeded)
            result.put("chats", createChatJSONArr(chats,
                    request.containsKey("start_by_admin") ||
                            request.getString("section").equalsIgnoreCase(
                                    TicketSection.UPGRADELEVEL.getName())
                    )
            );

        result.put("id", request.getObjectId("_id").toString());

        return result;
    }
}
