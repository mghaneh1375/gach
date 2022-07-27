package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
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

            db.replaceOne(quizId, quiz);
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

            db.replaceOne(quizId, quiz);
            return irysc.gachesefid.Utility.Utility.returnRemoveResponse(excepts, removedIds);

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String get(Common db, ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);
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
            List<Document> questions = quiz.getList("questions", Document.class);

            ArrayList<Document> questionsDoc = new ArrayList<>();

            for (Document itr : questions) {

                Document question = questionRepository.findById(itr.getObjectId("_id"));

                if (question == null)
                    continue;

                questionsDoc.add(Document.parse(question.toJson()).append("mark", itr.get("mark")));
            }

            JSONArray jsonArray = Utilities.convertList(questionsDoc, true, true, true, true);

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
                        quiz.getList("questions", Document.class),
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
            List<Document> questions, String folder
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

            List<Document> questions = quiz.getList("questions", Document.class);

            String filename = FileUtils.uploadTempFile(file);
            ArrayList<Row> rows = Excel.read(filename);
            FileUtils.removeTempFile(filename);

            if (rows == null)
                return generateErr("File is not valid");

            rows.remove(0);

            JSONArray excepts = new JSONArray();
            int rowIdx = 0;

            for (Row row : rows) {

                rowIdx++;

                try {

                    if (row.getLastCellNum() < 2) {
                        excepts.put(rowIdx);
                        continue;
                    }

                    String organizationId = row.getCell(1).getStringCellValue();
                    double mark = row.getCell(2).getNumericCellValue();

                    Document question = questionRepository.findBySecKey(organizationId);

                    if (question == null) {
                        excepts.put(rowIdx);
                        continue;
                    }

                    questions.add(new Document("mark", mark)
                            .append("_id", question.getObjectId("_id"))
                    );

                    int used = (int) question.getOrDefault("used", 0);

                    questionRepository.updateOne(
                            question.getObjectId("_id"),
                            set("used", used + 1)
                    );

                } catch (Exception x) {
                    excepts.put(rowIdx);
                }
            }

            db.replaceOne(quizId, quiz);

            if (excepts.length() == 0)
                return generateSuccessMsg(
                        "excepts", "تمامی سوالات به درستی به آزمون اضافه شدند"
                );

            return generateSuccessMsg(
                    "excepts",
                    "بجز ردیف های زیر سایرین به درستی به آزمون اضافه گردیدند. " + excepts
            );

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

            Document questions = quiz.get("questions", Document.class);
            JSONArray excepts = new JSONArray();
            JSONArray addedItems = new JSONArray();
            ArrayList<Document> questionsArr = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {

                try {

                    String organizationId = jsonArray.getString(i);
                    Document question = questionRepository.findBySecKey(organizationId);

                    if (question == null) {
                        excepts.put(i + 1);
                        continue;
                    }

                    questionsArr.add(question);

                } catch (Exception x) {
                    excepts.put(i + 1);
                }
            }


            Binary types = (Binary) questions.getOrDefault("types", new Binary(new byte[0]));
            Binary answers = (Binary) questions.getOrDefault("answers", new Binary(new byte[0]));

            byte[] allTypes = new byte[types.length() + questionsArr.size()];
            ArrayList<Byte> bytes = new ArrayList<>();

            List<Double> marks = questions.containsKey("marks") ? questions.getList("marks", Double.class) : new ArrayList<>();
            List<ObjectId> ids = questions.containsKey("_ids") ? questions.getList("_ids", ObjectId.class) : new ArrayList<>();
            int idx = 0;

            for(byte b : types.getData())
                allTypes[idx++] = b;

//            idx = 0;
//            for(byte b : answers.getData())
//                allAnswers[idx++] = b;

            for(Document question : questionsArr) {

                ids.add(question.getObjectId("_id"));
                marks.add(mark);
                allTypes[idx] = Utilities.convertTypeToByte(question.getString("kind_question"));
//                Utilities.convertAnswerToByte(allAnswers, idx, question.get("answer"));

                addedItems.put(
                        QuestionController.convertDocToJSON(question)
                                .put("mark", mark)
                );

                int used = (int) question.getOrDefault("used", 0);

                questionRepository.updateOne(
                        question.getObjectId("_id"),
                        set("used", used + 1)
                );

            }

            questions.put("types", allTypes);
            questions.put("marks", marks);
            questions.put("_ids", ids);

            db.replaceOne(quizId, quiz);

            return irysc.gachesefid.Utility.Utility.returnAddResponse(
                    excepts, addedItems
            );
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
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

                List<Document> questions = quiz.getList("questions", Document.class);

                questions.add(new Document("mark", mark)
                        .append("_id", question.getObjectId("_id"))
                );

                db.replaceOne(quizId, quiz);

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
            JSONArray jsonArray = new JSONArray();

            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if(student == null)
                return JSON_NOT_VALID_ID;

            student.put("answers", Utility.getByteArr(answers));
            doc.put("students", students);

            db.replaceOne(quizId, doc);
            return JSON_OK;

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
            JSONArray jsonArray = new JSONArray();

            for (Document student : students) {

                Document user = userRepository.findById(
                        student.getObjectId("_id")
                );

                String answerSheet = (String) student.getOrDefault("answer_sheet", "");
                String answerSheetAfterCorrection = (String) student.getOrDefault("answer_sheet_after_correction", "");

                JSONObject jsonObject = new JSONObject()
                        .put("answers", Utility.getNumbers(student.get("answers", org.bson.types.Binary.class)))
                        .put("answerSheet", answerSheet.isEmpty() ? "" :
                                STATICS_SERVER + "answer_sheets/" + answerSheet)
                        .put("answerSheetAfterCorrection", answerSheetAfterCorrection.isEmpty() ? "" :
                                STATICS_SERVER + "answer_sheets/" + answerSheetAfterCorrection);

                irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("data", jsonArray);

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
            if (doc.getBoolean("is_online") ||
                    !doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName())
            )
                return JSON_NOT_VALID_ID;

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
}
