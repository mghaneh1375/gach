package irysc.gachesefid.Controllers.Question;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.DB.EscapeQuizQuestionRepository;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JUST_ID;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;


public class Utilities {

    public static void checkAnswer(JSONObject jsonObject) throws InvalidFieldsException {

        if (jsonObject.has("subjectId")) {

            if (!ObjectIdValidator.isValid(jsonObject.getString("subjectId")))
                throw new InvalidFieldsException("آی دی مبحث نامعتبر است.");

            ObjectId subjectId = new ObjectId(jsonObject.getString("subjectId"));

            if (subjectRepository.findById(subjectId) == null)
                throw new InvalidFieldsException("آی دی مبحث نامعتبر است.");

            jsonObject.put("subjectId", subjectId);
        }

        if (jsonObject.has("authorId")) {

            if (!ObjectIdValidator.isValid(jsonObject.getString("authorId")))
                throw new InvalidFieldsException("آی دی مولف نامعتبر است.");

            ObjectId authorId = new ObjectId(jsonObject.getString("authorId"));

            if (authorRepository.findById(authorId) == null)
                throw new InvalidFieldsException("آی دی مولف نامعتبر است.");

            jsonObject.put("authorId", authorId);
        }

        if (jsonObject.has("organizationId")) {
            if (questionRepository.exist(
                    eq("organization_id", jsonObject.getString("organizationId"))
            ))
                throw new InvalidFieldsException("کد سازمانی سوال در سامانه موجود است.");
        }

        if (!jsonObject.has("kindQuestion") ||
                jsonObject.getString("kindQuestion").equals(QuestionType.TEST.getName())
        ) {

            if (!(jsonObject.get("answer") instanceof Integer))
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");

            if (jsonObject.getInt("answer") < 1 || jsonObject.getInt("answer") > jsonObject.getInt("choicesCount"))
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");

        }

        if (jsonObject.has("kindQuestion") && jsonObject.getString("kindQuestion").equals(QuestionType.SHORT_ANSWER.getName())) {

            if (!(jsonObject.get("answer") instanceof Number))
                throw new InvalidFieldsException("پاسخ سوال باید یک عدد باشد.");

        }

        if (jsonObject.has("kindQuestion") && jsonObject.getString("kindQuestion").equals(QuestionType.MULTI_SENTENCE.getName())) {

            if (!jsonObject.has("sentencesCount"))
                throw new InvalidFieldsException("تعداد گزاره ها را تعیین کنید.");

            if (!(jsonObject.get("answer") instanceof String) &&
                    !jsonObject.get("answer").toString().matches("[01]*")
            )
                throw new InvalidFieldsException("پاسخ سوال باید یک رشته از ۰ و ۱ باشد.");

            if (jsonObject.get("answer").toString().length() !=
                    jsonObject.getInt("sentencesCount")
            )
                throw new InvalidFieldsException("تعداد گزاره ها با پاسخ تعیین شده هماهنگ نیست.");

            jsonObject.put("answer", jsonObject.get("answer").toString());
        }


    }

    public static JSONArray convertList(List<Document> docs,
                                        boolean isSubjectsNeeded,
                                        boolean isAuthorsNeeded,
                                        boolean isQuestionFileNeeded,
                                        boolean isAnswerFileNeeded,
                                        boolean isDetailNeeded,
                                        boolean isFromDataset) {

        JSONArray jsonArray = new JSONArray();
        HashMap<ObjectId, String> subjects = new HashMap<>();

        for (Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("no", doc.getInteger("no"))
                    .put("isPublic", doc.getOrDefault("is_public", true))
                    .put("level", doc.getString("level"))
                    .put("levelFa", doc.getString("level").equals("hard") ? "دشوار" :
                            doc.getString("level").equals("mid") ? "متوسط" : "آسان");

            if (doc.containsKey("stdAns")) {
                if (doc.get("stdAns") instanceof Double && Double.isNaN((Double) doc.get("stdAns")))
                    jsonObject.put("stdAns", "");
                else
                    jsonObject.put("stdAns", doc.get("stdAns"));
            }

            if (doc.containsKey("stdMark"))
                jsonObject.put("stdMark", doc.get("stdMark"));

            if (doc.containsKey("markDesc"))
                jsonObject.put("markDesc", doc.get("markDesc"));

            if (isDetailNeeded || isAnswerFileNeeded) {

                jsonObject.put("answer", doc.get("answer"))
                        .put("oldCorrect", doc.getInteger("old_correct"))
                        .put("oldIncorrect", doc.getInteger("old_incorrect"))
                        .put("oldWhite", doc.getInteger("old_white"));

                if (doc.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName()) && jsonObject.has("stdAns")) {
                    jsonObject.put("stdMark",
                            QuizAbstract.QuestionStat.getStdMarkInMultiSentenceQuestion(doc.getString("answer"), jsonObject.getString("stdAns"), doc.getDouble("mark")).getKey()
                    );
                } else if (doc.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()) && jsonObject.has("stdAns")) {

                    try {
                        double stdAns = jsonObject.getNumber("stdAns").doubleValue();

                        if (doc.getDouble("answer") - doc.getDouble("telorance") < stdAns &&
                                doc.getDouble("answer") + doc.getDouble("telorance") > stdAns
                        )
                            jsonObject.put("stdMark", doc.getDouble("mark"));
                        else
                            jsonObject.put("stdMark", 0);
                    }
                    catch (Exception err) {
                        jsonObject.put("stdMark", 0);
                    }
                }

            }

