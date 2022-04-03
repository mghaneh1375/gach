package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindAnswer;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;


public class Utility {

    static void checkFields(String[] mandatoryFields, String[] forbiddenFields, JSONObject jsonObject
    ) throws InvalidFieldsException {

        Set<String> keys = jsonObject.keySet();
        boolean error = false;

        for(String mandatoryFiled : mandatoryFields) {

            if(!keys.contains(mandatoryFiled)) {
                error = true;
                break;
            }

        }

        if(error)
            throw new InvalidFieldsException(JSON_NOT_VALID_PARAMS);

        for(String forbiddenField : forbiddenFields) {

            if(keys.contains(forbiddenField)) {
                error = true;
                break;
            }

        }

        if(error)
            throw new InvalidFieldsException(JSON_NOT_VALID_PARAMS);
    }

    static void isValid(Document quiz) throws InvalidFieldsException {

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

        if (quiz.containsKey("desc_after_mode") &&
                quiz.getString("desc_after_mode").equals(DescMode.FILE.getName()) &&
                quiz.containsKey("desc_after")
        )
            throw new InvalidFieldsException("زمانی که فایل توضیحات بعد آزمون را بر روی فایل ست می کنید نباید فیلد descAfter را ست نمایید.");

//        if(
//                !quiz.getString("desc_after_mode").equals(DescMode.NONE.getName()) &&
//                        !quiz.containsKey()
//        )

    }

    static JSONObject convertQuizToJSONInList(Document quiz) {

        JSONObject jsonObject = new JSONObject();


        jsonObject.put("studentsCount", quiz.getList("students", Document.class).size());
        jsonObject.put("id", quiz.getObjectId("_id").toString());

        return jsonObject;
    }

    static JSONArray getQuestions(boolean owner, boolean showResults,
                                  List<Document> questions,
                                  List<Document> studentAnswers) {

        JSONArray questionsJSON = new JSONArray();

        int idx = 0;
        for (Document question : questions) {

            JSONObject questionObj;

            if (question.containsKey("group_idx") &&
                    question.getInteger("group_idx") > 0)
                questionObj = convertQuestionToJSON(question, -1, owner);
            else
                questionObj = convertQuestionToJSON(question, idx++, owner);

            if (studentAnswers != null) {

                Document studentAnswer = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        studentAnswers, "question_id", question.getObjectId("_id")
                );

                JSONObject studentAnswerObj = new JSONObject()
                        .put("id", studentAnswer.getObjectId("_id").toString())
                        .put("answer", studentAnswer.get("answer"))
                        .put("answerAt", Utility.getSolarDate(studentAnswer.getLong("answer_at")));

                if (studentAnswer.containsKey("mark") && (owner || showResults)) {
                    studentAnswerObj.put("mark", Utils.getMark(studentAnswer.get("mark")));
                    studentAnswerObj.put("markDesc", studentAnswer.getOrDefault("mark_desc", ""));
                }

                if (
                        question.getString("answer_type").equals(KindAnswer.FILE.getName()) ||
                                question.getString("answer_type").equals(KindAnswer.VOICE.getName())
                )
                    if (!studentAnswer.containsKey("answer") ||
                            studentAnswer.getString("answer") == null ||
                            studentAnswer.getString("answer").isEmpty())
                        studentAnswerObj.put("answer", "");
                    else
                        studentAnswerObj.put("answer",
                                StaticValues.STATICS_SERVER + LibraryQuizRepository.FOLDER + "/" +
                                        studentAnswer.getString("answer")
                        );
                else
                    studentAnswerObj.put("answer", !studentAnswer.containsKey("answer") || studentAnswer.get("answer") == null ?
                            "" : studentAnswer.get("answer")
                    );


                questionObj.put("studentAnswer", studentAnswerObj);
            }

            questionsJSON.put(questionObj);
        }

        return makeGroups(questionsJSON);
    }

}
