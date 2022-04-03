package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.Authorization;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;


public class QuizController {

    private static Document hasAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        if (userId != null && !quiz.getObjectId("created_by").equals(userId))
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        return quiz;
    }

    public static Document store(ObjectId userId, JSONObject data
    ) throws InvalidFieldsException {

        Document newDoc = new Document();

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        Utility.isValid(newDoc);

        if (!newDoc.containsKey("desc_after_mode") ||
                newDoc.getString("desc_after_mode").equals(DescMode.FILE.getName())
        )
            newDoc.put("desc_after_mode", DescMode.NONE.getName());


        newDoc.put("visibility", true);
        newDoc.put("students", new ArrayList<>());
        newDoc.put("remove_questions", new ArrayList<>());
        newDoc.put("tags", new ArrayList<>());
        newDoc.put("attaches", new ArrayList<>());
        newDoc.put("created_by", userId);
        newDoc.put("created_at", System.currentTimeMillis());

        //todo: consider other modes
        if (newDoc.getString("mode").equals(KindQuiz.REGULAR.getName()) ||
                newDoc.getString("mode").equals(KindQuiz.OPEN.getName())
        )
            newDoc.put("questions", new ArrayList<>());

        return newDoc;
    }

    public static String toggleVisibility(Common db, ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            quiz.put("visibility", quiz.getBoolean("visibility"));
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    x.getMessage()
            );
        }

    }

    public static String forceRegistry(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            List<Document> students = quiz.getList("students", Document.class);
            JSONArray skipped = new JSONArray();

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ObjectId studentId = new ObjectId(jsonObject.getString("id"));

                Document student = userRepository.findById(studentId);

                if (student == null) {
                    skipped.put(i);
                    continue;
                }

                if (userId != null && !Authorization.hasAccessToThisStudent(studentId, userId)) {
                    skipped.put(i);
                    continue;
                }

                quizAbstract.registry(student, quiz, jsonObject.getInt("paid"));
            }

            quiz.put("students", students);
            db.replaceOne(quizId, quiz);

            if (skipped.length() == 0)
                return JSON_OK;

            return new JSONObject()
                    .put("status", "nok")
                    .put("skipped", skipped)
                    .toString();
        } catch (InvalidFieldsException x) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    x.getMessage()
            );
        }

    }

    public static String forceDeportation(Common db, ObjectId userId,
                                          ObjectId quizId, JSONArray jsonArray) {


        try {

            Document quiz = hasAccess(db, userId, quizId);
            List<Document> students = quiz.getList("students", Document.class);
            JSONArray skipped = new JSONArray();

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            for (int i = 0; i < jsonArray.length(); i++) {

                ObjectId studentId = new ObjectId(jsonArray.getString(i));

                Document student = userRepository.findById(studentId);

                if (student == null) {
                    skipped.put(i);
                    continue;
                }

                quizAbstract.quit(student, quiz);
            }

            quiz.put("students", students);
            db.replaceOne(quizId, quiz);

            if (skipped.length() == 0)
                return JSON_OK;

            return new JSONObject()
                    .put("status", "nok")
                    .put("skipped", skipped)
                    .toString();

        } catch (InvalidFieldsException x) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    x.getMessage()
            );
        }

    }

    public static String remove(Common db, ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            // todo: imp

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    x.getMessage()
            );
        }

    }

    public static String buy(Document user, ObjectId quizId) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null || !quiz.getBoolean("visibility"))
            return JSON_NOT_ACCESS;

        QuizAbstract quizAbstract;

        // todo : complete this section
        if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
            quizAbstract = new RegularQuizController();
        else
            quizAbstract = new TashrihiQuizController();

        return quizAbstract.buy(user, quiz);

    }

    public static String getParticipants(Common db,
                                         ObjectId userId,
                                         ObjectId quizId,
                                         ObjectId studentId,
                                         Boolean isStudentAnswersNeeded,
                                         Boolean isResultsNeeded,
                                         Boolean justMarked,
                                         Boolean justNotMarked,
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

                if (justMarked != null && justMarked &&
                        student.containsKey("all_marked") &&
                        !student.getBoolean("all_marked")
                )
                    continue;

                if (justNotMarked != null && justNotMarked &&
                        student.containsKey("all_marked") &&
                        student.getBoolean("all_marked")
                )
                    continue;

                Document user = userRepository.findById(student.getObjectId("_id"));
                if (user == null)
                    continue;

                JSONObject jsonObject = new JSONObject()
                        .put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"))
                        .put("username", user.getString("username"))
                        .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                        .put("studentId", user.getObjectId("_id").toString())
                        .put("startAt", student.containsKey("start_at") ?
                                Utility.getSolarDate(student.getLong("start_at")) :
                                ""
                        )
                        .put("finishAt", student.containsKey("finish_at") ?
                                Utility.getSolarDate(student.getLong("finish_at")) :
                                ""
                        );

                if(student.containsKey("all_marked"))
                    jsonObject.put("allMarked", student.getBoolean("all_marked"));

                if (isResultsNeeded != null && isResultsNeeded)
                    jsonObject.put("totalMark", student.getOrDefault("total_mark", ""));

                if (isStudentAnswersNeeded != null && isStudentAnswersNeeded) {

                    if (!student.containsKey("answers"))
                        jsonObject.put("answers", new JSONArray());

                    else {
                        jsonObject.put("answers", Utility.getQuestions(
                                true,
                                false,
                                task.getList("questions", Document.class),
                                student.getList("answers", Document.class)
                        ));
                    }
                }


                jsonArray.put(jsonObject);
            }

            return Utility.generateSuccessMsg("students", jsonArray);
        }
        catch (InvalidFieldsException x) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    x.getMessage()
            );
        }
    }
}
