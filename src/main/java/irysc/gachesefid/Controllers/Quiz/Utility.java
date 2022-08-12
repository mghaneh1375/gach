package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindAnswer;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
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

            long duration = quiz.getInteger("duration");
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
                                  Document questions,
                                  List<Document> studentAnswers,
                                  String folder) {

        JSONArray questionsJSON = new JSONArray();

//        for (Document question : questions) {
//
//            JSONObject questionObj = convertQuestionToJSON(question, folder, owner);
//
//            if (studentAnswers != null) {
//
//                Document studentAnswer = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
//                        studentAnswers, "question_id", question.getObjectId("_id")
//                );
//
//                JSONObject studentAnswerObj = new JSONObject()
//                        .put("id", studentAnswer.getObjectId("_id").toString())
//                        .put("answer", studentAnswer.get("answer"))
//                        .put("answerAt", irysc.gachesefid.Utility.Utility.getSolarDate(studentAnswer.getLong("answer_at")));
//
//                if (studentAnswer.containsKey("mark") && (owner || showResults)) {
//                    studentAnswerObj.put("mark", getMark(studentAnswer.get("mark")));
//                    studentAnswerObj.put("markDesc", studentAnswer.getOrDefault("mark_desc", ""));
//                }
//
//                if (
//                        question.getString("answer_type").equals(KindAnswer.FILE.getName())
//                )
//                    if (!studentAnswer.containsKey("answer") ||
//                            studentAnswer.getString("answer") == null ||
//                            studentAnswer.getString("answer").isEmpty())
//                        studentAnswerObj.put("answer", "");
//                    else
//                        studentAnswerObj.put("answer",
//                                StaticValues.STATICS_SERVER + folder + "/studentAnswers/" +
//                                        studentAnswer.getString("answer")
//                        );
//                else
//                    studentAnswerObj.put("answer",
//                            !studentAnswer.containsKey("answer") || studentAnswer.get("answer") == null ?
//                                    "" : studentAnswer.get("answer")
//                    );
//
//
//                questionObj.put("studentAnswer", studentAnswerObj);
//            }
//
//            questionsJSON.put(questionObj);
//        }

        return questionsJSON;
    }


    public static byte[] getByteArrFromCharArr(char[] sentences) {

        BitSet bitSet = new BitSet(sentences.length * 2);
        int k = 0;

        for (char sentence : sentences) {

            if (sentence == '_')
                bitSet.set(k, k + 2, false);
            else if (sentence == '1')
                bitSet.set(k, k + 2, true);
            else {
                bitSet.set(k, true);
                bitSet.set(k + 1, false);
            }
            k += 2;
        }

        byte[] sentencesByteArr = bitSet.toByteArray();
        byte[] out = new byte[sentencesByteArr.length + 1];

        out[0] = (byte) sentences.length;
        System.arraycopy(sentencesByteArr, 0, out, 1, out.length - 1);

        return out;
    }

    public static ArrayList<PairValue> getAnswers(byte[] bytes) {

        ArrayList<PairValue> numbers = new ArrayList<>();

        int currIdx = 0;
        int next;

        while (currIdx < bytes.length) {

            // TEST
            if (bytes[currIdx] == 0x00) {

                int i = currIdx + 1;
                while (i < bytes.length && bytes[i] != (byte) 0xff)
                    i++;

                for (int j = currIdx + 1; j < i; j++) {
                    int choicesCount = (bytes[j] & 0xf0) >> 4;
                    int ans = bytes[j] & 0x0f;
                    numbers.add(new PairValue(QuestionType.TEST.getName(), new PairValue(choicesCount, ans)));
                }

                next = i + 1;
            }
            //SHORT_ANSWER
            else if (bytes[currIdx] == 0x01) {
                byte[] tmp = Arrays.copyOfRange(bytes, currIdx + 1, currIdx + 9);
                numbers.add(new PairValue(QuestionType.SHORT_ANSWER.getName(), ByteBuffer.wrap(tmp).getDouble()));
                next = currIdx + 9;
            }
            // MULTI_SENTENCE
            else if (bytes[currIdx] == 0x02) {

                int sentencesCount = bytes[currIdx + 1] & 0xff;
                int streamLen = (int) Math.ceil(sentencesCount / 4.0);

                next = currIdx + 1 + streamLen + 1;
                int counter = 0;
                ArrayList<Character> booleans = new ArrayList<>();

                for (int j = currIdx + 2; j < next; j++) {

                    for (int k = 0; k < 8; k += 2) {

                        if (counter >= sentencesCount)
                            break;

                        int a = bytes[j] & ((1 << k) | (1 << k + 1));

                        if (a == 0)
                            booleans.add('_');
                        else if (a == 3 || a == 12 || a == 48 || a == 192)
                            booleans.add('1');
                        else if (a == 1 || a == 4 || a == 16 || a == 64)
                            booleans.add('0');
                        counter++;
                    }

                    if (counter >= sentencesCount)
                        break;
                }

                numbers.add(new PairValue(QuestionType.MULTI_SENTENCE, booleans));
            } else {
                break;
            }

            currIdx = next;
        }

        return numbers;
    }

    public static byte[] getByteArr(Object o) {

        if (o instanceof JSONArray) {
            ArrayList<Object> tmp = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) o;
            for (int i = 0; i < jsonArray.length(); i++)
                tmp.add(jsonArray.get(i));

            return getByteArr(tmp);
        }

        byte[] output = null;

        if (o instanceof Double) {
            output = new byte[8];
            long lng = Double.doubleToLongBits((Double) o);
            for (int i = 0; i < 8; i++) output[i] = (byte) ((lng >> ((7 - i) * 8)) & 0xff);
        } else if (o instanceof PairValue) {
            output = new byte[1];
            output[0] = convertPairValueToByte((PairValue) o);
        } else if (o instanceof ArrayList) {
            if (((ArrayList) o).get(0) instanceof PairValue) {
                output = new byte[((ArrayList) o).size()];
                int idx = 0;

                for (Object num : (ArrayList) o)
                    output[idx++] = convertPairValueToByte((PairValue) num);
            }
        }
        else if(o instanceof char[])
            return getByteArrFromCharArr((char[]) o);

        return output;
    }

    private static byte convertPairValueToByte(PairValue p) {

        int choicesCount = (int) p.getKey();
        int ans = (int) p.getValue();

        byte firstSection = (byte) (choicesCount == 0 ?
                0x00 : choicesCount == 1 ? 0x10 :
                choicesCount == 2 ? 0x20 :
                        choicesCount == 3 ? 0x30 :
                                choicesCount == 4 ? 0x40 :
                                        choicesCount == 5 ? 0x50 :
                                                choicesCount == 6 ? 0x60 :
                                                        choicesCount == 7 ? 0x70 : 0x80);

        return (byte) (((byte) (short) ans) | firstSection);
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

            bytes.add(Utilities.convertTypeToByte(questions.get(i).getString("kind_question")));

            if (questions.get(i).getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName())) {

                ArrayList<PairValue> answers = new ArrayList<>();

                answers.add(
                        new PairValue(
                                questions.get(i).getInteger("choices_count"),
                                questions.get(i).getInteger("answer")
                        )
                );

                int j;

                for (j = i + 1; j < ids.size(); j++) {

                    if (!questions.get(j).getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()))
                        break;

                    answers.add(
                            new PairValue(
                                    questions.get(j).getInteger("choices_count"),
                                    questions.get(j).getInteger("answer")
                            )
                    );
                }

                bytes.add(getByteArr(answers));
                bytes.add(new byte[]{(byte) 0xff});

                i = j;
            } else if (questions.get(i).getString("kind_question").equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
                bytes.add(getByteArr(questions.get(i).getDouble("answer")));
                i++;
            } else if (questions.get(i).getString("kind_question").equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
                bytes.add(getByteArr(questions.get(i).getString("answer").toCharArray()));
                i++;
            }
        }

        int neededSize = 0;
        for (byte[] itr : bytes)
            neededSize += itr.length;

        ByteBuffer buff = ByteBuffer.wrap(new byte[neededSize]);

        for (byte[] itr : bytes)
            buff.put(itr);

        return buff.array();
    }

    static byte[] addAnswerToByteArr(byte[] answers, String type, Object answer) {

        try {
            ByteBuffer buff;

            if (answers.length > 0 &&
                    answers[answers.length - 1] == (byte) 0xff &&
                    type.equalsIgnoreCase(QuestionType.TEST.getName())
            ) {
                buff = ByteBuffer.wrap(new byte[answers.length + 1]);
                answers[answers.length - 1] = convertPairValueToByte((PairValue) answer);
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
                    answerBytes = getByteArr(((String) answer).toCharArray());
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
        } catch (Exception x) {
            x.printStackTrace();
        }
        return answers;
    }
    
    
    public static byte[] getStdAnswersByteArr(ArrayList<PairValue> pairValues) {

        ArrayList<byte[]> bytes = new ArrayList<>();

        int i = 0;
        while (i < pairValues.size()) {

            String type = pairValues.get(i).getKey().toString();

            bytes.add(Utilities.convertTypeToByte(type));
            Object ans = pairValues.get(i).getValue();

            if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {

                ArrayList<PairValue> answers = new ArrayList<>();

                answers.add((PairValue) ans);
                int j;

                for (j = i + 1; j < pairValues.size(); j++) {

                    String tmpType = pairValues.get(j).getKey().toString();
                    if (!tmpType.equalsIgnoreCase(QuestionType.TEST.getName()))
                        break;

                    answers.add((PairValue) pairValues.get(j).getValue());
                }

                bytes.add(getByteArr(answers));
                bytes.add(new byte[]{(byte) 0xff});

                i = j;
            } else {
                bytes.add(getByteArr(ans));
                i++;
            }
//            else if(type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
//
//            }
//            else if(type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
//                bytes.add(getByteArr(ans));
//                i++;
//            }
        }

        int neededSize = 0;
        for (byte[] itr : bytes)
            neededSize += itr.length;

        ByteBuffer buff = ByteBuffer.wrap(new byte[neededSize]);

        for (byte[] itr : bytes)
            buff.put(itr);

        return buff.array();
    }


}
