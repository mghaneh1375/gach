package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindAnswer;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.*;

import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
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


    private static byte[] getByteArrFromBooleans(ArrayList<Boolean> sentences) {

        int sum = 0;
        for(int i = 0; i < sentences.size(); i++) {
            if(sentences.get(i))
                sum += Math.pow(2, i);
        }

        int needed = (int) Math.ceil(sentences.size() / 8.0);

        byte[] tmp;

        if(needed == 1)
            tmp = new byte[] {(byte) sum};

        else if(needed == 2)
            tmp = new byte[] { (byte)(sum >>> 8), (byte) sum};

        else if(needed == 3)
            tmp = new byte[] { (byte)(sum >>> 16), (byte)(sum >>> 8), (byte) sum};

        else
            tmp = new byte[] { (byte)(sum >>> 24), (byte)(sum >>> 16), (byte)(sum >>> 8), (byte) sum};

        byte[] out = new byte[tmp.length + 1];
        out[0] = (byte) sentences.size();
        System.arraycopy(tmp, 0, out, 1, out.length - 1);

        return  out;
    }

    public static ArrayList<PairValue> getAnswers(byte[] bytes) {

        ArrayList<PairValue> numbers = new ArrayList<>();

        int currIdx = 0;
        int next = 1;

        while (currIdx < bytes.length) {

            if(bytes[currIdx] == 0x00) {

                int i = currIdx + 1;
                while(i < bytes.length && bytes[i] != (byte)0xff)
                    i++;

                for(int j = currIdx + 1; j < i; j++)
                    numbers.add(new PairValue(QuestionType.TEST.getName(), bytes[j] & 0xff));

                next = i + 1;
            }
            else if(bytes[currIdx] == 0x01) {
                byte[] tmp = Arrays.copyOfRange(bytes, currIdx + 1, currIdx + 9);
                numbers.add(new PairValue(QuestionType.SHORT_ANSWER.getName(), ByteBuffer.wrap(tmp).getDouble()));
                next = currIdx + 9;
            }
            else if(bytes[currIdx] == 0x02) {

                int senetencesCount = bytes[currIdx + 1] & 0xff;

                next = currIdx + 1 + (int) Math.ceil(senetencesCount / 8.0) + 1;
                int counter = 0;
                ArrayList<Boolean> booleans = new ArrayList<>();

                for(int j = next - 1; j >= currIdx + 2; j--) {

                    for (int k = 0; k < 8 ; k++){

                        if(counter == senetencesCount)
                            break;

                        booleans.add((bytes[j] & (1 << k)) != 0);
                        counter++;
                    }

                    if(counter == senetencesCount)
                        break;
                }

                numbers.add(new PairValue(QuestionType.MULTI_SENTENCE, booleans));
            }

            currIdx = next;
        }

        return numbers;
    }

    public static byte[] getByteArr(Object o) {

        if(o instanceof JSONArray) {
            ArrayList<Object> tmp = new ArrayList<>();
            JSONArray jsonArray = (JSONArray)o;
            for(int i = 0; i < jsonArray.length(); i++)
                tmp.add(jsonArray.get(i));

            return getByteArr(tmp);
        }

        byte[] output = null;

        if(o instanceof Double) {
            output = new byte[8];
            long lng = Double.doubleToLongBits((Double) o);
            for (int i = 0; i < 8; i++) output[i] = (byte) ((lng >> ((7 - i) * 8)) & 0xff);
        }

        else if(o instanceof Integer) {
            int a = (int) o;
            output = new byte[]{(byte) a};
        }

        else if(o instanceof ArrayList) {
            if(((ArrayList) o).get(0) instanceof Integer) {
                output = new byte[((ArrayList) o).size()];
                int idx = 0;

                for (Object num : (ArrayList) o)
                    output[idx++] = (byte) num;
            }
            else
                return getByteArrFromBooleans((ArrayList<Boolean>) o);
        }

        return output;
    }

    public static byte[] getAnswersByteArr(List<ObjectId> ids) {

        ArrayList<byte[]> bytes = new ArrayList<>();
        ArrayList<Document> questions = new ArrayList<>();

        for (ObjectId id : ids) {

            Document question = questionRepository.findById(id);

            if (question == null)
                continue;

            questions.add(question);
        }

        int i = 0;
        while (i < questions.size()) {

            bytes.add(Utilities.convertTypeToByte(questions.get(i).getString("type")));

            if(questions.get(i).getString("type").equalsIgnoreCase(QuestionType.TEST.getName())) {

                ArrayList<Integer> answers = new ArrayList<>();

                answers.add(questions.get(i).getInteger("answer"));

                int j;

                for(j = i + 1; j < ids.size(); j++) {

                    if(!questions.get(j).getString("type").equalsIgnoreCase(QuestionType.TEST.getName()))
                        break;

                    answers.add(questions.get(j).getInteger("answer"));
                }

                bytes.add(getByteArr(answers));
                bytes.add(new byte[] {(byte) 0xff});

                i = j;
            }
            else if(questions.get(i).getString("type").equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
                bytes.add(getByteArr(questions.get(i).getDouble("answer")));
                i++;
            }
            else if(questions.get(i).getString("type").equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {

                ArrayList<Boolean> answers = new ArrayList<>();
                String str = questions.get(i).getString("answer");

                for(int j = 0; j < str.length(); j++) {
                    if(str.charAt(j) == '1')
                        answers.add(true);
                    else
                        answers.add(false);
                }

                bytes.add(getByteArr(answers));
                i++;
            }
        }

        int neededSize = 0;
        for(byte[] itr : bytes)
            neededSize += itr.length;

        ByteBuffer buff = ByteBuffer.wrap(new byte[neededSize]);

        for(byte[] itr : bytes)
            buff.put(itr);

        return buff.array();
    }

    public static byte[] addAnswerToByteArr(byte[] answers, String type, Object answer) {

        try {
            ByteBuffer buff;

            if (answers.length > 0 &&
                    answers[answers.length - 1] == (byte) 0xff &&
                    type.equalsIgnoreCase(QuestionType.TEST.getName())
            ) {
                buff = ByteBuffer.wrap(new byte[answers.length + 1]);
                answers[answers.length - 1] = (byte) (int)answer;
                buff.put(answers);
                buff.put((byte) 0xff);
            } else {
                byte[] typeBytes = Utilities.convertTypeToByte(type);
                byte[] answerBytes = null;
                int neededSize = answers.length + 1;

                if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
                    neededSize += 8;
                    answerBytes = getByteArr(answer);
                } else if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {
                    neededSize += 2;
                    answerBytes = getByteArr(answer);
                } else if (type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
                    String str = (String) answer;
                    ArrayList<Boolean> vals = new ArrayList<>();

                    for (int i = 0; i < str.length(); i++)
                        vals.add(str.charAt(i) == '1');

                    answerBytes = getByteArr(vals);
                    neededSize += answerBytes.length;
                }

                buff = ByteBuffer.wrap(new byte[neededSize]);
                buff.put(answers);
                buff.put(typeBytes);
                if (answerBytes != null)
                    buff.put(answerBytes);

                if (type.equalsIgnoreCase(QuestionType.TEST.getName()))
                    buff.put((byte) 0xff);
            }

            return buff.array();
        }
        catch (Exception x) {
            x.printStackTrace();
        }
        return answers;
    }

}
