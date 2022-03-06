package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.DB.RegularQuizRepository;
import irysc.gachesefid.DB.RegularQuizResultRepository;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static irysc.gachesefid.Main.GachesefidApplication.regularQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

public class RegularQuizController {

    public static String create(ObjectId userId, JSONObject jsonObject) {
        return null;
    }

    public static void delete(ObjectId quizId, ObjectId userId, String access) {

//        if(access.equals("admin")) {
//            RegularQuizRepository.delete(KindQuiz.REGULAR.getName(), quizId);
//            return;
//        }
//
//        RegularQuizRepository.delete(KindQuiz.REGULAR.getName(), quizId, userId);
    }

    public static String createKarname(ObjectId userId, String access, ObjectId quizId) {

        Document quiz = regularQuizRepository.findById(quizId);

        if(quiz == null ||
                (!quiz.getObjectId("user_id").equals(userId) && !access.equals("admin")))
            return JSON_NOT_ACCESS;

        String[] questionIdsStr = quiz.getString("questions").split("-");
        int[] questionIds = new int[questionIdsStr.length];

        for(int i = 0; i < questionIdsStr.length; i++)
            questionIds[i] = Integer.parseInt(questionIdsStr[i]);

        HashMap<Integer, Question> questionsInfo = QuestionRepository.getAnswers(questionIds);
        ArrayList<Document> studentResults = RegularQuizResultRepository.getRegularQuizResults(quizId);

        for(Document studentResult : studentResults) {

            String[] studentAnswersStr = studentResult.getString("student_answers").split("-");
            String[] studentQuestionIdsStr = studentResult.getString("question_ids").split("-");

            if(studentAnswersStr.length != studentQuestionIdsStr.length) {
                System.out.println("heyyy corrupt");
                continue;
            }

            for(int i = 0; i < studentAnswersStr.length; i++) {
                questionsInfo.get(Integer.parseInt(studentQuestionIdsStr[i]));
            }

        }

        return JSON_OK;
    }

    public static String myQuizes(ObjectId userId) {
        return null;
    }

    public static String myPassedQuizes(ObjectId userId) {
        return null;
    }
}
