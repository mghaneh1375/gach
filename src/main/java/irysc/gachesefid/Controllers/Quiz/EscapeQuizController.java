package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.GiftType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static irysc.gachesefid.Controllers.Config.GiftController.*;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class EscapeQuizController extends QuizAbstract {

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price", "title",
            "end", "priority", "endRegistry",
            "capacity", "topStudentsCount", "maxTry", "shouldComplete"
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database", "isRegistrable", "isUploadable",
            "kind", "payByStudent", "launchMode", "permute", "duration",
            "minusMark", "backEn", "showResultsAfterCorrection",
            "showResultsAfterCorrectionNotLoginUsers",
            "isQRNeeded", "maxTeams", "perTeam"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);

            if (jsonObject.getLong("start") < System.currentTimeMillis())
                return generateErr("زمان شروع آزمون باید از امروز بزرگتر باشد");

            if (jsonObject.getLong("end") < jsonObject.getLong("start"))
                return generateErr("زمان پایان آزمون باید بزرگ تر از زمان آغاز آن باشد");

            Document newDoc = QuizController.store(userId, jsonObject, AllKindQuiz.ESCAPE.getName());
            escapeQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz",
                    new EscapeQuizController().convertDocToJSON(
                            newDoc, false, true,
                            false, false
                    )
            );

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    e.getMessage()
            );
        }

    }

    private static ArrayList<Document> createQuizQuestionsList(List<ObjectId> questions) {

        ArrayList<Document> questionsList = new ArrayList<>();

        int i = 0;

        for (ObjectId itr : questions) {

            Document question = escapeQuizQuestionRepository.findById(itr);

            if (question == null) {
                i++;
                continue;
            }

            questionsList.add(Document.parse(question.toJson()).append("no", i + 1));
            i++;
        }

        return questionsList;
    }

    public static String returnQuiz(Document quiz, Document stdDoc,
                                    boolean isStatNeeded, JSONObject quizJSON) {

        Document questionsDoc = quiz.get("questions", Document.class);

        List<ObjectId> questions = (List<ObjectId>) questionsDoc.getOrDefault(
                "_ids", new ArrayList<ObjectId>()
        );

        ArrayList<Document> questionsList = createQuizQuestionsList(questions);

        ArrayList<Document> stdAnswers = stdDoc == null ? new ArrayList<>() :
                (ArrayList<Document>) stdDoc.getOrDefault("answers", new ArrayList<>());

        int i = 0;

        for (Document question : questionsList) {

            if (i >= stdAnswers.size() || stdAnswers.get(i) == null)
                question.put("stdAns", "");
            else
                question.put("stdAns", stdAnswers.get(i).get("ans"));

            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertEscapeQuestionsList(
                questionsList, true, isStatNeeded, isStatNeeded
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);

    }

    public static PairValue saveStudentAnswers(Document doc, Object stdAns,
                                               Document student, ObjectId questionId
    ) throws InvalidFieldsException {

        if (stdAns.toString().isEmpty())
            throw new InvalidFieldsException("لطفا پاسخ خود را وارد نمایید");

        Document questions = doc.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);

        int idx = questionIds.indexOf(questionId);

        if (idx < 0)
            throw new InvalidFieldsException("id is not valid");

        List<Object> answers = questions.getList("answers", Object.class);
        List<Document> stdAnswers = (List<Document>) student.getOrDefault("answers", new ArrayList<>());
        Object questionAnswer = answers.get(idx);

        if (stdAnswers.size() != answers.size()) {
            for (int i = 0; i < answers.size(); i++)
                stdAnswers.add(null);
        }

        boolean isCorrect = questionAnswer.toString().equals(stdAns.toString());

        if (isCorrect && stdAnswers.get(idx) != null && stdAnswers.get(idx).containsKey("ans")) {

            if (idx == answers.size() - 1 && !student.containsKey("can_continue")) {
                student.put("can_continue", false);
                student.put("success", true);
                escapeQuizRepository.replaceOneWithoutClearCache(doc.getObjectId("_id"), doc);
                return new PairValue(true, true);
            }

            return new PairValue(true, false);
        }

        Document d = stdAnswers.get(idx) == null ? new Document("tries", 0) : stdAnswers.get(idx);

        int maxTry = doc.getInteger("max_try");

        if (d.getInteger("tries") >= maxTry) {
            if (!student.containsKey("can_continue")) {
                student.put("can_continue", false);
                escapeQuizRepository.replaceOneWithoutClearCache(doc.getObjectId("_id"), doc);
            }
            throw new InvalidFieldsException("شما حداکثر می توانید " + maxTry + " بار به این سوال پاسخ دهید");
        }

        d.put("tries", d.getInteger("tries") + 1);

        if (isCorrect) {
            d.put("ans", stdAns);
            d.put("answer_at", System.currentTimeMillis());

            if (idx == answers.size() - 1) {
                student.put("can_continue", false);
                student.put("success", true);
            }
        } else if (d.getInteger("tries") == maxTry)
            student.put("can_continue", false);

        stdAnswers.set(idx, d);
        student.put("answers", stdAnswers);

        escapeQuizRepository.replaceOneWithoutClearCache(doc.getObjectId("_id"), doc);
        return new PairValue(isCorrect, idx == answers.size() - 1);
    }

    public static String storeAnswer(ObjectId quizId, ObjectId questionId,
                                     ObjectId studentId, Object answer) {

        try {

            OnlineStandingController.QuizInfo a = checkStoreAnswer(studentId, quizId, true);

            try {
                PairValue p = saveStudentAnswers(a.quiz, answer, a.student, questionId);

                boolean isCorrect = (boolean) p.getKey();
                if (isCorrect) {
                    if ((boolean) p.getValue()) {

                        Document quiz = escapeQuizRepository.findById(quizId);
                        List<Document> students = quiz.getList("students", Document.class);

                        long myFinishAt = a.student.getLong("finish_at");
                        int rank = 1;

                        for (Document student : students) {

                            if (!student.containsKey("success") ||
                                    student.getObjectId("_id").equals(studentId) ||
                                    student.getLong("finish_at") >= myFinishAt
                            )
                                continue;

                            rank++;
                        }

                        return generateSuccessMsg("reminder", a.reminder,
                                new PairValue("isCorrect", true),
                                new PairValue("rank", rank)
                        );

                    }
                }

                return generateSuccessMsg("reminder", a.reminder,
                        new PairValue("isCorrect", isCorrect)
                );
            } catch (Exception x) {
                return generateErr(x.getMessage());
            }

        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    public static String reviewQuiz(ObjectId quizId, ObjectId studentId, boolean isStudent) {
        try {

            Document doc = escapeQuizRepository.findById(quizId);

            if (doc == null)
                return JSON_NOT_VALID_ID;

            List<Document> students = doc.getList("students", Document.class);
            Document stdDoc = null;

            if (isStudent) {

                stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        students, "_id", studentId
                );

                if (stdDoc == null)
                    return JSON_NOT_ACCESS;

                if (doc.getLong("end") > System.currentTimeMillis())
                    return generateErr("زمان مرور آزمون هنوز فرانرسیده است.");
            }

            int neededTime = (int) ((doc.getLong("end") - doc.getLong("start")) / 1000);

            Document questions = doc.get("questions", Document.class);

            int qNo = 0;

            if (questions.containsKey("_ids"))
                qNo = questions.getList("_ids", ObjectId.class).size();

            List<String> attaches = (List<String>) doc.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            String baseFolder = EscapeQuizRepository.FOLDER;

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + baseFolder + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", doc.getString("title"))
                    .put("id", doc.getObjectId("_id").toString())
                    .put("questionsNo", qNo)
                    .put("description", doc.getOrDefault("description", ""))
                    .put("attaches", jsonArray);

            return returnQuiz(doc, stdDoc, true, quizJSON);

        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }
    }

    public static String fetchQuestions(ObjectId quizId) {

        try {

            Document quiz = hasAccess(escapeQuizRepository, null, quizId);
            Document questionsDoc = quiz.get("questions", Document.class);

            ArrayList<Document> questionsList = new ArrayList<>();
            List<ObjectId> questions = (List<ObjectId>) questionsDoc.getOrDefault(
                    "_ids", new ArrayList<ObjectId>()
            );

            int i = 0;

            for (ObjectId itr : questions) {

                Document question = escapeQuizQuestionRepository.findById(itr);

                if (question == null) {
                    i++;
                    continue;
                }

                Document tmpDoc = Document.parse(question.toJson())
                        .append("no", i + 1);

                questionsList.add(tmpDoc);
                i++;
            }

            JSONArray jsonArray = Utilities.convertEscapeQuestionsList(
                    questionsList, true, true, true
            );

            return generateSuccessMsg("data", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }


    private static OnlineStandingController.QuizInfo checkStoreAnswer(ObjectId studentId, ObjectId quizId, boolean allowDelay
    ) throws InvalidFieldsException {

        long allowedDelay = allowDelay ? 300000 : 0; // 5min

        Document doc = escapeQuizRepository.findById(quizId);

        if (doc == null)
            throw new InvalidFieldsException("id is not valid");

        List<Document> students = doc.getList("students", Document.class);
        Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                students, "_id", studentId
        );

        if (stdDoc == null)
            throw new InvalidFieldsException("not access");

        long end = doc.getLong("end") + allowedDelay;
        long curr = System.currentTimeMillis();

        if (doc.containsKey("start") &&
                (
                        doc.getLong("start") > curr ||
                                end < curr
                )
        )
            throw new InvalidFieldsException("در زمان ارزیابی قرار نداریم.");

        long neededTime = (doc.getLong("end") - doc.getLong("start")) / 1000;
        int reminder = (int) ((doc.getLong("end") - curr) / 1000);

        if (reminder + allowedDelay / 1000 <= 0)
            throw new InvalidFieldsException("زمان این آزمون به پایان رسیده است");

        OnlineStandingController.QuizInfo a = new OnlineStandingController.QuizInfo(doc, reminder, stdDoc, neededTime);
        if (stdDoc.getOrDefault("start_at", null) == null)
            stdDoc.put("start_at", curr);

        stdDoc.put("finish_at", curr);
        escapeQuizRepository.replaceOneWithoutClearCache(quizId, doc);

        return a;
    }

    public static String launch(ObjectId quizId, ObjectId studentId) {

        try {

            OnlineStandingController.QuizInfo a = checkStoreAnswer(studentId, quizId, false);

            long curr = System.currentTimeMillis();

            List<String> attaches = (List<String>) a.quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            String folderBase = EscapeQuizRepository.FOLDER;

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + folderBase + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", a.quiz.getString("title"))
                    .put("id", a.quiz.getObjectId("_id").toString())
                    .put("questionsNo", a.quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", a.quiz.getOrDefault("desc", ""))
                    .put("attaches", jsonArray)
                    .put("duration", a.neededTime)
                    .put("reminder", a.reminder)
                    .put("isNewPerson", !a.student.containsKey("start_at") ||
                            a.student.get("start_at") == null ||
                            a.student.getLong("start_at") == curr
                    );

            return returnQuiz(a.quiz, a.student, false, quizJSON);

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }

    }

    public static String gifts(ObjectId quizId) {

        Document quiz = escapeQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_PARAMS;

        List<Document> gifts = (List<Document>) quiz.getOrDefault("gifts", new ArrayList<Document>());

        JSONArray jsonArray = new JSONArray();

        for (int i = 1; i <= quiz.getInteger("top_students_count"); i++) {

            Document gift = searchInDocumentsKeyVal(gifts, "rank", i);
            if (gift == null)
                jsonArray.put(new JSONObject().put("rank", i));
            else
                jsonArray.put(convertGiftDocToJSON(gift));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String removeGift(ObjectId quizId, int rank) {

        Document quiz = escapeQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (!quiz.containsKey("gifts"))
            return JSON_NOT_VALID_PARAMS;

        List<Document> gifts = quiz.getList("gifts", Document.class);

        int idx = searchInDocumentsKeyValIdx(gifts, "rank", rank);
        if (idx < 0)
            return JSON_NOT_VALID_PARAMS;

        gifts.remove(idx);

        escapeQuizRepository.replaceOne(quizId, quiz);
        return JSON_OK;
    }

    public static String addGift(ObjectId quizId, JSONObject data) {

        if (data.getString("type").equalsIgnoreCase(
                GiftType.FREE.getName()
        ) !=
                data.has("description")
        )
            return generateErr("لطفا جایزه موردنظر خود را وارد کنید");


        if (data.getString("type").equalsIgnoreCase(GiftType.FREE.getName()) == data.has("amount"))
            return generateErr("لطفا مقدار موردنظر خود را وارد نمایید");

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("useFor")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("offCodeType")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("expireAt")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.has("expireAt") && data.getLong("expireAt") < System.currentTimeMillis())
            return JSON_NOT_VALID_PARAMS;

        Document quiz = escapeQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_PARAMS;

        if (data.getInt("rank") > quiz.getInteger("top_students_count"))
            return generateErr("رتبه باید کمتر مساوی " + quiz.getInteger("top_students_count") + " باشد");

        List<Document> gifts = (List<Document>) quiz.getOrDefault("gifts", new ArrayList<Document>());

        Document newDoc = searchInDocumentsKeyVal(gifts, "rank", data.getInt("rank"));
        if (newDoc != null)
            return generateErr("برای این رتبه جایزه ساخته شده است");

        newDoc = new Document();

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        gifts.add(newDoc);
        quiz.put("gifts", gifts);

        escapeQuizRepository.replaceOne(quizId, quiz);
        return generateSuccessMsg("data", convertGiftDocToJSON(newDoc));
    }

    private static JSONObject convertGiftDocToJSON(Document doc) {

        JSONObject jsonObject = new JSONObject()
                .put("type", doc.getString("type"))
                .put("rank", doc.get("rank"));

        if (doc.getString("type").equalsIgnoreCase(GiftType.OFFCODE.getName()))
            jsonObject
                    .put("typeFa",
                            translateType(doc.getString("type")) + " - " +
                                    "برای " + translateUseFor(doc.getString("use_for")) + " - " +
                                    " به صورت " + translateOffCodeType(doc.getString("off_code_type")) + " -" +
                                    " تاریخ انقضا " + irysc.gachesefid.Utility.Utility.getSolarDate(doc.getLong("expire_at"))
                    )
                    .put("offCodeType", doc.getString("off_code_type"))
                    .put("expireAt", doc.getLong("expire_at"))
                    .put("useFor", doc.getString("use_for"));
        else
            jsonObject
                    .put("typeFa", translateType(doc.getString("type")));

        if (doc.containsKey("amount"))
            jsonObject.put("amount", doc.get("amount"));

        if (doc.containsKey("description"))
            jsonObject.put("description", doc.getString("description"));

        return jsonObject;
    }

    public static String getRanking(ObjectId quizId, boolean isAdmin) {

        Document quiz = escapeQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (isAdmin && !quiz.containsKey("ranking"))
            return generateErr("لطفا ابتدا با اهدای جوایز جدول رتبه بندی را بسازید");

        if (!isAdmin && !quiz.containsKey("ranking"))
            return generateErr("نتایج هنوز آماده نیست و باید منتظر اعلام نتایج باشید");

        JSONArray jsonArray = new JSONArray();
        List<Document> students = quiz.getList("students", Document.class);
        List<Document> rankingUsers = userRepository.findByIds(quiz.getList("ranking", Document.class)
                .stream().map(document -> document.getObjectId("user_id"))
                .collect(Collectors.toList()), false, JUST_NAME
        );

        if (rankingUsers == null)
            return JSON_NOT_UNKNOWN;

        for (Document rank : quiz.getList("ranking", Document.class)) {
            rankingUsers
                    .stream().filter(document -> document.getObjectId("_id").equals(rank.getObjectId("user_id")))
                    .findFirst().ifPresent(user -> {
                        JSONObject jsonObject = new JSONObject()
                                .put("userId", user.getObjectId("_id").toString())
                                .put("user", user.getString("first_name") + " " + user.getString("last_name"))
                                .put("rank", rank.get("rank"))
                                .put("solved", rank.get("solved"))
                                .put("isComplete", rank.getBoolean("is_complete"))
                                .put("lastAnswer", (rank.getLong("last_answer") - quiz.getLong("start")) / 1000);

                        if (isAdmin) {
                            Document std = searchInDocumentsKeyVal(students, "_id", rank.getObjectId("user_id"));
                            if (std == null) {
                                jsonArray.put(jsonObject);
                                return;
                            }

                            jsonObject.put("startAt", std.containsKey("start_at") && std.get("start_at") != null ? getSolarDate(std.getLong("start_at")) : "")
                                    .put("finishAt", std.containsKey("finish_at") && std.get("finish_at") != null ? getSolarDateWithSecond(std.getLong("finish_at")) : "");

                            if (std.containsKey("answers")) {
                                List<Object> answers = (List<Object>) std.get("answers");
                                JSONArray jsonArray1 = new JSONArray();

                                for (Object ans : answers) {
                                    if (ans == null)
                                        continue;

                                    Document answer = (Document) ans;
                                    jsonArray1.put(new JSONObject()
                                            .put("tries", answer.get("tries"))
                                            .put("answerAt", answer.containsKey("answer_at") ?
                                                    (answer.getLong("answer_at") - quiz.getLong("start")) / 1000 : ""
                                            )
                                    );
                                }
                                jsonObject.put("answers", jsonArray1);
                            } else
                                jsonObject.put("answers", new JSONArray());
                        }

                        jsonArray.put(jsonObject);
                    });
        }

        return generateSuccessMsg("data", jsonArray);
    }

    static class Rank {

        boolean isComplete;
        long lastComplete;
        int solved;
        ObjectId userId;
        int rank;

        public Rank(boolean isComplete, long lastComplete, int solved, ObjectId userId) {
            this.isComplete = isComplete;
            this.lastComplete = lastComplete;
            this.solved = solved;
            this.userId = userId;
        }
    }

    public static JSONObject convertStudentDocToJSON(
            Document student, Document user
    ) {
        JSONObject jsonObject = new JSONObject()
                .put("id", user.getObjectId("_id").toString());

        if (student.containsKey("paid")) {
            jsonObject.put("paid", student.get("paid"));
            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
        } else {
            jsonObject.put("name", user.getString("first_name") + " " + user.getString("last_name"));
            jsonObject.put("NID", user.getString("NID"));
            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
        }

        if (student.containsKey("register_at"))
            jsonObject.put("registerAt", getSolarDate(student.getLong("register_at")));

        if (student.containsKey("start_at")) {
            jsonObject.put("startAt", student.containsKey("start_at") && student.get("start_at") != null ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("start_at")) :
                    ""
            ).put("finishAt", student.containsKey("finish_at") && student.get("finish_at") != null ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("finish_at")) :
                    ""
            );
        }

        if (student.containsKey("rate")) {
            jsonObject.put("rate", student.get("rate"))
                    .put("rateAt", getSolarDate((Long) student.getOrDefault("rate_at", System.currentTimeMillis())));
        }

        jsonObject.put("canContinue", student.getOrDefault("can_continue", true));

        int answers = 0;

        List<Document> stdAnswers = (List<Document>) student.getOrDefault("answers", new ArrayList<>());

        for (Document tmp : stdAnswers) {

            if (tmp == null || !tmp.containsKey("answer_at"))
                break;

            answers++;
        }


        jsonObject.put("answers", answers);

        return jsonObject;
    }

    public static String reset(ObjectId quizId, ObjectId studentId) {

        try {
            Document quiz = hasAccess(escapeQuizRepository, null, quizId);

            List<Document> students = quiz.getList("students", Document.class);
            Document std = searchInDocumentsKeyVal(students, "_id", studentId);

            if (std == null)
                return JSON_NOT_VALID_ID;

            std.put("start_at", null);
            std.put("finish_at", null);
            std.remove("can_continue");
            std.remove("answers");
            std.remove("success");

            escapeQuizRepository.replaceOneWithoutClearCache(quizId, quiz);
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }

        return JSON_OK;
    }

    public static String getParticipants(ObjectId quizId) {

        try {
            Document quiz = hasAccess(escapeQuizRepository, null, quizId);

            JSONArray jsonArray = new JSONArray();

            List<Document> students = quiz.getList("students", Document.class);

            for (Document student : students) {

                Document user = userRepository.findById(student.getObjectId("_id"));
                if (user == null)
                    continue;

                jsonArray.put(convertStudentDocToJSON(student, user));
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    public static String giveGifts(Document quiz) {

        if (!quiz.containsKey("gifts"))
            return generateErr("لطفا ابتدا جوایز را تعیین کنید");

        boolean shouldComplete = quiz.getBoolean("should_complete");

        Document questions = quiz.get("questions", Document.class);
        List<Object> answers = questions.getList("answers", Object.class);

        List<Document> students = quiz.getList("students", Document.class);
        List<Rank> ranks = new ArrayList<>();

        for (Document student : students) {

            int stdSolved = 0;
            long stdLastSolved = -1;

            List<Document> stdAnswers = (List<Document>) student.getOrDefault("answers", new ArrayList<>());

            for (int i = 0; i < answers.size(); i++) {

                if (stdAnswers.size() <= i || stdAnswers.get(i) == null || !stdAnswers.get(i).containsKey("answer_at"))
                    continue;

                stdSolved++;
                stdLastSolved = stdAnswers.get(i).getLong("answer_at");
            }

            ranks.add(
                    new Rank(stdSolved == answers.size(), stdLastSolved,
                            stdSolved, student.getObjectId("_id"))
            );

        }

        ranks.sort((o1, o2) -> {

            if (o1.solved == o2.solved && o1.solved > 0)
                return o1.lastComplete < o2.lastComplete ? -1 : 1;

            if (o1.solved == o2.solved) return 0;

            return o1.solved > o2.solved ? -1 : 1;
        });

        int r = 1;

        for (Rank rank : ranks)
            rank.rank = r++;

        List<Document> gifts = quiz.getList("gifts", Document.class);
        List<Document> rankingList = new ArrayList<>();

        for (Rank rank : ranks) {

            rankingList.add(new Document("user_id", rank.userId)
                    .append("rank", rank.rank).append("solved", rank.solved)
                    .append("last_answer", rank.lastComplete).append("is_complete", rank.isComplete)
            );

            if (shouldComplete && !rank.isComplete)
                break;

            Document gift = searchInDocumentsKeyVal(gifts, "rank", rank.rank);
            if (gift == null || gift.getString("type").equalsIgnoreCase("free"))
                continue;

            Document user = userRepository.findById(rank.userId);
            if (user == null)
                continue;

            switch (gift.getString("type")) {
                case "coin":
                    double d = ((Number) user.get("coin")).doubleValue() + ((Number) gift.get("amount")).doubleValue();
                    user.put("coin", Math.round((d * 100.0)) / 100.0);
                    userRepository.replaceOne(user.getObjectId("_id"), user);
                    break;
                case "money":
                    user.put("money", ((Number) user.get("money")).doubleValue() + ((Number) gift.get("amount")).doubleValue());
                    userRepository.replaceOne(user.getObjectId("_id"), user);
                    break;
                case "offcode":
                    Document newDoc = new Document("type", gift.getString("off_code_type"))
                            .append("amount", gift.getInteger("amount"))
                            .append("expire_at", gift.getLong("expire_at"))
                            .append("section", gift.getString("use_for"))
                            .append("user_id", user.getObjectId("_id"))
                            .append("used", false)
                            .append("created_at", System.currentTimeMillis());

                    offcodeRepository.insertOne(newDoc);
                    break;
            }

        }

        quiz.put("ranking", rankingList);
        escapeQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);

        return JSON_OK;
    }

    public static String removeQuestions(ObjectId quizId, JSONArray jsonArray) {

        Document quiz = escapeQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray removeIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        Document questions = quiz.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);
        List<ObjectId> removed = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId qId = new ObjectId(id);
            if (!questionIds.contains(qId)) {
                excepts.put(i + 1);
                continue;
            }

            removeIds.put(qId);
            removed.add(qId);
        }

        if (removeIds.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        List<Object> answers = questions.getList("answers", Object.class);

        List<ObjectId> newQuestionsIds = new ArrayList<>();
        List<Object> newAnswers = new ArrayList<>();

        int idx = 0;

        for (ObjectId qId : questionIds) {

            if (removed.contains(qId)) {
                idx++;
                continue;
            }

            newQuestionsIds.add(qId);
            newAnswers.add(answers.get(idx));
            idx++;
        }

        questions.put("answers", newAnswers);
        questions.put("_ids", newQuestionsIds);

        quiz.put("questions", questions);
        escapeQuizRepository.replaceOne(quizId, quiz);

        return irysc.gachesefid.Utility.Utility.returnRemoveResponse(
                excepts, removeIds
        );
    }

    @Override
    public List<Document> registry(ObjectId studentId, String phone,
                                   String mail, List<ObjectId> quizIds,
                                   int paid, ObjectId transactionId, String stdName
    ) {

        ArrayList<Document> added = new ArrayList<>();

        for (ObjectId quizId : quizIds) {

            try {
                Document quiz = escapeQuizRepository.findById(quizId);

                List<Document> students = quiz.getList("students", Document.class);

                if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        students, "_id", studentId
                ) != -1)
                    continue;

                Document stdDoc = new Document("_id", studentId)
                        .append("paid", paid / quizIds.size())
                        .append("register_at", System.currentTimeMillis())
                        .append("finish_at", null)
                        .append("start_at", null);

                students.add(stdDoc);
                added.add(stdDoc);
                quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);

                escapeQuizRepository.replaceOne(
                        quizId, quiz
                );

                if (transactionId != null && mail != null) {
                    new Thread(() -> sendMail(mail, SERVER + "recp/" + transactionId, "successQuiz", stdName)).start();
                }

                //todo : send notif
            } catch (Exception ignore) {
            }
        }

        return added;
    }


    @Override
    void quit(Document student, Document quiz) {

        List<Document> students = quiz.getList("students", Document.class);
        int idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        );

        if (idx == -1)
            return;

        students.remove(idx);

    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin,
                                boolean afterBuy, boolean isDescNeeded) {

        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("start", quiz.getLong("start"))
                .put("end", quiz.getLong("end"))
                .put("generalMode", AllKindQuiz.ESCAPE.getName())
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
                .put("tags", quiz.getList("tags", String.class))
                .put("capacity", quiz.getInteger("capacity"))
                .put("rate", quiz.getOrDefault("rate", 5))
                .put("shouldComplete", quiz.getBoolean("should_complete"))
                .put("maxTry", quiz.getInteger("max_try"))
                .put("id", quiz.getObjectId("_id").toString());

        int questionsCount = 0;
        try {
            questionsCount = quiz.get("questions", Document.class)
                    .getList("_ids", ObjectId.class).size();
        } catch (Exception ignore) {
        }

        long curr = System.currentTimeMillis();

        if (afterBuy) {

            if (quiz.getLong("end") < curr) {
                jsonObject.put("status", "finished")
                        .put("questionsCount", questionsCount);
            } else if (quiz.getLong("start") <= curr &&
                    quiz.getLong("end") > curr
            ) {
                jsonObject
                        .put("status", "inProgress")
                        .put("questionsCount", questionsCount);
            } else
                jsonObject.put("status", "notStart");

        } else {
            jsonObject.put("startRegistry", quiz.getLong("start_registry"))
                    .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                    .put("price", quiz.get("price"))
                    .put("reminder", Math.max(quiz.getInteger("capacity") - quiz.getInteger("registered"), 0));
        }

        if (isAdmin) {
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("priority", quiz.getInteger("priority"))
                    .put("questionsCount", questionsCount);
        }

        if (isAdmin && quiz.getLong("end") < curr)
            jsonObject.put("reportStatus", "ready");


        if (!isDigest || isDescNeeded)
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""));

        if (!isDigest) {

            jsonObject
                    .put("topStudentsCount", quiz.getInteger("top_students_count"));

            if (isAdmin) {
                JSONArray attaches = new JSONArray();
                if (quiz.containsKey("attaches")) {
                    for (String attach : quiz.getList("attaches", String.class))
                        attaches.put(STATICS_SERVER + EscapeQuizRepository.FOLDER + "/" + attach);
                }

                jsonObject.put("descBefore", quiz.getOrDefault("desc", ""));
                jsonObject.put("descAfter", quiz.getOrDefault("desc_after", ""));
                jsonObject.put("attaches", attaches);
            }
        }

        return jsonObject;
    }
}
