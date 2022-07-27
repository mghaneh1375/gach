package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindAnswer;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.*;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;


public class Utility {

    static void checkFields(String[] mandatoryFields, String[] forbiddenFields, JSONObject jsonObject
    ) throws InvalidFieldsException {

        Set<String> keys = jsonObject.keySet();
        boolean error = false;

        for (String mandatoryFiled : mandatoryFields) {

            if (!keys.contains(mandatoryFiled)) {
                error = true;
                break;
            }

        }

        if (error)
            throw new InvalidFieldsException(JSON_NOT_VALID_PARAMS);

        for (String forbiddenField : forbiddenFields) {

            if (keys.contains(forbiddenField)) {
                error = true;
                break;
            }

        }

        if (error)
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

        if (quiz.containsKey("desc_mode") &&
                quiz.getString("desc_mode").equals(DescMode.FILE.getName()) &&
                quiz.containsKey("desc")
        )
            throw new InvalidFieldsException("زمانی که فایل توضیحات آزمون را بر روی فایل ست می کنید نباید فیلد desc را ست نمایید.");

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

    static JSONObject convertTashrihiQuizToJSONDigestForTeachers(Document quiz) {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("start", irysc.gachesefid.Utility.Utility.getSolarDate(quiz.getLong("start")));
        jsonObject.put("end", irysc.gachesefid.Utility.Utility.getSolarDate(quiz.getLong("end")));

        jsonObject.put("title", quiz.getString("title"));
        jsonObject.put("id", quiz.getObjectId("_id").toString());

        return jsonObject;
    }

    private static JSONObject convertQuestionToJSON(Document question, String folder, boolean owner) {

        JSONObject jsonObject = new JSONObject();

        if (owner) {
            for (String key : question.keySet()) {

                if (question.get(key) instanceof ObjectId)
                    jsonObject.put(irysc.gachesefid.Utility.Utility.camel(key, false), question.get(key).toString());
                else
                    jsonObject.put(irysc.gachesefid.Utility.Utility.camel(key, false), question.get(key));
            }

            if (jsonObject.has("correctAnswer") && jsonObject.getString("correctAnswerType").equals(KindAnswer.FILE.getName()))
                jsonObject.put("correctAnswer", StaticValues.STATICS_SERVER + folder + "/answers/" + jsonObject.getString("correctAnswer"));

        } else {

            jsonObject.put("descMode", question.getString("desc_mode"));

            if (question.containsKey("needed_time"))
                jsonObject.put("neededTime", question.getInteger("needed_time"));

            if (question.containsKey("question"))
                jsonObject.put("question", question.get("question"));

            if (question.containsKey("desc"))
                jsonObject.put("desc", question.get("desc"));

            jsonObject.put("id", question.getObjectId("_id").toString());
            jsonObject.put("text", question.getString("text"));
            jsonObject.put("choices", question.get("choices"));
            jsonObject.put("mark", question.get("mark"));
            jsonObject.put("answerType", question.getString("answer_type"));
            jsonObject.put("questionType", question.getString("question_type"));
            jsonObject.put("questionFile", question.getString("question_file"));
        }

        if (jsonObject.has("desc") && jsonObject.getString("descMode").equals(KindAnswer.FILE.getName()))
            jsonObject.put("desc", StaticValues.STATICS_SERVER + folder + "/descs/" + jsonObject.getString("desc"));

        if (jsonObject.has("questionFile"))
            jsonObject.put("questionFile", StaticValues.STATICS_SERVER + folder + "/questions/" + jsonObject.getString("questionFile"));

        return jsonObject;
    }

    static JSONArray getQuestions(boolean owner, boolean showResults,
                                  List<Document> questions,
                                  List<Document> studentAnswers,
                                  String folder) {

        JSONArray questionsJSON = new JSONArray();

        for (Document question : questions) {

            JSONObject questionObj = convertQuestionToJSON(question, folder, owner);

            if (studentAnswers != null) {

                Document studentAnswer = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        studentAnswers, "question_id", question.getObjectId("_id")
                );

                JSONObject studentAnswerObj = new JSONObject()
                        .put("id", studentAnswer.getObjectId("_id").toString())
                        .put("answer", studentAnswer.get("answer"))
                        .put("answerAt", irysc.gachesefid.Utility.Utility.getSolarDate(studentAnswer.getLong("answer_at")));

                if (studentAnswer.containsKey("mark") && (owner || showResults)) {
                    studentAnswerObj.put("mark", getMark(studentAnswer.get("mark")));
                    studentAnswerObj.put("markDesc", studentAnswer.getOrDefault("mark_desc", ""));
                }

                if (
                        question.getString("answer_type").equals(KindAnswer.FILE.getName())
                )
                    if (!studentAnswer.containsKey("answer") ||
                            studentAnswer.getString("answer") == null ||
                            studentAnswer.getString("answer").isEmpty())
                        studentAnswerObj.put("answer", "");
                    else
                        studentAnswerObj.put("answer",
                                StaticValues.STATICS_SERVER + folder + "/studentAnswers/" +
                                        studentAnswer.getString("answer")
                        );
                else
                    studentAnswerObj.put("answer",
                            !studentAnswer.containsKey("answer") || studentAnswer.get("answer") == null ?
                                    "" : studentAnswer.get("answer")
                    );


                questionObj.put("studentAnswer", studentAnswerObj);
            }

            questionsJSON.put(questionObj);
        }

        return questionsJSON;
    }

