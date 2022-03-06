package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.RegularQuizRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.regularQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;

public class AdminReportController {

    public static String generalQuizReport(String access, int reportNo, ObjectId userId, ObjectId quizId, String quizMode) {

        Document quiz = regularQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        if(
                (access.equals("school") && !quiz.getObjectId("schoolId").equals(userId)) ||
                        (access.equals("namayande") && !quiz.getObjectId("namayandeId").equals(userId))
        )
            return JSON_NOT_ACCESS;

        switch (reportNo) {
            case 5:
                return A5(quiz);
        }

        return JSON_NOT_VALID_ID;
    }

    private static String A5(Document quiz) {

        JSONObject result = new JSONObject();

        return result.put("status", "ok").toString();
    }


}