            if (isDetailNeeded) {
                jsonObject
                        .put("used", doc.getOrDefault("used", 0))
                        .put("neededTime", doc.get("needed_time"))
                        .put("visibility", doc.getBoolean("visibility"))
                        .put("organizationId", doc.getString("organization_id"));

                if (doc.containsKey("telorance"))
                    jsonObject.put("telorance", doc.get("telorance"));

                if (doc.containsKey("needed_line"))
                    jsonObject.put("neededLine", doc.get("needed_line"));

            }

            if (doc.containsKey("allMarked"))
                jsonObject.put("allMarked", doc.get("allMarked"));

            if (doc.containsKey("total"))
                jsonObject.put("total", doc.get("total"));

            if (doc.containsKey("mark"))
                jsonObject.put("mark", doc.get("mark"));

            if (doc.containsKey("corrector"))
                jsonObject.put("corrector", doc.get("corrector"));

            if (doc.containsKey("correctorId"))
                jsonObject.put("correctorId", doc.get("correctorId"));

            if (doc.containsKey("year"))
                jsonObject.put("year", doc.get("year"));

            if (doc.containsKey("tags"))
                jsonObject.put("tags", doc.get("tags"));

            if (doc.containsKey("choices_count"))
                jsonObject.put("choicesCount", doc.get("choices_count"));

            if (doc.containsKey("sentences_count"))
                jsonObject.put("sentencesCount", doc.get("sentences_count"));

            if (doc.containsKey("can_upload"))
                jsonObject.put("canUpload", doc.get("can_upload"));

            if (isQuestionFileNeeded && doc.containsKey("question_file")) {
                if (isFromDataset)
                    jsonObject.put("questionFile", STATICS_SERVER + QuestionRepository.FOLDER + "/" + doc.getString("question_file"));
                else
                    jsonObject.put("questionFile", STATICS_SERVER + "school_quizzes/questions/" + doc.getString("question_file"));
            }

            if (isAnswerFileNeeded && doc.containsKey("answer_file")) {
                if (isFromDataset)
                    jsonObject.put("answerFile", STATICS_SERVER + QuestionRepository.FOLDER + "/" + doc.getString("answer_file"));
                else
                    jsonObject.put("answerFile", STATICS_SERVER + "school_quizzes/questions/" + doc.getString("answer_file"));
            }

