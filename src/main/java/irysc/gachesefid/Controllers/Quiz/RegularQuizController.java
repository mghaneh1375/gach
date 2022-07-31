package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Finance.Utilities;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.offcodeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.transactionRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class RegularQuizController extends QuizAbstract {

    // endRegistry consider as optional field

    // topStudentsGiftCoin or topStudentsGiftMoney or topStudentsCount
    // are optional and can inherit from config
//"duration",

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "launchMode", "showResultsAfterCorrection",
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database"
    };

    public static String create(ObjectId userId, JSONObject jsonObject, String mode) {

        try {

            Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", mode);
            Document newDoc = QuizController.store(userId, jsonObject);
            iryscQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz", new RegularQuizController()
                            .convertDocToJSON(newDoc, false, true)
            );

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    e.getMessage()
            );
        }

    }

    public static String delete(ObjectId quizId, ObjectId userId) {

        Document quiz = iryscQuizRepository.findOneAndDelete(and(
                eq("_id", quizId),
                eq("mode", KindQuiz.REGULAR.getName())
        ));

        if (quiz == null)
            return JSON_NOT_VALID;

        iryscQuizRepository.cleanRemove(quiz);

        return JSON_OK;
    }

    public static String createKarname(ObjectId userId, String access, ObjectId quizId) {

//        Document quiz = regularQuizRepository.findById(quizId);
//
//        if(quiz == null ||
//                (!quiz.getObjectId("user_id").equals(userId) && !access.equals("admin")))
//            return JSON_NOT_ACCESS;
//
//        String[] questionIdsStr = quiz.getString("questions").split("-");
//        int[] questionIds = new int[questionIdsStr.length];
//
//        for(int i = 0; i < questionIdsStr.length; i++)
//            questionIds[i] = Integer.parseInt(questionIdsStr[i]);
//
//        HashMap<Integer, Question> questionsInfo = QuestionRepository.getAnswers(questionIds);
//        ArrayList<Document> studentResults = RegularQuizResultRepository.getRegularQuizResults(quizId);
//
//        for(Document studentResult : studentResults) {
//
//            String[] studentAnswersStr = studentResult.getString("student_answers").split("-");
//            String[] studentQuestionIdsStr = studentResult.getString("question_ids").split("-");
//
//            if(studentAnswersStr.length != studentQuestionIdsStr.length) {
//                System.out.println("heyyy corrupt");
//                continue;
//            }
//
//            for(int i = 0; i < studentAnswersStr.length; i++) {
//                questionsInfo.get(Integer.parseInt(studentQuestionIdsStr[i]));
//            }
//
//        }

        return JSON_OK;
    }

    public static String myQuizes(ObjectId userId) {
        return null;
    }

    public static String myPassedQuizes(ObjectId userId) {
        return null;
    }

    @Override
    public String buy(Document user, Document quiz) {

        if (quiz.containsKey("end_registry") &&
                quiz.getLong("end_registry") < System.currentTimeMillis())
            return irysc.gachesefid.Utility.Utility.generateErr(
                    "زمان ثبت نام در آزمون موردنظر گذشته است."
            );

        ObjectId userId = user.getObjectId("_id");

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                quiz.getList("students", Document.class), "_id", userId
        ) != -1)
            return irysc.gachesefid.Utility.Utility.generateErr(
                    "شما در آزمون موردنظر ثبت نام کرده اید."
            );

        PairValue p = Utilities.calcPrice(quiz.getInteger("price"),
                user.getInteger("money"), userId, OffCodeSections.GACH_EXAM.getName()
        );

        int price = (int) p.getKey();

        if (price <= 100) {

            new Thread(() -> {

                registry(user, quiz, 0);

                if (p.getValue() != null) {

                    PairValue offcode = (PairValue) p.getValue();
                    int finalAmount = (int) offcode.getValue();

                    BasicDBObject update = new BasicDBObject("used", true)
                            .append("used_at", System.currentTimeMillis())
                            .append("used_section", OffCodeSections.GACH_EXAM.getName())
                            .append("used_for", quiz.getObjectId("_id"))
                            .append("amount", finalAmount);

                    Document off = offcodeRepository.findOneAndUpdate(
                            (ObjectId) offcode.getKey(),
                            new BasicDBObject("$set", update)
                    );

                    if (off.getInteger("amount") != finalAmount) {

                        Document newDoc = new Document("type", off.getString("type"))
                                .append("amount", off.getInteger("amount") - finalAmount)
                                .append("expire_at", off.getInteger("expire_at"))
                                .append("section", off.getString("section"))
                                .append("used", false)
                                .append("created_at", System.currentTimeMillis())
                                .append("user_id", userId);

                        offcodeRepository.insertOne(newDoc);
                    }
                }

                transactionRepository.insertOne(
                        new Document("user_id", userId)
                                .append("amount", 0)
                                .append("created_at", System.currentTimeMillis())
                                .append("status", "success")
                                .append("for", OffCodeSections.GACH_EXAM.getName())
                                .append("off_code", p.getValue() == null ? null : ((PairValue) p.getValue()).getKey())
                );

            }).start();

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success"
            );
        }

        ObjectId transactionId = transactionRepository.insertOneWithReturnObjectId(
                new Document("user_id", userId)
                        .append("amount", price)
                        .append("created_at", System.currentTimeMillis())
                        .append("status", "init")
                        .append("for", OffCodeSections.GACH_EXAM.getName())
                        .append("off_code", p.getValue() == null ? null : ((PairValue) p.getValue()).getKey())
        );

        return Utilities.goToPayment(price, userId, transactionId);
    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("start", quiz.getLong("start"))
                .put("end", quiz.getLong("end"))
                .put("startRegistry", quiz.getLong("start_registry"))
                .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                .put("price", quiz.getInteger("price"))
                .put("generalMode", "IRYSC")
                .put("mode", quiz.getString("mode"))
                .put("launchMode", quiz.getString("launch_mode"))
                .put("tags", quiz.getString("tags"))
                .put("id", quiz.getObjectId("_id").toString());

        if(isAdmin)
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("capacity", quiz.getInteger("capacity"));

        try {
            jsonObject
                    .put("questionsCount", quiz.get("questions", Document.class)
                            .getList("_ids", ObjectId.class).size());
        }
        catch (Exception x) {
            jsonObject.put("questionsCount", 0);
        }

        if(quiz.containsKey("capacity"))
            jsonObject.put("reminder", Math.max(quiz.getInteger("capacity") - quiz.getInteger("registered"), 0));

        if(!isDigest) {
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""))
                    .put("topStudentsCount", quiz.getInteger("top_students_count"))
                    .put("showResultsAfterCorrection", quiz.getBoolean("show_results_after_correction"));
        }

        return jsonObject;
    }

    @Override
    Document registry(Document student, Document quiz, int paid) {

        List<Document> students = quiz.getList("students", Document.class);

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        ) != -1)
            return null;

        Document stdDoc = new Document("_id", student.getObjectId("_id"))
                .append("paid", paid)
                .append("register_at", System.currentTimeMillis())
                .append("finish_at", null)
                .append("start_at", null)
                .append("answers", new ArrayList<>());

        if ((boolean) quiz.getOrDefault("permute", false))
            stdDoc.put("question_indices", new ArrayList<>());

        students.add(stdDoc);

        return stdDoc;
        //todo : send notif
    }

    @Override
    void quit(Document student, Document quiz) {

        List<Document> students = quiz.getList("students", Document.class);
        int idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        );

        if (idx == -1)
            return;

        students.remove(idx);

        // todo: send notif
    }
}
