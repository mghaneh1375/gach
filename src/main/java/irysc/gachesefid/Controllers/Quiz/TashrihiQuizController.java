package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class TashrihiQuizController extends QuizAbstract {

    //duration is optional field and if exist entity will be a tashrihi quiz
    // and else will be a HW
    private final static String[] mandatoryFields = {
            "showResultsAfterCorrection",
            "showResultsAfterCorrectionNotLoginUsers",
            "isUploadable", "isRegistrable",
            "isQRNeeded", "priority"
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database", "minusMark"
    };

    public static String corrector(Common db, ObjectId userId, ObjectId quizId, ObjectId correctorId) {
        try {

            Document quiz = hasAccess(db, userId, quizId);
            List<Document> correctors = quiz.getList("correctors", Document.class);

            Document corrector = Utility.searchInDocumentsKeyVal(
                    correctors, "_id", correctorId
            );

            if (corrector == null)
                return JSON_NOT_VALID_ID;

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
                            answer.containsKey("mark") &&
                                    (
                                            myStudents.contains(student.getObjectId("_id")) ||
                                                    myQuestions.contains(answer.getObjectId("question_id"))
                                    )
                    )
                        allMarkedQuestions++;
                }

            }

            Document questionsDoc = quiz.get("questions", Document.class);
            int qSize = questionsDoc.getList("_ids", ObjectId.class).size();
            int sNo = 0;

            if (corrector.containsKey("students")) {
                sNo = corrector.getList("students", ObjectId.class).size();
                allCorrectorQuestions += sNo * qSize;
            }

            if (corrector.containsKey("questions")) {

                int qNo = corrector.getList("questions", ObjectId.class).size();

                if (corrector.containsKey("students"))
                    allCorrectorQuestions -= qNo * sNo;

                allCorrectorQuestions += qNo * students.size();
            }


            return generateSuccessMsg("data",
                    new JSONObject()
                            .put("allQuestions", allCorrectorQuestions)
                            .put("allMarked", allMarkedQuestions)
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeCorrectors(Common db, ObjectId userId,
                                          ObjectId quizId, JSONArray items) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!quiz.containsKey("correctors"))
                return JSON_NOT_ACCESS;

            List<Document> correctors = quiz.getList("correctors", Document.class);
            JSONArray excepts = new JSONArray();
            JSONArray doneIds = new JSONArray();

            for (int i = 0; i < items.length(); i++) {

                String id = items.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId oId = new ObjectId(id);

                int idx = searchInDocumentsKeyValIdx(
                        correctors, "_id", oId
                );

                if (idx < 0) {
                    excepts.put(i + 1);
                    continue;
                }

                correctors.remove(idx);
                doneIds.put(id);
            }

            db.replaceOne(quizId, quiz);
            return returnRemoveResponse(
                    excepts, doneIds
            );
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String addCorrector(Common db, ObjectId userId, ObjectId quizId, String NID) {

        if (!Utility.validationNationalCode(NID))
            return generateErr("کد ملی وارد شده معتبر نمی باشد");

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!quiz.containsKey("correctors"))
                return JSON_NOT_ACCESS;

            Document user = userRepository.findBySecKey(NID);
            if (user == null ||
                    !user.getList("accesses", String.class).contains(Access.TEACHER.getName())
            )
                return generateErr("کدملی وارد شده مربوط به یک دبیر نیست");

            List<Document> correctors = quiz.getList("correctors", Document.class);
            ObjectId id = new ObjectId();

            correctors.add(new Document("_id", id)
                    .append("user_id", user.getObjectId("_id"))
            );

            db.replaceOne(quizId, quiz);

            return generateSuccessMsg("data", new JSONObject()
                    .put("id", id.toString())
                    .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                    .put("questions", 0)
                    .put("students", 0)
            );
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String correctors(Common db, ObjectId userId, ObjectId quizId) {
        try {
            Document quiz = hasAccess(db, userId, quizId);
            List<Document> correctors = quiz.getList("correctors", Document.class);
            JSONArray jsonArray = new JSONArray();

            for (Document corrector : correctors) {
                Document user = userRepository.findById(corrector.getObjectId("user_id"));
                if (user == null)
                    continue;

                jsonArray.put(new JSONObject()
                        .put("id", corrector.getObjectId("_id").toString())
                        .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                        .put("questions",
                                corrector.containsKey("questions") ?
                                        corrector.getList("questions", ObjectId.class).size() : 0
                        )
                        .put("students",
                                corrector.containsKey("students") ?
                                        corrector.getList("students", ObjectId.class).size() : 0
                        )
                );
            }

            return generateSuccessMsg("data", jsonArray);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String create(ObjectId userId, JSONObject jsonObject, String mode) {

        try {

            checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", KindQuiz.TASHRIHI.getName());

            if (jsonObject.getBoolean("isRegistrable") && (
                    !jsonObject.has("price") ||
                            !jsonObject.has("startRegistry") ||
                            !jsonObject.has("endRegistry")
            )
            )
                return generateErr("لطفا تمام فیلدهای لازم را پر نمایید");

            if (jsonObject.getBoolean("isUploadable") && (
                    !jsonObject.has("start") ||
                            !jsonObject.has("end")
            ))
                return generateErr("لطفا تمام فیلدهای لازم را پر نمایید");

            if (jsonObject.getBoolean("isQRNeeded") && (
                    jsonObject.has("duration")
            ))
                return generateErr("فیلد مدت آزمون نامعتبر است");

            Document newDoc = QuizController.store(userId, jsonObject);
            iryscQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz", new TashrihiQuizController()
                            .convertDocToJSON(newDoc, false, true,
                                    false, false
                            )
            );


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
                                       ObjectId quizId, String taskMode) {

        if (
                !taskMode.equalsIgnoreCase("student") &&
                        !taskMode.equalsIgnoreCase("question")
        )
            return JSON_NOT_VALID_PARAMS;

        try {
            PairValue p = hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int idx = (int) p.getValue();
            if (idx == -1)
                return JSON_NOT_ACCESS;

            List<ObjectId> studentIds = null;
            List<ObjectId> questionIds = null;
            JSONArray jsonArray = new JSONArray();

            Document doc = quiz.getList("correctors", Document.class).get(idx);

            if (taskMode.equals("student") && doc.get("students") != null)
                studentIds = doc.getList("students", ObjectId.class);

            if (taskMode.equals("question") && doc.get("questions") != null)
                questionIds = doc.getList("questions", ObjectId.class);

            List<Document> students = quiz.getList("students", Document.class);

            if (questionIds != null) {

                Document questions = quiz.get("questions", Document.class);
                List<ObjectId> ids = questions.getList("_ids", ObjectId.class);
                List<Double> marks = questions.getList("marks", Double.class);

                for (ObjectId id : questionIds) {

                    int qIdx = ids.indexOf(id);

                    if (qIdx == -1)
                        continue;

                    int marked = 0;
                    int total = 0;

                    for (Document student : students) {

                        if (!student.containsKey("answers"))
                            continue;

                        Document ans = searchInDocumentsKeyVal(
                                student.getList("answers", Document.class),
                                "question_id", id
                        );

                        if (ans == null)
                            continue;

                        if (ans.containsKey("mark"))
                            marked++;

                        total++;
                    }

                    jsonArray.put(
                            new JSONObject()
                                    .put("id", id.toString())
                                    .put("no", qIdx + 1)
                                    .put("allMarked", marked)
                                    .put("total", total)
                                    .put("mark", marks.get(idx))
                    );

                }

            }

            if (studentIds != null) {

                for (ObjectId stdId : studentIds) {

                    Document std = Utility.searchInDocumentsKeyVal(
                            students, "_id", stdId
                    );

                    if (std == null)
                        continue;

                    Document user = userRepository.findById(stdId);
                    if (user == null)
                        continue;

                    JSONObject jsonObject = new JSONObject()
                            .put("allMarked", std.getOrDefault("all_marked", false))
                            .put("totalMark", std.getOrDefault("total_mark", 0));

                    Utility.fillJSONWithUser(jsonObject, user);
                    jsonArray.put(jsonObject);
                }
            }

            return Utility.generateSuccessMsg("data", jsonArray);

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    public static String setCorrectorByQuestionMode(Common db, ObjectId userId,
                                                    ObjectId quizId, ObjectId correctorId,
                                                    JSONArray questionsJSONArr) {
        try {

            Document quiz = hasAccess(db, userId, quizId);
            List<Document> correctors = quiz.getList("correctors", Document.class);

            Document corrector = Utility.searchInDocumentsKeyVal(
                    correctors, "_id", correctorId
            );

            if (corrector == null)
                return JSON_NOT_VALID_ID;

            List<ObjectId> myQuestions = new ArrayList<>();

            Document questions = quiz.get("questions", Document.class);

            List<ObjectId> ids = questions.getList("_ids", ObjectId.class);
            JSONArray doneIds = new JSONArray();
            JSONArray excepts = new JSONArray();

            for (int i = 0; i < questionsJSONArr.length(); i++) {

                try {
                    String id = questionsJSONArr.getString(i);
                    if (!ObjectId.isValid(id)) {
                        excepts.put(i + 1);
                        continue;
                    }

                    ObjectId oId = new ObjectId(id);
                    if (!ids.contains(oId)) {
                        excepts.put(i + 1);
                        continue;
                    }

                    for (Document c : correctors) {

                        if (!c.containsKey("questions") || c.getObjectId("_id").equals(correctorId))
                            continue;

                        List<ObjectId> qs = c.getList("questions", ObjectId.class);
                        if (qs.contains(oId)) {
                            qs.remove(oId);
                            break;
                        }

                    }

                    myQuestions.add(oId);
                    doneIds.put(id);
                } catch (Exception ex) {
                    excepts.put(i + 1);
                }
            }

            corrector.put("questions", myQuestions);
            db.replaceOne(quizId, quiz);

            return returnAddResponse(excepts, doneIds);
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String setCorrectorByStudentMode(Common db, ObjectId userId,
                                                   ObjectId quizId, ObjectId correctorId,
                                                   JSONArray studentsJSONArr) {
        try {

            Document quiz = hasAccess(db, userId, quizId);
            List<Document> correctors = quiz.getList("correctors", Document.class);

            Document corrector = Utility.searchInDocumentsKeyVal(
                    correctors, "_id", correctorId
            );

            if (corrector == null)
                return JSON_NOT_VALID_ID;

            List<ObjectId> myStudents = new ArrayList<>();

            List<Document> students = quiz.getList("students", Document.class);
            JSONArray doneIds = new JSONArray();
            JSONArray excepts = new JSONArray();

            for (int i = 0; i < studentsJSONArr.length(); i++) {

                try {
                    String id = studentsJSONArr.getString(i);
                    if (!ObjectId.isValid(id)) {
                        excepts.put(i + 1);
                        continue;
                    }

                    ObjectId oId = new ObjectId(id);
                    if (Utility.searchInDocumentsKeyValIdx(students, "_id", oId) == -1) {
                        excepts.put(i + 1);
                        continue;
                    }

                    for (Document c : correctors) {

                        if (!c.containsKey("students") || c.getObjectId("_id").equals(correctorId))
                            continue;

                        List<ObjectId> stds = c.getList("students", ObjectId.class);
                        if (stds.contains(oId)) {
                            stds.remove(oId);
                            break;
                        }

                    }

                    myStudents.add(oId);
                    doneIds.put(id);
                } catch (Exception ex) {
                    excepts.put(i + 1);
                }
            }

            corrector.put("students", myStudents);
            db.replaceOne(quizId, quiz);

            return returnAddResponse(excepts, doneIds);
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String getMyMarkListForSpecificStudent(Common db, ObjectId userId,
                                                         ObjectId quizId, ObjectId studentId) {

        try {
            PairValue p = hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int idx = (int) p.getValue();

            List<ObjectId> studentIds = null;

            if (idx != -1) {
                Document doc = quiz.getList("correctors", Document.class).get(idx);

                if (!doc.containsKey("students"))
                    return JSON_NOT_ACCESS;

                studentIds = doc.getList("students", ObjectId.class);
            }

            if (studentIds != null && !studentIds.contains(studentId))
                return JSON_NOT_ACCESS;

            List<Document> students = quiz.getList("students", Document.class);
            Document student = Utility.searchInDocumentsKeyVal(students, "_id", studentId);

            if (student == null)
                return JSON_NOT_VALID_PARAMS;

            boolean allMarked = (boolean) student.getOrDefault("all_marked", false);

            Document user = userRepository.findById(
                    student.getObjectId("_id")
            );

            List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", quiz.getString("title"))
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("generalMode",
                            db instanceof IRYSCQuizRepository ? AllKindQuiz.IRYSC.getName() : "school")
                    .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", quiz.getOrDefault("desc", ""))
                    .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                    .put("mode", quiz.getString("mode"))
                    .put("attaches", jsonArray);


            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("allMarked", allMarked)
                    .put("quizInfo", quizJSON)
                    .put("student", new JSONObject()
                            .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                            .put("id", student.getObjectId("_id").toString())
                    )
                    .put("answers", irysc.gachesefid.Controllers.Quiz.Utility.getTashrihiQuestions(
                            true, true, (boolean) quiz.getOrDefault("is_q_r_needed", false),
                            quiz.get("questions", Document.class),
                            student.getList("answers", Document.class),
                            db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                    ))
            );

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    public static String getMyMarks(Common db, ObjectId quizId, ObjectId studentId) {

        try {
            Document quiz = hasProtectedAccess(db, studentId, quizId);

            List<Document> students = quiz.getList("students", Document.class);
            Document student = Utility.searchInDocumentsKeyVal(students, "_id", studentId);

            boolean allMarked = (boolean) student.getOrDefault("all_marked", false);
            if(!allMarked)
                return generateErr("پاسخبرگ شما هنوز تصحیح نشده است");

            List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", quiz.getString("title"))
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("generalMode",
                            db instanceof IRYSCQuizRepository ? AllKindQuiz.IRYSC.getName() : "school")
                    .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", quiz.getOrDefault("desc", ""))
                    .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                    .put("mode", quiz.getString("mode"))
                    .put("totalMark", student.get("total_mark"))
                    .put("attaches", jsonArray);


            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("allMarked", allMarked)
                    .put("quizInfo", quizJSON)
                    .put("answers", irysc.gachesefid.Controllers.Quiz.Utility.getTashrihiQuestions(
                            true, true, (boolean) quiz.getOrDefault("is_q_r_needed", false),
                            quiz.get("questions", Document.class),
                            student.getList("answers", Document.class),
                            db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                    ))
            );

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    public static String getMyMarkListForSpecificQuestion(Common db, ObjectId userId,
                                                          ObjectId quizId, ObjectId questionId) {

        try {
            PairValue p = hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int idx = (int) p.getValue();

            List<ObjectId> questionIds = null;

            if (idx != -1) {

                Document doc = quiz.getList("correctors", Document.class).get(idx);

                if (!doc.containsKey("questions"))
                    return JSON_NOT_ACCESS;

                questionIds = doc.getList("questions", ObjectId.class);
            }

            if (questionIds != null && !questionIds.contains(questionId))
                return JSON_NOT_ACCESS;

            Document question = quiz.get("questions", Document.class);
            List<ObjectId> ids = question.getList("_ids", ObjectId.class);

            int qIdx = ids.indexOf(questionId);

            if (qIdx < 0)
                return JSON_NOT_VALID_PARAMS;

            Document q = questionRepository.findById(questionId);
            if (q == null)
                return JSON_NOT_UNKNOWN;

            JSONArray qJSONArr = Utilities.convertList(new ArrayList<>() {{
                                                           add(q);
                                                       }},
                    true, true, true, true, true
            );

            if (qJSONArr.length() != 1)
                return JSON_NOT_UNKNOWN;

            List<Double> marks = question.getList("marks", Double.class);
            double qMark = marks.get(qIdx);

            JSONObject qJSON = qJSONArr.getJSONObject(0);

            List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", quiz.getString("title"))
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("generalMode",
                            db instanceof IRYSCQuizRepository ? AllKindQuiz.IRYSC.getName() : "school")
                    .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", quiz.getOrDefault("desc", ""))
                    .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                    .put("mode", quiz.getString("mode"))
                    .put("attaches", jsonArray);


            boolean allMarked = true;
            HashMap<ObjectId, Document> stdAnswers = new HashMap<>();
            List<Document> students = quiz.getList("students", Document.class);

            for (Document std : students) {

                if (!std.containsKey("answers"))
                    continue;

                List<Document> answers = std.getList("answers", Document.class);
                Document ans = Utility.searchInDocumentsKeyVal(
                        answers, "question_id", questionId
                );

                if (ans == null)
                    continue;

                if (allMarked && !ans.containsKey("mark"))
                    allMarked = false;

                stdAnswers.put(std.getObjectId("_id"), ans);
            }

            boolean correctWithQR = (boolean) quiz.getOrDefault("is_q_r_needed", false);
            String prefix;
            String folder = db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER;

            if (correctWithQR) {
                prefix = StaticValues.STATICS_SERVER;
                prefix += "/";
            } else
                prefix = StaticValues.STATICS_SERVER + folder + "/studentAnswers/";

            JSONArray data = new JSONArray();

            for (ObjectId std : stdAnswers.keySet()) {

                Document user = userRepository.findById(std);
                if (user == null)
                    continue;

                JSONObject questionObj = new JSONObject(qJSON.toString());
                questionObj.put("mark", qMark);
                Document studentAnswer = stdAnswers.get(std);

                JSONObject studentAnswerObj = new JSONObject()
                        .put("id", studentAnswer.getObjectId("_id").toString())
                        .put("answerAt", studentAnswer.containsKey("answer_at") ?
                                irysc.gachesefid.Utility.Utility.getSolarDate(studentAnswer.getLong("answer_at")) : ""
                        );

                if (studentAnswer.containsKey("mark")) {
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
                questionObj.put("student", new JSONObject()
                        .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                        .put("id", std.toString())
                );

                data.put(questionObj);
            }

            return Utility.generateSuccessMsg("data", new JSONObject()
                    .put("allMarked", allMarked)
                    .put("quizInfo", quizJSON)
                    .put("qIdx", qIdx + 1)
                    .put("answers", data)
            );

        } catch (InvalidFieldsException e) {
            return e.getMessage();
        }

    }

    // defaults: curr: true, future: true, pass: false
    public static String getMyTasks(ObjectId userId, String mode) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (mode != null && !EnumValidatorImp.isValid(mode, GeneralKindQuiz.class))
            return JSON_NOT_VALID_PARAMS;

        filters.add(eq("mode", KindQuiz.TASHRIHI.getName()));
        filters.add(eq("correctors.user_id", userId));

        JSONArray tasks = new JSONArray();

        if (mode == null || mode.equals(GeneralKindQuiz.IRYSC.getName())) {

            ArrayList<Document> docs = iryscQuizRepository.find(and(filters),
                    TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS, Sorts.descending("start")
            );

            for (Document doc : docs) {

                Document corrector = Utility.searchInDocumentsKeyVal(
                        doc.getList("correctors", Document.class),
                        "user_id", userId
                );

                if (corrector == null)
                    continue;

                JSONObject jsonObject = convertTashrihiQuizToJSONDigestForTeachers(doc, corrector, "irysc");
                tasks.put(jsonObject);
            }
        }

        if (mode == null || mode.equals(GeneralKindQuiz.SCHOOL.getName())) {

            ArrayList<Document> docs = schoolQuizRepository.find(and(filters),
                    TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS, Sorts.descending("start")
            );

            for (Document doc : docs) {

                Document corrector = Utility.searchInDocumentsKeyVal(
                        doc.getList("correctors", Document.class),
                        "user_id", userId
                );

                if (corrector == null)
                    continue;

                tasks.put(
                        convertTashrihiQuizToJSONDigestForTeachers(doc, corrector, "school")
                );
            }

        }

        return Utility.generateSuccessMsg("data", tasks);

    }

    public static String setMark(Common db, ObjectId userId,
                                 ObjectId quizId, ObjectId questionId,
                                 ObjectId studentId, JSONObject markData) {
        try {

            PairValue p = hasCorrectorAccess(db, userId, quizId);
            Document quiz = (Document) p.getKey();
            int correctorIdx = (int) p.getValue();

            if (correctorIdx != -1) {

                List<ObjectId> studentIds = null;
                List<ObjectId> questionIds = null;

                Document doc = quiz.getList("correctors", Document.class).get(correctorIdx);

                if (doc.containsKey("students"))
                    studentIds = doc.getList("students", ObjectId.class);

                if (doc.containsKey("questions"))
                    questionIds = doc.getList("questions", ObjectId.class);

                boolean hasAccess = (studentIds != null && studentIds.contains(studentId)) ||
                        (questionIds != null && questionIds.contains(questionId));

                if (!hasAccess)
                    return JSON_NOT_ACCESS;
            }

            if (!DEV_MODE && quiz.containsKey("end") &&
                    quiz.getLong("end") > System.currentTimeMillis()
            )
                return Utility.generateErr("آزمون موردنظر هنوز به اتمام نرسیده است.");

            List<Document> students = quiz.getList("students", Document.class);

            int stdIdx = searchInDocumentsKeyValIdx(students, "_id", studentId);

            if (stdIdx == -1)
                return JSON_NOT_VALID_ID;

            if (!students.get(stdIdx).containsKey("answers"))
                return Utility.generateErr("دانش آموز موردنظر در این آزمون غایب بوده است.");

            List<Document> studentAnswers = students.get(stdIdx).getList("answers", Document.class);

            int answerIdx = searchInDocumentsKeyValIdx(studentAnswers, "question_id", questionId);
            if (answerIdx == -1)
                return JSON_NOT_VALID_ID;

            double mark = markData.getNumber("mark").doubleValue();

            Document questions = quiz.get("questions", Document.class);
            List<ObjectId> ids = questions.getList("_ids", ObjectId.class);

            int qIdx = ids.indexOf(questionId);
            if (qIdx < 0)
                return JSON_NOT_VALID_ID;

            if (questions.getList("marks", Double.class).get(qIdx) < mark)
                return Utility.generateErr("حداکثر نمره مجاز برای این سوال " + questions.getList("marks", Double.class).get(qIdx) + " می باشد.");

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
            else
                students.get(stdIdx).remove("all_marked");

            db.replaceOne(quizId, quiz);
            return JSON_OK;
        } catch (Exception x) {
            return Utility.generateErr(x.getMessage());
        }
    }

    @Override
    List<Document> registry(ObjectId studentId, String phone, String mail,
                            List<ObjectId> quizIds, int paid, ObjectId transactionId, String stdName) {

        ArrayList<Document> added = new ArrayList<>();

        for (ObjectId quizId : quizIds) {

            try {
                Document quiz = iryscQuizRepository.findById(quizId);

                if (quiz == null || !quiz.getString("mode").equalsIgnoreCase("tashrihi"))
                    continue;

                List<Document> students = quiz.getList("students", Document.class);

                if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        students, "_id", studentId
                ) != -1)
                    continue;

                Document stdDoc = new Document("_id", studentId)
                        .append("paid", paid / quizIds.size())
                        .append("register_at", System.currentTimeMillis())
                        .append("finish_at", null)
                        .append("start_at", null)
                        .append("all_marked", false)
                        .append("answers", new ArrayList<>());

                students.add(stdDoc);
                added.add(stdDoc);
                quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);

                iryscQuizRepository.replaceOne(
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

    }

//    @Override
//    String buy(Document student, Document quiz) {
//        return null;
//    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin,
                                boolean afterBuy, boolean isDescNeeded
    ) {
        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("generalMode", AllKindQuiz.IRYSC.getName())
                .put("mode", quiz.getString("mode"))
                .put("tags", quiz.getList("tags", String.class))
                .put("reportStatus", quiz.getOrDefault("report_status", "not_ready"))
                .put("isQRNeeded", quiz.getBoolean("is_q_r_needed"))
                .put("isRegistrable", quiz.getBoolean("is_registrable"))
                .put("id", quiz.getObjectId("_id").toString());

        if (quiz.containsKey("start"))
            jsonObject.put("start", quiz.getLong("start"));

        if (quiz.containsKey("end"))
            jsonObject.put("end", quiz.getLong("end"));

        if (quiz.containsKey("launch_mode"))
            jsonObject.put("launchMode", quiz.getString("launch_mode"));

        boolean isUploadable = quiz.getBoolean("is_uploadable");

        int questionsCount = 0;
        try {
            questionsCount = quiz.get("questions", Document.class)
                    .getList("_ids", ObjectId.class).size();
        } catch (Exception ignore) {
        }

        if (afterBuy) {

            long curr = System.currentTimeMillis();

            if (
                    (quiz.containsKey("end") && quiz.getLong("end") < curr) ||
                            (!quiz.containsKey("end") && quiz.containsKey("report_status"))
            ) {
                boolean canSeeResult = quiz.getBoolean("show_results_after_correction") &&
                        quiz.containsKey("report_status") &&
                        quiz.getString("report_status").equalsIgnoreCase("ready");

                if (canSeeResult)
                    jsonObject.put("status", "finished")
                            .put("questionsCount", questionsCount);
                else
                    jsonObject.put("status", "waitForResult")
                            .put("questionsCount", questionsCount);
            } else if (quiz.getBoolean("is_uploadable") &&
                    quiz.containsKey("start") &&
                    quiz.containsKey("end") &&
                    quiz.getLong("start") <= curr &&
                    quiz.getLong("end") > curr
            ) {
                jsonObject
                        .put("status", "inProgress")
                        .put("duration", calcLen(quiz))
                        .put("questionsCount", questionsCount);
            } else if (quiz.getBoolean("is_uploadable") &&
                    quiz.containsKey("start") &&
                    quiz.getLong("start") > curr
            )
                jsonObject.put("status", "notStart");
            else
                jsonObject.put("status", "wait");

        } else if (quiz.containsKey("start_registry") &&
                quiz.containsKey("price")
        ) {
            jsonObject.put("startRegistry", quiz.getLong("start_registry"))
                    .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                    .put("price", quiz.get("price"));
        }

        if (isAdmin) {
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("questionsCount", questionsCount)
                    .put("priority", quiz.getInteger("priority"))
                    .put("capacity", quiz.getOrDefault("capacity", 10000));
        }

        if (quiz.containsKey("capacity") && !quiz.containsKey("duration"))
            jsonObject.put("reminder", Math.max(quiz.getInteger("capacity") - quiz.getInteger("registered"), 0));

        if (!isDigest || isDescNeeded)
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""));

        if (!isDigest) {
            jsonObject
                    .put("topStudentsCount", quiz.getInteger("top_students_count"))
                    .put("showResultsAfterCorrection", quiz.getBoolean("show_results_after_correction"));

            if (isAdmin) {
                JSONArray attaches = new JSONArray();
                if (quiz.containsKey("attaches")) {
                    for (String attach : quiz.getList("attaches", String.class))
                        attaches.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);
                }

                if (isUploadable) {
                    jsonObject.put("lenMode", quiz.containsKey("duration") ? "custom" : "question");
                    if (quiz.containsKey("duration"))
                        jsonObject.put("len", quiz.getInteger("duration"));

                    jsonObject.put("backEn", quiz.getOrDefault("back_en", false));
                    jsonObject.put("permute", quiz.getOrDefault("permute", false));
                }


                jsonObject.put("descBefore", quiz.getOrDefault("desc", ""));
                jsonObject.put("descAfter", quiz.getOrDefault("desc_after", ""));
                jsonObject.put("attaches", attaches);

                jsonObject.put("showResultsAfterCorrectionNotLoginUsers",
                        quiz.getOrDefault("show_results_after_correction_not_login_users", false)
                );
            }
        }

        return jsonObject;
    }


    static class Taraz {

        private Document quiz;

        private ArrayList<TashrihiQuestionStat> lessonsStat;
        private ArrayList<TashrihiQuestionStat> subjectsStat;
        private List<Document> questionsList;
        private List<ObjectId> questionIds;

        private List<Double> marks;
        private List<Document> students;
        private ArrayList<TashrihiQuestionStat> studentsStat;
        public ArrayList<byte[]> questionStats;
        private ArrayList<Document> studentsData;

        private HashMap<ObjectId, ObjectId> states;
        private HashMap<ObjectId, PairValue> usersCities;

        private HashMap<ObjectId, Integer> cityRanking;
        private HashMap<Object, Integer> stateRanking;

        private HashMap<ObjectId, Integer> citySkip;
        private HashMap<Object, Integer> stateSkip;

        private HashMap<ObjectId, Double> cityOldT;
        private HashMap<Object, Double> stateOldT;

        private ArrayList<Document> rankingList;
        private List<Document> subjectsGeneralStat;
        private List<Document> lessonsGeneralStat;

        HashMap<ObjectId, List<TarazRanking>> lessonsTarazRanking = new HashMap<>();
        HashMap<ObjectId, List<TarazRanking>> subjectsTarazRanking = new HashMap<>();
        HashMap<ObjectId, ObjectId> statesDic = new HashMap<>();

        Taraz(Document quiz, Common db) {

            this.quiz = quiz;
            Document questions = quiz.get("questions", Document.class);
            marks = questions.getList("marks", Double.class);

            students = quiz.getList("students", Document.class);
            questionIds = questions.getList("_ids", ObjectId.class);

            lessonsStat = new ArrayList<>();
            subjectsStat = new ArrayList<>();
            questionsList = new ArrayList<>();
            studentsStat = new ArrayList<>();
            questionStats = new ArrayList<>();
            rankingList = new ArrayList<>();

            states = new HashMap<>();
            usersCities = new HashMap<>();

            cityRanking = new HashMap<>();
            stateRanking = new HashMap<>();

            citySkip = new HashMap<>();
            stateSkip = new HashMap<>();

            cityOldT = new HashMap<>();
            stateOldT = new HashMap<>();
            subjectsGeneralStat = new ArrayList<>();
            lessonsGeneralStat = new ArrayList<>();

            fetchQuestions();
            initStudentStats();

            doCorrectStudents();
            calcSubjectMarkSum();
            calcLessonMarkSum();

            calcSubjectsStandardDeviationAndTaraz();
            calcLessonsStandardDeviationAndTaraz();

            for (TashrihiQuestionStat aStudentsStat : studentsStat)
                aStudentsStat.calculateTotalTaraz();

            studentsStat.sort(TashrihiQuestionStat::compareTo);

            fetchUsersData();
            saveStudentsStats();

            prepareForCityRanking();
            calcCityRanking();

            calcSubjectsStats();
            calcLessonsStats();

            save(db);
        }

        private void fetchQuestions() {

            int k = -1;

            for (ObjectId id : questionIds) {

                Document question = questionRepository.findById(id);
                k++;

                if (question == null)
                    continue;

                Document tmp = Document.parse(question.toJson());
                tmp.put("mark", marks.get(k));

                ObjectId subjectId = question.getObjectId("subject_id");

                boolean isSubjectAdded = false;

                tmp.put("subject_id", subjectId);

                for (TashrihiQuestionStat itr : subjectsStat) {
                    if (itr.equals(subjectId)) {
                        isSubjectAdded = true;
                        tmp.put("lesson_id", itr.additionalId);
                        break;
                    }
                }

                if (!isSubjectAdded) {

                    Document subject = subjectRepository.findById(subjectId);

                    Document lesson = subject.get("lesson", Document.class);
                    ObjectId lessonId = lesson.getObjectId("_id");

                    subjectsStat.add(
                            new TashrihiQuestionStat(
                                    subjectId, subject.getString("name"), lessonId
                            )
                    );

                    tmp.put("lesson_id", lessonId);

                    boolean isLessonAdded = false;

                    for (TashrihiQuestionStat itr : lessonsStat) {
                        if (itr.equals(lessonId)) {
                            isLessonAdded = true;
                            break;
                        }
                    }

                    if (!isLessonAdded)
                        lessonsStat.add(
                                new TashrihiQuestionStat(lessonId, lesson.getString("name"))
                        );
                }

                questionsList.add(tmp);
            }
        }

        private void initStudentStats() {
            for (Document student : students) {
                studentsStat.add(new TashrihiQuestionStat(
                        student.getObjectId("_id"), "",
                        student.getList("answers", Document.class)
                ));
            }
        }

        private void doCorrectStudents() {

            int idx = 0;
            for (Document question : questionsList) {
                for (TashrihiQuestionStat aStudentsStat : studentsStat)
                    aStudentsStat.doCorrect(question, idx);
                idx++;
            }
        }

        private void calcSubjectMarkSum() {
            for (TashrihiQuestionStat itr : subjectsStat) {
                for (TashrihiQuestionStat aStudentsStat : studentsStat) {
                    itr.marks.add(
                            (aStudentsStat.subjectMark.get(itr.id) / aStudentsStat.subjectTotalMark.get(itr.id)) * 100.0
                    );
                }
            }
        }

        private void calcLessonMarkSum() {
            for (TashrihiQuestionStat itr : lessonsStat) {
                for (TashrihiQuestionStat aStudentsStat : studentsStat) {
                    itr.marks.add(
                            (aStudentsStat.lessonMark.get(itr.id) / aStudentsStat.lessonTotalMark.get(itr.id)) * 100.0
                    );
                }
            }
        }

        private void calcSubjectsStandardDeviationAndTaraz() {
            for (TashrihiQuestionStat itr : subjectsStat) {
                itr.calculateSD();
                for (TashrihiQuestionStat aStudentsStat : studentsStat)
                    aStudentsStat.calculateTaraz(
                            itr.mean, itr.sd,
                            itr.id, true
                    );
            }
        }

        private void calcLessonsStandardDeviationAndTaraz() {
            for (TashrihiQuestionStat itr : lessonsStat) {
                itr.calculateSD();
                for (TashrihiQuestionStat aStudentsStat : studentsStat)
                    aStudentsStat.calculateTaraz(
                            itr.mean, itr.sd,
                            itr.id, false
                    );
            }
        }

        private void saveStudentsStats() {

            for (TashrihiQuestionStat aStudentsStat : studentsStat) {

                Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        students, "_id", aStudentsStat.id
                );

                ArrayList<Document> lessonsStats = new ArrayList<>();

                for (TashrihiQuestionStat itr : lessonsStat) {

                    lessonsStats.add(new Document
                            ("stat", aStudentsStat.encode(itr.id, false))
                            .append("name", itr.name)
                            .append("_id", itr.id)
                    );

                }

                student.put("lessons", lessonsStats);

                ArrayList<Document> subjectStats = new ArrayList<>();

                for (TashrihiQuestionStat itr : subjectsStat) {

                    subjectStats.add(new Document
                            ("stat", aStudentsStat.encode(itr.id, true))
                            .append("name", itr.name)
                            .append("_id", itr.id)
                    );

                }

                student.put("subjects", subjectStats);
            }

        }

        private void fetchUsersData() {

            ArrayList<ObjectId> studentIds = new ArrayList<>();

            for (TashrihiQuestionStat itr : studentsStat)
                studentIds.add(itr.id);

            studentsData = userRepository.findByIds(
                    studentIds, true
            );

            initTarazRankingLists();

            for (ObjectId subjectId : subjectsTarazRanking.keySet()) {
                List<TarazRanking> allTarazRanking = subjectsTarazRanking.get(subjectId);
                calcStateRanking(allTarazRanking, true, subjectId);
                calcCountryRanking(allTarazRanking, true, subjectId);
                calcCityRanking(allTarazRanking, true, subjectId);
                calcSchoolRanking(allTarazRanking, true, subjectId);
            }

            for (ObjectId lessonId : lessonsTarazRanking.keySet()) {
                List<TarazRanking> allTarazRanking = lessonsTarazRanking.get(lessonId);
                calcStateRanking(allTarazRanking, false, lessonId);
                calcCountryRanking(allTarazRanking, false, lessonId);
                calcCityRanking(allTarazRanking, false, lessonId);
                calcSchoolRanking(allTarazRanking, false, lessonId);
            }

        }

        private void initTarazRankingLists() {

            int k = 0;
            ObjectId unknownCity = cityRepository.findOne(
                    eq("name", "نامشخص"), null
            ).getObjectId("_id");

            ObjectId iryscSchool = schoolRepository.findOne(
                    eq("name", "آیریسک تهران"), null
            ).getObjectId("_id");

            for (TashrihiQuestionStat itr : studentsStat) {

                ObjectId cityId;
                ObjectId schoolId;

                if (!studentsData.get(k).containsKey("city") ||
                        studentsData.get(k).get("city") == null
                )
                    cityId = unknownCity;
                else
                    cityId = studentsData.get(k).get("city", Document.class).getObjectId("_id");

                if (!studentsData.get(k).containsKey("school") ||
                        studentsData.get(k).get("school") == null
                )
                    schoolId = iryscSchool;
                else
                    schoolId = studentsData.get(k).get("school", Document.class).getObjectId("_id");

                ObjectId stateId;

                if (statesDic.containsKey(cityId))
                    stateId = statesDic.get(cityId);
                else {
                    stateId = cityRepository.findById(cityId).getObjectId("state_id");
                    statesDic.put(cityId, stateId);
                }

                for (ObjectId oId : itr.subjectTaraz.keySet()) {

                    TarazRanking t = new TarazRanking(
                            schoolId, cityId, stateId,
                            itr.subjectTaraz.get(oId)
                    );

                    if (subjectsTarazRanking.containsKey(oId))
                        subjectsTarazRanking.get(oId).add(t);
                    else
                        subjectsTarazRanking.put(oId, new ArrayList<>() {{
                            add(t);
                        }});
                }

                for (ObjectId oId : itr.lessonTaraz.keySet()) {

                    TarazRanking t = new TarazRanking(
                            schoolId, cityId, stateId,
                            itr.lessonTaraz.get(oId)
                    );

                    if (lessonsTarazRanking.containsKey(oId))
                        lessonsTarazRanking.get(oId).add(t);
                    else
                        lessonsTarazRanking.put(oId, new ArrayList<>() {{
                            add(t);
                        }});
                }

                k++;
            }
        }

        private void calcSchoolRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for (TarazRanking t : allTarazRanking) {

                if (t.schoolRank != -1)
                    continue;

                ObjectId wantedSchoolId = t.schoolId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for (TarazRanking ii : allTarazRanking) {
                    if (!ii.schoolId.equals(wantedSchoolId))
                        continue;
                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for (int i = filterSorted.size() - 1; i >= 0; i--) {

                    if (oldTaraz != filterSorted.get(i).taraz) {
                        rank += skip;
                        skip = 1;
                    } else
                        skip++;

                    filterSorted.get(i).schoolRank = rank;
                    oldTaraz = filterSorted.get(i).taraz;
                }
            }

            int k = 0;
            for (TashrihiQuestionStat itr : studentsStat) {
                if (isForSubject)
                    itr.subjectSchoolRanking.put(oId, allTarazRanking.get(k++).schoolRank);
                else
                    itr.lessonSchoolRanking.put(oId, allTarazRanking.get(k++).schoolRank);
            }

        }

        private void calcStateRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for (TarazRanking t : allTarazRanking) {

                if (t.stateRank != -1)
                    continue;

                ObjectId wantedStateId = t.stateId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for (TarazRanking ii : allTarazRanking) {
                    if (!ii.stateId.equals(wantedStateId))
                        continue;
                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for (int i = filterSorted.size() - 1; i >= 0; i--) {

                    if (oldTaraz != filterSorted.get(i).taraz) {
                        rank += skip;
                        skip = 1;
                    } else
                        skip++;

                    filterSorted.get(i).stateRank = rank;
                    oldTaraz = filterSorted.get(i).taraz;
                }
            }

            int k = 0;
            for (TashrihiQuestionStat itr : studentsStat) {
                if (isForSubject)
                    itr.subjectStateRanking.put(oId, allTarazRanking.get(k++).stateRank);
                else
                    itr.lessonStateRanking.put(oId, allTarazRanking.get(k++).stateRank);
            }

        }

        private void calcCityRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for (TarazRanking t : allTarazRanking) {

                if (t.cityRank != -1)
                    continue;

                ObjectId wantedStateId = t.cityId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for (TarazRanking ii : allTarazRanking) {

                    if (!ii.cityId.equals(wantedStateId))
                        continue;

                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for (int i = filterSorted.size() - 1; i >= 0; i--) {

                    if (oldTaraz != filterSorted.get(i).taraz) {
                        rank += skip;
                        skip = 1;
                    } else
                        skip++;

                    filterSorted.get(i).cityRank = rank;
                    oldTaraz = filterSorted.get(i).taraz;
                }
            }

            int k = 0;
            for (TashrihiQuestionStat itr : studentsStat) {
                if (isForSubject)
                    itr.subjectCityRanking.put(oId, allTarazRanking.get(k++).cityRank);
                else
                    itr.lessonCityRanking.put(oId, allTarazRanking.get(k++).cityRank);
            }

        }

        private void calcCountryRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for (TarazRanking t : allTarazRanking) {

                if (t.countryRank != -1)
                    continue;

                List<TarazRanking> filterSorted =
                        allTarazRanking.stream()
                                .sorted(Comparator.comparingInt(t2 -> t2.taraz))
                                .collect(Collectors.toList());

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for (int i = filterSorted.size() - 1; i >= 0; i--) {

                    if (oldTaraz != filterSorted.get(i).taraz) {
                        rank += skip;
                        skip = 1;
                    } else
                        skip++;

                    filterSorted.get(i).countryRank = rank;
                    oldTaraz = filterSorted.get(i).taraz;
                }
            }

            int k = 0;
            for (TashrihiQuestionStat itr : studentsStat) {
                if (isForSubject)
                    itr.subjectCountryRanking.put(oId, allTarazRanking.get(k++).countryRank);
                else
                    itr.lessonCountryRanking.put(oId, allTarazRanking.get(k++).countryRank);
            }

        }

        private void prepareForCityRanking() {

            ObjectId unknownCity = cityRepository.findOne(
                    eq("name", "نامشخص"), null
            ).getObjectId("_id");

            for (Document itr : studentsData) {

                ObjectId cityId = itr.get("city") == null ? unknownCity :
                        itr.get("city", Document.class).getObjectId("_id");
                ObjectId stateId;

                if (states.containsKey(cityId))
                    stateId = states.get(cityId);
                else {
                    Document city = cityRepository.findById(cityId);
                    stateId = city.getObjectId("state_id");
                    states.put(cityId, stateId);
                }

                if (
                        !stateRanking.containsKey(stateId)
                ) {
                    stateRanking.put(stateId, 0);
                    stateOldT.put(stateId, -1.0);
                    stateSkip.put(stateId, 1);
                }

                if (
                        !cityRanking.containsKey(cityId)
                ) {
                    cityRanking.put(cityId, 0);
                    cityOldT.put(cityId, -1.0);
                    citySkip.put(cityId, 1);
                }

                usersCities.put(
                        itr.getObjectId("_id"),
                        new PairValue(cityId, stateId)
                );
            }
        }

        private void calcCityRanking() {

            int rank = 0;
            int skip = 1;
            double oldTaraz = -1;

            for (TashrihiQuestionStat aStudentsStat : studentsStat) {

                PairValue p = usersCities.get(aStudentsStat.id);

                ObjectId stateId = (ObjectId) p.getValue();
                ObjectId cityId = (ObjectId) p.getKey();
                double currTaraz = aStudentsStat.taraz;

                if (oldTaraz != currTaraz) {
                    rank += skip;
                    skip = 1;
                } else
                    skip++;

                if (stateOldT.get(stateId) != currTaraz) {
                    stateRanking.put(stateId, stateRanking.get(stateId) + stateSkip.get(stateId));
                    stateSkip.put(stateId, 1);
                } else
                    stateSkip.put(stateId, stateSkip.get(stateId) + 1);

                if (cityOldT.get(cityId) != currTaraz) {
                    cityRanking.put(cityId, cityRanking.get(cityId) + citySkip.get(cityId));
                    citySkip.put(cityId, 1);
                } else
                    citySkip.put(cityId, citySkip.get(cityId) + 1);

                double stdMark = 0;
                for (Double d : aStudentsStat.marks)
                    stdMark += d;

                rankingList.add(
                        new Document("_id", aStudentsStat.id)
                                .append("stat", encodeFormatGeneralTashrihi(stdMark,
                                        (int) currTaraz, rank, cityRanking.get(cityId),
                                        stateRanking.get(stateId)
                                ))
                );

                oldTaraz = currTaraz;
                stateOldT.put(stateId, currTaraz);
                cityOldT.put(cityId, currTaraz);
            }
        }

        private void calcSubjectsStats() {
            for (TashrihiQuestionStat itr : subjectsStat) {
                subjectsGeneralStat.add(
                        new Document("avg", itr.mean)
                                .append("max", itr.max)
                                .append("min", itr.min)
                                .append("_id", itr.id)
                                .append("name", itr.name)
                );
            }
        }

        private void calcLessonsStats() {
            for (TashrihiQuestionStat itr : lessonsStat) {
                lessonsGeneralStat.add(
                        new Document("avg", itr.mean)
                                .append("max", itr.max)
                                .append("min", itr.min)
                                .append("_id", itr.id)
                                .append("name", itr.name)
                );
            }
        }

        private void save(Common db) {

            quiz.put("ranking_list", rankingList);
            quiz.put("report_status", "ready");
            quiz.put("general_stat",
                    new Document("lessons", lessonsGeneralStat)
                            .append("subjects", subjectsGeneralStat)
            );

            quiz.put("question_stat", questionStats);

            db.replaceOne(
                    quiz.getObjectId("_id"), quiz
            );

        }

    }

    void createTaraz(Document quiz) {
        new TashrihiQuizController.Taraz(quiz, iryscQuizRepository);
    }
}