            if (isSubjectsNeeded) {

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

            if (isAuthorsNeeded)
                jsonObject.put("author", doc.getString("author"));

            jsonObject
                    .put("kindQuestion", doc.getOrDefault("kind_question", "test").toString());

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    public static JSONArray convertEscapeQuestionsList(List<Document> docs,
                                                       boolean isQuestionFileNeeded,
                                                       boolean isAnswerFileNeeded,
                                                       boolean isDetailNeeded) {

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("no", doc.getInteger("no"));

            if (doc.containsKey("stdAns")) {
                if (doc.get("stdAns") instanceof Double && Double.isNaN((Double) doc.get("stdAns")))
                    jsonObject.put("stdAns", "");
                else
                    jsonObject.put("stdAns", doc.get("stdAns"));
            }

            if (isDetailNeeded || isAnswerFileNeeded) {
                jsonObject.put("answer", doc.get("answer"));
            }

            if (isDetailNeeded) {
                jsonObject
                        .put("organizationId", doc.getString("organization_id"));
            }

            if (isQuestionFileNeeded && doc.containsKey("question_file")) {
                jsonObject.put("questionFile", STATICS_SERVER + EscapeQuizQuestionRepository.FOLDER + "/" + doc.getString("question_file"));
            }

            if (isAnswerFileNeeded && doc.containsKey("answer_file")) {
                jsonObject.put("answerFile", STATICS_SERVER + EscapeQuizQuestionRepository.FOLDER + "/" + doc.getString("answer_file"));
            }

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    public static byte[] convertTypeToByte(String type) {

        if (type.equalsIgnoreCase(QuestionType.TEST.getName()))
            return new byte[]{(byte) 0};

        if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
            return new byte[]{(byte) 1};

        if (type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName()))
            return new byte[]{(byte) 2};

        return new byte[]{(byte) 3};
    }

    public static ArrayList<Bson> fetchFilter(
            String tag,
            ObjectId gradeId,
            ObjectId lessonId,
            ObjectId subjectId,
            String level,
            String author
    ) throws InvalidFieldsException {

        ArrayList<Bson> filters = new ArrayList<>();

        filters.add(or(
                exists("is_public", false),
                eq("is_public", true)
        ));

        if (level != null) {

            if (!EnumValidatorImp.isValid(level, QuestionLevel.class))
                throw new InvalidFieldsException("level is not correct");

            filters.add(eq("level", level));
        }

        if (tag != null) {
            filters.add(
                    and(
                            exists("tags"),
                            in("tags", tag)
                    )
            );
        }

        if (gradeId != null || lessonId != null) {
            ArrayList<Bson> subFilters = new ArrayList<>();

            if (gradeId != null)
                subFilters.add(eq("grade._id", gradeId));

            if (lessonId != null)
                subFilters.add(eq("lesson._id", lessonId));

            ArrayList<Document> subjects = subjectRepository.find(
                    and(subFilters), JUST_ID
            );

            ArrayList<ObjectId> subjectIds = new ArrayList<>();

            for (Document subject : subjects)
                subjectIds.add(subject.getObjectId("_id"));

            filters.add(in("subject_id", subjectIds));
        }

        if (subjectId != null)
            filters.add(eq("subject_id", subjectId));

        if (author != null)
            filters.add(eq("author", author));

        return filters;
    }

    public static void updateQuestionsStat(ArrayList<Document> questions, List<Binary> questionsStat) {

        int idx = 0;
        List<WriteModel<Document>> writes = new ArrayList<>();

        for (Binary itr : questionsStat) {
            writes.add(updateQuestionStat(itr.getData(), questions.get(idx)));
            questionRepository.clearFromCache(questions.get(idx).getObjectId("_id"));
            idx++;
        }

        questionRepository.bulkWrite(writes);
    }

    public static void updateQuestionsStatWithByteArr(ArrayList<Document> questions, List<byte[]> questionsStat) {

        int idx = 0;
        List<WriteModel<Document>> writes = new ArrayList<>();

        for (byte[] itr : questionsStat) {
            writes.add(updateQuestionStat(itr, questions.get(idx)));
            questionRepository.clearFromCache(questions.get(idx).getObjectId("_id"));
            idx++;
        }

        questionRepository.bulkWrite(writes);
    }

    private static UpdateOneModel<Document> updateQuestionStat(byte[] bytes, Document question) {

        int whites = (bytes[0] & 0xff);
        int corrects = (bytes[1] & 0xff);
        int incorrects = (bytes[2] & 0xff);

        int oldWhite = (int) question.getOrDefault("old_white", 0) + whites;
        int oldCorrect = (int) question.getOrDefault("old_correct", 0) + corrects;
        int oldIncorrect = (int) question.getOrDefault("old_incorrect", 0) + incorrects;
        int total = oldCorrect + oldIncorrect + oldWhite;

        String level = question.getString("level");

        if (total < 100) {

            double p = (oldCorrect * 100.0) / total;

            level = "easy";
            if (p < 0.25)
                level = "hard";
            else if (p < 0.5)
                level = "mid";
        }

        return new UpdateOneModel<>(
                eq("_id", question.getObjectId("_id")),
                new BasicDBObject("$set", new BasicDBObject(
                        "level", level
                )
                        .append("old_correct", oldCorrect)
                        .append("old_incorrect", oldIncorrect)
                        .append("old_white", oldWhite)
                )
        );

    }

}
