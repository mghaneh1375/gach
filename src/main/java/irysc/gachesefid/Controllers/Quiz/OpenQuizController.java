package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.regex;
import static irysc.gachesefid.Controllers.Quiz.Utility.checkFields;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class OpenQuizController {

    private final static String[] mandatoryFields = {
            "price", "priority"
    };

    private final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "launchMode", "showResultsAfterCorrection",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperTheme", "database",
            "maxTeams", "perTeam", "maxTry", "shouldComplete"
    };

    private final static String[] forbiddenTransferFields = {
            "start_registry", "start", "_id",
            "end", "end_registry", "show_results_after_correction", "launch_mode",
            "show_results_after_correction_not_login_users",
            "permute", "visibility", "back_en", "mysql_id", "created_by",
            "created_at", "top_students_count", "students", "registered",
            "ranking_list", "report_status", "general_stat", "question_stat",
            "isRegistrable", "isUploadable", "kind",
            "is_registrable", "is_uploadable"
    };

    private final static String[] forbiddenCopyFields = {
            "rate", "rate_count", "removed_questions",
            "questions", "last_build_at", "last_finished_at",
            "title"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            checkFields(mandatoryFields, forbiddenFields, jsonObject);
            Document newDoc = QuizController.store(userId, jsonObject, "irysc");
            newDoc.put("visibility", true);

            openQuizRepository.insertOne(newDoc);

            return generateSuccessMsg(
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
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        Document newQuiz = new Document("created_at", System.currentTimeMillis())
                .append("students", new ArrayList<>())
                .append("last_build_at", null)
                .append("last_finished_at", null)
                .append("registered", 0);


        List<String> forbiddenKeys = Arrays.asList(forbiddenTransferFields);

        for (String key : quiz.keySet()) {

            if (forbiddenKeys.contains(key))
                continue;

            newQuiz.append(key, quiz.get(key));
        }

        newQuiz.put("visibility", true);
        openQuizRepository.insertOne(newQuiz);
        return JSON_OK;
    }
    public static String copy(ObjectId userId, ObjectId quizId, JSONObject data) {

        Document quiz = openQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        Document newQuiz = new Document("created_at", System.currentTimeMillis())
                .append("students", new ArrayList<>())
                .append("last_build_at", null)
                .append("last_finished_at", null)
                .append("questions", new Document())
                .append("title", data.getString("title"))
                .append("created_by", userId)
                .append("registered", 0);

        List<String> forbiddenKeys = Arrays.asList(forbiddenTransferFields);
        forbiddenKeys.addAll(Arrays.asList(forbiddenCopyFields));

        for (String key : quiz.keySet()) {
            if (forbiddenKeys.contains(key))
                continue;
            newQuiz.append(key, quiz.get(key));
        }

        if(data.has("description"))
            newQuiz.put("description", data.getString("description"));
        newQuiz.put("visibility", true);
        openQuizRepository.insertOne(newQuiz);
        return JSON_OK;
    }

    public static String getAllForAdmin(String tag, Integer pageIndex, Integer pageSize) {
        ArrayList<Document> quizzes = pageIndex != null && pageSize != null ?
                openQuizRepository.findLimited(
                        tag != null ? regex("tag", Pattern.compile(Pattern.quote(tag), Pattern.CASE_INSENSITIVE)) : exists("_id"),
                        QUIZ_DIGEST, Sorts.ascending("created_at"),
                        (pageIndex - 1) * pageIndex,
                        pageSize
                ) :
                openQuizRepository.find(
                        tag != null ? regex("tag", Pattern.compile(Pattern.quote(tag), Pattern.CASE_INSENSITIVE)) : null,
                        QUIZ_DIGEST
                );
        OpenQuiz openQuiz = new OpenQuiz();
        JSONArray jsonArray = new JSONArray();
        quizzes.forEach(document -> jsonArray.put(openQuiz.convertDocToJSON(document, true, true, false, false)));
        return generateSuccessMsg("data", new JSONObject()
                .put("items", jsonArray)
                .put("tags", openQuizRepository.distinctTags("tags"))
        );
    }
}