    public static Object getMark(Object mark) {

        if (mark instanceof Double)
            return String.format("%.2f", mark);

        return mark.toString();
    }

    public static byte[] getByteArr(ArrayList<Integer> numbers) {

        byte[] output = new byte[numbers.size()];
        int idx = 0;

        for (int num : numbers)
            output[idx++] = (byte) num;

        return output;
    }
    public static byte[] getByteArr(JSONArray numbers) {

        byte[] output = new byte[numbers.length()];

        for (int i = 0; i < numbers.length(); i++)
            output[i] = (byte) numbers.getInt(i);

        return output;
    }

    public static List<byte[]> tokens(byte[] array, byte[] delimiter) {
        List<byte[]> byteArrays = new LinkedList<>();
        if (delimiter.length == 0) {
            return byteArrays;
        }
        int begin = 0;

        outer:
        for (int i = 0; i < array.length - delimiter.length + 1; i++) {
            for (int j = 0; j < delimiter.length; j++) {
                if (array[i + j] != delimiter[j]) {
                    continue outer;
                }
            }
            byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + delimiter.length;
        }
        byteArrays.add(Arrays.copyOfRange(array, begin, array.length));
        return byteArrays;
    }

    public static ArrayList<Number> getNumbers(byte[] bytes) {

        byte[] delimeter = new byte[3];
        delimeter[0] = (byte) 0xff;
        delimeter[1] = (byte) 0xff;
        delimeter[2] = (byte) 0xff;

        System.out.println(bytes.length);

        ArrayList<Number> numbers = new ArrayList<>();
        List<byte[]> tokens = tokens(bytes, delimeter);
        System.out.println(tokens.size());

        for(byte[] itr : tokens) {

            byte type = itr[0];
            if(type == 0x00) {
                boolean first = true;
                for (byte aByte : itr) {
                    if(first) {
                        first = false;
                        continue;
                    }
                    numbers.add(aByte & 0xFF);
                }
            }
            else if(type == 0x01) {
//                itr = Arrays.copyOfRange(itr, 1, bytes.length);
//                System.out.println(itr.length);
                numbers.add(ByteBuffer.wrap(itr).getDouble());
            }
        }

        return numbers;
    }

    public static ArrayList<Integer> getNumbers(Binary binary) {

        ArrayList<Integer> numbers = new ArrayList<>();

        for (byte aByte : binary.getData())
            numbers.add(aByte & 0xFF);

        return numbers;
    }
}
