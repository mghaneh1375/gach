package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Question.QuestionController;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.PDF.PDFUtils;
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
import static irysc.gachesefid.Controllers.Certification.AdminCertification.addUserToCert;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;


public class QuizController {

    public static String getDistinctTags() {
        return generateSuccessMsg("data",
                iryscQuizRepository.distinctTags("tags")
        );
    }

    public static String getAllQuizzesDigest(Boolean isOpenQuizzesNeeded) {

        JSONArray all = new JSONArray();
        ArrayList<Document> quizzes = iryscQuizRepository.find(null, null);

        for(Document quiz : quizzes) {

            JSONObject jsonObject1 = new JSONObject()
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("name", quiz.getString("title") + " در آزمون های آیریسک")
                    .put("mode", "irysc");

            all.put(jsonObject1);
        }

        if(isOpenQuizzesNeeded == null || isOpenQuizzesNeeded) {

            quizzes = openQuizRepository.find(null, null);

            for(Document quiz : quizzes) {

                JSONObject jsonObject1 = new JSONObject()
                        .put("id", quiz.getObjectId("_id").toString())
                        .put("name", quiz.getString("title") + " در آزمون های باز")
                        .put("mode", "open");

                all.put(jsonObject1);
            }

        }

        return generateSuccessMsg("data", all);
    }

