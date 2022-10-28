package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Utility.Authorization;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Quiz.Utility.checkFields;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasProtectedAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class OpenQuizController {

    private final static String[] mandatoryFields = {
            "price", "duration"
    };

    private final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "isOnline", "showResultsAfterCorrection",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperTheme", "database",
    };

    private final static String[] forbiddenTransferFields = {
            "start_registry", "start", "_id",
            "end", "end_registry", "show_results_after_correction", "launch_mode",
            "permute", "visibility", "back_en", "mysql_id", "created_by",
            "created_at", "top_students_count", "students", "registered",
            "ranking_list", "report_status", "general_stat", "question_stat"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", KindQuiz.OPEN.getName());
            Document newDoc = QuizController.store(userId, jsonObject);

            return iryscQuizRepository.insertOneWithReturn(newDoc);

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
