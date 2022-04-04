package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import jdk.jfr.consumer.RecordedThread;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class TashrihiQuizController extends QuizAbstract {

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "isOnline", "showResultsAfterCorrect",
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database", "minusMark"
    };


    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            irysc.gachesefid.Controllers.Quiz.Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", KindQuiz.TASHRIHI.getName());
            Document newDoc = QuizController.store(userId, jsonObject);

            return iryscQuizRepository.insertOneWithReturn(newDoc);

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    e.getMessage()
            );
        }

    }

    @Override
    void registry(Document student, Document quiz, int paid) {

        List<Document> students = quiz.getList("students", Document.class);

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        ) != -1)
            return;

        Document stdDoc = new Document("_id", student.getObjectId("_id"))
                .append("paid", paid)
                .append("register_at", System.currentTimeMillis())
                .append("finish_at", null)
                .append("start_at", null)
                .append("answers", new ArrayList<>())
                .append("all_marked", false);

        if ((boolean) quiz.getOrDefault("permute", false))
            stdDoc.put("question_indices", new ArrayList<>());

        students.add(stdDoc);


    }

    @Override
    void quit(Document student, Document quiz) {

    }

    @Override
    String buy(Document student, Document quiz) {
        return null;
    }

    // rebuild list on each call
    // school can dispatch questions between correctors by question or student
    public static String setCorrectors(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray) {

        try {
            Document quiz = QuizController.hasAccess(db, userId, quizId);

            if (!quiz.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
                return JSON_NOT_ACCESS;

            List<Document> correctors = new ArrayList<>();
            List<Document> questions = quiz.getList("questions", Document.class);
            List<Document> students = quiz.getList("students", Document.class);

            JSONArray skipped = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ObjectId teacherId = new ObjectId(jsonObject.getString("teacherId"));

                Document teacher = userRepository.findById(teacherId);
                if (teacher == null ||
                        (userId != null && !Authorization.hasAccessToThisTeacher(teacherId, userId))
                ) {
                    skipped.put(i);
                    continue;
                }

                Document doc = new Document("_id", teacherId);

                if (jsonObject.has("questionIds")) {

                    ArrayList<ObjectId> questionIds = new ArrayList<>();
                    JSONArray jsonArray1 = jsonObject.getJSONArray("questionIds");

                    for (int j = 0; j < jsonArray1.length(); j++) {

                        ObjectId questionId = new ObjectId(jsonArray1.getString(j));
                        if (Utility.searchInDocumentsKeyValIdx(
                                questions, "_id", questionId
                        ) == -1)
                            continue;

                        questionIds.add(questionId);
                    }

                    doc.put("questions", questionIds);
                } else
                    doc.put("questions", null);

                if (jsonObject.has("studentIds")) {

                    ArrayList<ObjectId> studentIds = new ArrayList<>();
                    JSONArray jsonArray1 = jsonObject.getJSONArray("studentIds");

                    for (int j = 0; j < jsonArray1.length(); j++) {

                        ObjectId studentId = new ObjectId(jsonArray1.getString(j));
                        if (Utility.searchInDocumentsKeyValIdx(
                                students, "_id", studentId
                        ) == -1)
                            continue;

                        studentIds.add(studentId);
                    }

                    doc.put("students", studentIds);
                } else
                    doc.put("students", null);

                correctors.add(doc);
            }

            quiz.put("correctors", correctors);

            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return Utility.generateErr(e.getMessage());
        }

    }

    public static String getMyMarkList(Common db, ObjectId userId,
                                       ObjectId quizId, Boolean justMarked,
                                       Boolean justNotMark) {

        if (justMarked != null && justMarked &&
                justNotMark != null && justNotMark)
            return JSON_NOT_VALID_PARAMS;

        try {
            PairValue p = QuizController.hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int idx = (int) p.getValue();

            List<ObjectId> studentIds = null;
            List<ObjectId> questionIds = null;

            if (idx != -1) {

                Document doc = quiz.getList("correctors", Document.class).get(idx);

                if (doc.get("students") != null)
                    studentIds = doc.getList("students", ObjectId.class);

                if (doc.get("questions") != null)
                    questionIds = doc.getList("questions", ObjectId.class);

            }

            List<Document> students = quiz.getList("students", Document.class);
            JSONArray data = new JSONArray();

            for (Document student : students) {

                if (studentIds != null &&
                        !studentIds.contains(student.getObjectId("user_id"))
                )
                    continue;

                boolean allMarked = questionIds == null && student.getBoolean("all_marked");

                if (questionIds != null) {

                    List<Document> answers = student.getList("answers", Document.class);
                    boolean hasAnyNotMark = false;

                    for (ObjectId questionId : questionIds) {

                        if (!Utility.searchInDocumentsKeyVal(
                                answers, "question_id", questionId
                        ).containsKey("mark")) {
                            hasAnyNotMark = true;
                            break;
                        }

                    }

                    allMarked = !hasAnyNotMark;
                }


                if (justMarked != null && justMarked && !allMarked)
                    continue;

                if (justNotMark != null && justNotMark && allMarked)
                    continue;

                Document user = userRepository.findById(
                        student.getObjectId("user_id")
                );

                data.put(new JSONObject()
                        .put("allMarked", allMarked)
                        .put("studentName", user.getString("name_fa") + " " + user.getString("last_name_fa"))
                        .put("studentId", student.getObjectId("user_id").toString())
                );
            }

            return Utility.generateSuccessMsg("data", data);

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    public static String getMyMarkListForSpecificStudent(Common db, ObjectId userId,
                                                         ObjectId quizId, ObjectId studentId) {

        try {
            PairValue p = QuizController.hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int idx = (int) p.getValue();

            List<ObjectId> studentIds = null;
            List<ObjectId> questionIds = null;

            if (idx != -1) {

                Document doc = quiz.getList("correctors", Document.class).get(idx);

                if (doc.get("students") != null)
                    studentIds = doc.getList("students", ObjectId.class);

                if (doc.get("questions") != null)
                    questionIds = doc.getList("questions", ObjectId.class);

            }

            if(studentIds != null && !studentIds.contains(studentId))
                return JSON_NOT_ACCESS;

            List<Document> students = quiz.getList("students", Document.class);
            Document student = Utility.searchInDocumentsKeyVal(students, "user_id", studentId);

            if(student == null)
                return JSON_NOT_VALID_PARAMS;

            boolean allMarked = questionIds == null && student.getBoolean("all_marked");

            if (questionIds != null) {

                List<Document> answers = student.getList("answers", Document.class);
                boolean hasAnyNotMark = false;

                for (ObjectId questionId : questionIds) {

                    if (!Utility.searchInDocumentsKeyVal(
                            answers, "question_id", questionId
                    ).containsKey("mark")) {
                        hasAnyNotMark = true;
                        break;
                    }

                }

                allMarked = !hasAnyNotMark;
            }

            Document user = userRepository.findById(
                    student.getObjectId("user_id")
            );

            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("allMarked", allMarked)
                    .put("student", new JSONObject()
                            .put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"))
                            .put("id", student.getObjectId("user_id").toString())
                            .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                    )
                    .put("answers", irysc.gachesefid.Controllers.Quiz.Utility.getQuestions(
                            true, true,
                            quiz.getList("questions", Document.class),
                            student.getList("answers", Document.class),
                            db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                    ))
            );

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    public static String getMyTasks(Common db, ObjectId userId,
                                    boolean curr, boolean future,
                                    boolean pass) {

        return JSON_OK;
    }

}