    public static String getAllContentQuizzesDigest() {

        JSONArray all = new JSONArray();
        ArrayList<Document> quizzes = contentQuizRepository.find(null, null);

        for(Document quiz : quizzes) {

            JSONObject jsonObject1 = new JSONObject()
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("name", quiz.getString("title"));

            all.put(jsonObject1);
        }

        return generateSuccessMsg("data", all);
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

        if (newDoc.containsKey("mode") && newDoc.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
            newDoc.put("correctors", new ArrayList<>());

        if(newDoc.containsKey("price") && newDoc.get("price") instanceof String) {
            try {
                newDoc.put("price", Integer.parseInt(newDoc.getString("price")));
            }
            catch (Exception x) {
                newDoc.put("price", 0);
            }
        }

        return newDoc;
    }

    public static String update(Common db, ObjectId userId,
                                ObjectId quizId, JSONObject data) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            for (String key : data.keySet())
                quiz.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), data.get(key));

            if (!data.has("tags"))
                quiz.put("tags", new ArrayList<>());

            if(quiz.containsKey("price") && quiz.get("price") instanceof String) {
                try {
                    quiz.put("price", Integer.parseInt(quiz.getString("price")));
                }
                catch (Exception x) {
                    quiz.put("price", 0);
                }
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

    public static String getAll(Common db, ObjectId userId,
                                String name,
                                Long startDateSolar,
                                Long startDateSolarEndLimit,
                                Long startRegistryDateSolar,
                                Long startRegistrySolarEndLimit
    ) {

        ArrayList<Document> docs;
        ArrayList<Bson> filters = new ArrayList<>();
        if(userId != null)
            filters.add(eq("created_by", userId));

        if(name != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE)));

        if(startDateSolar != null)
            filters.add(gte("start", startDateSolar));

        if(startDateSolarEndLimit != null)
            filters.add(lte("start", startDateSolarEndLimit));

        if(startRegistryDateSolar != null)
            filters.add(gte("start_registry", startRegistryDateSolar));

        if(startRegistrySolarEndLimit != null)
            filters.add(lte("start_registry", startRegistrySolarEndLimit));

        docs = db.find(filters.size() == 0 ? null : and(filters),
                userId == null ? QUIZ_DIGEST_MANAGEMENT : QUIZ_DIGEST,
                Sorts.descending("created_at")
        );
        QuizAbstract quizAbstract;

        JSONArray jsonArray = new JSONArray();

        if (db instanceof IRYSCQuizRepository)
            quizAbstract = new RegularQuizController();
        else if (db instanceof ContentQuizRepository)
            quizAbstract = new ContentQuizController();
        else
            quizAbstract = new OpenQuiz();

        for (Document quiz : docs)
            jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, true, false, false));

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

                if (student == null || !student.containsKey("city") ||
                        student.get("city") == null || !student.containsKey("school") ||
                        student.get("school") == null
                ) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId studentId = student.getObjectId("_id");

                if (userId != null && !Authorization.hasAccessToThisStudent(studentId, userId)) {
                    excepts.put(i + 1);
                    continue;
                }

                ArrayList<ObjectId> quizIds = new ArrayList<>();
                quizIds.add(quizId);

                List<Document> added = quizAbstract.registry(
                        student.getObjectId("_id"), student.getString("phone"),
                        student.getString("mail"), quizIds, paid,
                        null, null
                );

                if (added.size() > 0)
                    addedItems.put(convertStudentDocToJSON(added.get(0), student,
                            null, null, null, null)
                    );
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

    public static String get(Common db, Object user,
                             ObjectId quizId
    ) {

        try {

            Document quiz = hasPublicAccess(db, user, quizId);
            QuizAbstract quizAbstract = null;

            if (db instanceof IRYSCQuizRepository)
                quizAbstract = new RegularQuizController();

            else if (db instanceof OpenQuizRepository)
                quizAbstract = new OpenQuiz();

            else if (db instanceof ContentQuizRepository)
                quizAbstract = new ContentQuizController();

            if (quizAbstract != null) {
                return generateSuccessMsg("data",
                        quizAbstract.convertDocToJSON(quiz, false, true, false, false)
                );
            }

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

                questionsList.add(Document.parse(question.toJson()).append("no", i + 1).append("mark", questionsMark.get(i)));
                i++;
            }

            JSONArray jsonArray = Utilities.convertList(questionsList, true, true, true, true, true);

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

                db.deleteOne(quizId);
                db.cleanRemove(quiz);
                removedIds.put(quizId);

            } catch (InvalidFieldsException x) {
                return generateErr(
                        x.getMessage()
                );
            }
        }

        return returnRemoveResponse(excepts, removedIds);

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
                                   MultipartFile file) {

        try {

            if (file == null)
                return JSON_NOT_VALID_PARAMS;

            Document quiz = hasAccess(db, userId, quizId);

            List<String> attaches = quiz.containsKey("attaches") ?
                    quiz.getList("attaches", String.class) : new ArrayList<>();

            if (db instanceof SchoolQuizRepository) {

                Document config = irysc.gachesefid.Utility.Utility.getConfig();
                if (config.getBoolean("school_quiz_attaches_just_link"))
                    return JSON_NOT_ACCESS;

                if (attaches.size() >= config.getInteger("schoolQuizAttachesMax"))
                    return generateErr(
                            "شما می توانید حداکثر " + config.getInteger("schoolQuizAttachesMax") + " پیوست داشته باشید."
                    );
            }

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

            String filename = FileUtils.uploadFile(file, base);
            if (filename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;

            attaches.add(filename);
            quiz.put("attaches", attaches);

            db.replaceOne(quizId, quiz);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "url", STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + filename
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeAttach(Common db,
                                      ObjectId userId,
                                      ObjectId quizId,
                                      String attach) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            List<String> attaches = quiz.containsKey("attaches") ?
                    quiz.getList("attaches", String.class) : new ArrayList<>();

            String[] splited = attach.split("/");

            int idx = attaches.indexOf(splited[splited.length - 1]);
            if(idx < 0)
                return JSON_NOT_VALID_PARAMS;

            FileUtils.removeFile(splited[splited.length - 1], IRYSCQuizRepository.FOLDER);
            attaches.remove(idx);
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
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

            if (doc.containsKey("start") && doc.getLong("start") < current)
                return generateErr("زمان آزمون/تمرین مورد نظر فرارسیده است و امکان ویرایش سوالات وجود ندارد.");

            if (!doc.containsKey("start") && doc.getList("students", Document.class).size() > 0)
                return generateErr("زمان آزمون/تمرین مورد نظر فرارسیده است و امکان ویرایش سوالات وجود ندارد.");

            JSONArray questionIds = jsonObject.getJSONArray("questionIds");
            Document questions = doc.get("questions", Document.class);
            List<ObjectId> questionOIds = questions.getList("_ids", ObjectId.class);

            if (questionOIds.size() != questionIds.length())
                return JSON_NOT_VALID_PARAMS;

            List<Double> questionMarks = questions.getList(
                    "marks", Double.class
            );

            ArrayList<ObjectId> newArrange = new ArrayList<>();
            ArrayList<Double> marks = new ArrayList<>();

            for (int i = 0; i < questionIds.length(); i++) {

                for (int j = i + 1; j < questionIds.length(); j++) {
                    if (questionIds.getString(j).equals(questionIds.getString(i)))
                        return JSON_NOT_VALID_PARAMS;
                }

                if (!ObjectIdValidator.isValid(questionIds.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

                int idx = questionOIds.indexOf(new ObjectId(questionIds.getString(i)));
                if (idx == -1)
                    return JSON_NOT_VALID_PARAMS;

                newArrange.add(questionOIds.get(idx));
                marks.add(questionMarks.get(idx));
            }

            questions.put("marks", marks);
            questions.put("_ids", newArrange);
            questions.put("answers",
                    Utility.getAnswersByteArr(newArrange)
            );
            doc.put("questions", questions);
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

                    if(row.getCell(0) == null)
                        break;

                    if (row.getLastCellNum() < 2) {
                        excepts.put(rowIdx);
                        continue;
                    }

                    jsonArray.put(
                            new JSONObject()
                                    .put("organizationId", Excel.getCellValue(row.getCell(0)).toString())
                                    .put("mark", Double.parseDouble(Excel.getCellValue(row.getCell(1)).toString()))
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
                    addedItems, true, true, true, true, true
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

    public static String finalizeQuizResult(ObjectId quizId) {

        try {
            Document quiz = hasAccess(iryscQuizRepository, null, quizId);

            if(!quiz.containsKey("report_status") ||
                    !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("ابتدا باید جدول تراز آزمون ساخته شود.");

            List<Binary> questionsStat = quiz.getList("question_stat", Binary.class);
            ArrayList<Document> questions = questionRepository.findByIds(
                    quiz.get("questions", Document.class).getList("_ids", ObjectId.class), true
            );

            if(questions == null || questions.size() != questionsStat.size())
                return JSON_NOT_UNKNOWN;

            Utilities.updateQuestionsStat(questions, questionsStat);

            Document config = getConfig();

            if(
                    (config.containsKey("quiz_money") &&
                            config.getInteger("quiz_money") > 0) ||
                    (config.containsKey("quiz_coin") &&
                            config.getDouble("quiz_coin") > 0)

            ) {
                List<Document> rankingList = quiz.getList("ranking_list", Document.class);

                String date = irysc.gachesefid.Utility.Utility.getSolarDate(
                        quiz.getLong("start")
                ).split(" ")[0];
                String quizName = quiz.getString("title");

                if(rankingList.size() > 0)
                    giveQuizGiftToUser(rankingList.get(0).getObjectId("_id"), config, 1,
                            date, quizName
                    );

                if(rankingList.size() > 1)
                    giveQuizGiftToUser(rankingList.get(1).getObjectId("_id"), config, 2,
                            date, quizName
                    );

                if(rankingList.size() > 2)
                    giveQuizGiftToUser(rankingList.get(2).getObjectId("_id"), config, 3,
                            date, quizName
                    );

                if(rankingList.size() > 3)
                    giveQuizGiftToUser(rankingList.get(3).getObjectId("_id"), config, 4,
                            date, quizName
                    );

                if(rankingList.size() > 4)
                    giveQuizGiftToUser(rankingList.get(4).getObjectId("_id"), config, 5,
                            date, quizName
                    );
            }


            return JSON_OK;
        }
        catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    private static void giveQuizGiftToUser(ObjectId userId, Document config,
                                           int rank, String data, String quizName) {

        if(rank > 3 && !config.containsKey("forth_rank_cert_id"))
            return;

        Document user = userRepository.findById(userId);
        if(user == null)
            return;

        if(rank < 4) {
            if (
                    (config.containsKey("quiz_money") &&
                            config.getInteger("quiz_money") > 0)
            )
                user.put("money", ((Number)user.get("money")).doubleValue() + config.getInteger("quiz_money"));

            if (
                    (config.containsKey("quiz_coin") &&
                            config.getDouble("quiz_coin") > 0)
            )
                user.put("coin", user.getDouble("coin") + config.getDouble("quiz_coin"));

            userRepository.replaceOne(
                    userId, user
            );
        }

        JSONArray params = new JSONArray();
        params.put(user.getString("first_name") + " " + user.getString("last_name"));
        params.put(quizName);
        params.put(data);
        params.put(rank + "");

        if(rank == 1 && config.containsKey("first_rank_cert_id"))
            addUserToCert(null, config.getObjectId("first_rank_cert_id"),
                    user.getString("NID"), params);

        if(rank == 2 && config.containsKey("second_rank_cert_id"))
            addUserToCert(null, config.getObjectId("second_rank_cert_id"),
                    user.getString("NID"), params);

        if(rank == 3 && config.containsKey("third_rank_cert_id"))
            addUserToCert(null, config.getObjectId("third_rank_cert_id"),
                    user.getString("NID"), params);

        if(rank == 4 && config.containsKey("forth_rank_cert_id"))
            addUserToCert(null, config.getObjectId("forth_rank_cert_id"),
                    user.getString("NID"), params);

        if(rank == 5 && config.containsKey("fifth_rank_cert_id"))
            addUserToCert(null, config.getObjectId("fifth_rank_cert_id"),
                    user.getString("NID"), params);

    }

    public static String getRegistrable(Common db, boolean isAdmin,
                                        String tag, Boolean finishedIsNeeded) {

        ArrayList<Bson> filters = new ArrayList<>();
        long curr = System.currentTimeMillis();

        if (isAdmin) {
            filters.add(and(
                    gt("start", curr),
                    or(
                            exists("end_registry", false),
                            gt("end_registry", curr)
                    )
            ));

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

            jsonArray.put(quizAbstract.convertDocToJSON(doc, true, isAdmin, false, true));
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("items", jsonArray)
                .put("tags", db.distinctTags("tags"))
        );
    }

    public static String getFinishedQuizzes() {

        ArrayList<Bson> filters = new ArrayList<>();
        long curr = System.currentTimeMillis();

        filters.add(eq("visibility", true));
        filters.add(lt("end", curr));
        filters.add(exists("report_status"));
        filters.add(exists("ranking_list"));
        filters.add(eq("report_status", "ready"));

        ArrayList<Document> docs = iryscQuizRepository.find(and(filters), QUIZ_DIGEST);

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            QuizAbstract quizAbstract;

            if (doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName()))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            JSONObject tmp = quizAbstract.convertDocToJSON(doc, true, false, false, false);
            tmp.remove("price");
            jsonArray.put(tmp);
        }

        return generateSuccessMsg("data", jsonArray);
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

            if (doc.containsKey("question_stat")) {
                questionStat = doc.getList("question_stat", Binary.class);
                if (questionStat.size() != answers.size())
                    questionStat = null;
            }
//            if(answers.size() != marks.size())
//                return JSON_NOT_UNKNOWN;
            fillWithAnswerSheetData(jsonArray, questionStat, answers, marks);
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

            if (quiz.containsKey("end") && quiz.getLong("end") > curr)
                return generateErr("زمان ساخت نتایج هنوز فرانرسیده است.");

            if(db instanceof OpenQuizRepository)
                new RegularQuizController.Taraz(quiz, openQuizRepository);
            else {
                new RegularQuizController().createTaraz(quiz);

                new Thread(() -> {

                    ArrayList<ObjectId> userIds = new ArrayList<>();

                    for(Document doc : quiz.getList("students", Document.class))
                        userIds.add(doc.getObjectId("_id"));

                    List<Document> students = userRepository.findByIds(userIds, false);

                    String prefix = quiz.getString("title") + "_" + SERVER + "result/irysc/" + quiz.getObjectId("_id").toString() + "/";

                    for(Document student : students) {

                        if(!student.containsKey("mail"))
                            continue;

                        mailQueueRepository.insertOne(
                                new Document("created_at", System.currentTimeMillis())
                                        .append("status", "pending")
                                        .append("mail", student.getString("mail"))
                                        .append("name", student.getString("first_name") + " " + student.getString("last_name"))
                                        .append("mode", "karname")
                                        .append("msg", prefix + student.getObjectId("_id").toString())
                        );
                    }

                }).start();
            }

            return JSON_OK;
        } catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
            return JSON_NOT_ACCESS;
        }
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

            return saveStudentAnswers(doc, answers, student, db);

        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String resetStudentQuizEntryTime(Common db, ObjectId userId,
                                                   ObjectId quizId, ObjectId studentId) {
        try {
            Document quiz = hasAccess(db, userId, quizId);

            Document student = searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class),
                    "_id", studentId
            );

            if(student == null)
                return JSON_NOT_VALID_ID;

            student.put("start_at", null);
            student.put("finish_at", null);

            db.replaceOne(quizId, quiz);
            return JSON_OK;
        }
        catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeQuestions(Common db, ObjectId quizId, JSONArray jsonArray) {

        Document quiz = db.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        if(db instanceof IRYSCQuizRepository) {

            if(quiz.getLong("start") < System.currentTimeMillis())
                return generateErr("زمان آزمون موردنظر رسیده است و امکان حذف سوال از آزمون وجود ندارد.");

        }
        else if(db instanceof OpenQuizRepository) {

            if(quiz.getList("students", Document.class).size() > 0)
                return generateErr("دانش آموز/دانش آموزانی در این آزمون شرکت کرده اند و امکان حذف سوال وجود ندارد.");

        }

        JSONArray removeIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        Document questions = quiz.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);
        List<Double> marks = questions.getList("marks", Double.class);
        List<ObjectId> removed = new ArrayList<>();

        for(int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if(!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId qId = new ObjectId(id);
            if(!questionIds.contains(qId)) {
                excepts.put(i + 1);
                continue;
            }

            removeIds.put(qId);
            removed.add(qId);

            if (db instanceof IRYSCQuizRepository) {

                Document question = questionRepository.findById(qId);

                if (question == null)
                    continue;

                int used = (int) question.getOrDefault("used", 0);

                questionRepository.updateOne(
                        question.getObjectId("_id"),
                        set("used", used - 1)
                );
            }

        }

        if(removeIds.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        List<ObjectId> newQuestionsIds = new ArrayList<>();
        List<Double> newMarks = new ArrayList<>();

        int idx = 0;

        for(ObjectId qId : questionIds) {

            if(removed.contains(qId)) {
                idx++;
                continue;
            }

            newQuestionsIds.add(qId);
            newMarks.add(marks.get(idx));
            idx++;
        }

        questions.put("marks", newMarks);
        questions.put("_ids", newQuestionsIds);
        questions.put("answers",
                Utility.getAnswersByteArr(newQuestionsIds)
        );

        quiz.put("questions", questions);
        db.replaceOne(quizId, quiz);


        return irysc.gachesefid.Utility.Utility.returnRemoveResponse(
                excepts, removeIds
        );
    }

}
