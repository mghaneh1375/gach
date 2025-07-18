package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Point.PointController;
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
import irysc.gachesefid.Validator.EnumValidatorImp;
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
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Certification.AdminCertification.addUserToCert;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.FileUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;


public class QuizController {

    public static String setPDFQuizAnswerSheet(Common db, ObjectId quizId,
                                               ObjectId userId, JSONArray answersJSON
    ) {

        try {
            Document doc = hasAccess(db, userId, quizId);

            if (!doc.containsKey("q_no"))
                return generateErr("ابتدا باید تعداد سوالات را وارد نمایید");

            if (doc.getInteger("q_no") != answersJSON.length())
                return generateErr("تعداد پاسخ ها معتبر نمی باشد.");

            List<Integer> answers = new ArrayList<>();

            for (int i = 0; i < answersJSON.length(); i++)
                answers.add(answersJSON.getInt(i));

            Document questions = doc.get("questions", Document.class);
            questions.put("answers", answers);

            db.replaceOne(quizId, doc);
            return JSON_OK;

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String getPDFQuizAnswerSheet(Common db, ObjectId quizId, ObjectId userId) {

        try {
            Document doc = hasAccess(db, userId, quizId);

            JSONArray jsonArray = new JSONArray();

            Document questions = doc.get("questions", Document.class);
            List<Integer> answers = questions.containsKey("answers") ?
                    questions.getList("answers", Integer.class) :
                    new ArrayList<>();

            if (answers.size() == 0 && !doc.containsKey("q_no"))
                return generateErr("ابتدا باید تعداد سوالات را وارد نمایید");

            List<Number> marks = questions.containsKey("marks") ? questions.getList("marks", Number.class) : new ArrayList<>();

            if (answers.size() == 0) {
                for (int i = 0; i < doc.getInteger("q_no"); i++) {
                    answers.add(0);
                    marks.add(3);
                }
            } else {
                for (int i = 0; i < doc.getInteger("q_no"); i++) {
                    marks.add(3);
                }
            }

            List<Binary> questionStat = null;

            if (doc.containsKey("question_stat")) {
                questionStat = doc.getList("question_stat", Binary.class);
                if (questionStat.size() != answers.size())
                    questionStat = null;
            }

            List<Integer> choicesCounts = questions.getList("choices_counts", Integer.class);

            fillWithAnswerSheetDataPDFQuiz(jsonArray, questionStat, answers, marks, choicesCounts);
            return generateSuccessMsg("data", jsonArray);

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String getPDFQuizQuestions(Common db, ObjectId quizId, ObjectId userId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد تنها برای آزمون\u200Cهای PDF ای قابل فراحوانی می باشد");

            String base = db instanceof SchoolQuizRepository ?
                    SchoolQuizRepository.FOLDER : IRYSCQuizRepository.FOLDER;

            return generateSuccessMsg("data", new JSONObject()
                    .put("file", quiz.containsKey("question_file") ? STATICS_SERVER + base + "/" + quiz.getString("question_file") : "")
                    .put("qNo", quiz.getOrDefault("q_no", 0))
            );

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

    public static String getPDFQuizSubjects(Common db, ObjectId quizId, ObjectId userId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد تنها برای آزمون\u200Cهای PDF ای قابل فراحوانی می باشد");

            String base = db instanceof SchoolQuizRepository ?
                    SchoolQuizRepository.FOLDER : IRYSCQuizRepository.FOLDER;

            Document questions = quiz.get("questions", Document.class);
            List<ObjectId> subjectIds = questions.containsKey("subjects") ?
                    questions.getList("subjects", ObjectId.class) : new ArrayList<>();

            JSONObject data = new JSONObject();

            if (subjectIds.size() > 0) {
                data.put("subjects", subjectRepository.findByIds(subjectIds, true)
                        .stream().map(x -> x.getString("name")).collect(Collectors.toList()));
                data.put("choicesCounts", questions.getList("choices_counts", Integer.class));
            } else {
                data.put("subjects", new ArrayList<>());
                data.put("choicesCount", new ArrayList<>());
            }

            return generateSuccessMsg("data", data);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String setPDFQuizQuestions(Common db, ObjectId quizId, int qNo,
                                             MultipartFile file, ObjectId userId
    ) {

        String base = null;

        if (file != null) {

            base = db instanceof SchoolQuizRepository ?
                    SchoolQuizRepository.FOLDER : IRYSCQuizRepository.FOLDER;

            if (db instanceof SchoolQuizRepository &&
                    file.getSize() > MAX_FILE_SIZE)
                return generateErr(
                        "حداکثر حجم مجاز، " + MAX_FILE_SIZE + " مگ است."
                );

            String fileType = uploadPdfFile(file);
            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد برای این نوع از آزمون قابل فراخوانی نیست");

            if (quiz.getLong("start") < System.currentTimeMillis())
                return generateErr("آزمون موردنظر شروع شده است");

            if (base != null) {

                String filename = FileUtils.uploadFile(file, base);
                if (filename == null)
                    return JSON_UNKNOWN_UPLOAD_FILE;

                quiz.put("question_file", filename);

            }

            quiz.put("q_no", qNo);
            db.replaceOne(quizId, quiz);

            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String setPDFQuizInfo(Common db, ObjectId quizId,
                                        ObjectId userId, JSONArray jsonArray
    ) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد برای این نوع از آزمون قابل فراخوانی نیست");

            if (quiz.getLong("start") < System.currentTimeMillis())
                return generateErr("آزمون موردنظر شروع شده است");

            int qNo = quiz.getInteger("q_no", 0);
            if (qNo == 0)
                return generateErr("لطفا ابتدا تعداد سوالات را وارد نمایید");

            if (jsonArray.length() != qNo)
                return generateErr("تعداد رکوردها با تعداد سوالات برابر نمی باشد");

            List<ObjectId> distinctSubjectIds = new ArrayList<>();
            List<ObjectId> subjectIds = new ArrayList<>();
            List<Integer> choicesCounts = new ArrayList<>();
            List<Integer> answers = new ArrayList<>();
            List<Integer> marks = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {

                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (!jsonObject.has("mark") ||
                            !jsonObject.has("choicesCount") ||
                            !jsonObject.has("ans") ||
                            !jsonObject.has("subject")
                    )
                        return generateErr("اطلاعات وارد شده نامعتبر است");

                    String sId = jsonObject.getString("subject");
                    if (!ObjectId.isValid(sId))
                        return generateErr("اطلاعات وارد شده نامعتبر است");

                    if (jsonObject.getInt("choicesCount") > 7 || jsonObject.getInt("choicesCount") < 2)
                        return generateErr("اطلاعات وارد شده نامعتبر است");

                    if (jsonObject.getInt("ans") < 1 ||
                            jsonObject.getInt("choicesCount") < jsonObject.getInt("ans")
                    )
                        return generateErr("اطلاعات وارد شده نامعتبر است");

                    if (jsonObject.getInt("mark") < 1)
                        return generateErr("اطلاعات وارد شده نامعتبر است");

                    ObjectId oId = new ObjectId(sId);

                    if (!distinctSubjectIds.contains(oId))
                        distinctSubjectIds.add(oId);

                    subjectIds.add(oId);

                    answers.add(jsonObject.getInt("ans"));
                    marks.add(jsonObject.getInt("mark"));
                    choicesCounts.add(jsonObject.getInt("choicesCount"));
                } catch (Exception x) {
                    return generateErr("اطلاعات وارد شده نامعتبر است");
                }

            }

            if (subjectRepository.findByIds(distinctSubjectIds, true) == null)
                return generateErr("اطلاعات وارد شده نامعتبر است");

            Document questions = quiz.get("questions", Document.class);
            questions.put("subjects", subjectIds);
            questions.put("choices_counts", choicesCounts);
            questions.put("answers", answers);
            questions.put("marks", marks);

            db.replaceOne(quizId, quiz);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    private static void fillGradeJSON(JSONArray jsonArray, Document doc,
                                      List<ObjectId> selectedGrades,
                                      List<ObjectId> selectedLessons,
                                      boolean isOlympiad,
                                      HashMap<ObjectId, JSONArray> subjectsInLesson) {

        JSONObject grade = new JSONObject()
                .put("id", doc.getObjectId("_id").toString())
                .put("item", doc.getString("name"))
                .put("isOlympiad", isOlympiad);

        if (selectedGrades.contains(doc.getObjectId("_id"))) {

            JSONArray lessonsJSON = new JSONArray();
            List<Document> lessons = doc.getList("lessons", Document.class);

            if (lessons != null) {

                for (Document lesson : lessons) {

                    ObjectId lId = lesson.getObjectId("_id");

                    JSONObject lessonJSON = new JSONObject()
                            .put("item", lesson.getString("name"))
                            .put("id", lId.toString());

                    if (selectedLessons.contains(lesson.getObjectId("_id"))) {

                        if (subjectsInLesson.containsKey(lId))
                            lessonJSON.put("subjects", subjectsInLesson.get(lId));
                        else {
                            ArrayList<Document> subjects = subjectRepository.find(
                                    eq("lesson._id", lId), null
                            );

                            JSONArray subjectsJSON = new JSONArray();

                            for (Document subject : subjects) {
                                subjectsJSON.put(
                                        new JSONObject()
                                                .put("item", subject.getString("name"))
                                                .put("id", subject.getObjectId("_id").toString())
                                );
                            }

                            subjectsInLesson.put(lId, subjectsJSON);
                            lessonJSON.put("subjects", subjectsJSON);
                        }
                    }

                    lessonsJSON.put(lessonJSON);
                }

                grade.put("lessons", lessonsJSON);
            }
        }

        jsonArray.put(grade);

    }

    public static String getGradesAndBranches(Common db, ObjectId quizId, ObjectId userId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            Document questions = quiz.get("questions", Document.class);

            List<ObjectId> subjectIds = questions.containsKey("subjects") ?
                    questions.getList("subjects", ObjectId.class) : new ArrayList<>();

            List<ObjectId> selectedGrades = new ArrayList<>();
            List<ObjectId> selectedLessons = new ArrayList<>();

            if (subjectIds.size() > 0) {
                List<Document> subjects = subjectRepository.findByIds(subjectIds, true);
                if (subjects != null) {
                    selectedGrades.addAll(subjects.stream()
                            .map(document -> document.get("grade", Document.class).getObjectId("_id"))
                            .collect(Collectors.toList())
                    );
                    selectedLessons.addAll(subjects.stream()
                            .map(document -> document.get("lesson", Document.class).getObjectId("_id"))
                            .collect(Collectors.toList())
                    );
                }
            }

            JSONArray jsonArray = new JSONArray();

            ArrayList<Document> docs = gradeRepository.find(null, null);
            HashMap<ObjectId, JSONArray> subjectsInLesson = new HashMap<>();

            for (Document doc : docs)
                fillGradeJSON(jsonArray, doc, selectedGrades, selectedLessons, false, subjectsInLesson);

            docs = branchRepository.find(null, null);
            for (Document doc : docs)
                fillGradeJSON(jsonArray, doc, selectedGrades, selectedLessons, true, subjectsInLesson);

            return generateSuccessMsg("data", jsonArray);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getPDFQuizInfo(Common db, ObjectId quizId, ObjectId userId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد تنها برای آزمون\u200Cهای PDF ای قابل فراحوانی می باشد");

            Document questions = quiz.get("questions", Document.class);

            List<ObjectId> subjectIds = questions.containsKey("subjects") ?
                    questions.getList("subjects", ObjectId.class) : new ArrayList<>();

            List<Integer> answers = questions.containsKey("answers") ?
                    questions.getList("answers", Integer.class) : new ArrayList<>();

            List<Integer> choicesCounts = questions.containsKey("choices_counts") ?
                    questions.getList("choices_counts", Integer.class) : new ArrayList<>();

            List<Integer> marks = questions.containsKey("marks") ?
                    questions.getList("marks", Integer.class) : new ArrayList<>();

            JSONObject data = new JSONObject();
            List<Document> subjects = subjectRepository.findByIds(subjectIds, true);

            if (subjectIds.size() > 0) {
                data.put("subjects",
                        subjects.stream().map(x -> x.getObjectId("_id").toString()).collect(Collectors.toList())
                );
                data.put("grades", subjects.stream().map(x -> x.get("grade", Document.class).getObjectId("_id").toString()).collect(Collectors.toList()));
                data.put("lessons", subjects.stream().map(x -> x.get("lesson", Document.class).getObjectId("_id").toString()).collect(Collectors.toList()));
            } else {
                data.put("subjects", new ArrayList<>());
            }

            data.put("choicesCounts", choicesCounts);
            data.put("answers", answers);
            data.put("marks", marks);

            return generateSuccessMsg("data", data);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String setPDFQuizSubjectsAndChoicesCount(Common db, ObjectId quizId,
                                                           MultipartFile subjects, ObjectId userId
    ) {

        if (subjects == null)
            return JSON_NOT_VALID_PARAMS;

        if (db instanceof SchoolQuizRepository &&
                subjects.getSize() > MAX_FILE_SIZE)
            return generateErr(
                    "حداکثر حجم مجاز، " + MAX_FILE_SIZE + " مگ است."
            );

        String fileType = uploadExcelFile(subjects);
        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("pdf_quiz", false))
                return generateErr("این متد برای این نوع از آزمون قابل فراخوانی نیست");

            if (quiz.getLong("start") < System.currentTimeMillis())
                return generateErr("آزمون موردنظر شروع شده است");

            int qNo = quiz.getInteger("q_no", 0);
            if (qNo == 0)
                return generateErr("لطفا ابتدا تعداد سوالات را وارد نمایید");

            String filename = FileUtils.uploadTempFile(subjects);
            if (filename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;

            ArrayList<Row> rows = Excel.read(filename);

            FileUtils.removeTempFile(filename);

            if (rows == null)
                return generateErr("File is not valid");

            rows.remove(0);
            List<ObjectId> subjectIds = new ArrayList<>();
            List<Integer> choicesCounts = new ArrayList<>();
            List<Integer> marks = new ArrayList<>();

            for (Row row : rows) {
                try {

                    int code = (int) getCellValue(row.getCell(1));
                    Document subject = subjectRepository.findBySecKey(String.format("%03d", code));
                    if (subject == null)
                        return generateErr("کد " + code + " معتبر نمی باشد");

                    int choicesCount = (int) getCellValue(row.getCell(2));
                    if (choicesCount < 2 || choicesCount > 6)
                        return generateErr("تعداد گزینه ها باید بین 2 و 6 باشد");

                    marks.add(choicesCount - 1);
                    subjectIds.add(subject.getObjectId("_id"));
                    choicesCounts.add(choicesCount);

                } catch (Exception exception) {
                    return generateErr(exception.getMessage());
                }
            }

            if (subjectIds.size() != qNo)
                return generateErr("تعداد مباحث با تعداد سوالات برابر نمی باشد");

            Document questions = quiz.get("questions", Document.class);
            questions.put("subjects", subjectIds);
            questions.put("choices_counts", choicesCounts);
            questions.put("marks", marks);

            db.replaceOne(quizId, quiz);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String reviewContentQuiz(ObjectId quizId) {

        Document quiz = contentQuizRepository.findById(quizId);

        if (quiz == null)
            return JSON_NOT_VALID_ID;

        int neededTime = ContentQuizController.calcLenStatic(quiz);

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
                .put("duration", neededTime);

        List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (String attach : attaches)
            jsonArray.put(STATICS_SERVER + ContentQuizRepository.FOLDER + "/" + attach);

        quizJSON.put("attaches", jsonArray);


        return StudentQuizController.returnQuiz(quiz, null, true, quizJSON);
    }

    public static String getLog(Common db, ObjectId quizId) {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        return generateSuccessMsg("data", new JSONObject()
                .put("cropped", quiz.getOrDefault("cropped", false))
                .put("logs", quiz.getOrDefault("logs", ""))
        );
    }

    public static String getDistinctTags() {
        return generateSuccessMsg("data",
                iryscQuizRepository.distinctTags("tags")
        );
    }

    public static String getAllQuizzesDigest(Boolean isOpenQuizzesNeeded) {

        JSONArray all = new JSONArray();
        ArrayList<Document> quizzes = iryscQuizRepository.find(null, null);

        for (Document quiz : quizzes) {

            JSONObject jsonObject1 = new JSONObject()
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("name", quiz.getString("title") + " در آزمون\u200Cهای آیریسک")
                    .put("mode", "irysc");

            all.put(jsonObject1);
        }

        if (isOpenQuizzesNeeded == null || isOpenQuizzesNeeded) {

            quizzes = openQuizRepository.find(null, null);

            for (Document quiz : quizzes) {

                JSONObject jsonObject1 = new JSONObject()
                        .put("id", quiz.getObjectId("_id").toString())
                        .put("name", quiz.getString("title") + " در آزمون\u200Cهای باز")
                        .put("mode", "open");

                all.put(jsonObject1);
            }

        }

        return generateSuccessMsg("data", all);
    }

    public static String getAllContentQuizzesDigest() {

        JSONArray all = new JSONArray();
        ArrayList<Document> quizzes = contentQuizRepository.find(null, null);

        for (Document quiz : quizzes) {

            JSONObject jsonObject1 = new JSONObject()
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("name", quiz.getString("title"));

            all.put(jsonObject1);
        }

        return generateSuccessMsg("data", all);
    }

    public static Document store(ObjectId userId, JSONObject data, String mode
    ) throws InvalidFieldsException {
        Document newDoc = new Document();
        for (String key : data.keySet()) {
            if (key.equalsIgnoreCase("tags") || key.equalsIgnoreCase("kind"))
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

        newDoc.put("students", new ArrayList<>());
        newDoc.put("registered", 0);
        newDoc.put("removed_questions", new ArrayList<>());
        newDoc.put("attaches", new ArrayList<>());
        newDoc.put("created_by", userId);
        newDoc.put("created_at", System.currentTimeMillis());

        newDoc.put("questions", new Document());

//        //todo: consider other modes
//        if (newDoc.getString("mode").equals(KindQuiz.REGULAR.getName()) ||
//                newDoc.getString("mode").equals(KindQuiz.OPEN.getName())
//        )

        if (!mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName())) {

            if (newDoc.containsKey("mode") && newDoc.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
                newDoc.put("correctors", new ArrayList<>());

            if (!newDoc.containsKey("mode"))
                newDoc.put("mode", "regular");

            newDoc.put("visibility", true);
            newDoc.put("tags", tagsArr);

            if (newDoc.containsKey("price") && newDoc.get("price") instanceof String) {
                try {
                    newDoc.put("price", Integer.parseInt(newDoc.getString("price")));
                } catch (Exception x) {
                    newDoc.put("price", 0);
                }
            }
        } else {
            newDoc.put("mode", "regular");
            newDoc.put("status", "init");
            newDoc.put("visibility", false);
        }

        return newDoc;
    }

    public static String update(Common db, ObjectId userId,
                                ObjectId quizId, JSONObject data, boolean isAdvisor) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (!isAdvisor && data.has("payByStudent"))
                data.remove("payByStudent");

            if (isAdvisor && data.has("launchMode"))
                data.remove("launchMode");

            if (data.has("max_teams") && data.getInt("max_teams") < 2)
                return generateErr("تعداد تیم ها باید حداقل دو باشد");

            if (data.has("per_team") && data.getInt("per_team") < 1)
                return generateErr("تعداد نفرات هر تیم باید حداقل یک باشد");

            //todo : check after finish status in school quiz

            for (String key : data.keySet()) {

                if (key.equalsIgnoreCase("tags") || key.equalsIgnoreCase("kind"))
                    continue;

                quiz.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), data.get(key));
            }

            ArrayList<String> tagsArr = new ArrayList<>();

            if (data.has("tags")) {

                JSONArray tags = data.getJSONArray("tags");

                for (int i = 0; i < tags.length(); i++)
                    tagsArr.add(tags.getString(i));
            }

            quiz.put("tags", tagsArr);

            if (quiz.containsKey("price") && quiz.get("price") instanceof String) {
                try {
                    quiz.put("price", Integer.parseInt(quiz.getString("price")));
                } catch (Exception x) {
                    quiz.put("price", 0);
                }
            }

            db.replaceOne(quizId, quiz);
            QuizAbstract quizAbstract = null;

            if (db instanceof HWRepository)
                return generateSuccessMsg("data",
                        new RegularQuizController().convertHWDocToJSON(quiz, false, null)
                );

            if (db instanceof IRYSCQuizRepository ||
                    db instanceof SchoolQuizRepository
            )
                quizAbstract = new RegularQuizController();

            else if (db instanceof OpenQuizRepository)
                quizAbstract = new OpenQuiz();

            else if (db instanceof EscapeQuizRepository)
                quizAbstract = new EscapeQuizController();

            else if (db instanceof ContentQuizRepository)
                quizAbstract = new ContentQuizController();

            else if (db instanceof OnlineStandQuizRepository)
                quizAbstract = new OnlineStandingController();

            if (quizAbstract != null)
                return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                        "quiz",
                        quizAbstract.convertDocToJSON(
                                quiz, false, true,
                                false, false
                        )
                );

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        return JSON_NOT_UNKNOWN;
    }

    public static String toggleVisibility(Common db, ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            quiz.put("visibility", !quiz.getBoolean("visibility"));
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    private static PairValue isSchoolQuizReadyForPay(Document quiz) throws InvalidFieldsException {

        int studentsCount = quiz.getList("students", Document.class).size();
        if (studentsCount == 0)
            throw new InvalidFieldsException("لطفا ابتدا دانش آموز/دانش آموزان خود را به آزمون اضافه کنید");

        Document config = getConfig();
        int maxStd = (int) config.getOrDefault("max_student_quiz_per_day", 10);

        if (maxStd < studentsCount)
            throw new InvalidFieldsException("حداکثر تعداد دانش آموز در یک آزمون می تواند " + maxStd + " باشد");

        Document question = quiz.get("questions", Document.class);
        boolean pdfQuiz = quiz.getBoolean("pdf_quiz", false);

        if (!pdfQuiz && !question.containsKey("_ids"))
            throw new InvalidFieldsException("لطفا ابتدا سوال/سوالات خود را به آزمون اضافه کنید");

        if (pdfQuiz && !question.containsKey("subjects"))
            throw new InvalidFieldsException("لطفا ابتدا مباحث سوال/سوالات خود را مشخص کنید");

        if (pdfQuiz && !question.containsKey("answers"))
            throw new InvalidFieldsException("لطفا ابتدا پاسخ سوال/سوالات خود را مشخص کنید");

        int qNo;
        List<ObjectId> questionIds = null;

        if (!pdfQuiz) {
            questionIds = question.getList("_ids", ObjectId.class);
            qNo = questionIds.size();
        } else
            qNo = (Integer) quiz.getOrDefault("q_no", 0);

        if (qNo == 0)
            throw new InvalidFieldsException("لطفا ابتدا سوال/سوالات خود را به آزمون اضافه کنید");

        int maxQ = (int) config.getOrDefault("max_question_per_quiz", 20);
        if (maxQ < qNo)
            throw new InvalidFieldsException("حداکثر تعداد سوال در هر آزمون می تواند " + maxQ + " باشد");

        if (!pdfQuiz && quiz.getBoolean("database")) {

            List<Document> questions = questionRepository.findByIds(
                    questionIds, true
            );

            if (questions == null)
                throw new InvalidFieldsException("خطای نامشخص");

            return new PairValue(studentsCount, questions);

        }

        return new PairValue(studentsCount,
                config.getOrDefault("quiz_per_student_price", 1000)
        );
    }

    private static PairValue calcPrice(List<Document> questions,
                                       int studentsCount, boolean isRecpRowNeeded
    ) throws InvalidFieldsException {

        List<SchoolRecpRow> rows = new ArrayList<>();
        HashMap<ObjectId, Document> subjects = new HashMap<>();
        double total = 0;

        for (Document question : questions) {

            ObjectId subjectId = question.getObjectId("subject_id");
            Document subject;

            if (!subjects.containsKey(subjectId)) {

                subject = subjectRepository.findById(subjectId);

                if (subject == null)
                    throw new InvalidFieldsException("unknown exception");

                subjects.put(subjectId, subject);
            } else
                subject = subjects.get(subjectId);

            int basePrice = question.getString("level").equals("easy") ?
                    subject.getInteger("school_easy_price") :
                    question.getString("level").equals("mid") ?
                            subject.getInteger("school_mid_price") :
                            subject.getInteger("school_hard_price");

            double price = basePrice + basePrice * Math.floor(studentsCount / 10.0) * 0.15;
            total += price;

            if (isRecpRowNeeded) {
                SchoolRecpRow row = new SchoolRecpRow(question.getString("level"),
                        subject.getString("name"), (int) price);

                int idx = rows.indexOf(row);
                if (idx < 0)
                    rows.add(row);
                else
                    rows.get(idx).inc();
            }
        }

        if (isRecpRowNeeded) {
            JSONArray jsonArray = new JSONArray();
            for (SchoolRecpRow row : rows)
                jsonArray.put(row.toJSON());

            return new PairValue(total, jsonArray);
        }

        return new PairValue(total, null);
    }

    public static String recp(ObjectId quizId, ObjectId userId) {

        try {
            Document quiz = hasAccess(schoolQuizRepository, userId, quizId);

            PairValue p = isSchoolQuizReadyForPay(quiz);
            int studentsCount = (int) p.getKey();
            JSONArray jsonArray;

            if (quiz.getBoolean("database")) {

                List<Document> questions = (List<Document>) p.getValue();

                PairValue res = calcPrice(questions, studentsCount, true);
                jsonArray = (JSONArray) res.getValue();
                int total = (int) ((double) res.getKey());

                jsonArray.put(new JSONObject()
                        .put("level", "-")
                        .put("subject", "جمع کل")
                        .put("price", "-")
                        .put("totalPrice", total)
                        .put("count", questions.size())
                );

            } else {
                jsonArray = new JSONArray();
                int price = (int) p.getValue();
                jsonArray.put(new JSONObject()
                        .put("price", price)
                        .put("totalPrice", studentsCount * price)
                        .put("count", studentsCount)
                );
            }

            return generateSuccessMsg("data", jsonArray);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

    public static String getTotalPrice(ObjectId quizId, ObjectId userId,
                                       double money) {

        try {

            Document quiz = hasAccess(schoolQuizRepository, userId, quizId);
            if (quiz.getString("status").equalsIgnoreCase("finish"))
                return generateErr("این آزمون قبلا نهایی شده است");

            PairValue p = isSchoolQuizReadyForPay(quiz);

            int studentsCount = (int) p.getKey();
            int total;

            if (quiz.getBoolean("database") && !quiz.getBoolean("pdf_quiz", false)) {
                List<Document> questions = (List<Document>) p.getValue();
                PairValue res = calcPrice(questions, studentsCount, false);
                total = (int) ((double) res.getKey());
            } else {
                int price = (int) p.getValue();
                total = studentsCount * price;
            }

            if ((boolean) quiz.getOrDefault("pay_by_student", false)) {
                quiz.put("status", "semi_finish");
                quiz.put("price", total);
                schoolQuizRepository.replaceOne(quizId, quiz);

                return generateSuccessMsg("data", new JSONObject().put("status", "ready"));
            }

            long curr = System.currentTimeMillis();

            Document offDoc = findAccountOff(
                    userId, curr, OffCodeSections.SCHOOL_QUIZ.getName()
            );

            JSONObject jsonObject = new JSONObject()
                    .put("total", total);

            double shouldPayDouble = total;

            if (offDoc != null) {

                double offAmount =
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount");

                jsonObject.put("off", offAmount);
                shouldPayDouble -= offAmount;
            } else
                jsonObject.put("off", 0);

            if (shouldPayDouble > 0) {
                if (money >= shouldPayDouble) {
                    jsonObject.put("usedFromWallet", shouldPayDouble);
                    shouldPayDouble = 0;
                } else {
                    jsonObject.put("usedFromWallet", money);
                    shouldPayDouble -= money;
                }
            } else
                jsonObject.put("usedFromWallet", 0);

            shouldPayDouble = Math.max(0, shouldPayDouble);
            jsonObject.put("shouldPay", (int) shouldPayDouble);

            return generateSuccessMsg("data", jsonObject);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

    static double payFromWallet(int shouldPay, double money, ObjectId userId) {
        double newUserMoney = money;
        newUserMoney -= Math.min(shouldPay, money);
        Document user = userRepository.findById(userId);
        user.put("money", newUserMoney);
        userRepository.replaceOne(userId, user);
        return newUserMoney;
    }

    public static String finalizeQuiz(ObjectId quizId, ObjectId userId,
                                      String off, double money) {

        try {

            Document quiz = hasAccess(schoolQuizRepository, userId, quizId);

            PairValue p = isSchoolQuizReadyForPay(quiz);
            int studentsCount = (int) p.getKey();
            int total;

            if (quiz.getBoolean("database")) {
                List<Document> questions = (List<Document>) p.getValue();
                PairValue res = calcPrice(questions, studentsCount, false);
                total = (int) ((double) res.getKey());
            } else {
                int price = (int) p.getValue();
                total = studentsCount * price;
            }

            long curr = System.currentTimeMillis();
            Document offDoc;

            if (off == null)
                offDoc = findAccountOff(
                        userId, curr, OffCodeSections.SCHOOL_QUIZ.getName()
                );
            else {

                offDoc = validateOffCode(
                        off, userId, curr,
                        OffCodeSections.SCHOOL_QUIZ.getName()
                );

                if (offDoc == null)
                    return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");

            }

            double offAmount = 0;
            double shouldPayDouble = total * 1.0;

            if (offDoc != null) {
                offAmount +=
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount")
                ;
                shouldPayDouble = total - offAmount;
            }

            int shouldPay = (int) shouldPayDouble;

            if (shouldPay - money <= 100) {

                if (shouldPay > 100)
                    money = payFromWallet(shouldPay, money, userId);

                Document doc = new Document("user_id", userId)
                        .append("amount", 0)
                        .append("account_money", shouldPay > 100 ? shouldPay : 0)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.SCHOOL_QUIZ.getName())
                        .append("products", quizId);

                if (offDoc != null) {
                    doc.append("off_code", offDoc.getObjectId("_id"));
                    doc.append("off_amount", (int) offAmount);
                }

                ObjectId tId = transactionRepository.insertOneWithReturnId(doc);
                quiz.put("status", "finish");
                schoolQuizRepository.replaceOne(quizId, quiz);

                if (offDoc != null) {

                    BasicDBObject update;

                    if (offDoc.containsKey("is_public") &&
                            offDoc.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = offDoc.getList("students", ObjectId.class);
                        students.add(userId);
                        update = new BasicDBObject("students", students);
                    } else {

                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.SCHOOL_QUIZ.getName())
                                .append("used_for", quizId);
                    }

                    offcodeRepository.updateOne(
                            offDoc.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }

                return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                        "action", "success",
                        new PairValue("refId", money),
                        new PairValue("transactionId", tId.toString())
                );
            }

            long orderId = Math.abs(new Random().nextLong());
            while (transactionRepository.exist(
                    eq("order_id", orderId)
            )) {
                orderId = Math.abs(new Random().nextLong());
            }

            Document doc =
                    new Document("user_id", userId)
                            .append("account_money", money)
                            .append("amount", (int) (shouldPay - money))
                            .append("created_at", curr)
                            .append("status", "init")
                            .append("order_id", orderId)
                            .append("products", quizId)
                            .append("section", OffCodeSections.SCHOOL_QUIZ.getName());

            if (off != null) {
                doc.append("off_code", offDoc.getObjectId("_id"));
                doc.append("off_amount", (int) offAmount);
            }

            return goToPayment((int) (shouldPay - money), doc);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getAll(Common db, ObjectId userId,
                                String name,
                                Long startDateSolar,
                                Long startDateSolarEndLimit,
                                Long startRegistryDateSolar,
                                Long startRegistrySolarEndLimit,
                                String kind
    ) {

        ArrayList<Document> docs;
        ArrayList<Bson> filters = new ArrayList<>();
        if (userId != null)
            filters.add(eq("created_by", userId));

        if (name != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE)));

        if (kind != null && EnumValidatorImp.isValid(kind, KindQuiz.class))
            filters.add(eq("mode", kind));

        if (startDateSolar != null)
            filters.add(gte("start", startDateSolar));

        if (startDateSolarEndLimit != null)
            filters.add(lte("start", startDateSolarEndLimit));

        if (startRegistryDateSolar != null)
            filters.add(gte("start_registry", startRegistryDateSolar));

        if (startRegistrySolarEndLimit != null)
            filters.add(lte("start_registry", startRegistrySolarEndLimit));

        docs = db.find(filters.size() == 0 ? null : and(filters),
                db instanceof HWRepository ? HW_DIGEST_MANAGEMENT : QUIZ_DIGEST_MANAGEMENT,
                Sorts.descending("created_at")
        );

        QuizAbstract quizAbstract;

        JSONArray jsonArray = new JSONArray();
        RegularQuizController regularQuizController = new RegularQuizController();

        for (Document quiz : docs) {

            if (db instanceof IRYSCQuizRepository) {
                if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                    quizAbstract = new TashrihiQuizController();
                else
                    quizAbstract = new RegularQuizController();
            } else if (db instanceof ContentQuizRepository)
                quizAbstract = new ContentQuizController();
            else if (db instanceof OnlineStandQuizRepository)
                quizAbstract = new OnlineStandingController();
            else if (db instanceof EscapeQuizRepository)
                quizAbstract = new EscapeQuizController();
            else if (db instanceof SchoolQuizRepository || db instanceof HWRepository)
                quizAbstract = regularQuizController;
            else
                quizAbstract = new OpenQuiz();

            if (db instanceof HWRepository)
                jsonArray.put(regularQuizController.convertHWDocToJSON(quiz, true, null));
            else
                jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, true, false, false));
        }

        return generateSuccessMsg("data", jsonArray);

    }

    public static String forceRegistry(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray,
                                       int paid, boolean isAdvisor) {

        try {
            Document quiz = hasAccess(db, userId, quizId);
            QuizAbstract quizAbstract;

            if (db instanceof OpenQuizRepository)
                quizAbstract = new OpenQuiz();
            else if (db instanceof EscapeQuizRepository)
                quizAbstract = new EscapeQuizController();
            else if (KindQuiz.REGULAR.getName().equals(quiz.getOrDefault("mode", "regular").toString()))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            JSONArray excepts = new JSONArray();
            JSONArray addedItems = new JSONArray();

            ArrayList<ObjectId> quizIds = new ArrayList<>();
            quizIds.add(quizId);

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

                if (db instanceof SchoolQuizRepository && isAdvisor && !student.containsKey("my_advisors")) {
                    excepts.put(i + 1);
                    continue;
                }

                if (db instanceof SchoolQuizRepository && !isAdvisor &&
                        (
                                !student.containsKey("school") ||
                                        student.get("school") == null
                        )
                ) {
                    excepts.put(i + 1);
                    continue;
                }

                ObjectId studentId = student.getObjectId("_id");

                if (userId != null && !Authorization.hasAccessToThisStudent(studentId, userId)) {
                    excepts.put(i + 1);
                    continue;
                }

                if (db instanceof SchoolQuizRepository) {
                    if (((RegularQuizController) quizAbstract).schoolQuizRegistry(
                            student.getObjectId("_id"), quiz
                    ))
                        addedItems.put(student.getObjectId("_id"));
                } else if (db instanceof HWRepository) {
                    if (((RegularQuizController) quizAbstract).hwRegistry(
                            student.getObjectId("_id"), quiz
                    ))
                        addedItems.put(student.getObjectId("_id"));
                } else {
                    List<Document> added = quizAbstract.registry(
                            student.getObjectId("_id"), student.getString("phone"),
                            student.getString("mail"), quizIds, paid,
                            null, null
                    );

                    if (added.size() > 0)
                        addedItems.put(convertStudentDocToJSON(added.get(0), student));
                }
            }

            if (db instanceof SchoolQuizRepository && addedItems.length() > 0) {
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

            if (db instanceof OnlineStandQuizRepository)
                quizAbstract = new OnlineStandingController();
            else if (db instanceof EscapeQuizRepository)
                quizAbstract = new EscapeQuizController();
            else if (KindQuiz.REGULAR.getName().equals(quiz.getOrDefault("mode", "regular").toString()))
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

            if (db instanceof HWRepository)
                return generateSuccessMsg("data",
                        new RegularQuizController().convertHWDocToJSON(quiz, false, null)
                );

            if (db instanceof IRYSCQuizRepository ||
                    db instanceof SchoolQuizRepository
            )
                quizAbstract = new RegularQuizController();

            else if (db instanceof OpenQuizRepository)
                quizAbstract = new OpenQuiz();

            else if (db instanceof ContentQuizRepository)
                quizAbstract = new ContentQuizController();

            else if (db instanceof OnlineStandQuizRepository)
                quizAbstract = new OnlineStandingController();

            else if (db instanceof EscapeQuizRepository)
                quizAbstract = new EscapeQuizController();

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

            List<Boolean> uploadableList = null;
            if (quiz.containsKey("mode") && quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                uploadableList = (List<Boolean>) questionsDoc.getOrDefault(
                        "uploadable_list", new ArrayList<Double>()
                );

            if (questionsMark.size() != questions.size())
                return JSON_NOT_UNKNOWN;

            HashMap<ObjectId, String> correctors = null;
            List<Document> correctorDocs = null;

            if (quiz.containsKey("mode") &&
                    quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()) &&
                    quiz.containsKey("correctors")
            ) {

                correctors = new HashMap<>();

                correctorDocs = quiz.getList("correctors", Document.class);
                for (Document doc : correctorDocs) {

                    Document user = userRepository.findById(doc.getObjectId("user_id"));

                    if (user != null)
                        correctors.put(doc.getObjectId("_id"), user.getString("first_name") + " " + user.getString("last_name"));
                }
            }

            List<Document> students = quiz.getList("students", Document.class);

            int i = 0;
            boolean useFromDataset = (boolean)
                    quiz.getOrDefault("database", true);

            for (ObjectId itr : questions) {

                Document question = useFromDataset ?
                        questionRepository.findById(itr) :
                        schoolQuestionRepository.findById(itr);

                if (question == null) {
                    i++;
                    continue;
                }

                Document tmpDoc;

                if (uploadableList != null && uploadableList.size() > i)
                    tmpDoc = Document.parse(question.toJson())
                            .append("no", i + 1)
                            .append("mark", questionsMark.get(i))
                            .append("can_upload", uploadableList.get(i));
                else
                    tmpDoc = Document.parse(question.toJson())
                            .append("no", i + 1)
                            .append("mark", questionsMark.get(i));

                if (!tmpDoc.containsKey("kind_question"))
                    tmpDoc.append("kind_question", "test");

                if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName())) {

                    int marked = 0;
                    for (Document student : students) {
                        if (student.containsKey("answers")) {
                            List<Document> answers = student.getList("answers", Document.class);
                            Document q = searchInDocumentsKeyVal(answers, "question_id", question.getObjectId("_id"));
                            if (q != null && q.containsKey("mark"))
                                marked++;
                        }
                    }

                    String corrector = null;
                    String correctorId = null;

                    if (correctorDocs != null) {

                        for (Document correctorDoc : correctorDocs) {

                            if (!correctorDoc.containsKey("questions"))
                                continue;

                            if (correctorDoc.getList("questions", ObjectId.class).contains(question.getObjectId("_id"))) {
                                corrector = correctors.get(correctorDoc.getObjectId("_id"));
                                correctorId = correctorDoc.getObjectId("_id").toString();
                                break;
                            }
                        }

                    }

                    if (corrector != null) {
                        tmpDoc.append("corrector", corrector)
                                .append("correctorId", correctorId);
                    }
                    tmpDoc.append("total", students.size());
                    tmpDoc.append("allMarked", marked);

                }

                questionsList.add(tmpDoc);
                i++;
            }

            JSONArray jsonArray = Utilities.convertList(questionsList, true, true,
                    true, true, true, useFromDataset);

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

        boolean schoolQuizzes = db instanceof SchoolQuizRepository;

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId quizId = new ObjectId(id);

            try {

                Document quiz = hasAccess(db, userId, quizId);

                if (!schoolQuizzes || quiz.getString("status").equals("finish")) {

                    if (quiz.getList("students", Document.class).size() > 0) {
                        excepts.put("مورد " + (i + 1) + " " + "دانش آموز/دانش آموزانی در این آزمون شرکت کرده اند و امکان حذف آن وجود ندارد.");
                        continue;
                    }

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
                                         Boolean justMarked,
                                         Boolean justNotMarked,
                                         Boolean justAbsents,
                                         Boolean justPresence) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            JSONArray jsonArray = new JSONArray();

            List<Document> students = quiz.getList("students", Document.class);
            HashMap<ObjectId, String> correctors = null;
            List<Document> correctorDocs = null;

            if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()) &&
                    quiz.containsKey("correctors")
            ) {

                correctors = new HashMap<>();

                correctorDocs = quiz.getList("correctors", Document.class);
                for (Document doc : correctorDocs) {

                    Document user = userRepository.findById(doc.getObjectId("user_id"));

                    if (user != null)
                        correctors.put(doc.getObjectId("_id"), user.getString("first_name") + " " + user.getString("last_name"));
                }
            }

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

                if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName())) {

                    JSONObject jsonObject = convertStudentDocToJSONInTashrihiQuiz(student, user);

                    String corrector = null;
                    String correctorId = null;

                    if (correctorDocs != null) {

                        for (Document correctorDoc : correctorDocs) {

                            if (!correctorDoc.containsKey("students"))
                                continue;

                            if (correctorDoc.getList("students", ObjectId.class).contains(student.getObjectId("_id"))) {
                                corrector = correctors.get(correctorDoc.getObjectId("_id"));
                                correctorId = correctorDoc.getObjectId("_id").toString();
                                break;
                            }
                        }

                    }

                    if (corrector != null) {
                        jsonObject.put("corrector", corrector)
                                .put("correctorId", correctorId);
                    }

                    jsonArray.put(jsonObject);
                } else
                    jsonArray.put(convertStudentDocToJSON(student, user));
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    private static JSONObject convertStudentDocToJSONInTashrihiQuiz(
            Document student, Document user
    ) {

        JSONObject jsonObject = new JSONObject()
                .put("paid", student.get("paid"))
                .put("id", user.getObjectId("_id").toString())
                .put("totalMark", student.getOrDefault("total_mark", ""))
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

        if (student.containsKey("rate")) {
            jsonObject.put("rate", student.get("rate"))
                    .put("rateAt", getSolarDate((Long) student.getOrDefault("rate_at", System.currentTimeMillis())));
        }

        if (student.containsKey("all_marked"))
            jsonObject.put("allMarked", student.getBoolean("all_marked"));

        boolean hasAccepted = false;

        if (student.containsKey("ans_files")) {

            for (Document itr : student.getList("ans_files", Document.class)) {
                if (itr.getString("status").equals("accepted")) {
                    hasAccepted = true;
                    break;
                }
            }
        }

        if (!hasAccepted && student.containsKey("answers")) {

            List<Document> tmp = student.getList("answers", Document.class);

            if (tmp.size() > 0 && tmp.get(0).containsKey("answer"))
                hasAccepted = true;
        }

        jsonObject.put("hasAcceptedAnswerSheet", hasAccepted);

        return jsonObject;
    }

    public static JSONObject convertStudentDocToJSON(
            Document student, Document user
    ) {

        JSONObject jsonObject = new JSONObject()
                .put("id", user.getObjectId("_id").toString());

        if (student.containsKey("paid")) {
            jsonObject.put("paid", student.get("paid"));
            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
        } else {
            jsonObject.put("name", user.getString("first_name") + " " + user.getString("last_name"));
            jsonObject.put("NID", user.getString("NID"));
            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
        }

        if (student.containsKey("register_at"))
            jsonObject.put("registerAt", getSolarDate(student.getLong("register_at")));

        if (jsonObject.has("start_at")) {
            jsonObject.put("startAt", student.containsKey("start_at") ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("start_at")) :
                    ""
            ).put("finishAt", student.containsKey("finish_at") ?
                    irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("finish_at")) :
                    ""
            );
        }

        if (student.containsKey("rate")) {
            jsonObject.put("rate", student.get("rate"))
                    .put("rateAt", getSolarDate((Long) student.getOrDefault("rate_at", System.currentTimeMillis())));
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

            if (db instanceof SchoolQuizRepository ||
                    db instanceof HWRepository
            ) {

//                Document config = irysc.gachesefid.Utility.Utility.getConfig();

//                if (config.getBoolean("school_quiz_attaches_just_link"))
//                    return JSON_NOT_ACCESS;

//                config.getInteger("schoolQuizAttachesMax")
//                config.getInteger("schoolQuizAttachesMax")

                if (attaches.size() >= 2)
                    return generateErr(
                            "شما می توانید حداکثر " + 2 + " پیوست داشته باشید."
                    );
            }


            String base = db instanceof SchoolQuizRepository ?
                    SchoolQuizRepository.FOLDER :
                    db instanceof EscapeQuizRepository ?
                            EscapeQuizRepository.FOLDER :
                            db instanceof OnlineStandQuizRepository ?
                                    OnlineStandQuizRepository.FOLDER :
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
                    "url", STATICS_SERVER + base + "/" + filename
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
            if (idx < 0)
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

    public static String arrangeEscapeQuizQuestions(ObjectId quizId, JSONObject jsonObject) {
        try {
            Document doc = hasAccess(escapeQuizRepository, null, quizId);

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

            ArrayList<ObjectId> newArrange = new ArrayList<>();

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
            }

            questions.put("_ids", newArrange);
            ArrayList<Object> answers = new ArrayList<>();

            for (ObjectId id : newArrange) {
                Document q = escapeQuizQuestionRepository.findById(id);
                answers.add(q.get("answer"));
            }

            questions.put("answers", answers);
            doc.put("questions", questions);
            escapeQuizRepository.replaceOne(quizId, doc);

            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    static File doGenerateQuestionPDF(Document doc, String folder,
                                      String schoolName, String pic) {

        ArrayList<String> files = new ArrayList<>();
        Document questions = doc.get("questions", Document.class);
        List<ObjectId> ids = questions.getList("_ids", ObjectId.class);

        boolean useFromDatabase = (boolean) doc.getOrDefault("database", true);

        String prefix = useFromDatabase ?
                DEV_MODE ? uploadDir_dev + QuestionRepository.FOLDER + "/" :
                        uploadDir + QuestionRepository.FOLDER + "/" :
                DEV_MODE ? uploadDir_dev + "school_quizzes/questions/" :
                        uploadDir + "school_quizzes/questions/";

        for (ObjectId qId : ids) {

            Document questionDoc = useFromDatabase ?
                    questionRepository.findById(qId) :
                    schoolQuestionRepository.findById(qId);

            if (questionDoc == null)
                continue;

            files.add(prefix + questionDoc.getString("question_file"));
        }

        return PDFUtils.createExam(files, folder + doc.getObjectId("_id") + ".pdf", doc, schoolName, pic);

    }

    public static File generateQuestionPDF(Common db, ObjectId userId,
                                           ObjectId quizId) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            String prefix = DEV_MODE ? uploadDir_dev : uploadDir;

            String folder = db instanceof IRYSCQuizRepository ?
                    prefix + IRYSCQuizRepository.FOLDER + "/" :
                    prefix + SchoolQuizRepository.FOLDER + "/";

            String schoolName = null;
            String pic = null;

            if (db instanceof SchoolQuizRepository) {

                Document school = schoolRepository.findOne(
                        and(
                                exists("user_id", true),
                                eq("user_id", userId)
                        ), new BasicDBObject("name", 1)
                );

                if (school != null)
                    schoolName = school.getString("name");

                Document user = userRepository.findById(userId);
                if (user != null) {

                    if (user.containsKey("pic"))
                        pic = DEV_MODE ? uploadDir_dev + UserRepository.FOLDER + "/" + user.getString("pic") :
                                uploadDir + UserRepository.FOLDER + "/" + user.getString("pic");

                    else if (user.containsKey("avatar_id")) {
                        Document avatar = avatarRepository.findById(user.getObjectId("avatar_id"));
                        if (avatar != null)
                            pic = DEV_MODE ? uploadDir_dev + UserRepository.FOLDER + "/" + avatar.getString("file") :
                                    uploadDir + UserRepository.FOLDER + "/" + avatar.getString("file");
                    }
                }

            }

            return doGenerateQuestionPDF(doc, folder, schoolName, pic);
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
                    if (row.getCell(0) == null)
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

            if (db instanceof EscapeQuizRepository)
                return doAddQuestionsToEscapeQuiz(quiz, jsonArray, excepts);

            return doAddQuestionsToQuiz(db, quiz, jsonArray, excepts, 3,
                    quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()) ?
                            true : null
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

            if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                return doAddQuestionsToQuiz(db, quiz, jsonArray,
                        new JSONArray(), mark, true
                );

            if (db instanceof EscapeQuizRepository)
                return doAddQuestionsToEscapeQuiz(quiz, jsonArray, new JSONArray());

            //todo: check edit access
            //todo: check school access to questions
            return doAddQuestionsToQuiz(db, quiz, jsonArray,
                    new JSONArray(), mark, null
            );

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    static String doAddQuestionsToQuiz(Common db, Document quiz,
                                       Object questionsList,
                                       JSONArray excepts,
                                       double mark, Boolean can_upload
    ) throws InvalidFieldsException {
        ArrayList<Document> addedItems = new ArrayList<>();
        Document questions = quiz.get("questions", Document.class);

        List<Double> marks = questions.containsKey("marks") ? questions.getList("marks", Double.class) : new ArrayList<>();
        List<ObjectId> ids = questions.containsKey("_ids") ? questions.getList("_ids", ObjectId.class) : new ArrayList<>();
        List<Boolean> uploadable_list = null;
        boolean isTashrihi = quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName());

        if (can_upload != null)
            uploadable_list = questions.containsKey("uploadable_list") ?
                    questions.getList("uploadable_list", Boolean.class) : new ArrayList<>();

        HashMap<ObjectId, Integer> allUsed = new HashMap<>();
        List<Document> allQuestions = new ArrayList<>();

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

                    if (isTashrihi &&
                            !question.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TASHRIHI.getName())
                    ) {
                        excepts.put(i + 1);
                        continue;
                    } else if (!isTashrihi &&
                            question.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TASHRIHI.getName())) {
                        excepts.put(i + 1);
                        continue;
                    }

                    int used = (int) question.getOrDefault("used", 0);
                    question.put("used", used + 1);
                    allUsed.put(question.getObjectId("_id"), used + 1);

                    allQuestions.add(question);
                    marks.add(tmpMark);
                    ids.add(question.getObjectId("_id"));

                    if (can_upload != null) {
                        uploadable_list.add(can_upload);

                        addedItems.add(
                                Document.parse(question.toJson()).append("mark", mark)
                                        .append("canUpload", true)
                        );

                    } else
                        addedItems.add(
                                Document.parse(question.toJson()).append("mark", mark)
                        );

                } catch (Exception x) {
                    excepts.put(i + 1);
                }
            }

            List<WriteModel<Document>> writes = new ArrayList<>();

            for (ObjectId oId : allUsed.keySet()) {
                writes.add(new UpdateOneModel<>(
                        eq("_id", oId),
                        set("used", allUsed.get(oId))
                ));
            }
            if (writes.size() > 0)
                questionRepository.bulkWrite(writes);
        } else if (questionsList instanceof Document) {

            Document qTmp = (Document) questionsList;

            if (isTashrihi &&
                    !qTmp.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TASHRIHI.getName())
            ) {
                throw new InvalidFieldsException("تنها سوالات تشریحی می توانند به این آزمون افزوده شوند");
            } else if (!isTashrihi &&
                    qTmp.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TASHRIHI.getName())) {
                throw new InvalidFieldsException("سوالات تشریحی نمی توانند به این آزمون افزوده شوند");
            }

