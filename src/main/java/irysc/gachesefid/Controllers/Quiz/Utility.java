package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.DescMode;
import org.bson.Document;
import org.json.JSONObject;


public class Utility {

    static void isValid(Document quiz) throws Exception {

        if (quiz.containsKey("duration") &&
                quiz.containsKey("start") &&
                quiz.containsKey("end")
        ) {

            long duration = quiz.getLong("duration");
            long start = quiz.getLong("start");
            long end = quiz.getLong("end");

            if ((end - start) / (60000) < duration)
                throw new InvalidFieldsException("فاصله زمانی بین آغاز و پایان آزمون موردنظر باید حداقل " + duration + " ثانبه باشد.");

        }

//        if(
//                !quiz.getString("desc_after_mode").equals(DescMode.NONE.getName()) &&
//                        !quiz.containsKey()
//        )

    }

    static void filterFields(String[] mandatoryFields,
                             String[] forbiddenFields,
                             JSONObject jsonObject
    ) throws InvalidFieldsException {

        for (String forbiddenField : forbiddenFields) {

            for (String key : jsonObject.keySet()) {
                if (forbiddenField.equals(key))
                    throw new InvalidFieldsException("not proper fields");
            }
        }

        for (String mandatoryFiled : mandatoryFields) {

            boolean find = false;

            for (String key : jsonObject.keySet()) {

                if (mandatoryFiled.equals(key)) {
                    find = true;
                    break;
                }
            }

            if (!find)
                throw new InvalidFieldsException("not proper fields");

        }

    }

}
