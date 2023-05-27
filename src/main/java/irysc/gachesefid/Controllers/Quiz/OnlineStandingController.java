package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.QuestionType;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static irysc.gachesefid.Controllers.Quiz.AdminReportController.createQuizQuestionsList;
import static irysc.gachesefid.Controllers.Quiz.QuizAbstract.QuestionStat.getStdMarkInMultiSentenceQuestion;
import static irysc.gachesefid.Controllers.Quiz.QuizController.convertStudentDocToJSON;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class OnlineStandingController extends QuizAbstract {

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price", "title",
            "end", "priority", "perTeam", "maxTeams", "endRegistry"
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database", "isRegistrable", "isUploadable",
            "kind", "payByStudent", "launchMode", "duration", "permute",
            "minusMark", "backEn", "capacity", "showResultsAfterCorrection",
            "showResultsAfterCorrectionNotLoginUsers",
            "isQRNeeded"

    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);

            if (jsonObject.getInt("maxTeams") < 2)
                return generateErr("تعداد تیم ها باید حداقل دو باشد");

            if (jsonObject.getInt("perTeam") < 1)
                return generateErr("تعداد نفرات هر تیم باید حداقل یک باشد");

            if (jsonObject.getLong("start") < System.currentTimeMillis())
                return generateErr("زمان شروع آزمون باید از امروز بزرگتر باشد");

            if (jsonObject.getLong("end") < jsonObject.getLong("start"))
                return generateErr("زمان پایان آزمون باید بزرگ تر از زمان آغاز آن باشد");

            Document newDoc = QuizController.store(userId, jsonObject, AllKindQuiz.ONLINESTANDING.getName());
            onlineStandQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz",
                    new OnlineStandingController().convertDocToJSON(
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

    public static JSONObject convertOnlineStandingStudentToJSON(Document student, Document user) {

        JSONObject jsonObject = convertStudentDocToJSON(student, user);
        jsonObject.put("point", student.getOrDefault("point", 0))
                .put("teamName", student.getString("team_name"))
                .put("teamCount", student.getList("team", ObjectId.class).size() + 1)
        ;

        JSONArray team = new JSONArray();
        for (ObjectId stdId : student.getList("team", ObjectId.class)) {

            Document u = userRepository.findById(stdId);
            if (u == null)
                continue;

            JSONObject jsonObject1 = new JSONObject();
            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject1, u);
            jsonObject1.put("id", stdId.toString());
            team.put(jsonObject1);
        }

        jsonObject.put("team", team);
        return jsonObject;
    }

    public static String getParticipants(Common db,
                                         ObjectId userId,
                                         ObjectId quizId,
                                         ObjectId studentId,
                                         Boolean justAbsents,
                                         Boolean justPresence) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            JSONArray jsonArray = new JSONArray();
            List<Document> students = quiz.getList("students", Document.class);

            for (Document student : students) {

                if (studentId != null && !student.getObjectId("_id").equals(studentId))
                    continue;

                if (justAbsents != null && justAbsents && student.containsKey("start_at"))
                    continue;

                if (justPresence != null && justPresence && !student.containsKey("start_at"))
                    continue;

                Document user = userRepository.findById(student.getObjectId("_id"));
                if (user == null)
                    continue;


                jsonArray.put(convertOnlineStandingStudentToJSON(student, user));
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    public static String forceRegistry(ObjectId quizId, JSONArray jsonArray,
                                       int paid, String teamName) {

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (jsonArray.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        if (jsonArray.length() > quiz.getInteger("per_team"))
            return generateErr("حداکثر تعداد نفرات هر گروه می تواند " + quiz.getInteger("per_team") + " می تواند باشد");

        List<ObjectId> teamMembers = new ArrayList<>();
        Document mainMember = null;

        for (int i = 0; i < jsonArray.length(); i++) {

            String NID = jsonArray.getString(i);

            Document user = userRepository.findBySecKey(NID);
            if (user == null)
                return JSON_NOT_VALID_PARAMS;

            if (i == 0)
                mainMember = user;
            else
                teamMembers.add(user.getObjectId("_id"));
        }

        if (mainMember == null)
            return JSON_NOT_UNKNOWN;

        List<Document> added = new OnlineStandingController().registry(
                mainMember.getObjectId("_id"),
                mainMember.getOrDefault("phone", "").toString() + "__" +
                        mainMember.getOrDefault("mail", "").toString(),
                quizId.toString() + "__" + teamName, teamMembers, paid, null,
                mainMember.getString("first_name") + " " + mainMember.getString("last_name")
        );

        if (added == null)
            return JSON_NOT_UNKNOWN;

        return irysc.gachesefid.Utility.Utility.returnAddResponse(null, new JSONArray().put(
                convertOnlineStandingStudentToJSON(added.get(0), mainMember)
        ));
    }

    public static String changeMainMember(ObjectId quizId, ObjectId currMainMember, ObjectId userId) {

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", currMainMember
        );

        if (std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);
        int idx = team.indexOf(userId);

        if (idx < 0)
            return JSON_NOT_VALID_PARAMS;

        std.put("_id", userId);
        team.remove(idx);
        team.add(currMainMember);

        onlineStandQuizRepository.replaceOne(quizId, quiz);
        return JSON_OK;
    }

    public static String leftTeam(ObjectId userId, ObjectId quizId) {

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (quiz.getLong("start") <= System.currentTimeMillis())
            return JSON_NOT_ACCESS;

        boolean find = false;

        for (Document student : quiz.getList("students", Document.class)) {

            List<ObjectId> memberIds = student.getList("team", ObjectId.class);

            int idx = memberIds.indexOf(userId);
            if (idx < 0)
                continue;

            memberIds.remove(idx);
            find = true;
            break;
        }

        if (find) {
            onlineStandQuizRepository.replaceOne(quizId, quiz);
            return JSON_OK;
        }

        return JSON_NOT_VALID_PARAMS;
    }

    public static String removeMember(ObjectId userId, ObjectId quizId, ObjectId mainMember, JSONArray items) {

        if (userId != null && !mainMember.equals(userId))
            return JSON_NOT_ACCESS;

        if (items.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", mainMember
        );

        if (std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);
        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < items.length(); i++) {

            if (!ObjectId.isValid(items.getString(i))) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(items.getString(i));
            int idx = team.indexOf(oId);
            if (idx < 0) {
                excepts.put(i + 1);
                continue;
            }

            team.remove(idx);
            doneIds.put(oId.toString());
        }

        onlineStandQuizRepository.replaceOne(quizId, quiz);
        return returnRemoveResponse(excepts, doneIds);
    }

    public static String addMember(ObjectId userId, ObjectId quizId, ObjectId mainMember, JSONArray items) {

        if (userId != null && !mainMember.equals(userId))
            return JSON_NOT_ACCESS;

        if (items.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", mainMember
        );

        if (std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);

        if (team.size() + items.length() > quiz.getInteger("per_team"))
            return generateErr("حداکثر تعداد نفرات در هر گروه می تواند " + quiz.getInteger("per_team") + " نفر باشد");

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < items.length(); i++) {

            if (!validationNationalCode(items.getString(i))) {
                excepts.put(i + 1);
                continue;
            }

            Document newMember = userRepository.findBySecKey(items.getString(i));

            if (newMember == null) {
                excepts.put(i + 1);
                continue;
            }

            if (team.contains(newMember.getObjectId("_id"))) {
                excepts.put(i + 1);
                continue;
            }

            team.add(newMember.getObjectId("_id"));
            doneIds.put(newMember.getObjectId("_id").toString());
        }

        onlineStandQuizRepository.replaceOne(quizId, quiz);
        return returnAddResponse(excepts, doneIds);
    }


    static class QuizInfo {

        Document quiz;
        int reminder;
        Document student;
        long neededTime;

        public QuizInfo(Document quiz, int reminder, Document student, long neededTime) {
            this.quiz = quiz;
            this.reminder = reminder;
            this.student = student;
            this.neededTime = neededTime;
        }
    }

    private static QuizInfo checkStoreAnswer(ObjectId studentId, ObjectId quizId, boolean allowDelay
    ) throws InvalidFieldsException {

        long allowedDelay = allowDelay ? 300000 : 0; // 5min

        Document doc = onlineStandQuizRepository.findById(quizId);

        if (doc == null)
            throw new InvalidFieldsException("id is not valid");

        List<Document> students = doc.getList("students", Document.class);
        Document stdDoc = null;

        for (Document student : students) {

            if (student.getObjectId("_id").equals(studentId)) {
                stdDoc = student;
                break;
            }

            if (student.containsKey("team") &&
                    student.getList("team", ObjectId.class).contains(studentId)) {
                stdDoc = student;
                break;
            }

        }

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

        QuizInfo a = new QuizInfo(doc, reminder, stdDoc, neededTime);
        if (stdDoc.getOrDefault("start_at", null) == null)
            stdDoc.put("start_at", curr);

        stdDoc.put("finish_at", curr);
        onlineStandQuizRepository.replaceOne(quizId, doc);

        return a;
    }


    public static String returnQuiz(Document quiz, Document stdDoc,
                                    boolean isStatNeeded, JSONObject quizJSON) {

        Document questionsDoc = quiz.get("questions", Document.class);

        List<ObjectId> questions = (List<ObjectId>) questionsDoc.getOrDefault(
                "_ids", new ArrayList<ObjectId>()
        );
        List<Double> questionsMark = (List<Double>) questionsDoc.getOrDefault(
                "marks", new ArrayList<Double>()
        );

        if (questionsMark.size() != questions.size())
            return JSON_NOT_UNKNOWN;

        ArrayList<Document> questionsList = createQuizQuestionsList(questions, questionsMark, true);

        List<Binary> questionStats = null;
        if (isStatNeeded && quiz.containsKey("question_stat")) {
            questionStats = quiz.getList("question_stat", Binary.class);
            if (questionStats.size() != questionsMark.size())
                questionStats = null;
        }

        ArrayList<Object> stdAnswers = (ArrayList<Object>) stdDoc.getOrDefault("answers", new ArrayList<>());

        int i = 0;

        for (Document question : questionsList) {

            if (i >= stdAnswers.size() || stdAnswers.get(i) == null)
                question.put("stdAns", "");
            else
                question.put("stdAns", stdAnswers.get(i));

            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertList(
                questionsList, isStatNeeded, isStatNeeded, true,
                isStatNeeded, false, true
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);

    }

    public static String launch(ObjectId quizId, ObjectId studentId) {

        try {

            QuizInfo a = checkStoreAnswer(studentId, quizId, false);

            long curr = System.currentTimeMillis();

            List<String> attaches = (List<String>) a.quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            String folderBase = OnlineStandQuizRepository.FOLDER;

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + folderBase + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", a.quiz.getString("title"))
                    .put("id", a.quiz.getObjectId("_id").toString())
                    .put("questionsNo", a.quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", a.quiz.getOrDefault("desc", ""))
                    .put("attaches", jsonArray)
                    .put("refresh", Math.abs(new Random().nextInt(5)) + 5)
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

    public static String getOnlineQuizRankingTable(ObjectId quizId) {

        Document quiz = onlineStandQuizRepository.findById(quizId);

        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (quiz.getLong("start") > System.currentTimeMillis() ||
                !quiz.getBoolean("visibility")
        )
            return JSON_NOT_ACCESS;

        List<Document> students = quiz.getList("students", Document.class);

        List<JSONObject> teams = new ArrayList<>();

        for (Document student : students) {
            teams.add(new JSONObject()
                    .put("teamName", student.getString("team_name"))
                    .put("point", student.getOrDefault("point", 0)));
        }

        teams.sort(Comparator.comparing(jsonObject -> jsonObject.getInt("point"), Comparator.reverseOrder()));

        return generateSuccessMsg("data", teams);
    }


    private static double doCorrect(double qMark, String type, Object studentAnswer,
                                    ObjectId questionId, Object answer) {

        if (type.equalsIgnoreCase(
                QuestionType.TEST.getName()
        )) {

            return Integer.parseInt(studentAnswer.toString()) == Integer.parseInt(answer.toString()) ? qMark : 0;

        } else if (type.equalsIgnoreCase(
                QuestionType.SHORT_ANSWER.getName()
        )) {

            Document question = questionRepository.findById(questionId);
            if (question == null)
                return 0;

            double telorance = ((Number) question.getOrDefault("telorance", 0)).doubleValue();
            double stdAns = Double.parseDouble(studentAnswer.toString());

            return question.getDouble("answer") - telorance < stdAns &&
                    question.getDouble("answer") + telorance > stdAns ? qMark : 0;
        }
        else if (type.equalsIgnoreCase(
                QuestionType.MULTI_SENTENCE.getName()
        )) {
            PairValue p = getStdMarkInMultiSentenceQuestion(answer.toString(), studentAnswer.toString(), qMark);
            return ((Number)p.getKey()).doubleValue();
        }


        return 0;
    }

    public static String saveStudentAnswers(Document doc, Object stdAns,
                                            Document student, ObjectId questionId) {

        if (stdAns.toString().isEmpty())
            return generateErr("لطفا پاسخ خود را وارد نمایید");

        Document questions = doc.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);

        int idx = questionIds.indexOf(questionId);

        if (idx < 0)
            return JSON_NOT_VALID_ID;

        ArrayList<PairValue> pairValues = Utility.getAnswers(
                ((Binary) questions.getOrDefault("answers", new byte[0])).getData()
        );

        if (idx >= pairValues.size())
            return JSON_NOT_UNKNOWN;

        List<Object> stdAnswers = (List<Object>) student.getOrDefault("answers", new ArrayList<>());
        if(stdAnswers.size() > idx && stdAnswers.get(idx) != null)
            return generateErr("تنها یکبار فرصت پاسخدهی به هر سوال وجود دارد");

        List<Number> marks = questions.getList("marks", Number.class);
        Object questionAnswer = null;
        PairValue p = pairValues.get(idx);

        String type = p.getKey().toString();

        try {
            if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {
                int s = Integer.parseInt(stdAns.toString());

                PairValue pp = (PairValue) p.getValue();

                if (s > (int) pp.getKey() || s < 0)
                    return JSON_NOT_VALID_PARAMS;

                questionAnswer = pp.getValue();

            } else if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName())) {
                Double.parseDouble(stdAns.toString());
                questionAnswer = p.getValue();
            }
            else if (type.equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {

                if (p.getValue().toString().length() != stdAns.toString().length())
                    return JSON_NOT_VALID_PARAMS;

                if (!stdAns.toString().matches("^[01_]+$"))
                    return JSON_NOT_VALID_PARAMS;

                questionAnswer = p.getValue();
            }
        } catch (Exception x) {
            return generateErr("پاسخ موردنظر معتبر نمی باشد");
        }

        if(questionAnswer == null)
            return JSON_NOT_UNKNOWN;

        double stdMark = doCorrect(marks.get(idx).doubleValue(), type, stdAns, questionId, questionAnswer);

        if (stdAnswers.size() != pairValues.size()) {
            for (int i = 0; i < pairValues.size(); i++)
                stdAnswers.add(null);
        }

        stdAnswers.set(idx, stdAns);

        if(stdMark > 0) {
            int point = (int) student.getOrDefault("point", 0);
            double totalMark = 0;
            for (Number n : marks)
                totalMark += n.doubleValue();

            double reminder = Math.max(0, (1.0 * doc.getLong("end") - System.currentTimeMillis()) /
                    (doc.getLong("end") - doc.getLong("start")));

            point += (stdMark / totalMark) * reminder * 1000.0;
            student.put("point", point);
        }
        student.put("answers", stdAnswers);

        onlineStandQuizRepository.replaceOne(doc.getObjectId("_id"), doc);
        return JSON_OK;
    }

    public static String storeAnswer(ObjectId quizId, ObjectId questionId,
                                     ObjectId studentId, Object answer) {


        try {

            QuizInfo a = checkStoreAnswer(studentId, quizId, true);
            String result = saveStudentAnswers(a.quiz, answer, a.student, questionId);

            if (result.contains("nok"))
                return result;

            return generateSuccessMsg("reminder", a.reminder);
        }
        catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    @Override
    public List<Document> registry(ObjectId studentId, String phoneAndMail, String quizIdAndTeamStr, List<ObjectId> members,
                                   int paid, ObjectId transactionId, String stdName) {

        ArrayList<Document> added = new ArrayList<>();
        String[] splited = quizIdAndTeamStr.split("__");
        ObjectId quizId = new ObjectId(splited[0]);

        try {
            Document quiz = onlineStandQuizRepository.findById(quizId);

            if (quiz == null)
                return null;

            List<Document> students = quiz.getList("students", Document.class);

            if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    students, "_id", studentId
            ) != -1)
                return null;

            Document stdDoc = new Document("_id", studentId)
                    .append("paid", paid)
                    .append("register_at", System.currentTimeMillis())
                    .append("finish_at", null)
                    .append("team_name", splited[1])
                    .append("team", members)
                    .append("start_at", null);

            students.add(stdDoc);
            added.add(stdDoc);
            quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);

            onlineStandQuizRepository.replaceOne(
                    quizId, quiz
            );

            splited = phoneAndMail.split("__");
            String phone = splited[0];
            String mail = splited[1];

            if (transactionId != null && mail != null) {
                new Thread(() -> sendMail(mail, SERVER + "recp/" + transactionId, "successQuiz", stdName)).start();
            }

            //todo : send notif
        } catch (Exception ignore) {
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
                .put("generalMode", AllKindQuiz.ONLINESTANDING.getName())
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
                .put("tags", quiz.getList("tags", String.class))
                .put("maxTeams", quiz.getInteger("max_teams"))
                .put("perTeam", quiz.getInteger("per_team"))
                .put("rate", quiz.getOrDefault("rate", 5))
                .put("id", quiz.getObjectId("_id").toString());

        int questionsCount = 0;
        try {
            questionsCount = quiz.get("questions", Document.class)
                    .getList("_ids", ObjectId.class).size();
        } catch (Exception ignore) {
        }

        if (afterBuy) {
            long curr = System.currentTimeMillis();

            if (quiz.getLong("end") < curr) {

                jsonObject.put("status", "finished")
                        .put("questionsCount", questionsCount);

            } else if (quiz.getLong("start") <= curr &&
                    quiz.getLong("end") > curr
            ) {
                jsonObject
                        .put("status", "inProgress")
                        .put("duration", calcLen(quiz))
                        .put("questionsCount", questionsCount);
            } else
                jsonObject.put("status", "notStart");

            jsonObject.put("canChange", quiz.getLong("start") > curr);

        } else {
            jsonObject.put("startRegistry", quiz.getLong("start_registry"))
                    .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                    .put("price", quiz.get("price"))
                    .put("reminder", Math.max(quiz.getInteger("max_teams") - quiz.getInteger("registered"), 0));
        }

        if (quiz.containsKey("students")) {
            JSONArray teams = new JSONArray();
            for (Document std : quiz.getList("students", Document.class)) {
                Document user = userRepository.findById(std.getObjectId("_id"));
                teams.put(convertOnlineStandingStudentToJSON(std, user));
            }

            jsonObject.put("teams", teams);
        }

        if (isAdmin) {
            jsonObject
                    .put("teamsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("priority", quiz.getInteger("priority"))
                    .put("questionsCount", questionsCount);
        }

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
                        attaches.put(STATICS_SERVER + OnlineStandQuizRepository.FOLDER + "/" + attach);
                }
                ;

                jsonObject.put("descBefore", quiz.getOrDefault("desc", ""));
                jsonObject.put("descAfter", quiz.getOrDefault("desc_after", ""));
                jsonObject.put("attaches", attaches);
            }
        }

        return jsonObject;
    }
}
