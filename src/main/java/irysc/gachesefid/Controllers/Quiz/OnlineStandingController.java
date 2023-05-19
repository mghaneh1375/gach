package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.OnlineStandQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Controllers.Quiz.QuizController.convertStudentDocToJSON;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

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

                JSONObject jsonObject = convertStudentDocToJSON(student, user);
                jsonObject.put("point", student.getOrDefault("point", 0))
                        .put("teamName", student.getString("team_name"))
                        .put("teamCount", student.getList("team", ObjectId.class).size())
                ;

                jsonArray.put(jsonObject);
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    @Override
    List<Document> registry(ObjectId studentId, String phone, String mail, List<ObjectId> quizIds, int paid, ObjectId transactionId, String stdName) {
        return null;
    }

    @Override
    void quit(Document student, Document quiz) {

    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin, boolean afterBuy, boolean isDescNeeded) {

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

        } else
            jsonObject.put("startRegistry", quiz.getLong("start_registry"))
                    .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                    .put("price", quiz.get("price"));

        if (isAdmin) {
            jsonObject
                    .put("teamsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("priority", quiz.getInteger("priority"))
                    .put("questionsCount", questionsCount);
        }

        jsonObject.put("reminder", Math.max(quiz.getInteger("max_teams") - quiz.getInteger("registered"), 0));

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
