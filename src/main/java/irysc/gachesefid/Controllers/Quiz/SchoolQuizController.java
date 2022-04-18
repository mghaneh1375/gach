package irysc.gachesefid.Controllers.Quiz;

import org.bson.types.ObjectId;
import org.json.JSONObject;

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

//        try {
//            filterFields(mandatoryFields, forbiddenFields, jsonObject);
//        } catch (InvalidFieldsException e) {
//            return irysc.gachesefid.Utility.Utility.generateErr(e.getMessage());
//        }
//
//        if(jsonObject.)

        return "as";
    }
}
