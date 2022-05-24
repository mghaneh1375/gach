package irysc.gachesefid.Controllers.Question;

import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.QuestionType;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;


public class Utilities {

    static void checkAnswer(JSONObject jsonObject) throws InvalidFieldsException {

        if(jsonObject.has("authorId")) {

            if(userRepository.findById(
                    new ObjectId(jsonObject.getString("authorId"))
            ) == null)
                throw new InvalidFieldsException("آی دی مولف نامعتبر است.");
        }

        if(questionRepository.exist(
                eq("organization_id", jsonObject.getString("organizationId"))
        ))
            throw new InvalidFieldsException("کد سازمانی سوال در سامانه موجود است.");

        if (jsonObject.getString("kindQuestion").equals(QuestionType.TEST.getName())) {

            if(!(jsonObject.get("answer") instanceof Integer))
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");

            if(jsonObject.getInt("answer") < 1 || jsonObject.getInt("answer") > jsonObject.getInt("choicesCount"))
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");

        }

        if (jsonObject.getString("kindQuestion").equals(QuestionType.SHORT_ANSWER.getName())) {

            if(!(jsonObject.get("answer") instanceof Number))
                throw new InvalidFieldsException("پاسخ سوال باید یک عدد باشد.");

        }

        if (jsonObject.getString("kindQuestion").equals(QuestionType.MULTI_SENTENCE.getName())) {

            if(!jsonObject.has("sentencesCount"))
                throw new InvalidFieldsException("تعداد گزاره ها را تعیین کنید.");

            if(!(jsonObject.get("answer") instanceof String) &&
                    !jsonObject.getString("answer").matches("[01]*")
            )
                throw new InvalidFieldsException("پاسخ سوال باید یک رشته از ۰ و ۱ باشد.");

            if(jsonObject.getString("answer").length() !=
                    jsonObject.getInt("sentencesCount")
            )
                throw new InvalidFieldsException("تعداد گزاره ها با پاسخ تعیین شده هماهنگ نیست.");
        }


    }

}
