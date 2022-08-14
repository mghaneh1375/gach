package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Question.QuestionController;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.PDF.PDFUtils;
import irysc.gachesefid.Validator.LinkValidator;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Quiz.Utility.getAnswers;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;


public class QuizController {

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
            if(user != null && !quiz.getBoolean("visibility"))
                throw new InvalidFieldsException(JSON_NOT_ACCESS);
            return quiz;
        }

        if(user.toString().isEmpty())
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        ObjectId userId = (ObjectId) user;

        if (quiz.getObjectId("created_by").equals(userId))
            return quiz;

        if(!quiz.getBoolean("visibility"))
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

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
        if (quiz == null || !quiz.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        int idx = -1;

        if (userId != null && !quiz.getObjectId("created_by").equals(userId)) {

            List<Document> correctors = quiz.getList("correctors", Document.class);
            idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    correctors, "_id", userId);

            if (idx == -1)
                throw new InvalidFieldsException(JSON_NOT_VALID_ID);
        }

        return new PairValue(quiz, idx);
    }


    // ##################### PACKAGE ###################

    public static String createPackage(JSONObject jsonObject) {

        ObjectId gradeId = new ObjectId(jsonObject.getString("gradeId"));

        Document grade = gradeRepository.findById(gradeId);

        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        ObjectId lessonId = new ObjectId(jsonObject.getString("lessonId"));

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                grade.getList("lessons", Document.class),
                "_id", lessonId
        ) == -1)
            return JSON_NOT_VALID_PARAMS;

        Document newDoc = new Document("title", jsonObject.getString("title"))
                .append("off_percent", jsonObject.getInt("offPercent"))
                .append("min_select", jsonObject.getInt("minSelect"))
                .append("description", jsonObject.has("description") ? jsonObject.getString("description") : "")
                .append("quizzes", new ArrayList<>())
                .append("lesson_id", lessonId)
                .append("grade_id", gradeId)
                .append("buyers", 0)
                .append("expire_at", System.currentTimeMillis());

        return packageRepository.insertOneWithReturn(newDoc);
    }

    public static String editPackage(ObjectId packageId, JSONObject jsonObject) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        if (jsonObject.has("lessonId") && jsonObject.has("gradeId")) {

            ObjectId gradeId = new ObjectId(jsonObject.getString("gradeId"));

            Document grade = gradeRepository.findById(gradeId);

            if (grade == null)
                return JSON_NOT_VALID_PARAMS;

            ObjectId lessonId = new ObjectId(jsonObject.getString("lessonId"));

            if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    grade.getList("lessons", Document.class),
                    "_id", lessonId
            ) == -1)
                return JSON_NOT_VALID_PARAMS;

            packageDoc.put("lesson_id", lessonId);
            packageDoc.put("grade_id", gradeId);

        }

        if (jsonObject.has("title"))
            packageDoc.put("title", jsonObject.getString("title"));

        if (jsonObject.has("offPercent"))
            packageDoc.put("off_percent", jsonObject.getInt("offPercent"));

        if (jsonObject.has("minSelect"))
            packageDoc.put("min_select", jsonObject.getInt("minSelect"));

        if (jsonObject.has("description"))
            packageDoc.put("description", jsonObject.getString("description"));

        packageRepository.replaceOne(packageId, packageDoc);
        return JSON_OK;
    }

    public static String addQuizzesToPackage(ObjectId packageId, JSONArray jsonArray) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id))
                continue;

            ObjectId quizId = new ObjectId(id);

            Document quiz = iryscQuizRepository.findById(quizId);
            if (quiz == null)
                continue;

            long endRegistry = quiz.containsKey("end_registry") ?
                    quiz.getLong("end_registry") : quiz.getLong("end");

            if (endRegistry < System.currentTimeMillis())
                continue;
//                return generateErr("زمان ثبت نام آزمون موردنظر به اتمام رسیده است.");

            if (quizzes.contains(quizId))
                continue;
