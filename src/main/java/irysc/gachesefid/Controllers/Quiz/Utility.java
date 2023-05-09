package irysc.gachesefid.Controllers.Quiz;


import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.util.*;

import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.schoolQuestionRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx;


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

    static JSONObject convertTashrihiQuizToJSONDigestForTeachers(Document quiz, Document corrector, String mode) {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("start", quiz.containsKey("start") ? irysc.gachesefid.Utility.Utility.getSolarDate(quiz.getLong("start")) : "...");
        jsonObject.put("end", quiz.containsKey("end") ? irysc.gachesefid.Utility.Utility.getSolarDate(quiz.getLong("end")) : "...");

        jsonObject.put("title", quiz.getString("title"));
        jsonObject.put("id", quiz.getObjectId("_id").toString());
        jsonObject.put("mode", mode);

        List<ObjectId> myStudents = corrector.containsKey("students") ?
                corrector.getList("students", ObjectId.class) :
                new ArrayList<>();

        List<ObjectId> myQuestions = corrector.containsKey("questions") ?
                corrector.getList("questions", ObjectId.class) :
                new ArrayList<>();

        List<Document> students = quiz.getList("students", Document.class);

        int allCorrectorQuestions = 0;
        int allMarkedQuestions = 0;

        for (Document student : students) {

            if (
                    (!myStudents.contains(student.getObjectId("_id")) && myQuestions.size() == 0) ||
                            !student.containsKey("answers")
            )
                continue;

            List<Document> answers = student.getList("answers", Document.class);
            for (Document answer : answers) {

                if (
                        myStudents.contains(student.getObjectId("_id")) ||
                                myQuestions.contains(answer.getObjectId("question_id"))
                ) {
                    allCorrectorQuestions++;
                    if (answer.containsKey("mark"))
                        allMarkedQuestions++;
                }
            }

        }

        jsonObject.put("allCorrectorQuestions", allCorrectorQuestions);
        jsonObject.put("allMarkedQuestions", allMarkedQuestions);

        return jsonObject;
    }


    static JSONArray getTashrihiQuestions(boolean owner, boolean showResults,
                                          boolean correctWithQR, Document questions,
                                          List<Document> studentAnswers, String folder
    ) {
        String prefix;

        if (correctWithQR)
            prefix = StaticValues.STATICS_SERVER;
        else
            prefix = StaticValues.STATICS_SERVER + folder + "/studentAnswers/";

        List<ObjectId> ids = questions.getList("_ids", ObjectId.class);
        List<Number> marks = questions.getList("marks", Number.class);

        List<Document> questionDocs = questionRepository.findByIds(ids, true);

        JSONArray questionsJSON = Utilities.convertList(questionDocs, true,
                true, true,
                true, true, true
        );

        for (int z = 0; z < questionsJSON.length(); z++) {

            JSONObject questionObj = questionsJSON.getJSONObject(z);

            questionObj.put("mark", marks.get(z));

            if (studentAnswers != null) {

                Document studentAnswer = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        studentAnswers, "question_id", questionDocs.get(z).getObjectId("_id")
                );

                if (studentAnswer != null) {

                    JSONObject studentAnswerObj = new JSONObject()
                            .put("id", studentAnswer.getObjectId("_id").toString())
                            .put("answerAt", studentAnswer.containsKey("answer_at") ?
                                    irysc.gachesefid.Utility.Utility.getSolarDate(studentAnswer.getLong("answer_at")) : ""
                            );

                    if (studentAnswer.containsKey("mark") && (owner || showResults)) {
                        studentAnswerObj.put("mark", studentAnswer.getDouble("mark"));
                        studentAnswerObj.put("markDesc", studentAnswer.getOrDefault("mark_desc", ""));
                    } else
                        studentAnswerObj.put("mark", -1);

                    if (studentAnswer.getOrDefault("type", "").toString().equalsIgnoreCase("file")) {
                        if (!studentAnswer.containsKey("answer") ||
                                studentAnswer.getString("answer") == null ||
                                studentAnswer.getString("answer").isEmpty()
                        )
                            studentAnswerObj.put("answer", "");
                        else {
                            studentAnswerObj.put("answer",
                                    prefix + studentAnswer.getString("answer")
                            ).put("type", "file");
                        }
                    } else
                        studentAnswerObj.put("answer",
                                !studentAnswer.containsKey("answer") || studentAnswer.get("answer") == null ?
                                        "" : studentAnswer.get("answer")
                        );


                    questionObj.put("stdAns", studentAnswerObj);
                }
                else
                    questionObj.put("stdAns", "");

            }
            else
                questionObj.put("stdAns", "");
        }

        return questionsJSON;
    }


    public static byte[] getByteArrFromCharArr(char[] sentences) {

        BitSet bitSet = new BitSet(sentences.length * 2);
        int bitSum = 0;
        int k = 0;

        for (char sentence : sentences) {

//            if (sentence == '_')
//                bitSet.set(k, k + 2, false);
//            if (sentence == '1')
//                bitSet.set(k, k + 2, true);
//            else {
//                bitSet.set(k, true);
//                bitSet.set(k + 1, false);
//            }

            if (sentence == '1')
                bitSum += (int) Math.pow(2, k) + (int) Math.pow(2, k + 1);
            else if (sentence == '0')
                bitSum += (int) Math.pow(2, k);

            k += 2;
        }

        byte[] b = ByteBuffer.allocate(4).putInt(bitSum).array();
        byte[] sentencesByteArr = new byte[(int) Math.ceil(sentences.length * 2 / 8.0)];
        for (int i = 0; i < sentencesByteArr.length; i++) {
            sentencesByteArr[i] = b[3 - i];
        }

        byte[] out = new byte[sentencesByteArr.length + 1];

        out[0] = (byte) sentences.length;
        System.arraycopy(sentencesByteArr, 0, out, 1, out.length - 1);

        return out;
    }

    public static ArrayList<PairValue> getAnswers(byte[] bytes) {

        if (bytes == null)
            return new ArrayList<>();

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

                if (sentencesCount == 255) {
                    sentencesCount = 5;
                    streamLen = 2;
                }

                next = currIdx + 1 + streamLen + 1;
                int counter = 0;
                ArrayList<Character> booleans = new ArrayList<>();

                for (int j = currIdx + 2; j < next; j++) {

                    for (int k = 0; k < 8; k += 2) {

                        if (counter >= sentencesCount)
                            break;

                        int a = bytes[j] & ((1 << k) | (1 << k + 1));

                        if (a == 3 || a == 12 || a == 48 || a == 192)
                            booleans.add('1');
                        else if (a == 1 || a == 4 || a == 16 || a == 64)
                            booleans.add('0');
                        else
                            booleans.add('_');

                        counter++;
                    }

                    if (counter >= sentencesCount)
                        break;
                }

                StringBuilder builder = new StringBuilder(booleans.size());
                for (Character ch : booleans)
                    builder.append(ch);

                numbers.add(new PairValue(QuestionType.MULTI_SENTENCE, builder.toString()));
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

        if (o == null || o instanceof Double) {
            output = new byte[8];
            if (o == null)
                for (int i = 0; i < 8; i++) output[i] = (byte) 0xff;
            else {
                long lng = Double.doubleToLongBits((Double) o);
                for (int i = 0; i < 8; i++) output[i] = (byte) ((lng >> ((7 - i) * 8)) & 0xff);
            }
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
        } else if (o instanceof char[]) {
            return getByteArrFromCharArr((char[]) o);
        }

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

        ArrayList<Document> questions;
        questions = new ArrayList<>();

        for (ObjectId id : ids) {

            Document question = questionRepository.findById(id);

            if (question == null)
                continue;

            questions.add(question);
        }

        return calcAnswerByteArr(questions);
    }

    static byte[] getSchoolAnswersByteArr(List<ObjectId> ids) {

        ArrayList<Document> questions;
        questions = new ArrayList<>();

        for (ObjectId id : ids) {

            Document question = schoolQuestionRepository.findById(id);

            if (question == null)
                continue;

            questions.add(question);
        }

        return calcAnswerByteArr(questions);
    }

    public static PairValue getAnswersByteArrWithNeededTime(List<ObjectId> ids) {

        ArrayList<Document> questions = questionRepository.findByIds(ids, true);

        int totalNeededTime = 0;
        for (Document question : questions)
            totalNeededTime += question.getInteger("needed_time");

        return new PairValue(totalNeededTime, calcAnswerByteArr(questions));
    }

    private static byte[] calcAnswerByteArr(ArrayList<Document> questions) {

        ArrayList<byte[]> bytes = new ArrayList<>();

        int i = 0;
        while (i < questions.size()) {

            bytes.add(Utilities.convertTypeToByte(questions.get(i).getOrDefault("kind_question", "test").toString()));

            if (questions.get(i).getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TEST.getName())) {

                ArrayList<PairValue> answers = new ArrayList<>();

                answers.add(
                        new PairValue(
                                questions.get(i).getInteger("choices_count"),
                                questions.get(i).getInteger("answer")
                        )
                );

                int j;

                for (j = i + 1; j < questions.size(); j++) {

                    if (!questions.get(j).getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TEST.getName()))
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
            } else if (questions.get(i).getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
                bytes.add(getByteArr(questions.get(i).getDouble("answer")));
                i++;
            } else if (questions.get(i).getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
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
                byte[] t = getByteArr(ans);
                bytes.add(t);
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
        for (byte[] itr : bytes) {
//            if(itr == null)
//                continue;
            neededSize += itr.length;
        }

        ByteBuffer buff = ByteBuffer.wrap(new byte[neededSize]);

        for (byte[] itr : bytes) {
//            if(itr == null)
//                continue;
            buff.put(itr);
        }

        return buff.array();
    }


    static void fillWithAnswerSheetData(JSONArray jsonArray,
                                        List<Binary> questionStat,
                                        List<PairValue> answers,
                                        List<Number> marks) {

        for (int i = 0; i < answers.size(); i++) {

            double percent = -1;

            if (questionStat != null) {
                byte[] bytes = questionStat.get(i).getData();
                percent = ((bytes[1] & 0xff) * 100.0) / ((bytes[1] & 0xff) + (bytes[0] & 0xff) + (bytes[2] & 0xff));
            }

            int choicesCount = -1;
            Object answer;

            if (answers.get(i).getKey().toString().equalsIgnoreCase(
                    QuestionType.TEST.getName()
            )) {
                PairValue pp = (PairValue) answers.get(i).getValue();
                choicesCount = (int) pp.getKey();
                answer = pp.getValue();
            } else
                answer = answers.get(i).getValue();

            JSONObject jsonObject = new JSONObject()
                    .put("type", answers.get(i).getKey())
                    .put("answer", answer)
                    .put("mark", marks.get(i));

            if (choicesCount != -1)
                jsonObject.put("choicesCount", choicesCount);

            if (percent != -1)
                jsonObject.put("percent", Math.round((percent * 100.0) / 100.0));

            jsonArray.put(jsonObject);
        }

    }

    static Document hasAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        if (userId != null && !quiz.getObjectId("created_by").equals(userId))
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        return quiz;
    }

    static Document hasPublicAccess(Common db, Object user, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        if (db instanceof IRYSCQuizRepository || user == null) {
            if (user != null && !quiz.getBoolean("visibility"))
                throw new InvalidFieldsException(JSON_NOT_ACCESS);
            return quiz;
        }

        if (user.toString().isEmpty())
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        ObjectId userId = (ObjectId) user;

        if (quiz.getObjectId("created_by").equals(userId))
            return quiz;

        if (!quiz.getBoolean("visibility"))
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        if (searchInDocumentsKeyValIdx(
                quiz.getList("students", Document.class),
                "_id", userId
        ) == -1)
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        return quiz;
    }

    static Document hasProtectedAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);

        if (quiz == null)
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        if (db instanceof IRYSCQuizRepository || userId == null) {

            if (userId != null && !quiz.getBoolean("visibility"))
                throw new InvalidFieldsException(JSON_NOT_ACCESS);

            if (userId != null && searchInDocumentsKeyValIdx(
                    quiz.getList("students", Document.class),
                    "_id", userId
            ) == -1)
                throw new InvalidFieldsException(JSON_NOT_ACCESS);

            return quiz;
        }

        if (db instanceof SchoolQuizRepository) {

            if(quiz.getObjectId("created_by").equals(userId))
                return quiz;

            else if(quiz.getString("status").equals("finish") && quiz.getBoolean("visibility") &&
                    searchInDocumentsKeyValIdx(
                            quiz.getList("students", Document.class),
                            "_id", userId
                    ) != -1
            )
                return quiz;

            else if(quiz.getString("status").equals("semi_finish") && quiz.getBoolean("visibility")) {

                Document studentDoc = searchInDocumentsKeyVal(
                        quiz.getList("students", Document.class),
                        "_id", userId
                );

                if(studentDoc != null && studentDoc.containsKey("paid"))
                    return quiz;

            }

            throw new InvalidFieldsException(JSON_NOT_ACCESS);
        }

        if (searchInDocumentsKeyValIdx(
                quiz.getList("students", Document.class),
                "_id", userId
        ) == -1)
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        return quiz;
    }

    static PairValue hasCorrectorAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null || !quiz.getOrDefault("mode", "regular").toString().equals(KindQuiz.TASHRIHI.getName()))
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        int idx = -1;

        if (userId != null && !quiz.getObjectId("created_by").equals(userId)) {

            List<Document> correctors = quiz.getList("correctors", Document.class);
            idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    correctors, "user_id", userId);

            if (idx == -1)
                throw new InvalidFieldsException(JSON_NOT_VALID_ID);
        }

        return new PairValue(quiz, idx);
    }

    static String saveStudentTashrihiAnswers(Document doc, JSONArray answers,
                                             List<Document> stdAnswers, Common db) {

        Document questions = doc.get("questions", Document.class);
        List<Boolean> uploadableList = questions.getList("uploadable_list", Boolean.class);
        List<ObjectId> ids = questions.getList("_ids", ObjectId.class);
        long curr = System.currentTimeMillis();

        if (uploadableList.size() < answers.length())
            return JSON_NOT_VALID_PARAMS;

        HashMap<ObjectId, String> answersHashMap = new HashMap<>();

        for (int i = 0; i < answers.length(); i++) {

            JSONObject jsonObject = answers.getJSONObject(i);
            if (!jsonObject.has("questionId") || !jsonObject.has("answer"))
                return JSON_NOT_VALID_PARAMS;

            String qId = jsonObject.getString("questionId");
            if (!ObjectId.isValid(qId))
                return JSON_NOT_VALID_PARAMS;

            ObjectId oId = new ObjectId(qId);
            int idx = ids.indexOf(oId);

            if (idx < 0 || answersHashMap.containsKey(oId) || uploadableList.get(idx))
                return JSON_NOT_VALID_PARAMS;

            answersHashMap.put(oId, jsonObject.getString("answer"));
        }

        try {

            for (ObjectId qId : answersHashMap.keySet()) {

                Document ans = searchInDocumentsKeyVal(
                        stdAnswers, "question_id", qId
                );

                if (ans == null) {

                    ans = new Document("_id", new ObjectId())
                            .append("answer_at", curr)
                            .append("question_id", qId)
                            .append("answer", answersHashMap.get(qId));

                    stdAnswers.add(ans);
                } else {
                    ans.put("answer_at", curr);
                    ans.put("answer", answersHashMap.get(qId));
                }

            }
        } catch (Exception x) {
            return JSON_NOT_VALID_PARAMS;
        }

        db.replaceOne(doc.getObjectId("_id"), doc);
        return JSON_OK;
    }

    static String saveStudentTashrihiAnswers(Document doc, ObjectId questionId,
                                             List<Document> stdAnswers, Common db,
                                             MultipartFile file)
            throws InvalidFieldsException {

        Document questions = doc.get("questions", Document.class);
        List<Boolean> uploadableList = questions.getList("uploadable_list", Boolean.class);
        List<ObjectId> ids = questions.getList("_ids", ObjectId.class);
        long curr = System.currentTimeMillis();

        int idx = ids.indexOf(questionId);
        if (idx < 0)
            throw new InvalidFieldsException("param is not valid");

        if (!uploadableList.get(idx))
            throw new InvalidFieldsException("not access");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new InvalidFieldsException("حداکثر سایز قابل بارگذاری در این قسمت 2MB می باشد.");

        String fileType = FileUtils.uploadImageFile(file);
        if (fileType == null)
            throw new InvalidFieldsException("تنها عکس می توان در این قسمت بارگذاری کرد.");

        String folder = db instanceof IRYSCQuizRepository ?
                IRYSCQuizRepository.FOLDER + "/studentAnswers/" :
                SchoolQuizRepository.FOLDER + "/studentAnswers/";

        String filename = FileUtils.uploadFile(file, folder);

        if (filename == null)
            throw new InvalidFieldsException("unknown file upload");

        Document ans = searchInDocumentsKeyVal(
                stdAnswers, "question_id", questionId
        );

        if (ans == null) {
            ans = new Document("_id", new ObjectId())
                    .append("answer_at", curr)
                    .append("question_id", questionId)
                    .append("type", "file")
                    .append("answer", filename);

            stdAnswers.add(ans);
        } else {

            if (ans.containsKey("answer") && !ans.getString("answer").isEmpty())
                FileUtils.removeFile(ans.getString("answer"), folder);

            ans.put("answer_at", curr);
            ans.put("answer", filename);
        }

        db.replaceOne(doc.getObjectId("_id"), doc);
        return STATICS_SERVER + folder + filename;
    }

    public static String saveStudentAnswers(Document doc, JSONArray answers,
                                            Document student, Common db) {

        ArrayList<PairValue> pairValues;

        if (doc.containsKey("answers")) {
            pairValues = Utility.getAnswers(
                    ((Binary) doc.getOrDefault("answers", new byte[0])).getData()
            );
        } else {
            Document questions = doc.get("questions", Document.class);
            pairValues = Utility.getAnswers(
                    ((Binary) questions.getOrDefault("answers", new byte[0])).getData()
            );
        }

        if (pairValues.size() != answers.length())
            return JSON_NOT_VALID_PARAMS;

        int idx = -1;
        ArrayList<PairValue> stdAnswers = new ArrayList<>();

        try {
            for (PairValue p : pairValues) {

                idx++;
                String stdAns = answers.get(idx).toString();

                Object stdAnsAfterFilter;
                String type = p.getKey().toString();

                if (stdAns.isEmpty()) {
                    if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {
                        stdAnswers.add(new PairValue(
                                p.getKey(),
                                new PairValue(((PairValue) p.getValue()).getKey(),
                                        0)
                        ));
                    } else if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
                        stdAnswers.add(new PairValue(p.getKey(), null));
                    else if (type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
                        String s = "";
                        for (int z = 0; z < p.getValue().toString().length(); z++)
                            s += "_";

                        stdAnswers.add(new PairValue(p.getKey(), s.toCharArray()));
                    }
                    continue;
                }

                if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {
                    int s = Integer.parseInt(stdAns);

                    PairValue pp = (PairValue) p.getValue();

                    if (s > (int) pp.getKey() || s < 0)
                        return JSON_NOT_VALID_PARAMS;

                    stdAnsAfterFilter = new PairValue(
                            pp.getKey(),
                            s
                    );
                } else if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
                    stdAnsAfterFilter = Double.parseDouble(stdAns);
                else if (type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {

                    if (p.getValue().toString().length() != stdAns.length())
                        return JSON_NOT_VALID_PARAMS;

                    if (!stdAns.matches("^[01_]+$"))
                        return JSON_NOT_VALID_PARAMS;

                    stdAnsAfterFilter = stdAns.toCharArray();
                } else
                    stdAnsAfterFilter = stdAns;

                stdAnswers.add(new PairValue(p.getKey(), stdAnsAfterFilter));
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            return JSON_NOT_VALID_PARAMS;
        }

        if (student != null)
            student.put("answers", Utility.getStdAnswersByteArr(stdAnswers));
        else if (doc.containsKey("answers"))
            doc.put("student_answers", Utility.getStdAnswersByteArr(stdAnswers));

        db.replaceOne(doc.getObjectId("_id"), doc);
        return JSON_OK;

    }
}
