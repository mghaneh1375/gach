package irysc.gachesefid.Controllers.Quiz;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

public class StudentReportController {

    public static String generalQuizReport(String mode, ObjectId userId, ObjectId quizId, String quizMode) {

//        Document quiz = regularQuizRepository.findById(quizId);
//        if(quiz == null)
//            return JSON_NOT_VALID_ID;
//
//        List<Document> students = quiz.getList("students", Document.class);
//        String result = null;
//
//        for(Document student : students) {
//            if(student.getObjectId("_id").equals(userId)) {
//                result = student.getString("result");
//                break;
//            }
//        }
//
//        if(result == null)
//            return JSON_NOT_ACCESS;
//
//        switch (mode) {
//            case "question":
//                return questionReport(quiz, result);
//            case "general":
//                break;
//        }
//
        return JSON_NOT_VALID_PARAMS;
    }

    private static String questionReport(Document quiz, String result) {

        String[] splitedResult = result.split("-");
        String quizAnswers = quiz.getString("quizAnswers");
        String[] splitedQuizAnswers = quizAnswers.split("-");
        JSONObject jsonObject = new JSONObject();

        if(splitedQuizAnswers.length != splitedResult.length)
            return jsonObject.put("status", "nok").put("msg", "student result size is not equal to quiz result size").toString();

        JSONArray jsonArray = new JSONArray();

        for(int i = 0; i < splitedQuizAnswers.length; i++) {
            JSONObject tmp = new JSONObject();
            tmp.put("student_answer", splitedResult[i]);
            tmp.put("question_answer", splitedQuizAnswers[i]);
            jsonArray.put(tmp);
        }

        return jsonObject.put("status", "ok").put("questions", jsonArray).toString();
    }


}
