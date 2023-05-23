package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.OnlineStandQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.AllKindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

            if(jsonObject.getInt("maxTeams") < 2)
                return generateErr("تعداد تیم ها باید حداقل دو باشد");

            if(jsonObject.getInt("perTeam") < 1)
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
            if(u == null)
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
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        if(jsonArray.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        if(jsonArray.length() > quiz.getInteger("per_team"))
            return generateErr("حداکثر تعداد نفرات هر گروه می تواند " + quiz.getInteger("per_team") + " می تواند باشد");

        List<ObjectId> teamMembers = new ArrayList<>();
        Document mainMember = null;

        for(int i = 0; i < jsonArray.length(); i++) {

            String NID = jsonArray.getString(i);

            Document user = userRepository.findBySecKey(NID);
            if(user == null)
                return JSON_NOT_VALID_PARAMS;

            if(i == 0)
                mainMember = user;
            else
                teamMembers.add(user.getObjectId("_id"));
        }

        if(mainMember == null)
            return JSON_NOT_UNKNOWN;

        List<Document> added = new OnlineStandingController().registry(
                mainMember.getObjectId("_id"),
                mainMember.getOrDefault("phone", "").toString() + "__" +
                        mainMember.getOrDefault("mail", "").toString(),
                quizId.toString() + "__" + teamName, teamMembers, paid, null,
                mainMember.getString("first_name") + " " + mainMember.getString("last_name")
        );

        if(added == null)
            return JSON_NOT_UNKNOWN;

        return irysc.gachesefid.Utility.Utility.returnAddResponse(null, new JSONArray().put(
                convertOnlineStandingStudentToJSON(added.get(0), mainMember)
        ));
    }

    public static String changeMainMember(ObjectId quizId, ObjectId currMainMember, ObjectId userId) {

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", currMainMember
        );

        if(std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);
        int idx = team.indexOf(userId);

        if(idx < 0)
            return JSON_NOT_VALID_PARAMS;

        std.put("_id", userId);
        team.remove(idx);
        team.add(currMainMember);

        onlineStandQuizRepository.replaceOne(quizId, quiz);
        return JSON_OK;
    }

    public static String leftTeam(ObjectId userId, ObjectId quizId) {

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        if(quiz.getLong("start") <= System.currentTimeMillis())
            return JSON_NOT_ACCESS;

        boolean find = false;

        for(Document student : quiz.getList("students", Document.class)) {

            List<ObjectId> memberIds = student.getList("team", ObjectId.class);

            int idx = memberIds.indexOf(userId);
            if(idx < 0)
                continue;

            memberIds.remove(idx);
            find = true;
            break;
        }

        if(find) {
            onlineStandQuizRepository.replaceOne(quizId, quiz);
            return JSON_OK;
        }

        return JSON_NOT_VALID_PARAMS;
    }

    public static String removeMember(ObjectId userId, ObjectId quizId, ObjectId mainMember, JSONArray items) {

        if(userId != null && !mainMember.equals(userId))
            return JSON_NOT_ACCESS;

        if (items.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", mainMember
        );

        if(std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);
        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for(int i = 0; i < items.length(); i++) {

            if(!ObjectId.isValid(items.getString(i))) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(items.getString(i));
            int idx = team.indexOf(oId);
            if(idx < 0) {
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

        if(userId != null && !mainMember.equals(userId))
            return JSON_NOT_ACCESS;

        if (items.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        Document quiz = onlineStandQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        Document std = searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", mainMember
        );

        if(std == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> team = std.getList("team", ObjectId.class);

        if(team.size() + items.length() > quiz.getInteger("per_team"))
            return generateErr("حداکثر تعداد نفرات در هر گروه می تواند " + quiz.getInteger("per_team") + " نفر باشد");

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for(int i = 0; i < items.length(); i++) {

            if(!validationNationalCode(items.getString(i))) {
                excepts.put(i + 1);
                continue;
            }

            Document newMember = userRepository.findBySecKey(items.getString(i));

            if(newMember == null) {
                excepts.put(i + 1);
                continue;
            }

            if(team.contains(newMember.getObjectId("_id"))) {
                excepts.put(i + 1);
                continue;
            }

            team.add(newMember.getObjectId("_id"));
            doneIds.put(newMember.getObjectId("_id").toString());
        }

        onlineStandQuizRepository.replaceOne(quizId, quiz);
        return returnAddResponse(excepts, doneIds);
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
                    .append("start_at", null)
                    .append("answers", new byte[0]);

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
                boolean canSeeResult = quiz.getBoolean("show_results_after_correction") &&
                        quiz.containsKey("report_status") &&
                        quiz.getString("report_status").equalsIgnoreCase("ready");

                if (canSeeResult)
                    jsonObject.put("status", "finished")
                            .put("questionsCount", questionsCount);
                else
                    jsonObject.put("status", "waitForResult")
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

        if(quiz.containsKey("students")) {
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
                };

                jsonObject.put("descBefore", quiz.getOrDefault("desc", ""));
                jsonObject.put("descAfter", quiz.getOrDefault("desc_after", ""));
                jsonObject.put("attaches", attaches);
            }
        }

        return jsonObject;
    }
}
