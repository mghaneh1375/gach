package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx;

public class TashrihiQuizController extends QuizAbstract {

    //duration is optional field and if exist entity will be a tashrihi quiz
    // and else will be a HW
    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "isOnline", "showResultsAfterCorrection",
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database", "minusMark"
    };

    public int calcLen(Document quiz) {
        return 0;
    }

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

    // rebuild list on each call
    // school can dispatch questions between correctors by question or student
    public static String setCorrectors(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

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
                        if (searchInDocumentsKeyValIdx(
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
                        if (searchInDocumentsKeyValIdx(
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
            PairValue p = hasCorrectorAccess(db, userId, quizId);
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
            PairValue p = hasCorrectorAccess(db, userId, quizId);
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

            if (studentIds != null && !studentIds.contains(studentId))
                return JSON_NOT_ACCESS;

            List<Document> students = quiz.getList("students", Document.class);
            Document student = Utility.searchInDocumentsKeyVal(students, "user_id", studentId);

            if (student == null)
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
                            quiz.get("questions", Document.class),
                            student.getList("answers", Document.class),
                            db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                    ))
            );

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    // defaults: curr: true, future: true, pass: false
    public static String getMyTasks(ObjectId userId,
                                    String mode, boolean pass,
                                    boolean curr, boolean future) {

        ArrayList<Bson> filters = new ArrayList<>();

        filters.add(eq("mode", KindQuiz.TASHRIHI));
        filters.add(eq("correctors._id", userId));

        int today = Utility.getToday();

        if (!pass)
            filters.add(gt("end", today));

        if (!curr)
            filters.add(or(
                    lt("end", today),
                    gt("start", today)
            ));

        if (!future)
            filters.add(lt("start", today));

        JSONArray iryscTasks = new JSONArray();
        JSONArray schoolTasks = new JSONArray();

        if (mode == null || mode.equals(GeneralKindQuiz.IRYSC.getName())) {

            ArrayList<Document> docs = iryscQuizRepository.find(and(filters),
                    TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS
            );

            for (Document doc : docs) {
                iryscTasks.put(
                        convertTashrihiQuizToJSONDigestForTeachers(doc)
                );
            }
        }

        if (mode == null || mode.equals(GeneralKindQuiz.SCHOOL.getName())) {

            ArrayList<Document> docs = schoolQuizRepository.find(and(filters),
                    TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS
            );

            for (Document doc : docs) {
                schoolTasks.put(
                        convertTashrihiQuizToJSONDigestForTeachers(doc)
                );
            }

        }

        return Utility.generateSuccessMsg("iryscTasks", iryscTasks,
                new PairValue("schoolTasks", schoolTasks)
        );

    }

    public static String setMark(Common db, ObjectId userId,
                                 ObjectId quizId, ObjectId answerId,
                                 ObjectId studentId, JSONObject markData) {
        try {

            PairValue p = hasCorrectorAccess(db, userId, quizId);

            Document quiz = (Document) p.getKey();
            int correctorIdx = (int) p.getValue();

            if (!DEV_MODE && quiz.getLong("end") > System.currentTimeMillis())
                return Utility.generateErr("آزمون موردنظر هنوز به اتمام نرسیده است.");

            List<Document> students = quiz.getList("students", Document.class);

            int stdIdx = searchInDocumentsKeyValIdx(students, "_id", studentId);

            if (stdIdx == -1)
                return JSON_NOT_VALID_ID;

            if (!students.get(stdIdx).containsKey("answers"))
                return Utility.generateErr("دانش آموز موردنظر در این آزمون غایب بوده است.");

            List<Document> studentAnswers = students.get(stdIdx).getList("answers", Document.class);

            int answerIdx = searchInDocumentsKeyValIdx(studentAnswers, "_id", answerId);
            if (answerIdx == -1)
                return JSON_NOT_VALID_ID;

            double mark = markData.getNumber("mark").doubleValue();

            ObjectId questionId = studentAnswers.get(answerIdx).getObjectId("question_id");

            if(correctorIdx != -1) {

                Document correctorAccess = quiz.getList("correctors", Document.class).get(correctorIdx);

                if(correctorAccess.get("students") != null &&
                        !correctorAccess.getList("students", ObjectId.class).contains(studentId)
                )
                    return JSON_NOT_ACCESS;

                if(correctorAccess.get("questions") != null &&
                        !correctorAccess.getList("questions", ObjectId.class).contains(questionId)
                )
                    return JSON_NOT_ACCESS;

            }

            List<Document> questions = quiz.getList("questions", Document.class);
            int questionIdx = searchInDocumentsKeyValIdx(questions, "_id", questionId);

            if (questions.get(questionIdx).getInteger("mark") < mark)
                return Utility.generateErr("حداکثر نمره مجاز برای این سوال " + questions.get(questionIdx).getInteger("mark") + " می باشد.");

            double finalMark;

            if (!students.get(stdIdx).containsKey("total_mark")) {
                students.get(stdIdx).put("total_mark", mark);
            } else if (studentAnswers.get(answerIdx).containsKey("mark")) {
                finalMark = students.get(stdIdx).getDouble("total_mark") - studentAnswers.get(answerIdx).getDouble("mark") + mark;
                students.get(stdIdx).put("total_mark", finalMark);
            } else {
                finalMark = students.get(stdIdx).getDouble("total_mark") + mark;
                students.get(stdIdx).put("total_mark", finalMark);
            }

            studentAnswers.get(answerIdx).put("mark", mark);
            studentAnswers.get(answerIdx).put("mark_desc",
                    markData.has("description") ?
                            markData.getString("description") : ""
            );

            boolean allMarked = true;

            if (!students.get(stdIdx).containsKey("all_marked")) {

                for (Document studentAnswer : studentAnswers) {
                    if (!studentAnswer.containsKey("mark")) {
                        allMarked = false;
                        break;
                    }
                }
            }


            if (allMarked)
                students.get(stdIdx).put("all_marked", true);

            new Thread(() -> db.replaceOne(quizId, quiz)).start();
            return JSON_OK;
        }
        catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }
    }

    @Override
    ArrayList<Document> registry(ObjectId studentId, String phone, String mail,
                  ArrayList<ObjectId> quizIds, int paid) {

//        List<Document> students = quiz.getList("students", Document.class);
//
//        if (searchInDocumentsKeyValIdx(
//                students, "_id", studentId
//        ) != -1)
//            return null;
//
//        Document stdDoc = new Document("_id", studentId)
//                .append("paid", paid)
//                .append("register_at", System.currentTimeMillis())
//                .append("finish_at", null)
//                .append("start_at", null)
//                .append("answers", new ArrayList<>())
//                .append("all_marked", false);
//
//        if ((boolean) quiz.getOrDefault("permute", false))
//            stdDoc.put("question_indices", new ArrayList<>());
//
//        students.add(stdDoc);
//
//        return stdDoc;
        return null;
    }

    @Override
    void quit(Document student, Document quiz) {

    }

//    @Override
//    String buy(Document student, Document quiz) {
//        return null;
//    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin, boolean afterBuy) {
        return null;
    }

}
