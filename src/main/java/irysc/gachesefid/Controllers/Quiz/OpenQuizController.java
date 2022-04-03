package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static irysc.gachesefid.Controllers.Quiz.Utility.checkFields;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;

public class OpenQuizController {

    private final static String[] mandatoryFields = {
            "price"
    };

    private final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "isOnline", "showResultsAfterCorrect",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperTheme", "database",
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


}