            ObjectId qIdTmp = qTmp.getObjectId("_id");

            if (ids.contains(qIdTmp)) {
                throw new InvalidFieldsException("duplicate");
            }

            allQuestions.add((Document) questionsList);
            marks.add(mark);
            ids.add(qIdTmp);

            if (can_upload != null)
                uploadable_list.add(can_upload);
        }

        byte[] answersByte;

        if (!isTashrihi) {

            if (questions.containsKey("answers"))
                answersByte = questions.get("answers", Binary.class).getData();
            else
                answersByte = new byte[0];

            for (Document question : allQuestions) {
                answersByte = Utility.addAnswerToByteArr(answersByte, question.getOrDefault("kind_question", "test").toString(),
                        question.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(QuestionType.TEST.getName()) ?
                                new PairValue(question.getInteger("choices_count"), question.get("answer")) :
                                question.get("answer")
                );
            }

            questions.put("answers", answersByte);
        }

        questions.put("marks", marks);
        questions.put("_ids", ids);
        quiz.put("questions", questions);

        if (uploadable_list != null)
            questions.put("uploadable_list", uploadable_list);

        db.replaceOne(quiz.getObjectId("_id"), quiz);

        if (questionsList instanceof Document)
            return "ok";

        PairValue p = new PairValue("doneIds", Utilities.convertList(
                addedItems, true, true, true,
                true, true, true
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

    static String doAddQuestionsToEscapeQuiz(Document quiz,
                                             Object questionsList,
                                             JSONArray excepts
    ) throws InvalidFieldsException {

        ArrayList<Document> addedItems = new ArrayList<>();

        Document questions = quiz.get("questions", Document.class);
        List<ObjectId> ids = questions.containsKey("_ids") ? questions.getList("_ids", ObjectId.class) : new ArrayList<>();
        List<String> answers;

        if (questions.containsKey("answers"))
            answers = questions.getList("answers", String.class);
        else
            answers = new ArrayList<>();

        if (questionsList instanceof JSONArray) {

            JSONArray jsonArray = (JSONArray) questionsList;

            for (int i = 0; i < jsonArray.length(); i++) {

                try {

                    String organizationId;

                    if (jsonArray.get(i) instanceof JSONObject) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        organizationId = jsonObject.getString("organizationId");
                    } else
                        organizationId = jsonArray.getString(i);

                    Document question = escapeQuizQuestionRepository.findBySecKey(organizationId);

                    if (question == null) {
                        excepts.put(i + 1);
                        continue;
                    }

                    answers.add(question.get("answer").toString());
                    ids.add(question.getObjectId("_id"));

                    addedItems.add(
                            Document.parse(question.toJson())
                    );

                } catch (Exception x) {
                    excepts.put(i + 1);
                }
            }

        } else if (questionsList instanceof Document) {

            Document qTmp = (Document) questionsList;

            ObjectId qIdTmp = qTmp.getObjectId("_id");

            if (ids.contains(qIdTmp)) {
                throw new InvalidFieldsException("duplicate");
            }

            answers.add(qTmp.get("answer").toString());
            ids.add(qIdTmp);
        }


        questions.put("answers", answers);
        questions.put("_ids", ids);
        quiz.put("questions", questions);

        escapeQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);

        if (questionsList instanceof Document)
            return "ok";

        PairValue p = new PairValue("doneIds", Utilities.convertEscapeQuestionsList(
                addedItems, true, true, true
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

    public static String addQuestionToQuizzes(String organizationCode, Common db,
                                              ObjectId userId, JSONArray jsonArray,
                                              double mark) {

        boolean isEscapeQuiz = db instanceof EscapeQuizRepository;

        Document question = isEscapeQuiz ?
                escapeQuizQuestionRepository.findBySecKey(organizationCode) :
                questionRepository.findBySecKey(organizationCode);

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
                // todo heyyy

                if (isEscapeQuiz) {

                    doAddQuestionsToEscapeQuiz(quiz, question, null);

                    addedItems.put(
                            QuestionController.convertDocToJSON(question)
                    );

                } else {
                    doAddQuestionsToQuiz(db, quiz,
                            question, null, mark, null
                    );

                    addedItems.put(
                            QuestionController.convertDocToJSON(question)
                                    .put("mark", mark)
                    );

                    used++;
                }
            } catch (Exception x) {
                excepts.put(i + 1);
            }
        }

        if (addedItems.length() > 0 && !(db instanceof EscapeQuizRepository))
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
                                            Number mark, String canUpload) {

        try {
            Document quiz = hasAccess(db, userId, quizId);
            Document questions = quiz.get("questions", Document.class);
            List<ObjectId> ids = questions.getList("_ids", ObjectId.class);

            int idx = ids.indexOf(questionId);
            if (idx < 0)
                return JSON_NOT_VALID_ID;

            List<Double> marks = questions.getList("marks", Double.class);
            marks.set(idx, mark.doubleValue());

            if (canUpload != null && questions.containsKey("uploadable_list") && (
                    canUpload.equalsIgnoreCase("yes") ||
                            canUpload.equalsIgnoreCase("no")
            )) {
                List<Boolean> uploadableList = questions.getList("uploadable_list", Boolean.class);
                uploadableList.set(idx, canUpload.equalsIgnoreCase("yes"));
            }

            quiz.put("questions", questions);
            db.replaceOne(quizId, quiz);
            return JSON_OK;
        } catch (Exception x) {
            return JSON_NOT_ACCESS;
        }
    }

    public static String finalizeQuizResult(ObjectId quizId, Common db) {
        try {
            Document quiz = hasAccess(db, null, quizId);
            boolean isEscapeQuiz = db instanceof EscapeQuizRepository;

            if (isEscapeQuiz) {
                if (quiz.getLong("end") > System.currentTimeMillis())
                    return generateErr("زمان آزمون هنوز تمام نشده است");

                return EscapeQuizController.giveGifts(quiz);
            } else if (!quiz.containsKey("report_status") ||
                    !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("ابتدا باید جدول تراز آزمون ساخته شود.");

            List<Binary> questionsStat = quiz.getList("question_stat", Binary.class);
            ArrayList<Document> questions = questionRepository.findByIds(
                    quiz.get("questions", Document.class).getList("_ids", ObjectId.class), true
            );

            if (questions == null || questions.size() != questionsStat.size())
                return JSON_NOT_UNKNOWN;

            Utilities.updateQuestionsStat(questions, questionsStat);
            Document config = getConfig();

            if (
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

                if (rankingList.size() > 0)
                    giveQuizGiftToUser(rankingList.get(0).getObjectId("_id"), config, 1,
                            date, quizName, quizId
                    );

                if (rankingList.size() > 1)
                    giveQuizGiftToUser(rankingList.get(1).getObjectId("_id"), config, 2,
                            date, quizName, quizId
                    );

                if (rankingList.size() > 2)
                    giveQuizGiftToUser(rankingList.get(2).getObjectId("_id"), config, 3,
                            date, quizName, quizId
                    );

                if (rankingList.size() > 3)
                    giveQuizGiftToUser(rankingList.get(3).getObjectId("_id"), config, 4,
                            date, quizName, quizId
                    );

                if (rankingList.size() > 4)
                    giveQuizGiftToUser(rankingList.get(4).getObjectId("_id"), config, 5,
                            date, quizName, quizId
                    );
            }
            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    private static void giveQuizGiftToUser(
            ObjectId userId, Document config,
            int rank, String data, String quizName,
            ObjectId quizId
    ) {

        if (rank > 3 && !config.containsKey("forth_rank_cert_id"))
            return;

        Document user = userRepository.findById(userId);
        if (user == null)
            return;

        if (rank < 4) {
            if (
                    (config.containsKey("quiz_money") &&
                            config.getInteger("quiz_money") > 0)
            )
                user.put("money", ((Number) user.get("money")).doubleValue() + config.getInteger("quiz_money"));

            if (
                    (config.containsKey("quiz_coin") &&
                            config.getDouble("quiz_coin") > 0)
            )
                user.put("coin", ((Number) user.get("coin")).doubleValue() +
                        config.getDouble("quiz_coin"));

            userRepository.replaceOneWithoutClearCache(userId, user);
            new Thread(() -> {
                // todo: check badge
                PointController.addPointForAction(userId, Action.RANK_IN_QUIZ, quizId, null);
            }).start();
        }

        JSONArray params = new JSONArray();
        params.put(user.getString("first_name") + " " + user.getString("last_name"));
        params.put(quizName);
        params.put(data);
        params.put(rank + "");

        if (rank == 1 && config.containsKey("first_rank_cert_id"))
            addUserToCert(null, config.getObjectId("first_rank_cert_id"),
                    user.getString("NID"), params);

        if (rank == 2 && config.containsKey("second_rank_cert_id"))
            addUserToCert(null, config.getObjectId("second_rank_cert_id"),
                    user.getString("NID"), params);

        if (rank == 3 && config.containsKey("third_rank_cert_id"))
            addUserToCert(null, config.getObjectId("third_rank_cert_id"),
                    user.getString("NID"), params);

        if (rank == 4 && config.containsKey("forth_rank_cert_id"))
            addUserToCert(null, config.getObjectId("forth_rank_cert_id"),
                    user.getString("NID"), params);

        if (rank == 5 && config.containsKey("fifth_rank_cert_id"))
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

        QuizAbstract quizAbstract;

        if (db instanceof EscapeQuizRepository) {

            quizAbstract = new EscapeQuizController();

            for (Document doc : docs) {
                jsonArray.put(quizAbstract.convertDocToJSON(doc, true, isAdmin, false, true));
            }

            return generateSuccessMsg("data", new JSONObject()
                    .put("items", jsonArray)
            );
        } else {
            for (Document doc : docs) {

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

    }

    public static String getFinishedQuizzes() {

        ArrayList<Bson> filters = new ArrayList<>();
        long curr = System.currentTimeMillis();

        filters.add(eq("visibility", true));
        filters.add(lt("end", curr));
        filters.add(exists("report_status"));
        filters.add(exists("ranking_list"));
        filters.add(eq("report_status", "ready"));

        ArrayList<Document> docs = iryscQuizRepository.find(and(filters), QUIZ_DIGEST, Sorts.descending("created_at"));
        docs.addAll(escapeQuizRepository.find(and(
                eq("visibility", true),
                lt("end", curr),
                exists("ranking")
        ), null));

        docs.addAll(onlineStandQuizRepository.find(and(
                eq("visibility", true),
                lt("end", curr)
        ), null));

        long zero = 0;
        docs.sort((document, t1) -> ((long) document.getOrDefault("start", zero) - (long) t1.getOrDefault("start", zero)) > 0 ? 1 : -1);

        JSONArray jsonArray = new JSONArray();

        RegularQuizController regularQuizController = new RegularQuizController();
        TashrihiQuizController tashrihiQuizController = new TashrihiQuizController();
        EscapeQuizController escapeQuizController = new EscapeQuizController();
        OnlineStandingController onlineStandingController = new OnlineStandingController();

        QuizAbstract quizAbstract;

        for (int i = docs.size() - 1; i >= 0; i--) {

            Document doc = docs.get(i);

            if (doc.containsKey("max_teams"))
                quizAbstract = onlineStandingController;
            else if (!doc.containsKey("launch_mode") && !doc.containsKey("minus_mark"))
                quizAbstract = escapeQuizController;
            else if (doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName()))
                quizAbstract = regularQuizController;
            else
                quizAbstract = tashrihiQuizController;

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

            List<Number> marks = questions.containsKey("marks") ? questions.getList("marks", Number.class) : new ArrayList<>();

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
            return generateErr(x.getMessage());
        }
    }

    public static String setQuizAnswerSheet(Common db, ObjectId userId,
                                            ObjectId quizId, ObjectId studentId,
                                            MultipartFile file) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            if (!doc.getOrDefault("launch_mode", "online").toString().equalsIgnoreCase(LaunchMode.PHYSICAL.getName()) ||
                    !doc.getString("mode").equalsIgnoreCase(KindQuiz.REGULAR.getName())
            )
                return JSON_NOT_VALID_ID;

            List<Document> students = doc.getList("students", Document.class);
            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (student == null)
                return JSON_NOT_VALID_ID;

            String folder = db instanceof SchoolQuizRepository ? "school_quizzes/answer_sheets" : "answer_sheets";

            String filename = FileUtils.uploadFile(file, folder);
            if (filename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;

            student.put("answer_sheet", filename);
            db.replaceOne(quizId, doc);

            return generateSuccessMsg("file", STATICS_SERVER + folder + "/" + filename);

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

            if (db instanceof OpenQuizRepository)
                new RegularQuizController.Taraz(quiz, openQuizRepository);
            else if (db instanceof SchoolQuizRepository) {
                if (quiz.getBoolean("pdf_quiz", false))
                    new RegularQuizController.Taraz().PDFQuizTaraz(quiz, schoolQuizRepository);
                else
                    new RegularQuizController.Taraz(quiz, schoolQuizRepository);
            } else if (quiz.getOrDefault("mode", "regular").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName())) {

                new TashrihiQuizController().createTaraz(quiz);

                new Thread(() -> {

                    if (DEV_MODE)
                        return;

                    ArrayList<ObjectId> userIds = new ArrayList<>();

                    for (Document doc : quiz.getList("students", Document.class))
                        userIds.add(doc.getObjectId("_id"));

                    List<Document> students = userRepository.findByIds(userIds, false);

                    String prefix = quiz.getString("title") + "_" + SERVER + "result/irysc/" + quiz.getObjectId("_id").toString() + "/";

                    for (Document student : students) {

                        if (!student.containsKey("mail"))
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
            } else {
                new RegularQuizController().createTaraz(quiz);

                new Thread(() -> {

                    ArrayList<ObjectId> userIds = new ArrayList<>();

                    for (Document doc : quiz.getList("students", Document.class))
                        userIds.add(doc.getObjectId("_id"));

                    List<Document> students = userRepository.findByIds(userIds, false);

                    String prefix = quiz.getString("title") + "_" + SERVER + "result/irysc/" + quiz.getObjectId("_id").toString() + "/";

                    for (Document student : students) {

                        if (!student.containsKey("mail"))
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

            if (student == null)
                return JSON_NOT_VALID_ID;

            student.put("start_at", null);
            student.put("finish_at", null);

            db.replaceOne(quizId, quiz);
            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeQuestions(Common db, ObjectId quizId, JSONArray jsonArray) {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (db instanceof IRYSCQuizRepository) {

//            if (quiz.containsKey("start") &&
//                    quiz.getLong("start") < System.currentTimeMillis()
//            )
//                return generateErr("زمان آزمون موردنظر رسیده است و امکان حذف سوال از آزمون وجود ندارد.");

        } else if (db instanceof OpenQuizRepository) {

            if (quiz.getList("students", Document.class).size() > 0)
                return generateErr("دانش آموز/دانش آموزانی در این آزمون شرکت کرده اند و امکان حذف سوال وجود ندارد.");

        }

        JSONArray removeIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        Document questions = quiz.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);
        List<Double> marks = questions.getList("marks", Double.class);
        List<ObjectId> removed = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId qId = new ObjectId(id);
            if (!questionIds.contains(qId)) {
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

        if (removeIds.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        List<ObjectId> newQuestionsIds = new ArrayList<>();
        List<Double> newMarks = new ArrayList<>();

        int idx = 0;

        for (ObjectId qId : questionIds) {

            if (removed.contains(qId)) {
                idx++;
                continue;
            }

            newQuestionsIds.add(qId);
            newMarks.add(marks.get(idx));
            idx++;
        }

        questions.put("marks", newMarks);
        questions.put("_ids", newQuestionsIds);

        if (db instanceof SchoolQuizRepository &&
                !(boolean) quiz.getOrDefault("database", true)
        )
            questions.put("answers",
                    Utility.getSchoolAnswersByteArr(newQuestionsIds)
            );
        else
            questions.put("answers",
                    Utility.getAnswersByteArr(newQuestionsIds)
            );

        quiz.put("questions", questions);
        db.replaceOne(quizId, quiz);

        return irysc.gachesefid.Utility.Utility.returnRemoveResponse(
                excepts, removeIds
        );
    }

    public static void calcQuestionsAgain(ObjectId quizId) {
        Document quiz = iryscQuizRepository.findById(quizId);
        if(quiz == null)
            return;

        Document questions = quiz.get("questions", Document.class);
        questions.put("answers",
                Utility.getAnswersByteArr(questions.getList("_ids", ObjectId.class))
        );

        quiz.put("questions", questions);
        iryscQuizRepository.replaceOne(quizId, quiz);
    }

    public static String rates(Common db, ObjectId quizId) {

        Document quiz = db.findById(quizId);

        if (quiz == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = quiz.getList("students", Document.class);
        JSONArray data = new JSONArray();
        long curr = System.currentTimeMillis();

        for (Document user : users) {

            if (!user.containsKey("rate"))
                continue;

            Document std = userRepository.findById(user.getObjectId("_id"));
            if (std == null)
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("rate", user.get("rate"))
                    .put("rateAt", getSolarDate((Long) user.getOrDefault("rate_at", curr)));

            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
            data.put(jsonObject);
        }

        return generateSuccessMsg("data", data);
    }

    private static class SchoolRecpRow {

        String level;
        String subject;
        int price;
        int count;

        public SchoolRecpRow(String level, String subject, int price) {
            this.level = level;
            this.subject = subject;
            this.price = price;
            this.count = 1;
        }

        public JSONObject toJSON() {
            return new JSONObject()
                    .put("level", level.equals("hard") ? "دشوار" :
                            level.equals("mid") ? "متوسط" : "آسان")
                    .put("subject", subject)
                    .put("price", price)
                    .put("totalPrice", price * count)
                    .put("count", count);
        }

        @Override
        public boolean equals(Object o) {

            if (o instanceof SchoolRecpRow) {
                SchoolRecpRow schoolRecpRow = (SchoolRecpRow) o;
                return schoolRecpRow.level.equals(level) && schoolRecpRow.subject.equals(subject);
            }

            return false;
        }

        public void inc() {
            this.count++;
        }
    }

}
