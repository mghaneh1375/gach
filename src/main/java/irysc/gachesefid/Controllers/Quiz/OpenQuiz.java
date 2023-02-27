package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.Models.AllKindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.mailQueueRepository;
import static irysc.gachesefid.Main.GachesefidApplication.openQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.SERVER;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.Utility.sendMail;

public class OpenQuiz extends QuizAbstract {

    @Override
    public List<Document> registry(ObjectId studentId, String phone,
                                   String mail, List<ObjectId> quizIds, int paid,
                                   ObjectId transactionId, String stdName
    ) {

        ArrayList<Document> added = new ArrayList<>();

        for (ObjectId quizId : quizIds) {

            try {
                Document quiz = openQuizRepository.findById(quizId);

                if(quiz == null)
                    continue;

                List<Document> students = quiz.getList("students", Document.class);

                if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        students, "_id", studentId
                ) != -1)
                    continue;

                Document stdDoc = new Document("_id", studentId)
                        .append("paid", paid / quizIds.size())
                        .append("register_at", System.currentTimeMillis())
                        .append("finish_at", null)
                        .append("start_at", null)
                        .append("answers", new byte[0]);

                students.add(stdDoc);
                added.add(stdDoc);
                quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);

                openQuizRepository.replaceOne(
                        quizId, quiz
                );

                if(transactionId != null && mail != null) {
                    new Thread(() -> sendMail(mail, SERVER + "recp/" + transactionId, "successQuiz", stdName)).start();
                }


                //todo : send notif
            } catch (Exception ignore) {}
        }

        return added;
    }

    @Override
    void quit(Document student, Document quiz) {

    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin, boolean afterBuy, boolean isDescNeeded) {

        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("mode", quiz.getString("mode"))
                .put("generalMode", AllKindQuiz.OPEN.getName())
                .put("tags", quiz.getList("tags", String.class))
                .put("rate", quiz.getOrDefault("rate", 5))
                .put("reportStatus", quiz.getOrDefault("report_status", "not_ready"))
                .put("id", quiz.getObjectId("_id").toString());

        int questionsCount = 0;
        try {
            questionsCount = quiz.get("questions", Document.class)
                    .getList("_ids", ObjectId.class).size();
        } catch (Exception ignore) {
        }

        jsonObject.put("duration", calcLen(quiz))
                .put("questionsCount", questionsCount);

        if (afterBuy)
            jsonObject
                    .put("status", "inProgress");
        else
            jsonObject.put("price", quiz.get("price"));

        if (isAdmin) {
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("priority", quiz.getInteger("priority"));
        }

        if(!isDigest || isDescNeeded)
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""));

        if (!isDigest && isAdmin) {

            JSONArray attaches = new JSONArray();
            if(quiz.containsKey("attaches")) {
                for (String attach : quiz.getList("attaches", String.class))
                    attaches.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);
            }

            jsonObject.put("lenMode", quiz.containsKey("duration") ? "custom" : "question");
            if(quiz.containsKey("duration"))
                jsonObject.put("len", quiz.getInteger("duration"));

            jsonObject.put("minusMark", quiz.getOrDefault("minus_mark", false));

            jsonObject.put("descBefore", quiz.getOrDefault("desc", ""));
            jsonObject.put("descAfter", quiz.getOrDefault("desc_after", ""));
            jsonObject.put("attaches", attaches);
        }

        return jsonObject;

    }

}
