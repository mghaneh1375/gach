package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.*;

import static irysc.gachesefid.Controllers.Quiz.Utility.checkFields;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;

public class OpenQuizController {

    private final static String[] mandatoryFields = {
            "price", "priority"
    };

    private final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "launchMode", "showResultsAfterCorrection",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperTheme", "database",
    };

    private final static String[] forbiddenTransferFields = {
            "start_registry", "start", "_id",
            "end", "end_registry", "show_results_after_correction", "launch_mode",
            "show_results_after_correction_not_login_users",
            "permute", "visibility", "back_en", "mysql_id", "created_by",
            "created_at", "top_students_count", "students", "registered",
            "ranking_list", "report_status", "general_stat", "question_stat",
            "isRegistrable", "isUploadable", "kind"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            checkFields(mandatoryFields, forbiddenFields, jsonObject);
            Document newDoc = QuizController.store(userId, jsonObject, "irysc");
            openQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz", new OpenQuiz()
                            .convertDocToJSON(newDoc, false, true,
                                    false, false
                            )
            );

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(e.getMessage());
        }
    }

    public static String createFromIRYSCQuiz(ObjectId quizId) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        Document newQuiz = new Document("created_at", System.currentTimeMillis())
                .append("students", new ArrayList<>())
                .append("last_build_at", null)
                .append("last_finished_at", null)
                .append("registered", 0);


        List<String> forbiddenKeys = Arrays.asList(forbiddenTransferFields);

        for (String key : quiz.keySet()) {

            if(forbiddenKeys.contains(key))
                continue;

            newQuiz.append(key, quiz.get(key));
        }

        openQuizRepository.insertOne(newQuiz);
        return JSON_OK;
    }

}
