package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static irysc.gachesefid.Controllers.Quiz.Utility.filterFields;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

public class OpenQuizController {

    final static String[] mandatoryFields = {
            "price"
    };

    final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "isOnline", "showResultsAfterCorrect",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperSize", "database",
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {
            filterFields(mandatoryFields, forbiddenFields, jsonObject);
        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(e.getMessage());
        }

        jsonObject.put("mode", KindQuiz.OPEN.getName());
        Document newDoc;

        try {
            newDoc = QuizController.store(userId, jsonObject);
        }
        catch (Exception x) {
            return irysc.gachesefid.Utility.Utility.generateErr(x.getMessage());
        }

        return iryscQuizRepository.insertOneWithReturn(newDoc);
    }


}
