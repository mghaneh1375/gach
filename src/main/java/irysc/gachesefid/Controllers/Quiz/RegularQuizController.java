package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;


import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class RegularQuizController {

    // endRegistry consider as optional field

    // topStudentsGiftCoin or topStudentsGiftMoney or topStudentsCount
    // are optional and can inherit from config


    final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "isOnline", "showResultsAfterCorrect",
    };

    final static String[] forbiddenFields = {
            "paperSize", "database"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        for(String mandatoryFiled : mandatoryFields) {

            boolean find = false;

            for(String key : jsonObject.keySet()) {

                if(mandatoryFiled.equals(key)) {
                    find = true;
                    break;
                }
            }

            if(!find)
                return JSON_NOT_VALID_PARAMS;

        }

        jsonObject.put("mode", KindQuiz.REGULAR.getName());
        Document newDoc;

        try {
            newDoc = QuizController.store(userId, jsonObject);
        }
        catch (Exception x) {
            return irysc.gachesefid.Utility.Utility.generateErr(x.getMessage());
        }


        return iryscQuizRepository.insertOneWithReturn(newDoc);
    }

    public static String delete(ObjectId quizId, ObjectId userId) {

        Document quiz = iryscQuizRepository.findOneAndDelete(and(
                eq("_id", quizId),
                eq("mode", KindQuiz.REGULAR.getName())
        ));

        if(quiz == null)
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
}
