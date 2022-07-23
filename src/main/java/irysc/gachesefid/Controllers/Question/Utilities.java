package irysc.gachesefid.Controllers.Question;

import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;


public class Utilities {

    static void checkAnswer(JSONObject jsonObject) throws InvalidFieldsException {

        if(jsonObject.has("subjectId")) {

            if(!ObjectIdValidator.isValid(jsonObject.getString("subjectId")))
                throw new InvalidFieldsException("آی دی مبحث نامعتبر است.");

            ObjectId subjectId = new ObjectId(jsonObject.getString("subjectId"));

            if(subjectRepository.findById(subjectId) == null)
                throw new InvalidFieldsException("آی دی مبحث نامعتبر است.");

            jsonObject.put("subjectId", subjectId);
        }

        if(jsonObject.has("authorId")) {

            if(!ObjectIdValidator.isValid(jsonObject.getString("authorId")))
                throw new InvalidFieldsException("آی دی مولف نامعتبر است.");

            ObjectId authorId = new ObjectId(jsonObject.getString("authorId"));

            if(userRepository.findById(authorId) == null)
                throw new InvalidFieldsException("آی دی مولف نامعتبر است.");

            jsonObject.put("authorId", authorId);
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
                    !jsonObject.get("answer").toString().matches("[01]*")
            )
                throw new InvalidFieldsException("پاسخ سوال باید یک رشته از ۰ و ۱ باشد.");

            if(jsonObject.get("answer").toString().length() !=
                    jsonObject.getInt("sentencesCount")
            )
                throw new InvalidFieldsException("تعداد گزاره ها با پاسخ تعیین شده هماهنگ نیست.");

            jsonObject.put("answer", jsonObject.get("answer").toString());
        }


    }

    static JSONArray convertList(ArrayList<Document> docs,
                                 boolean isSubjectsNeeded,
                                 boolean isAuthorsNeeded,
                                 boolean isQuestionFileNeeded,
                                 boolean isAnswerFileNeeded) {

        JSONArray jsonArray = new JSONArray();
        HashMap<ObjectId, String> subjects = new HashMap<>();
        HashMap<ObjectId, String> authors = new HashMap<>();

        for(Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("answer", doc.get("answer"))
                    .put("neededTime", doc.getInteger("needed_time"))
                    .put("organizationId", doc.getString("organization_id"))
                    .put("level", doc.getString("level"));

            if(isQuestionFileNeeded && doc.containsKey("question_file"))
                jsonObject.put("questionFile", STATICS_SERVER + QuestionRepository.FOLDER + "/" + doc.getString("question_file"));

            if(isAnswerFileNeeded && doc.containsKey("answer_file"))
                jsonObject.put("answerFile", STATICS_SERVER + QuestionRepository.FOLDER + "/" + doc.getString("answer_file"));

            if(isSubjectsNeeded) {

                ObjectId subjectId = doc.getObjectId("subject_id");

                if (subjects.containsKey(subjectId))
                    jsonObject.put("subject", new JSONObject()
                            .put("id", subjectId)
                            .put("name", subjects.get(subjectId))
                    );
                else {

                    Document subject = subjectRepository.findById(subjectId);

                    jsonObject.put("subject", new JSONObject()
                            .put("id", subjectId)
                            .put("name", subject.getString("name"))
                    );

                    subjects.put(subjectId, subject.getString("name"));
                }
            }

            if(isAuthorsNeeded) {

                ObjectId authorId = doc.getObjectId("author_id");
                // todo : what should i put in json

                if (authors.containsKey(authorId))
                    jsonObject.put("author", new JSONObject()
                            .put("id", authorId)
                            .put("name", authors.get(authorId))
                    );
                else {

                    Document user = userRepository.findById(authorId);

                    jsonObject.put("author", new JSONObject()
                            .put("id", authorId)
                            .put("name", user.getString("name"))
                    );

                    authors.put(authorId, user.getString("name"));
                }

            }

            jsonObject
                    .put("visibility", doc.getBoolean("visibility"))
                    .put("kindQuestion", doc.getString("kind_question"));

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

}