//                return JSON_OK;

            if (endRegistry > packageDoc.getLong("expire_at"))
                packageDoc.put("expire_at", endRegistry);

            quizzes.add(quizId);
        }

        packageDoc.put("quizzes", quizzes);
        packageRepository.replaceOne(packageId, packageDoc);

        return getPackageQuizzes(packageId, true);
    }

    public static String removeQuizzesFromPackage(ObjectId packageId, JSONArray jsonArray) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id))
                continue;

            ObjectId quizId = new ObjectId(id);

            Document quiz = iryscQuizRepository.findById(quizId);
            if (quiz == null)
                continue;

            if (!quizzes.contains(quizId))
                continue;

            long max = 13000000;

            for (ObjectId qId : quizzes) {

                if (qId.equals(quizId))
                    continue;

                Document q = iryscQuizRepository.findById(qId);

                long e = q.containsKey("end_registry") ?
                        q.getLong("end_registry") :
                        q.getLong("end");

                if (e > max)
                    max = e;
            }

            packageDoc.put("expire_at", max);
            quizzes.remove(quizId);
        }

        packageDoc.put("quizzes", quizzes);
        packageRepository.replaceOne(packageId, packageDoc);
        return getPackageQuizzes(packageId, true);
    }

    public static String getPackages(boolean isAdmin, ObjectId gradeId, ObjectId lessonId) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (!isAdmin)
            filters.add(gt("expire_at", System.currentTimeMillis()));

        if (gradeId != null)
            filters.add(eq("grade_id", gradeId));

        if (lessonId != null)
            filters.add(eq("lesson_id", lessonId));

        ArrayList<Document> packages = packageRepository.find(
                filters.size() == 0 ? null : and(filters), null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document packageDoc : packages) {

            Document grade = gradeRepository.findById(packageDoc.getObjectId("grade_id"));
            if (grade == null)
                continue;

            Document lesson = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    grade.getList("lessons", Document.class),
                    "_id", packageDoc.getObjectId("lesson_id")
            );

            jsonArray.put(new JSONObject()
                    .put("id", packageDoc.getObjectId("_id").toString())
                    .put("title", packageDoc.getString("title"))
                    .put("description", packageDoc.getOrDefault("description", ""))
                    .put("buyers", isAdmin ? packageDoc.getInteger("buyers") : 0)
                    .put("quizzes", packageDoc.getList("quizzes", ObjectId.class).size())
                    .put("grade", new JSONObject()
                            .put("id", grade.getObjectId("_id").toString())
                            .put("name", grade.getString("name"))
                    )
                    .put("lesson", new JSONObject()
                            .put("id", lesson.getObjectId("_id").toString())
                            .put("name", lesson.getString("name"))
                    )
                    .put("offPercent", packageDoc.getInteger("off_percent"))
                    .put("minSelect", packageDoc.getInteger("min_select"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getPackageQuizzes(ObjectId packageId, boolean isAdmin) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();
        for (ObjectId quizId : packageDoc.getList("quizzes", ObjectId.class)) {

            Document quiz = iryscQuizRepository.findById(quizId);

            if (quiz == null)
                continue;

            QuizAbstract quizAbstract;

            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, isAdmin));
        }

        return generateSuccessMsg("data", jsonArray);
    }


    // ##################### END PACKAGE ###################


    public static String getDistinctTags() {

        DistinctIterable<String> cursor = iryscQuizRepository.distinctTags();
        JSONArray jsonArray = new JSONArray();
        for (String itr : cursor)
            jsonArray.put(itr);

        return generateSuccessMsg("data", jsonArray);
    }

    public static Document store(ObjectId userId, JSONObject data
    ) throws InvalidFieldsException {

        Document newDoc = new Document();

        for (String key : data.keySet()) {
            if (key.equalsIgnoreCase("tags"))
                continue;
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        ArrayList<String> tagsArr = new ArrayList<>();

        if (data.has("tags")) {

            JSONArray tags = data.getJSONArray("tags");

            for (int i = 0; i < tags.length(); i++)
                tagsArr.add(tags.getString(i));
        }

        Utility.isValid(newDoc);

//        if (!newDoc.containsKey("desc_after_mode") ||
//                newDoc.getString("desc_after_mode").equals(DescMode.FILE.getName())
//        )
//            newDoc.put("desc_after_mode", DescMode.NONE.getName());
//
//        if (!newDoc.containsKey("desc_mode") ||
//                newDoc.getString("desc_mode").equals(DescMode.FILE.getName())
//        )
//            newDoc.put("desc_mode", DescMode.NONE.getName());

        newDoc.put("visibility", true);
        newDoc.put("students", new ArrayList<>());
        newDoc.put("registered", 0);
        newDoc.put("removed_questions", new ArrayList<>());
        newDoc.put("tags", tagsArr);
        newDoc.put("attaches", new ArrayList<>());
        newDoc.put("created_by", userId);
        newDoc.put("created_at", System.currentTimeMillis());

        newDoc.put("questions", new Document());

//        //todo: consider other modes
//        if (newDoc.getString("mode").equals(KindQuiz.REGULAR.getName()) ||
//                newDoc.getString("mode").equals(KindQuiz.OPEN.getName())
//        )

        if (newDoc.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
            newDoc.put("correctors", new ArrayList<>());

        return newDoc;
    }

    public static String update(Common db, ObjectId userId,
                                ObjectId quizId, JSONObject data) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            for (String key : data.keySet()) {
                quiz.put(key, data.get(key));
            }

            db.replaceOne(quizId, quiz);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        return JSON_OK;
    }

    public static String toggleVisibility(Common db, ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            quiz.put("visibility", quiz.getBoolean("visibility"));
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String getAll(Common db, ObjectId userId) {

        ArrayList<Document> docs;

        if (userId != null)
            docs = db.find(eq("created_by", userId), QUIZ_DIGEST, Sorts.descending("created_at"));
        else
            docs = db.find(null, QUIZ_DIGEST_MANAGEMENT, Sorts.descending("created_at"));

        QuizAbstract quizAbstract;

        JSONArray jsonArray = new JSONArray();

        for (Document quiz : docs) {
            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, true));
        }

        return generateSuccessMsg("data", jsonArray);

    }

    public static String forceRegistry(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray,
                                       int paid) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            JSONArray excepts = new JSONArray();
            JSONArray addedItems = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {

                String NID = jsonArray.getString(i);

                if (!irysc.gachesefid.Utility.Utility.validationNationalCode(NID)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document student = userRepository.findBySecKey(NID);

                if (student == null) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId studentId = student.getObjectId("_id");

                if (userId != null && !Authorization.hasAccessToThisStudent(studentId, userId)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document stdDoc = quizAbstract.registry(student, quiz, paid);
                if (stdDoc != null)
                    addedItems.put(convertStudentDocToJSON(stdDoc, student,
                            null, null, null, null)
                    );
            }

            if (addedItems.length() > 0) {
                quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + addedItems.length());
                db.replaceOne(quizId, quiz);
            }
            return irysc.gachesefid.Utility.Utility.returnAddResponse(excepts, addedItems);

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String forceDeportation(Common db, ObjectId userId,
                                          ObjectId quizId, JSONArray jsonArray) {


        try {

            Document quiz = hasAccess(db, userId, quizId);
            JSONArray excepts = new JSONArray();
            JSONArray removedIds = new JSONArray();

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            for (int i = 0; i < jsonArray.length(); i++) {

                String id = jsonArray.getString(i);
                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId studentId = new ObjectId(id);
                Document student = userRepository.findById(studentId);

                if (student == null) {
                    excepts.put(i + 1);
                    continue;
                }

                quizAbstract.quit(student, quiz);
                removedIds.put(studentId.toString());
            }

            if (removedIds.length() > 0) {
                quiz.put("registered", Math.max(0, (int) quiz.getOrDefault("registered", 0) - removedIds.length()));
                db.replaceOne(quizId, quiz);
            }

            db.replaceOne(quizId, quiz);
            return irysc.gachesefid.Utility.Utility.returnRemoveResponse(excepts, removedIds);

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String get(Common db, Object user, ObjectId quizId) {

        try {

            Document quiz = hasPublicAccess(db, user, quizId);
            QuizAbstract quizAbstract = null;

            if (quiz.getString("mode").equals(KindQuiz.REGULAR.getName()))
                quizAbstract = new RegularQuizController();

            if (quizAbstract != null)
                return generateSuccessMsg("data",
                        quizAbstract.convertDocToJSON(quiz, false, true)
                );

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String fetchQuestions(Common db, ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);
            Document questionsDoc = quiz.get("questions", Document.class);

            System.out.println(questionsDoc);
            ArrayList<Document> questionsList = new ArrayList<>();
            List<ObjectId> questions = (List<ObjectId>) questionsDoc.getOrDefault(
                    "_ids", new ArrayList<ObjectId>()
            );
            List<Double> questionsMark = (List<Double>) questionsDoc.getOrDefault(
                    "marks", new ArrayList<Double>()
            );

            if (questionsMark.size() != questions.size())
                return JSON_NOT_UNKNOWN;

            int i = 0;
            for (ObjectId itr : questions) {

                Document question = questionRepository.findById(itr);

                if (question == null) {
                    i++;
                    continue;
                }

                questionsList.add(Document.parse(question.toJson()).append("mark", questionsMark.get(i)));
                i++;
            }

            JSONArray jsonArray = Utilities.convertList(questionsList, true, true, true, true);

            return generateSuccessMsg("data", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    public static String remove(Common db, ObjectId userId, JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        JSONArray removedIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId quizId = new ObjectId(id);

            try {

                Document quiz = hasAccess(db, userId, quizId);

                if (quiz.getList("students", Document.class).size() > 0) {
                    excepts.put("مورد " + (i + 1) + " " + "دانش آموز/دانش آموزانی در این آزمون شرکت کرده اند و امکان حذف آن وجود ندارد.");
                    continue;
                }


                if (quiz.getLong("start") >= System.currentTimeMillis()) {
                    excepts.put("مورد " + (i + 1) + " " + "زمان آزمون فرارسیده و امکان حذف آن وجود ندارد.");
                    continue;
                }

                db.deleteOne(quizId);
                db.cleanRemove(quiz);
                removedIds.put(quizId);

            } catch (InvalidFieldsException x) {
                return generateErr(
                        x.getMessage()
                );
            }
        }

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی آزمون ها به درستی حذف شدند",
                    new PairValue("removedIds", removedIds)
            );

        return generateSuccessMsg(
                "excepts",
                "بجز موارد زیر سایرین به درستی حذف گردیدند. " + excepts,
                new PairValue("removedIds", removedIds)
        );

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

                jsonArray.put(convertStudentDocToJSON(student, user,
                        isResultsNeeded, isStudentAnswersNeeded,
                        quiz.get("questions", Document.class),
                        db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                ));
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    private static JSONObject convertStudentDocToJSON(
            Document student, Document user,
            Boolean isResultsNeeded, Boolean isStudentAnswersNeeded,
            Document questions, String folder
    ) {

        JSONObject jsonObject = new JSONObject()
                .put("paid", student.get("paid"))
                .put("id", user.getObjectId("_id").toString())
                .put("registerAt", getSolarDate(student.getLong("register_at")));

        irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);

        if (jsonObject.has("start_at")) {
            jsonObject.put("startAt", student.containsKey("start_at") ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("start_at")) :
                    ""
            ).put("finishAt", student.containsKey("finish_at") ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("finish_at")) :
                    ""
            );
        }

        if (student.containsKey("all_marked"))
            jsonObject.put("allMarked", student.getBoolean("all_marked"));

        if (isResultsNeeded != null && isResultsNeeded)
            jsonObject.put("totalMark", student.getOrDefault("total_mark", ""));

        if (isStudentAnswersNeeded != null && isStudentAnswersNeeded) {

            if (!student.containsKey("answers"))
                jsonObject.put("answers", new JSONArray());

            else {
                jsonObject.put("answers", Utility.getQuestions(
                        true, false,
                        questions, student.getList("answers", Document.class),
                        folder
                ));
            }
        }

        return jsonObject;
    }

    public static String addAttach(Common db,
                                   ObjectId userId,
                                   ObjectId quizId,
                                   MultipartFile file,
                                   String title,
                                   String link) {

        try {

            if (file == null && link == null)
                return JSON_NOT_VALID_PARAMS;

            Document quiz = hasAccess(db, userId, quizId);

            List<Document> attaches = quiz.getList("attaches", Document.class);

            if (db instanceof SchoolQuizRepository) {

                Document config = irysc.gachesefid.Utility.Utility.getConfig();
                if (config.getBoolean("school_quiz_attaches_just_link") && file != null)
                    return JSON_NOT_ACCESS;

                if (attaches.size() >= config.getInteger("schoolQuizAttachesMax"))
                    return generateErr(
                            "شما می توانید حداکثر " + config.getInteger("schoolQuizAttachesMax") + " پیوست داشته باشید."
                    );
            }

            if (link != null && !LinkValidator.isValid(link))
                return generateErr(
                        "لینک موردنظر نامعتبر است."
                );

            ObjectId id = new ObjectId();

            Document doc = new Document("title", title)
                    .append("is_external_link", link != null)
                    .append("_id", id);

            if (link != null)
                doc.put("link", link);
            else {

                String base = db instanceof SchoolQuizRepository ?
                        SchoolQuizRepository.FOLDER :
                        IRYSCQuizRepository.FOLDER;

                if (db instanceof SchoolQuizRepository &&
                        file.getSize() > MAX_QUIZ_ATTACH_SIZE)
                    return generateErr(
                            "حداکثر حجم مجاز، " + MAX_QUIZ_ATTACH_SIZE + " مگ است."
                    );

                String fileType = uploadPdfOrMultimediaFile(file);
                if (fileType == null)
                    return generateErr(
                            "فرمت فایل موردنظر معتبر نمی باشد."
                    );

                String filename = FileUtils.uploadFile(file, base + "/attaches");
                if (filename == null)
                    return JSON_UNKNOWN_UPLOAD_FILE;

                doc.put("link", filename);
            }

            attaches.add(doc);
            db.replaceOne(quizId, quiz);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "id", id.toString()
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeAttach(Common db,
                                      ObjectId userId,
                                      ObjectId quizId,
                                      ObjectId attachId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            List<Document> attaches = quiz.getList("attaches", Document.class);
            Document doc = searchInDocumentsKeyVal(
                    attaches, "_id", attachId
            );

            if (doc == null)
                return JSON_NOT_VALID_ID;

            if (!doc.getBoolean("is_external_link")) {

                String base = db instanceof SchoolQuizRepository ?
                        SchoolQuizRepository.FOLDER :
                        IRYSCQuizRepository.FOLDER;

                FileUtils.removeFile(doc.getString("link"), base + "/attaches");

            }

            attaches.remove(doc);
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String extend(Common db, ObjectId userId,
                                ObjectId quizId,
                                Long start, Long end) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (start != null && end != null && start >= end)
                return JSON_NOT_VALID_PARAMS;

            long curr = System.currentTimeMillis();

            if (!DEV_MODE && (
                    (start != null && start < curr) ||
                            (end != null && end < curr)
            ))
                return generateErr(
                        "زمان ها باید از اکنون بزرگ تر باشند."
                );

            long endAt = quiz.getLong("end");

            if (!DEV_MODE && endAt < curr)
                return generateErr("زمان آزمون/تمرین موردنظر به پایان رسیده است.");

            long startAt = quiz.getLong("start");

            if (!DEV_MODE && startAt < curr && start != null)
                return generateErr("به دلیل شروع شدن آزمون/تمرین، شما نمی توانید زمان شروع را تغییر دهید.");

            startAt = start != null ? start : startAt;
            endAt = end != null ? end : endAt;

            if (quiz.containsKey("duration")) {

                long diff = (endAt - startAt) / 1000;
                int duration = quiz.getInteger("duration") * 60;

                if (duration > diff)
                    return generateErr("فاصله بین زمان شروع آزمون/تمرین و پایان آن باید حداقل " + (int) Math.ceil(duration / 60.0) + " دقیقه باشد.");
            }

            if (end != null)
                quiz.put("end", end);

            if (start != null)
                quiz.put("start", start);

            db.replaceOne(quizId, quiz);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(
                    e.getMessage()
            );
        }
    }

    public static String arrangeQuestions(Common db, ObjectId userId,
                                          ObjectId quizId, JSONObject jsonObject
    ) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            if (doc.containsKey("permute") &&
                    doc.getBoolean("permute")) {
                return generateErr("آزمون/تمرین موردنظر دارای ویژگی بر زدن است و این ویژگی برای این آزمون/تمرین بکار نمی رود.");
            }

            long current = System.currentTimeMillis();

            if (doc.getLong("start") < current)
                return generateErr("زمان آزمون/تمرین مورد نظر فرارسیده است و امکان ویرایش سوالات وجود ندارد.");

            JSONArray questionIds = jsonObject.getJSONArray("questionIds");
            List<Document> questions = doc.getList("questions", Document.class);
            int totalQuestions = questions.size();

            if (totalQuestions != questionIds.length())
                return JSON_NOT_VALID_PARAMS;

            ArrayList<Document> newArrange = new ArrayList<>();

            for (int i = 0; i < questionIds.length(); i++) {

                for (int j = i + 1; j < questionIds.length(); j++) {
                    if (questionIds.getString(j).equals(questionIds.getString(i)))
                        return JSON_NOT_VALID_PARAMS;
                }

                if (!ObjectIdValidator.isValid(questionIds.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

                Document question = searchInDocumentsKeyVal(questions, "_id",
                        new ObjectId(questionIds.getString(i)));

                if (question == null)
                    return JSON_NOT_VALID_PARAMS;

                newArrange.add(question);
            }

            doc.put("questions", newArrange);

            db.replaceOne(quizId, doc);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static File generateQuestionPDF(Common db, ObjectId userId,
                                           ObjectId quizId) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            ArrayList<String> files = new ArrayList<>();
            List<Document> questions = doc.getList("questions", Document.class);
            for (Document question : questions) {

                Document questionDoc = questionRepository.findById(question.getObjectId("_id"));
                if (questionDoc == null)
                    continue;

                files.add(DEV_MODE ? uploadDir_dev + QuestionRepository.FOLDER + "/" + questionDoc.getString("question_file") :
                        uploadDir + QuestionRepository.FOLDER + "/" + questionDoc.getString("question_file"));
            }

            return PDFUtils.createExam(files);

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }


    public static String addBatchQuestionsToQuiz(Common db, ObjectId userId,
                                                 ObjectId quizId, MultipartFile file) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            //todo: check edit access
            //todo: check school access to questions

            String filename = FileUtils.uploadTempFile(file);
            ArrayList<Row> rows = Excel.read(filename);
            FileUtils.removeTempFile(filename);

            if (rows == null)
                return generateErr("File is not valid");

            rows.remove(0);

            JSONArray excepts = new JSONArray();
            JSONArray jsonArray = new JSONArray();

            int rowIdx = 0;

            for (Row row : rows) {

                rowIdx++;

                try {

                    if (row.getLastCellNum() < 2) {
                        excepts.put(rowIdx);
                        continue;
                    }

                    jsonArray.put(
                            new JSONObject()
                                    .put("organizationId", row.getCell(1).getStringCellValue())
                                    .put("mark", row.getCell(2).getNumericCellValue())
                    );

                } catch (Exception x) {
                    excepts.put(rowIdx);
                }
            }

            return doAddQuestionsToQuiz(db, quiz, jsonArray, excepts, 3);

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    public static String addBatchQuestionsToQuiz(Common db, ObjectId userId,
                                                 ObjectId quizId, JSONArray jsonArray,
                                                 double mark) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            //todo: check edit access
            //todo: check school access to questions
            return doAddQuestionsToQuiz(db, quiz, jsonArray,
                    new JSONArray(), mark
            );

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    private static String doAddQuestionsToQuiz(Common db, Document quiz,
                                               Object questionsList,
                                               JSONArray excepts,
                                               double mark) {

        ArrayList<Document> addedItems = new ArrayList<>();

        Document questions = quiz.get("questions", Document.class);

        List<Double> marks = questions.containsKey("marks") ? questions.getList("marks", Double.class) : new ArrayList<>();
        List<ObjectId> ids = questions.containsKey("_ids") ? questions.getList("_ids", ObjectId.class) : new ArrayList<>();

        if (questionsList instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) questionsList;

            for (int i = 0; i < jsonArray.length(); i++) {

                try {

                    double tmpMark = mark;
                    String organizationId;

                    if (jsonArray.get(i) instanceof JSONObject) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        organizationId = jsonObject.getString("organizationId");
                        if (jsonObject.has("mark"))
                            tmpMark = jsonObject.getDouble("mark");
                    } else
                        organizationId = jsonArray.getString(i);

                    Document question = questionRepository.findBySecKey(organizationId);

                    if (question == null) {
                        excepts.put(i + 1);
                        continue;
                    }

                    int used = (int) question.getOrDefault("used", 0);

                    questionRepository.updateOne(
                            question.getObjectId("_id"),
                            set("used", used + 1)
                    );

                    ids.add(question.getObjectId("_id"));
                    marks.add(tmpMark);

                    addedItems.add(
                            Document.parse(question.toJson()).append("mark", mark)
                    );

                } catch (Exception x) {
                    excepts.put(i + 1);
                }
            }

            questions.put("marks", marks);
            questions.put("_ids", ids);
            questions.put("answers",
                    Utility.getAnswersByteArr(ids)
            );
            quiz.put("questions", questions);

            db.replaceOne(quiz.getObjectId("_id"), quiz);

            PairValue p = new PairValue("doneIds", Utilities.convertList(
                    addedItems, true, true, true, true
            ));

            if (excepts.length() == 0)
                return generateSuccessMsg(
                        "excepts", "تمامی موارد به درستی اضافه گردیدند",
                        p
                );

            return generateSuccessMsg(
                    "excepts",
                    "بجز موارد زیر سایرین به درستی اضافه گردیدند." + excepts,
                    p
            );
        }

        Document question = (Document) questionsList;

        ids.add(question.getObjectId("_id"));
        marks.add(mark);

        byte[] answersByte;

        if (questions.containsKey("answers"))
            answersByte = questions.get("answers", Binary.class).getData();
        else
            answersByte = new byte[0];

        questions.put("answers",
                Utility.addAnswerToByteArr(answersByte, question.getString("kind_question"),
                        question.getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()) ?
                                new PairValue(question.getInteger("choices_count"), question.get("answer")) :
                                question.get("answer")
                )
        );

        questions.put("marks", marks);
        questions.put("_ids", ids);

        db.replaceOne(quiz.getObjectId("_id"), quiz);
        return "ok";
    }

    public static String addQuestionToQuizzes(String organizationCode, Common db,
                                              ObjectId userId, JSONArray jsonArray,
                                              double mark) {

        Document question = questionRepository.findBySecKey(organizationCode);
        if (question == null)
            return JSON_NOT_VALID_ID;

        JSONArray excepts = new JSONArray();
        JSONArray addedItems = new JSONArray();
        int used = (int) question.getOrDefault("used", 0);

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId quizId = new ObjectId(id);
            try {
                Document quiz = hasAccess(db, userId, quizId);
                doAddQuestionsToQuiz(db, quiz,
                        question, null, mark
                );
                addedItems.put(
                        QuestionController.convertDocToJSON(question)
                                .put("mark", mark)
                );
                used++;
            } catch (Exception x) {
                excepts.put(i + 1);
            }
        }

        if (addedItems.length() > 0)
            questionRepository.updateOne(
                    question.getObjectId("_id"),
                    set("used", used)
            );

        return irysc.gachesefid.Utility.Utility.returnAddResponse(
                excepts, addedItems
        );
    }


    public static String updateQuestionMark(Common db, ObjectId userId,
                                            ObjectId quizId, ObjectId questionId,
                                            Number mark) {

        try {
            Document quiz = hasAccess(db, userId, quizId);
            List<Document> questions = quiz.getList("questions", Document.class);

            Document question = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    questions, "_id", questionId
            );

            if (question == null)
                return JSON_NOT_VALID_ID;

            question.put("mark", mark.doubleValue());
            quiz.put("questions", questions);

            db.replaceOne(quizId, quiz);
            return JSON_OK;
        } catch (Exception x) {
            return JSON_NOT_ACCESS;
        }
    }

    public static String getRegistrable(Common db, boolean isAdmin,
                                        String tag, Boolean finishedIsNeeded) {

        ArrayList<Bson> filters = new ArrayList<>();
        long curr = System.currentTimeMillis();

        if (isAdmin) {
            filters.add(gt("start", curr));
        } else {
            filters.add(eq("visibility", true));

            if (finishedIsNeeded == null || !finishedIsNeeded)
                filters.add(gt("end", curr));
            else
                filters.add(gt("start_registry", curr));

            filters.add(
                    or(
                            exists("end_registry", false),
                            gt("end_registry", curr)
                    )
            );
        }

        if (tag != null)
            filters.add(regex("tag", Pattern.compile(Pattern.quote(tag), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> docs = db.find(and(filters), isAdmin ? QUIZ_DIGEST_MANAGEMENT : QUIZ_DIGEST);
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            QuizAbstract quizAbstract;

            if (doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName()))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            jsonArray.put(quizAbstract.convertDocToJSON(doc, true, isAdmin));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String storeAnswers(Common db, ObjectId userId,
                                      ObjectId quizId, ObjectId studentId,
                                      JSONArray answers) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            List<Document> students = doc.getList("students", Document.class);

            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (student == null)
                return JSON_NOT_VALID_ID;

            Document questions = doc.get("questions", Document.class);
            ArrayList<PairValue> pairValues = Utility.getAnswers(
                    ((Binary) questions.getOrDefault("answers", new byte[0])).getData()
            );

            if (pairValues.size() != answers.length())
                return JSON_NOT_VALID_PARAMS;

            int idx = -1;
            ArrayList<PairValue> stdAnswers = new ArrayList<>();

            try {
                for (PairValue p : pairValues) {

                    idx++;
                    String stdAns = answers.get(idx).toString();
                    Object stdAnsAfterFilter;

                    if (stdAns.isEmpty()) {
                        stdAnswers.add(new PairValue(p.getKey(), null));
                        continue;
                    }

                    String type = p.getKey().toString();
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

                        String ans = p.getValue().toString();

                        if (ans.length() != stdAns.length())
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
                x.printStackTrace();
                return JSON_NOT_VALID_PARAMS;
            }

            student.put("answers", Utility.getStdAnswersByteArr(stdAnswers));
            doc.put("students", students);

            db.replaceOne(quizId, doc);
            return JSON_OK;

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String getQuizAnswerSheet(Common db, ObjectId userId,
                                            ObjectId quizId) {
        try {
            Document doc = hasAccess(db, userId, quizId);
            JSONArray jsonArray = new JSONArray();

            Document questions = doc.get("questions", Document.class);
            ArrayList<PairValue> answers = questions.containsKey("answers") ?
                    getAnswers(questions.get("answers", Binary.class).getData()) :
                    new ArrayList<>();

            List<Double> marks = questions.containsKey("marks") ? questions.getList("marks", Double.class) : new ArrayList<>();

            List<Binary> questionStat = null;

            if(doc.containsKey("question_stat")) {
                questionStat = doc.getList("question_stat", Binary.class);
                if(questionStat.size() != answers.size())
                    questionStat = null;
            }
//            if(answers.size() != marks.size())
//                return JSON_NOT_UNKNOWN;

            for (int i = 0; i < marks.size(); i++) {

                int percent = -1;

                if(questionStat != null) {
                    byte[] bytes = questionStat.get(i).getData();
                    percent = (bytes[1] & 0xff) / ((bytes[1] & 0xff) + (bytes[0] & 0xff) + (bytes[2] & 0xff));
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

                if(choicesCount != -1)
                    jsonObject.put("choicesCount", choicesCount);

                if(percent != -1)
                    jsonObject.put("percent", percent);

                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("data", jsonArray);

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String getQuizAnswerSheets(Common db, ObjectId userId,
                                             ObjectId quizId) {
        try {
            Document doc = hasAccess(db, userId, quizId);

//            if(doc.getBoolean("is_online") ||
//                    !doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName())
//            )
//                return JSON_NOT_VALID_ID;

            List<Document> students = doc.getList("students", Document.class);
            JSONObject jsonObject = new JSONObject();

            JSONArray answersJsonArray = new JSONArray();

            Document questions = doc.get("questions", Document.class);
            List<Double> marks = questions.getList("marks", Double.class);
            ArrayList<PairValue> pairValues = Utility.getAnswers(((Binary) questions.getOrDefault("answers", new byte[0])).getData());

            for (int i = 0; i < pairValues.size(); i++) {

                if (pairValues.get(i).getKey().toString().equalsIgnoreCase(QuestionType.TEST.getName())) {
                    PairValue p = (PairValue) pairValues.get(i).getValue();
                    answersJsonArray.put(new JSONObject()
                            .put("type", pairValues.get(i).getKey())
                            .put("choicesCount", p.getKey())
                            .put("answer", p.getValue())
                            .put("mark", marks.get(i))
                    );
                } else
                    answersJsonArray.put(new JSONObject()
                            .put("type", pairValues.get(i).getKey())
                            .put("answer", pairValues.get(i).getValue())
                            .put("mark", marks.get(i))
                    );
            }

            jsonObject.put("answers", answersJsonArray);

            JSONArray jsonArray = new JSONArray();

            for (Document student : students) {

                Document user = userRepository.findById(
                        student.getObjectId("_id")
                );

                String answerSheet = (String) student.getOrDefault("answer_sheet", "");
                String answerSheetAfterCorrection = (String) student.getOrDefault("answer_sheet_after_correction", "");

                ArrayList<PairValue> stdAnswers = Utility.getAnswers(((Binary) student.getOrDefault("answers", new byte[0])).getData());

                JSONArray stdAnswersJSON = new JSONArray();

                for (int i = 0; i < pairValues.size(); i++) {

                    if (i >= stdAnswers.size())
                        stdAnswersJSON.put("");
                    else {
                        if (pairValues.get(i).getKey().toString().equalsIgnoreCase(QuestionType.TEST.getName()))
                            stdAnswersJSON.put(((PairValue) stdAnswers.get(i).getValue()).getValue());
                        else
                            stdAnswersJSON.put(stdAnswers.get(i).getValue());
                    }
                }

                JSONObject tmp = new JSONObject()
                        .put("answers", stdAnswersJSON)
                        .put("answerSheet", answerSheet.isEmpty() ? "" :
                                STATICS_SERVER + "answer_sheets/" + answerSheet)
                        .put("answerSheetAfterCorrection", answerSheetAfterCorrection.isEmpty() ? "" :
                                STATICS_SERVER + "answer_sheets/" + answerSheetAfterCorrection);

                irysc.gachesefid.Utility.Utility.fillJSONWithUser(tmp, user);
                jsonArray.put(tmp);
            }

            jsonObject.put("students", jsonArray);
            return generateSuccessMsg("data", jsonObject);

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String setQuizAnswerSheet(Common db, ObjectId userId,
                                            ObjectId quizId, ObjectId studentId,
                                            MultipartFile file) {
        try {
            Document doc = hasAccess(db, userId, quizId);
            //todo
//            if (doc.getBoolean("is_online") ||
//                    !doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName())
//            )
//                return JSON_NOT_VALID_ID;

            List<Document> students = doc.getList("students", Document.class);
            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (student == null)
                return JSON_NOT_VALID_ID;

            //todo
            String filename = FileUtils.uploadFile(file, "answer_sheets");
            if (filename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;

            student.put("answer_sheet", filename);
            db.replaceOne(quizId, doc);

            return generateSuccessMsg("file", STATICS_SERVER + "answer_sheets/" + filename);

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String createTaraz(Common db, ObjectId userId,
                                     ObjectId quizId) {
        try {

            Document quiz = hasAccess(db, userId, quizId);
            long curr = System.currentTimeMillis();

            if (quiz.getLong("end") > curr)
                return generateErr("زمان ساخت نتایج هنوز فرانرسیده است.");

            new RegularQuizController().createTaraz(quiz);

            return JSON_OK;
        } catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
            return JSON_NOT_ACCESS;
        }
    }

    public static String getRanking(Common db, boolean isAdmin,
                                    ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (
                    !quiz.containsKey("report_status") ||
                            !quiz.containsKey("ranking_list") ||
                            !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            if (!isAdmin &&
                    !quiz.getBoolean("show_results_after_correction"))
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            JSONArray jsonArray = new JSONArray();

            ArrayList<ObjectId> userIds = new ArrayList<>();

            for (Document doc : quiz.getList("ranking_list", Document.class))
                userIds.add(doc.getObjectId("_id"));

            ArrayList<Document> studentsInfo = userRepository.findByIds(
                    userIds, true
            );

            HashMap<ObjectId, String> stateNames = new HashMap<>();
            int k = 0;

            for (Document doc : quiz.getList("ranking_list", Document.class)) {

                ObjectId cityId = studentsInfo.get(k).get("city", Document.class).getObjectId("_id");
                Object[] stat = QuizAbstract.decodeFormatGeneral(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("name", studentsInfo.get(k).getString("first_name") + " " + studentsInfo.get(k).getString("last_name"))
                        .put("taraz", stat[0])
                        .put("cityRank", stat[3])
                        .put("stateRank", stat[2])
                        .put("rank", stat[1]);

                if (stateNames.containsKey(cityId))
                    jsonObject.put("state", stateNames.get(cityId));
                else {
                    Document city = cityRepository.findById(cityId);
                    Document state = stateRepository.findById(city.getObjectId("state_id"));
                    stateNames.put(cityId, state.getString("name"));
                    jsonObject.put("state", stateNames.get(cityId));
                }

                jsonObject.put("city", studentsInfo.get(k).get("city", Document.class).getString("name"));
                jsonObject.put("school", studentsInfo.get(k).get("school", Document.class).getString("name"));

                jsonArray.put(jsonObject);
                k++;
            }

            return generateSuccessMsg(
                    "data", jsonArray
            );

        } catch (InvalidFieldsException e) {
            return JSON_NOT_ACCESS;
        }


    }

    public static String getStudentStat(Common db, Object user,
                                        ObjectId quizId, ObjectId studentId) {

        try {

            Document studentDoc = userRepository.findById(studentId);

            if (studentDoc == null)
                return JSON_NOT_VALID_ID;

            Document quiz = hasPublicAccess(db, user, quizId);

//            if (
//                    !quiz.containsKey("report_status") ||
//                            !quiz.containsKey("ranking_list") ||
//                            !quiz.getString("report_status").equalsIgnoreCase("ready")
//            )
//                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");
//
//            if(!isAdmin &&
//                    !quiz.getBoolean("show_results_after_correction"))
//                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            List<Document> students = quiz.getList("students", Document.class);

            Document student = searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (student == null)
                return JSON_NOT_VALID_ID;

            Document studentGeneralStat = searchInDocumentsKeyVal(
                    quiz.getList("ranking_list", Document.class),
                    "_id", studentId
            );

            if (studentGeneralStat == null)
                return JSON_NOT_UNKNOWN;

            DecimalFormat df_obj = new DecimalFormat("#.##");

            JSONObject data = new JSONObject();
            JSONArray lessons = new JSONArray();

            Document generalStats = quiz.get("general_stat", Document.class);

            List<Document> subjectsGeneralStats = generalStats.getList("subjects", Document.class);
            List<Document> lessonsGeneralStats = generalStats.getList("lessons", Document.class);


            for (Document doc : student.getList("lessons", Document.class)) {

                Document generalStat = searchInDocumentsKeyVal(
                        lessonsGeneralStats, "_id", doc.getObjectId("_id")
                );

                Object[] stats = QuizAbstract.decode(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("name", doc.getString("name"))
                        .put("taraz", stats[0])
                        .put("whites", stats[1])
                        .put("corrects", stats[2])
                        .put("incorrects", stats[3])
                        .put("total", (int) stats[1] + (int) stats[2] + (int) stats[3])
                        .put("percent", stats[4])
                        .put("avg", df_obj.format(generalStat.getDouble("avg")))
                        .put("max", df_obj.format(generalStat.getDouble("max")))
                        .put("min", df_obj.format(generalStat.getDouble("min")));

                lessons.put(jsonObject);
            }

            JSONArray subjects = new JSONArray();

            for (Document doc : student.getList("subjects", Document.class)) {

                Document generalStat = searchInDocumentsKeyVal(
                        subjectsGeneralStats, "_id", doc.getObjectId("_id")
                );

                Object[] stats = QuizAbstract.decode(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("name", doc.getString("name"))
                        .put("taraz", stats[0])
                        .put("whites", stats[1])
                        .put("corrects", stats[2])
                        .put("incorrects", stats[3])
                        .put("percent", stats[4])
                        .put("total", (int) stats[1] + (int) stats[2] + (int) stats[3])
                        .put("avg", df_obj.format(generalStat.getDouble("avg")))
                        .put("max", df_obj.format(generalStat.getDouble("max")))
                        .put("min", df_obj.format(generalStat.getDouble("min")));

                subjects.put(jsonObject);
            }

            data.put("lessons", lessons);
            data.put("subjects", subjects);

            Object[] stat = QuizAbstract.decodeFormatGeneral(studentGeneralStat.get("stat", Binary.class).getData());

            JSONObject jsonObject = new JSONObject()
                    .put("taraz", stat[0])
                    .put("cityRank", stat[3])
                    .put("stateRank", stat[2])
                    .put("rank", stat[1]);

            data.put("rank", jsonObject);

            irysc.gachesefid.Utility.Utility.fillJSONWithUser(data, studentDoc);

            return generateSuccessMsg(
                    "data", data
            );

        } catch (InvalidFieldsException e) {
            return JSON_NOT_ACCESS;
        }


    }

    //todo : other accesses
    public static String getStateReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> stateTaraz = new HashMap<>();
        HashMap<ObjectId, ObjectId> citiesState = new HashMap<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for(Document itr : rankingList) {
            studentIds.add(itr.getObjectId("_id"));
        }

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for(Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            ObjectId cityId = studentsInfo.get(k)
                    .get("city", Document.class).getObjectId("_id");

            ObjectId stateId;

            if(citiesState.containsKey(cityId))
                stateId = citiesState.get(cityId);
            else {
                Document city = cityRepository.findById(cityId);
                stateId = city.getObjectId("state_id");
                citiesState.put(cityId, stateId);
            }

            ArrayList<Integer> tmp;
            if(stateTaraz.containsKey(stateId))
                tmp = stateTaraz.get(stateId);
            else
                tmp = new ArrayList<>();

            tmp.add((Integer) stats[0]);
            stateTaraz.put(stateId, tmp);
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for(ObjectId stateId : stateTaraz.keySet()) {

            Document state = stateRepository.findById(stateId);
            ArrayList<Integer> allTaraz = stateTaraz.get(stateId);

            int sum = 0;
            for(int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", state.getString("name"))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for(int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getCityReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> cityTaraz = new HashMap<>();
        HashMap<ObjectId, String> cities = new HashMap<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for(Document itr : rankingList)
            studentIds.add(itr.getObjectId("_id"));

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for(Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            Document city = studentsInfo.get(k)
                    .get("city", Document.class);

            ObjectId cityId = city.getObjectId("_id");

            if(!cities.containsKey(cityId))
                cities.put(cityId, city.getString("name"));

            ArrayList<Integer> tmp;
            if(cityTaraz.containsKey(cityId))
                tmp = cityTaraz.get(cityId);
            else
                tmp = new ArrayList<>();

            tmp.add((Integer) stats[0]);
            cityTaraz.put(cityId, tmp);
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for(ObjectId cityId : cityTaraz.keySet()) {

            ArrayList<Integer> allTaraz = cityTaraz.get(cityId);

            int sum = 0;
            for(int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", cities.get(cityId))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for(int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getSchoolReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> schoolTaraz = new HashMap<>();
        HashMap<ObjectId, String> schools = new HashMap<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for(Document itr : rankingList) {
            studentIds.add(itr.getObjectId("_id"));
        }

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for(Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            Document school = studentsInfo.get(k)
                    .get("school", Document.class);

            ObjectId schoolId = school.getObjectId("_id");

            if(!schools.containsKey(schoolId))
                schools.put(schoolId, school.getString("name"));

            ArrayList<Integer> tmp;
            if(schoolTaraz.containsKey(schoolId))
                tmp = schoolTaraz.get(schoolId);
            else
                tmp = new ArrayList<>();

            tmp.add((Integer) stats[0]);
            schoolTaraz.put(schoolId, tmp);
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for(ObjectId schoolId : schoolTaraz.keySet()) {

            ArrayList<Integer> allTaraz = schoolTaraz.get(schoolId);

            int sum = 0;
            for(int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", schools.get(schoolId))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for(int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getGenderReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();

        ArrayList<Integer> maleTaraz = new ArrayList<>();
        ArrayList<Integer> femaleTaraz = new ArrayList<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for(Document itr : rankingList)
            studentIds.add(itr.getObjectId("_id"));

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for(Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());

            if(studentsInfo.get(k).getString("sex").equalsIgnoreCase("male"))
                maleTaraz.add((int)stats[0]);
            else
                femaleTaraz.add((int)stats[0]);

            k++;
        }

        List<JSONObject> list = new ArrayList<>();

        int sum = 0;
        for(int itr : maleTaraz)
            sum += itr;

        list.add(new JSONObject()
                .put("label", "آقا")
                .put("count", maleTaraz.size())
                .put("avg", maleTaraz.size() == 0 ? 0 : sum / maleTaraz.size())
        );

        sum = 0;
        for(int itr : femaleTaraz)
            sum += itr;

        list.add(new JSONObject()
                .put("label", "خانم")
                .put("count", femaleTaraz.size())
                .put("avg", femaleTaraz.size() == 0 ? 0 : sum / femaleTaraz.size())
        );

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for(int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getAuthorReport(ObjectId quizId) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();

        HashMap<String, ArrayList<Integer>> authorPercents = new HashMap<>();

        List<ObjectId> questionIds = quiz.get("questions", Document.class).getList("_ids", ObjectId.class);

        ArrayList<Document> questions = questionRepository.findByIds(questionIds, true);

        if(questions == null)
            return JSON_NOT_UNKNOWN;

        List<Binary> questionStats = quiz.getList("question_stat", Binary.class);

        if(questionStats.size() != questions.size())
            return JSON_NOT_UNKNOWN;

        int k = 0;

        for(Document itr : questions) {

            String author = itr.getString("author");
            byte[] stats = questionStats.get(k).getData();
            int percent = (stats[1] * 100) / (stats[0] + stats[1] + stats[2]);

            ArrayList<Integer> tmp;
            if(authorPercents.containsKey(author))
                tmp = authorPercents.get(author);
            else
                tmp = new ArrayList<>();

            tmp.add(percent);
            authorPercents.put(author, tmp);
            k++;
        }

        for(String author : authorPercents.keySet()) {

            ArrayList<Integer> allPercent = authorPercents.get(author);

            int sum = 0;
            for(int itr : allPercent) {
                sum += itr;
            }

            data.put(new JSONObject()
                    .put("label", author)
                    .put("count", allPercent.size())
                    .put("avg", sum / allPercent.size())
            );
        }

        return generateSuccessMsg(
                "data", data
        );

    }

}
