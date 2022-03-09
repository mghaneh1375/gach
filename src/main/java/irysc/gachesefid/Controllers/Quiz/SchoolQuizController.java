package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static irysc.gachesefid.Controllers.Quiz.Utility.filterFields;

public class SchoolQuizController {

    final static String[] mandatoryFields = {
            "start", "end", "isOnline", "showResultsAfterCorrect",
    };

    final static String[] forbiddenFields = {
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "startRegistry", "price",
            "capacity", "endRegistry",
    };


    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {
            filterFields(mandatoryFields, forbiddenFields, jsonObject);
        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(e.getMessage());
        }

        if(jsonObject.)

        return "as";
    }
}
