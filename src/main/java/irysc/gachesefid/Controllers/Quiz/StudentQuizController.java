package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.OpenQuizRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Finance.TransactionController.fetchQuizInvoice;
import static irysc.gachesefid.Controllers.Question.Utilities.fetchFilter;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.uploadDir;
import static irysc.gachesefid.Utility.FileUtils.uploadDir_dev;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentQuizController {

    public static String getMyRecpForCustomQuiz(ObjectId quizId, ObjectId userId) {

        try {
            Document quiz = customQuizRepository.findById(quizId);
            if (quiz == null)
                return JSON_NOT_VALID_ID;

            if (!quiz.getObjectId("user_id").equals(userId) ||
                    quiz.getString("status").equalsIgnoreCase("wait")
            )
                return JSON_NOT_ACCESS;

            Document transaction = transactionRepository.findOne(
                    and(
                            eq("status", "success"),
                            eq("section", OffCodeSections.BANK_EXAM.getName()),
                            eq("user_id", userId),
                            eq("products", quizId)
                    ), new BasicDBObject("ref_id", 1).append("amount", 1)
                            .append("account_money", 1).append("off_amount", 1)
                            .append("created_at", 1)
            );

            if (transaction == null)
                return JSON_NOT_VALID_ID;

            String section = GiftController.translateUseFor(OffCodeSections.BANK_EXAM.getName()) + " - " + "خرید " +
                    quiz.getList("questions", ObjectId.class).size() +
                    " سوال ";

            JSONObject jsonObject = new JSONObject()
                    .put("paid", transaction.getInteger("amount"))
                    .put("account", transaction.getOrDefault("account", 0))
                    .put("offAmount", transaction.getOrDefault("off_amount", 0))
                    .put("createdAt", getSolarDate(transaction.getLong("created_at")))
                    .put("for", section);

            if (transaction.containsKey("ref_id") && transaction.get("ref_id") != null)
                jsonObject.put("refId", transaction.get("ref_id"));

            return generateSuccessMsg("data", jsonObject);
        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    public static String getMyRecp(Common db, ObjectId quizId, ObjectId userId) {

        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);

            Document std = searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class),
                    "_id", userId
            );

            String section = OffCodeSections.GACH_EXAM.getName();

            Document transaction = transactionRepository.findOne(
                    and(
                            eq("status", "success"),
                            eq("section", section),
                            eq("user_id", userId),
                            or(
                                    in("products", quizId),
                                    eq("products", quizId)
                            )
                    ), null
            );

            StringBuilder sectionFa = new StringBuilder(GiftController.translateUseFor(section));

            if (transaction != null)
                fetchQuizInvoice(sectionFa, transaction);
            else
                sectionFa.append(" - ").append(quiz.getString("title"));

            JSONObject jsonObject = new JSONObject()
                    .put("paid", transaction == null ? 0 : transaction.getOrDefault("amount", 0))
                    .put("account", transaction == null ? 0 : transaction.getOrDefault("account_money", 0))
                    .put("offAmount", transaction == null ? 0 : transaction.getOrDefault("off_amount", 0))
                    .put("createdAt", transaction == null ? getSolarDate(std.getLong("register_at")) :
                            getSolarDate(transaction.getLong("created_at"))
                    )
                    .put("for", sectionFa.toString());

            if (transaction != null && transaction.containsKey("ref_id"))
                jsonObject.put("refId", transaction.get("ref_id"));

            return generateSuccessMsg("data", jsonObject);
        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    public static String reviewQuiz(Common db, ObjectId quizId,
                                    ObjectId userId, boolean isStudent
    ) {
        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);

            if (!(boolean) quiz.getOrDefault("show_results_after_correction", true))
                return generateErr("زمان رویت نتایج فرانرسیده است.");

            long curr = System.currentTimeMillis();

            if (isStudent && quiz.containsKey("end") &&
                    quiz.getLong("end") > curr
            )
                return JSON_NOT_ACCESS;

            Document stdDoc = null;

            int neededTime = new RegularQuizController().calcLen(quiz);

            if (isStudent) {
                stdDoc = searchInDocumentsKeyVal(
                        quiz.getList("students", Document.class),
                        "_id", userId
                );

                if (db instanceof OpenQuizRepository) {

                    long startAt = stdDoc.getLong("start_at");

                    int reminder = neededTime -
                            (int) ((curr - startAt) / 1000);

                    if (reminder > 0)
                        return generateErr("هنوز زمان مشاهده نتایج فرا نرسیده است.");
                }
            }

            Document questions =
                    quiz.get("questions", Document.class);

            int qNo = 0;

            if (questions.containsKey("_ids"))
                qNo = questions.getList("_ids", ObjectId.class).size();

            List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", quiz.getString("title"))
                    .put("questionsNo", qNo)
                    .put("description", quiz.getOrDefault("desc", ""))
                    .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                    .put("mode", quiz.getString("mode"))
                    .put("attaches", jsonArray)
                    .put("duration", neededTime);

            if (quiz.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                return returnTashrihiQuiz(quiz, stdDoc.getList("answers", Document.class), quizJSON, true, db);

            return returnQuiz(quiz, stdDoc, true, quizJSON);
        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }
    }


    public static String rate(Common db, ObjectId quizId,
                              ObjectId userId, int rate
    ) {
        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);

            Document stdDoc = searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class),
                    "_id", userId
            );

            int oldRate = (int) stdDoc.getOrDefault("rate", 0);
            stdDoc.put("rate", rate);
            stdDoc.put("rate_at", System.currentTimeMillis());

            double oldTotalRate = (double) quiz.getOrDefault("rate", (double) 0);
            int rateCount = (int) quiz.getOrDefault("rate_count", 0);

            oldTotalRate *= rateCount;

            if (oldRate == 0)
                rateCount++;

            oldTotalRate -= oldRate;
            oldTotalRate += rate;

            quiz.put("rate", Math.round(oldTotalRate / rateCount * 100.0) / 100.0);
            quiz.put("rate_count", rateCount);

            db.replaceOne(quizId, quiz);

            return generateSuccessMsg("data", quiz.get("rate"));
        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }
    }

    public static String reviewCustomQuiz(ObjectId quizId, ObjectId userId) {

        Document doc = customQuizRepository.findOne(
                and(
                        eq("_id", quizId),
                        eq("user_id", userId),
                        ne("status", "wait")
                ), null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        if (!doc.containsKey("start_at") || doc.get("start_at") == null)
            return JSON_NOT_ACCESS;

        int neededTime = doc.getInteger("duration");
        long curr = System.currentTimeMillis();

        int untilYetInSecondFormat =
                (int) ((curr - doc.getLong("start_at")) / 1000);

        int reminder = neededTime - untilYetInSecondFormat;

        if (reminder > 0)
            return generateErr("زمان مرور آزمون هنوز فرانرسیده است.");

        List<ObjectId> questionIds = doc.getList("questions", ObjectId.class);

        JSONObject quizJSON = new JSONObject()
                .put("title", doc.getString("name"))
                .put("questionsNo", questionIds.size())
                .put("description", "")
                .put("mode", "regular")
                .put("duration", neededTime);

        if (!doc.getString("status").equalsIgnoreCase("finished")) {
            doc.put("status", "finished");

            ArrayList<PairValue> studentAnswers = Utility.getAnswers(
                    doc.get("student_answers", Binary.class).getData()
            );

            ArrayList<Document> questions = questionRepository.findByIds(
                    questionIds, true
            );

            if (questions == null)
                return JSON_NOT_UNKNOWN;

            RegularQuizController.Taraz t = new RegularQuizController.Taraz(
                    questions, userId, studentAnswers
            );

            doc.put("lessons", t.lessonsStatOutput);
            doc.put("subjects", t.subjectsStatOutput);
            customQuizRepository.replaceOne(quizId, doc);

            Utilities.updateQuestionsStatWithByteArr(
                    questions, t.questionStats
            );

            int i = 1;
            for (Document question : questions)
                question.put("no", i++);

            return returnCustomQuiz(questions, studentAnswers, quizJSON, true);

        }

        return returnCustomQuiz(doc, quizJSON, true);
    }

    public static String myQuizzes(Document user,
                                   String generalMode,
                                   String status) {

        if (generalMode != null &&
                !EnumValidatorImp.isValid(generalMode, GeneralKindQuiz.class))
            return JSON_NOT_VALID_PARAMS;

        List<String> accesses = user.getList("accesses", String.class);
        ObjectId userId = user.getObjectId("_id");

        boolean isSchool = Authorization.isSchool(accesses);
        if (isSchool && !user.containsKey("students"))
            return generateSuccessMsg("data", new ArrayList<>());

        JSONArray data = new JSONArray();
        ArrayList<Bson> filters = new ArrayList<>();
        if (isSchool)
            filters.add(in("students._id", user.getList("students", ObjectId.class)));
        else
            filters.add(in("students._id", userId));

        long curr = System.currentTimeMillis();

        if (status != null) {

            if (status.equalsIgnoreCase("finished"))
                filters.add(lt("end", curr));
            else if (status.equalsIgnoreCase("inProgress"))
                filters.add(
                        and(
                                lte("start", curr),
                                gt("end", curr)
                        )
                );
            else
                filters.add(gt("start", curr));
        }

        if (generalMode == null ||
                generalMode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName())
        ) {
            ArrayList<Document> quizzes = iryscQuizRepository.find(and(filters), null);

            if (generalMode == null)
                quizzes.addAll(openQuizRepository.find(and(filters), null));

            QuizAbstract regularQuizController = new RegularQuizController();
            QuizAbstract tashrihiQuizController = new TashrihiQuizController();
            QuizAbstract openQuizAbstract = new OpenQuiz();

            for (Document quiz : quizzes) {

                boolean isIRYSCQuiz = quiz.containsKey("launch_mode") ||
                        quiz.getOrDefault("mode", "").toString().equalsIgnoreCase(KindQuiz.TASHRIHI.getName());

                QuizAbstract quizAbstract = isIRYSCQuiz ?
                        quiz.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()) ?
                                tashrihiQuizController :
                                regularQuizController : openQuizAbstract;

                if (isSchool) {
                    data.put(quizAbstract.convertDocToJSON(
                            quiz, true, false, true, true
                    ));
                } else {

                    Document studentDoc = searchInDocumentsKeyVal(
                            quiz.getList("students", Document.class),
                            "_id", userId
                    );

                    if (studentDoc == null)
                        continue;

                    JSONObject jsonObject = quizAbstract.convertDocToJSON(
                            quiz, true, false, true, true
                    );

                    if (jsonObject.getString("status")
                            .equalsIgnoreCase("inProgress") &&
                            studentDoc.containsKey("start_at") &&
                            studentDoc.get("start_at") != null
                    ) {
                        int neededTime = quizAbstract.calcLen(quiz);
                        int untilYetInSecondFormat =
                                (int) ((curr - studentDoc.getLong("start_at")) / 1000);

                        int reminder = neededTime - untilYetInSecondFormat;

                        if (reminder < 0)
                            jsonObject.put("status", isIRYSCQuiz ? "waitForResult" : "finished");
                        else {
                            jsonObject.put("timeReminder", reminder);
                            if (!isIRYSCQuiz)
                                jsonObject.put("status", "continue");
                        }
                    }

                    if (studentDoc.containsKey("rate"))
                        jsonObject.put("stdRate", studentDoc.getInteger("rate"));

                    data.put(jsonObject);
                }
            }

        }

        return generateSuccessMsg("data", data);
    }

    public static String myCustomQuizzes(ObjectId userId) {

        JSONArray data = new JSONArray();

        long curr = System.currentTimeMillis();
        ArrayList<Document> quizzes = customQuizRepository.find(
                and(
                        eq("user_id", userId),
                        ne("status", "wait")
                ), null);

        for (Document quiz : quizzes) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", quiz.getObjectId("_id").toString())
                    .put("title", quiz.getString("name"));

            int neededTime = quiz.getInteger("duration");

            jsonObject.put("duration", neededTime);

            if (quiz.getString("status").equals("finished")) {
                jsonObject.put("status", "finished");
            } else {
                if (quiz.containsKey("start_at") &&
                        quiz.get("start_at") != null) {

                    int untilYetInSecondFormat =
                            (int) ((curr - quiz.getLong("start_at")) / 1000);
                    int reminder = neededTime - untilYetInSecondFormat;

                    if (reminder <= 0)
                        jsonObject.put("status", "finished");
                    else {
                        jsonObject.put("status", "continue");
                        jsonObject.put("timeReminder", reminder);
                    }

                } else
                    jsonObject.put("status", "inProgress");
            }

            jsonObject.put("questionsCount", quiz.getList("questions", ObjectId.class).size());
            jsonObject.put("createdAt",
                    getSolarDate(quiz.getLong("created_at"))
            );

            data.put(jsonObject);
        }

        return generateSuccessMsg("data", data);
    }

    public static String launchCustom(ObjectId quizId, ObjectId studentId) {

        Document doc = customQuizRepository.findOne(
                and(
                        eq("_id", quizId),
                        eq("user_id", studentId),
                        ne("status", "wait"),
                        ne("status", "finished")
                ), null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        int neededTime = doc.getInteger("duration");
        long curr = System.currentTimeMillis();

        if (doc.containsKey("start_at") &&
                doc.get("start_at") != null) {

            int untilYetInSecondFormat =
                    (int) ((curr - doc.getLong("start_at")) / 1000);

            if (untilYetInSecondFormat > neededTime)
                return generateErr("زمان ارزیابی موردنظر گذشته است.");
        }

        if (!doc.containsKey("start_at") || doc.get("start_at") == null) {
            doc.put("start_at", curr);
            customQuizRepository.replaceOne(quizId, doc);
        }

        int reminder = neededTime -
                (int) ((curr - doc.getLong("start_at")) / 1000);

        JSONObject quizJSON = new JSONObject()
                .put("title", doc.getString("name"))
                .put("id", doc.getObjectId("_id").toString())
                .put("generalMode", "custom")
                .put("questionsNo", doc.getList("questions", ObjectId.class).size())
                .put("description", "")
                .put("mode", "regular")
                .put("attaches", new JSONArray())
                .put("refresh", 2)
                .put("duration", neededTime)
                .put("reminder", reminder)
                .put("isNewPerson", doc.getLong("start_at") == curr);

        return returnCustomQuiz(doc, quizJSON, false);
    }

    public static File getMyQuestionPDF(Common db, ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);
            File f;

            String folder = db instanceof IRYSCQuizRepository ?
                    IRYSCQuizRepository.FOLDER + "/" :
                    SchoolQuizRepository.FOLDER + "/";

            if (quiz.containsKey("generated_pdf"))
                f = new File(quiz.getString("generated_pdf"));
            else {

                f = new File(DEV_MODE ?
                        FileUtils.uploadDir_dev + folder + quiz.getObjectId("_id").toString() + ".pdf" :
                        FileUtils.uploadDir + folder + quiz.getObjectId("_id").toString() + ".pdf"
                );

                quiz.put("generated_pdf", STATICS_SERVER + folder + quiz.getObjectId("_id").toString() + ".pdf");
            }

            if (!f.exists()) {

                String prefix = DEV_MODE ? uploadDir_dev : uploadDir;

                f = QuizController.doGenerateQuestionPDF(quiz, prefix + folder, null);
                if (f == null)
                    return null;
            }

            return f;
        } catch (Exception x) {
            return null;
        }
    }

    public static String launch(Common db, ObjectId quizId,
                                ObjectId studentId) {

        try {
            A a = checkStoreAnswer(db, studentId, quizId, false);

            long curr = System.currentTimeMillis();

            if (a.needUpdate)
                a.student.put("start_at", a.startAt);

            a.student.put("finish_at", curr);
            db.replaceOne(quizId, a.quiz);

            List<String> attaches = (List<String>) a.quiz.getOrDefault("attaches", new ArrayList<>());
            JSONArray jsonArray = new JSONArray();

            for (String attach : attaches)
                jsonArray.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);

            JSONObject quizJSON = new JSONObject()
                    .put("title", a.quiz.getString("title"))
                    .put("id", a.quiz.getObjectId("_id").toString())
                    .put("generalMode",
                            db instanceof IRYSCQuizRepository ? AllKindQuiz.IRYSC.getName() :
                                    db instanceof OpenQuizRepository ? AllKindQuiz.OPEN.getName() : "school")
                    .put("questionsNo", a.quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", a.quiz.getOrDefault("desc", ""))
                    .put("mode", a.quiz.getString("mode"))
                    .put("attaches", jsonArray)
                    .put("refresh", 3) //Math.abs(new Random().nextInt(5)) + 5
                    .put("duration", a.neededTime)
                    .put("reminder", a.reminder)
                    .put("isQRNeeded", a.quiz.getOrDefault("is_q_r_needed", false))
                    .put("isNewPerson", !a.student.containsKey("start_at") ||
                            a.student.get("start_at") == null ||
                            a.student.getLong("start_at") == curr
                    );

            if (a.quiz.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName())) {

                if ((boolean) a.quiz.getOrDefault("is_q_r_needed", false)) {

                    return generateSuccessMsg("data", new JSONObject()
                            .put("answerSheets", a.student.containsKey("ans_files") ?
                                    returnMySubmits(db, a.student) :
                                    new JSONArray()
                            )
                            .put("quizInfo", quizJSON)
                    );
                }

                return returnTashrihiQuiz(a.quiz, a.student.getList("answers",
                        Document.class), quizJSON, false, db);
            }

            return returnQuiz(a.quiz, a.student, false, quizJSON);

        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    private static JSONArray returnMySubmits(Common db, Document student) {

        JSONArray tmp = new JSONArray();
        String folder = db instanceof IRYSCQuizRepository ?
                IRYSCQuizRepository.FOLDER + "/studentAnswers/" :
                SchoolQuizRepository.FOLDER + "/studentAnswers/";

        List<Document> ansFiles = student.getList("ans_files", Document.class);
        boolean lastAccepted = false;

        for (int z = ansFiles.size() - 1; z >= 0; z--) {

            Document ansFile = ansFiles.get(z);

            JSONObject jsonObject = new JSONObject()
                    .put("stdAns", STATICS_SERVER + folder + ansFile.getString("filename"))
                    .put("status", ansFile.getString("status"))
                    .put("createdAt", getSolarDate(ansFile.getLong("created_at")))
                    .put("responseAt", ansFile.containsKey("response_at") ? getSolarDate(ansFile.getLong("response_at")) : "")
                    .put("msg", ansFile.getOrDefault("msg", ""));

            if (!lastAccepted && ansFile.getString("status").equals("accepted")) {
                jsonObject.put("main", true);
                lastAccepted = true;
            }

            tmp.put(jsonObject);
        }

        return tmp;
    }

    public static String getMySubmits(Common db, ObjectId quizId,
                                      ObjectId studentId) {

        if (db == null)
            return JSON_NOT_VALID_PARAMS;

        try {

            Document doc = hasProtectedAccess(db, studentId, quizId);

            if (!doc.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                return JSON_NOT_ACCESS;

            List<Document> students = doc.getList("students", Document.class);

            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (!(boolean) doc.getOrDefault("is_q_r_needed", false))
                return JSON_NOT_ACCESS;

            return generateSuccessMsg("data", student.containsKey("ans_files") ?
                    returnMySubmits(db, student) :
                    new JSONArray());
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String storeCustomAnswers(ObjectId quizId, ObjectId studentId,
                                            JSONArray answers) {

        Document doc = customQuizRepository.findOne(
                and(
                        eq("_id", quizId),
                        eq("user_id", studentId),
                        ne("status", "wait"),
                        ne("status", "finished")
                ), null
        );

        if (doc == null)
            return JSON_NOT_ACCESS;

        if (!doc.containsKey("start_at") || doc.get("start_at") == null)
            return JSON_NOT_ACCESS;

        int neededTime = doc.getInteger("duration");
        long curr = System.currentTimeMillis();

        int untilYetInSecondFormat =
                (int) ((curr - doc.getLong("start_at")) / 1000);

        int reminder = neededTime - untilYetInSecondFormat;

        if (reminder < 0)
            return generateErr("زمان ارزیابی موردنظر گذشته است.");

        String result = saveStudentAnswers(doc, answers, null, customQuizRepository);
        if (result.contains("nok"))
            return result;

        return generateSuccessMsg("reminder", reminder,
                new PairValue("refresh", 3)
        );
    }

    private static A checkStoreAnswer(Common db, ObjectId studentId,
                                      ObjectId quizId, boolean allowDelay
    ) throws InvalidFieldsException {

        long allowedDelay = allowDelay ? 3600000 : 0; // 1hour

        Document doc = hasProtectedAccess(db, studentId, quizId);

        long end = doc.containsKey("end") ?
                doc.getLong("end") + allowedDelay : -1;

        long curr = System.currentTimeMillis();

        if (doc.containsKey("start") &&
                (
                        doc.getLong("start") > curr ||
                                end < curr
                )
        )
            throw new InvalidFieldsException("در زمان ارزیابی قرار نداریم.");

        List<Document> students = doc.getList("students", Document.class);

        Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                students, "_id", studentId
        );

        if ((boolean) doc.getOrDefault("is_q_r_needed", false))
            return new A(doc, -1, student, -1, -1);

        int neededTime = new RegularQuizController().calcLen(doc);

        long startAt = student.containsKey("start_at") && student.get("start_at") != null ?
                student.getLong("start_at") : curr;

        int delay = doc.containsKey("end") ?
                (startAt + neededTime * 1000L - end) > 0 ? Math.max(
                        0,
                        (int) (startAt + neededTime * 1000L - end) / 1000
                ) : 0 : 0;

        int reminder = neededTime -
                (int) ((curr - startAt) / 1000) - delay;

        if (reminder + allowedDelay / 1000 <= 0)
            throw new InvalidFieldsException("شما در این آزمون شرکت کرده اید.");

        A a = new A(doc, reminder, student, startAt, neededTime);
        if (student.getOrDefault("start_at", null) == null)
            a.setNeedUpdate();

        return a;
    }

    public static String storeAnswers(Common db, ObjectId quizId,
                                      ObjectId studentId, JSONArray answers) {

        try {
            A a = checkStoreAnswer(db, studentId, quizId, true);
            long curr = System.currentTimeMillis();

//            int untilYetInSecondFormat = (int) ((curr - student.getLong("start_at")) / 1000);
//            if(untilYetInSecondFormat > neededTime)
//                return generateErr("شما در این آزمون شرکت کرده اید.");

            a.student.put("finish_at", curr);
            if (db instanceof OpenQuizRepository)
                a.quiz.put("last_finished_at", curr);

            String result;

            if (a.quiz.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                result = saveStudentTashrihiAnswers(a.quiz, answers,
                        a.student.getList("answers", Document.class), db
                );
            else
                result = saveStudentAnswers(a.quiz, answers, a.student, db);

            if (result.contains("nok"))
                return result;

            return generateSuccessMsg("reminder", a.reminder,
                    new PairValue("refresh", Math.abs(new Random().nextInt(3)) + 3)
            );

        } catch (Exception x) {
            x.printStackTrace();
            return generateErr(x.getMessage());
        }
    }

    public static String uploadAnswers(Common db, ObjectId quizId, ObjectId questionId,
                                       ObjectId studentId, MultipartFile file) {

        try {

            A a = checkStoreAnswer(db, studentId, quizId, true);

            if (!a.quiz.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                return JSON_NOT_ACCESS;

            long curr = System.currentTimeMillis();

            a.student.put("finish_at", curr);
            if (db instanceof OpenQuizRepository)
                a.quiz.put("last_finished_at", curr);

            String url = saveStudentTashrihiAnswers(a.quiz, questionId,
                    a.student.getList("answers", Document.class), db, file
            );

            return generateSuccessMsg("data", new JSONObject()
                    .put("reminder", a.reminder)
                    .put("url", url)
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String buy(ObjectId userId, ObjectId packageId,
                             JSONArray ids, JSONArray studentIds,
                             double money, String phone, String mail,
                             String name, String offcode) {

        Document quizPackage = null;
        Document off = null;
        long curr = System.currentTimeMillis();

        if (packageId != null) {
            quizPackage = packageRepository.findById(packageId);
            if (quizPackage == null || quizPackage.getLong("expire_at") < curr)
                return JSON_NOT_VALID_PARAMS;
        }

        if (offcode != null) {

            off = validateOffCode(
                    offcode, userId, curr,
                    OffCodeSections.GACH_EXAM.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");
        }

        ArrayList<ObjectId> quizIds = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {

            if (!ObjectId.isValid(ids.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            ObjectId quizId = new ObjectId(ids.getString(i));
            if (quizPackage != null &&
                    !quizPackage.getList("quizzes", ObjectId.class).contains(quizId))
                return JSON_NOT_VALID_PARAMS;

            quizIds.add(quizId);
        }

        ArrayList<Document> quizzes = iryscQuizRepository.find(
                and(
                        in("_id", quizIds),
                        eq("visibility", true),
                        lt("start_registry", curr),
                        nin("students._id", userId),
                        or(
                                exists("end_registry", false),
                                gt("end_registry", curr)
                        )
                ), null
        );


        ArrayList<Document> openQuizzes = openQuizRepository.find(
                and(
                        in("_id", quizIds),
                        nin("students._id", userId)
                ), null
        );


        if (quizzes.size() + openQuizzes.size() != quizIds.size())
            return JSON_NOT_VALID_PARAMS;

        if (studentIds != null && openQuizzes.size() > 0)
            return JSON_NOT_VALID_PARAMS;

        if (packageId != null && openQuizzes.size() > 0)
            return JSON_NOT_VALID_PARAMS;

        if (studentIds != null) {
            Document school = schoolRepository.findOne(eq("user_id", userId), JUST_ID);
            if (school == null)
                return JSON_NOT_ACCESS;

            ObjectId schoolId = school.getObjectId("_id");
            ArrayList<ObjectId> studentOIds = new ArrayList<>();

            for (int i = 0; i < studentIds.length(); i++) {

                if (!ObjectId.isValid(studentIds.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

                ObjectId studentId = new ObjectId(studentIds.getString(i));
                Document user = userRepository.findById(studentId);
                if (user == null || !user.containsKey("school") ||
                        !user.get("school", Document.class).get("_id").equals(schoolId)
                )
                    return JSON_NOT_ACCESS;

                studentOIds.add(studentId);
            }

            return doBuy(userId, phone, mail, name, money,
                    quizPackage, off, quizzes, null, studentOIds
            );
        }

        return doBuy(userId, phone, mail, name, money,
                quizPackage, off, quizzes, openQuizzes, null
        );
    }

    private static String doBuy(ObjectId studentId,
                                String phone,
                                String mail,
                                String stdName,
                                double money,
                                Document quizPackage,
                                Document off,
                                ArrayList<Document> quizzes,
                                ArrayList<Document> openQuizzes,
                                ArrayList<ObjectId> studentIds
    ) {

        long curr = System.currentTimeMillis();
        int totalPrice = 0;

        if (studentIds == null) {
            for (Document quiz : quizzes)
                totalPrice += quiz.getInteger("price");

            for (Document quiz : openQuizzes)
                totalPrice += quiz.getInteger("price");
        } else
            for (Document quiz : quizzes)
                totalPrice += quiz.getInteger("price") * studentIds.size();

        if (off == null)
            off = findAccountOff(
                    studentId, curr, OffCodeSections.GACH_EXAM.getName()
            );

        int packageOff = 0;
        boolean usePackageOff = false;

        if (quizPackage != null) {
            if (quizzes.size() >= quizPackage.getInteger("min_select")) {
                packageOff = quizPackage.getInteger("off_percent");
                usePackageOff = true;
            }
        }

        double offAmount = totalPrice * packageOff / 100.0;
        double shouldPayDouble = totalPrice - offAmount;

        if (studentIds != null) {
            int groupRegistrationOff = irysc.gachesefid.Utility.Utility.getConfig().getInteger("school_off_percent");
            offAmount += shouldPayDouble * groupRegistrationOff / 100.0;
            shouldPayDouble = totalPrice - offAmount;
        }

        if (off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPayDouble * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPayDouble = totalPrice - offAmount;
        }

        int shouldPay = (int) shouldPayDouble;

        ArrayList<ObjectId> quizIds = new ArrayList<>();
        for (Document quiz : quizzes)
            quizIds.add(quiz.getObjectId("_id"));

        ArrayList<ObjectId> openQuizIds = new ArrayList<>();
        for (Document quiz : openQuizzes)
            openQuizIds.add(quiz.getObjectId("_id"));

        ArrayList<ObjectId> allQuizzesIds = quizIds;
        allQuizzesIds.addAll(openQuizIds);

        if (shouldPay - money <= 100) {

            double newUserMoney = money;

            if (shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, money);
                Document user = userRepository.findById(studentId);
                user.put("money", newUserMoney);
                userRepository.replaceOne(studentId, user);
            }

            Document finalOff = off;
            boolean finalUsePackageOff = usePackageOff;
            double finalOffAmount = offAmount;
            new Thread(() -> {

                Document doc = new Document("user_id", studentId)
                        .append("amount", 0)
                        .append("account_money", shouldPay)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.GACH_EXAM.getName())
                        .append("products", allQuizzesIds);

                if (finalUsePackageOff)
                    doc.put("package_id", quizPackage.getObjectId("_id"));

                if (studentIds != null)
                    doc.append("student_ids", studentIds);

                if (finalOff != null) {
                    doc.append("off_code", finalOff.getObjectId("_id"));
                    doc.append("off_amount", (int) finalOffAmount);
                }

                transactionRepository.insertOne(doc);

                if (studentIds != null) {

                    new RegularQuizController()
                            .registry(studentIds, phone, mail, quizIds, 0);

                    //todo: group registration for tashrihi quiz

                } else {

                    new TashrihiQuizController()
                            .registry(studentId, phone, mail, quizIds, 0, doc.getObjectId("_id"), stdName);

                    new RegularQuizController()
                            .registry(studentId, phone, mail, quizIds, 0, doc.getObjectId("_id"), stdName);

                    new OpenQuiz()
                            .registry(studentId, phone, mail, openQuizIds, 0, doc.getObjectId("_id"), stdName);
                }

                if (finalOff != null) {

                    BasicDBObject update;

                    if (finalOff.containsKey("is_public") &&
                            finalOff.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                        students.add(studentId);
                        update = new BasicDBObject("students", students);
                    } else {

                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.GACH_EXAM.getName())
                                .append("used_for", allQuizzesIds);
                    }

                    offcodeRepository.updateOne(
                            finalOff.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }

                if (finalUsePackageOff) {
                    quizPackage.put("buyers", quizPackage.getInteger("buyers") + 1);
                    packageRepository.replaceOne(
                            quizPackage.getObjectId("_id"), quizPackage
                    );
                }

            }).start();

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", newUserMoney)
            );
        }

        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document doc =
                new Document("user_id", studentId)
                        .append("account_money", money)
                        .append("amount", (int) (shouldPay - money))
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", allQuizzesIds)
                        .append("section", OffCodeSections.GACH_EXAM.getName());

        if (studentIds != null)
            doc.append("student_ids", studentIds);

        if (quizPackage != null)
            doc.put("package_id", quizPackage.getObjectId("_id"));

        if (off != null) {
            doc.append("off_code", off.getObjectId("_id"));
            doc.append("off_amount", (int) offAmount);
        }

        return goToPayment((int) (shouldPay - money), doc);
    }

    public static String returnTashrihiQuiz(Document quiz, List<Document> stdAnswers,
                                            JSONObject quizJSON, boolean isStatNeeded,
                                            Common db) {

        Document questionsDoc = quiz.get("questions", Document.class);

        ArrayList<Document> questionsList = new ArrayList<>();
        List<ObjectId> questions = (List<ObjectId>) questionsDoc.getOrDefault(
                "_ids", new ArrayList<ObjectId>()
        );
        List<Double> questionsMark = (List<Double>) questionsDoc.getOrDefault(
                "marks", new ArrayList<Double>()
        );
        List<Boolean> uploadableList = (List<Boolean>) questionsDoc.getOrDefault(
                "uploadable_list", new ArrayList<Double>()
        );

        if (questionsMark.size() != questions.size())
            return JSON_NOT_UNKNOWN;

        if (questionsMark.size() != uploadableList.size())
            return JSON_NOT_UNKNOWN;

        int i = 0;
        for (ObjectId itr : questions) {

            Document question = questionRepository.findById(itr);

            if (question == null) {
                i++;
                continue;
            }

            questionsList.add(Document.parse(question.toJson())
                    .append("no", i + 1).append("mark", questionsMark.get(i))
                    .append("can_upload", uploadableList.get(i))
            );
            i++;
        }

        i = 0;

        for (Document question : questionsList) {

            int idx = searchInDocumentsKeyValIdx(stdAnswers, "question_id", questions.get(i));
            if (idx == -1) {
                question.put("stdAns", "");
                question.put("stdMark", 0);
            } else {

                if (uploadableList.get(i)) {

                    String folder = db instanceof IRYSCQuizRepository ?
                            IRYSCQuizRepository.FOLDER + "/studentAnswers/" :
                            SchoolQuizRepository.FOLDER + "/studentAnswers/";

                    question.put("stdAns", STATICS_SERVER + folder + stdAnswers.get(idx).getString("answer"));
                } else
                    question.put("stdAns", stdAnswers.get(idx).getString("answer"));

                if (isStatNeeded && stdAnswers.get(idx).containsKey("mark"))
                    question.put("stdMark", stdAnswers.get(idx).get("mark"));

                if (isStatNeeded && stdAnswers.get(idx).containsKey("mark_desc") && !stdAnswers.get(idx).getString("mark_desc").isEmpty())
                    question.put("markDesc", stdAnswers.get(idx).get("mark_desc"));
            }

            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertList(
                questionsList, isStatNeeded, isStatNeeded, true, isStatNeeded, false
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);

    }

    public static String returnQuiz(Document quiz, Document stdDoc,
                                    boolean isStatNeeded, JSONObject quizJSON) {

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

        List<Binary> questionStats = null;
        if (isStatNeeded && quiz.containsKey("question_stat")) {
            questionStats = quiz.getList("question_stat", Binary.class);
            if (questionStats.size() != questionsMark.size())
                questionStats = null;
        }

        Object tmp = null;

        if (stdDoc != null) {
            tmp = stdDoc.getOrDefault("answers", null);
            if (tmp != null)
                tmp = ((Binary) tmp).getData();
            else
                tmp = new byte[0];
        }

        ArrayList<PairValue> stdAnswers = tmp == null ? new ArrayList<>() : Utility.getAnswers((byte[]) tmp);

        i = 0;

        for (Document question : questionsList) {

            if (i >= stdAnswers.size())
                question.put("stdAns", "");
            else {
                if (question.getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()))
                    question.put("stdAns", ((PairValue) stdAnswers.get(i).getValue()).getValue());
                else
                    question.put("stdAns", stdAnswers.get(i).getValue());
            }
            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertList(
                questionsList, isStatNeeded, isStatNeeded, true, isStatNeeded, false
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);

    }

    private static String returnCustomQuiz(Document quiz, JSONObject quizJSON,
                                           boolean isStatsNeeded) {

        ArrayList<Document> questionsList = new ArrayList<>();
        List<ObjectId> questions = (List<ObjectId>) quiz.getOrDefault(
                "questions", new ArrayList<ObjectId>()
        );

        int i = 0;
        for (ObjectId itr : questions) {

            Document question = questionRepository.findById(itr);

            if (question == null) {
                i++;
                continue;
            }

            questionsList.add(Document.parse(question.toJson()).append("no", i + 1));
            i++;
        }

        ArrayList<PairValue> stdAnswers =
                !quiz.containsKey("student_answers") ? new ArrayList<>() :
                        Utility.getAnswers(((Binary) quiz.getOrDefault("student_answers", new byte[0])).getData());

        i = 0;

        for (Document question : questionsList) {

            if (i >= stdAnswers.size())
                question.put("stdAns", "");
            else {
                if (question.getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()))
                    question.put("stdAns", ((PairValue) stdAnswers.get(i).getValue()).getValue());
                else if (question.getString("kind_question").equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName()))
                    question.put("stdAns", stdAnswers.get(i).getValue().toString().matches("^[_]+$") ? "" : stdAnswers.get(i).getValue());
                else
                    question.put("stdAns", stdAnswers.get(i).getValue());
            }
            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertList(
                questionsList, isStatsNeeded, isStatsNeeded, true, isStatsNeeded, false
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);

    }

    protected static String returnCustomQuiz(ArrayList<Document> questions,
                                             ArrayList<PairValue> stdAnswers,
                                             JSONObject quizJSON,
                                             boolean isStatsNeeded) {

        int i = 0;

        for (Document question : questions) {

            if (i >= stdAnswers.size())
                question.put("stdAns", "");
            else {
                if (question.getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()))
                    question.put("stdAns", ((PairValue) stdAnswers.get(i).getValue()).getValue());
                else
                    question.put("stdAns", stdAnswers.get(i).getValue());
            }
            i++;
        }

        JSONArray questionsJSONArr = Utilities.convertList(
                questions, isStatsNeeded, isStatsNeeded, true, isStatsNeeded, false
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("questions", questionsJSONArr);
        jsonObject.put("quizInfo", quizJSON);

        return generateSuccessMsg("data", jsonObject);
    }

    public static String payCustomQuiz(Document user,
                                       ObjectId id,
                                       JSONObject data) {

        ObjectId userId = user.getObjectId("_id");
        double money = ((Number) user.get("money")).doubleValue();

        Document doc = customQuizRepository.findById(id);

        if (doc == null)
            return JSON_NOT_VALID_ID;

        if (!doc.getObjectId("user_id").equals(userId))
            return JSON_NOT_ACCESS;

        if (!doc.getString("status").equalsIgnoreCase("wait"))
            return generateErr("شما قبلا بهای این آزمون را پرداخت کرده اید.");

        long curr = System.currentTimeMillis();

        if (curr - doc.getLong("created_at") > 1200000)
            return JSON_NOT_VALID_PARAMS;


        String offcode = (String) irysc.gachesefid.Utility.Utility.getOrDefault(data, "offcode", null);
        Document off = null;

        if (offcode != null) {

            off = validateOffCode(
                    offcode, userId, curr,
                    OffCodeSections.BANK_EXAM.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");
        }

        int totalPrice = doc.getInteger("price");

        if (off == null)
            off = findAccountOff(
                    userId, curr, OffCodeSections.BANK_EXAM.getName()
            );

        double shouldPayDouble = totalPrice;
        double offAmount = 0;

        if (off != null) {

            offAmount =
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            totalPrice * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount");

            shouldPayDouble = totalPrice - offAmount;
        }

        int shouldPay = (int) shouldPayDouble;

        if (shouldPay - money <= 100) {

            double newUserMoney = money;

            if (shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, money);
                user.put("money", newUserMoney);
                userRepository.replaceOne(userId, user);
            }

            Document finalOff = off;
            doc.put("status", "paid");

            PairValue p = irysc.gachesefid.Controllers.Quiz.Utility.getAnswersByteArrWithNeededTime(
                    doc.getList("questions", ObjectId.class)
            );

            doc.put("answers", p.getValue());
            doc.put("duration", p.getKey());
            doc.put("start_at", null);

            customQuizRepository.replaceOne(id, doc);

            if (finalOff != null) {

                BasicDBObject update;

                if (finalOff.containsKey("is_public") &&
                        finalOff.getBoolean("is_public")
                ) {
                    List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                    students.add(userId);
                    update = new BasicDBObject("students", students);
                } else
                    update = new BasicDBObject("used", true)
                            .append("used_at", curr)
                            .append("used_section", OffCodeSections.BANK_EXAM.getName())
                            .append("used_for", id);

                offcodeRepository.updateOne(
                        finalOff.getObjectId("_id"),
                        new BasicDBObject("$set", update)
                );
            }

            Document transaction = new Document("user_id", userId)
                    .append("amount", 0)
                    .append("account_money", shouldPay)
                    .append("created_at", curr)
                    .append("status", "success")
                    .append("section", OffCodeSections.BANK_EXAM.getName())
                    .append("products", id);

            if (finalOff != null) {
                doc.append("off_amount", (int) offAmount);
                doc.append("off_code", finalOff.getObjectId("_id"));
            }

            transactionRepository.insertOne(transaction);
            if (user.containsKey("mail")) {
                new Thread(() -> sendMail(
                        user.getString("mail"),
                        SERVER + "recp/" + transaction.getObjectId("_id").toString(),
                        "successQuiz",
                        user.getString("first_name") + " " + user.getString("last_name")
                )).start();
            }

            return generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", newUserMoney),
                    new PairValue("transactionId", transaction.getObjectId("_id").toString())
            );
        }

        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document transaction =
                new Document("user_id", userId)
                        .append("account_money", money)
                        .append("amount", (int) (shouldPay - money))
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", id)
                        .append("section", OffCodeSections.BANK_EXAM.getName());

        if (off != null) {
            transaction.append("off_code", off.getObjectId("_id"));
            transaction.append("off_amount", (int) offAmount);
        }

        return goToPayment((int) (shouldPay - money), transaction);
    }

    public static String prepareCustomQuiz(ObjectId userId,
                                           JSONArray filters,
                                           String name
    ) {

        if (filters.length() == 0)
            return JSON_NOT_VALID_PARAMS;

        int totalPrice = 0;
        List<ObjectId> questionIds = new ArrayList<>();

        for (int i = 0; i < filters.length(); i++) {

            JSONObject jsonObject = filters.getJSONObject(i);
            if (!jsonObject.has("qNo"))
                return JSON_NOT_VALID_PARAMS;

            if (
                    !jsonObject.has("tag") &&
                            !jsonObject.has("author") &&
                            !jsonObject.has("gradeId") &&
                            !jsonObject.has("lessonId") &&
                            !jsonObject.has("subjectId")
            )
                return JSON_NOT_VALID_PARAMS;

        }

        for (int i = 0; i < filters.length(); i++) {

            JSONObject jsonObject = convertPersian(filters.getJSONObject(i));

            PairValue p;
            try {
                p = fetchQuestionsByFilter(
                        userId,
                        (String) getOrDefault(jsonObject, "tag", null),
                        getOrDefaultObjectId(jsonObject, "gradeId"),
                        getOrDefaultObjectId(jsonObject, "lessonId"),
                        getOrDefaultObjectId(jsonObject, "subjectId"),
                        (String) getOrDefault(jsonObject, "author", null),
                        Integer.parseInt(jsonObject.get("qNo").toString()),
                        (String) getOrDefault(jsonObject, "level", null)
                );
            } catch (InvalidFieldsException e) {
                return generateErr(e.getMessage());
            }

            totalPrice += (int) p.getValue();
            questionIds.addAll((List<ObjectId>) p.getKey());

        }

        Document doc = new Document("user_id", userId)
                .append("created_at", System.currentTimeMillis())
                .append("status", "wait")
                .append("price", totalPrice)
                .append("name", name)
                .append("questions", questionIds);

        ObjectId oId = customQuizRepository.insertOneWithReturnId(doc);

        Document off = irysc.gachesefid.Utility.Utility.findAccountOff(
                userId, System.currentTimeMillis(), OffCodeSections.BANK_EXAM.getName()
        );

        if (off == null)
            return generateSuccessMsg("data", new JSONObject()
                    .put("price", totalPrice)
                    .put("id", oId)
            );

        return generateSuccessMsg("data", new JSONObject()
                .put("price", totalPrice)
                .put("off", new JSONObject()
                        .put("type", off.getString("type"))
                        .put("amount", off.getInteger("amount"))
                )
                .put("id", oId)
        );
    }

    private static PairValue fetchQuestionsByFilter(
            ObjectId userId,
            String tag,
            ObjectId gradeId,
            ObjectId lessonId,
            ObjectId subjectId,
            String author,
            int questionsNo,
            String level) throws InvalidFieldsException {

        ArrayList<Bson> filters = fetchFilter(tag, gradeId,
                lessonId, subjectId, level, author
        );

//        filters.add(or(
//                eq("kind_question", "multi_sentence"),
//                eq("kind_question", "short_answer")
//        ));

        List<Document> docs = questionRepository.find(and(filters), JUST_ID);

        if (docs.size() < questionsNo)
            throw new InvalidFieldsException("تعداد سوالات سامانه کمتر از فیلتر انتخابی شما می باشد.");

        List<Integer> indices = getRandomElement(docs, questionsNo);
        ArrayList<ObjectId> questionIds = new ArrayList<>();

        for (int i : indices)
            questionIds.add(docs.get(i).getObjectId("_id"));

        int price = 0;

        if (subjectId != null && level != null) {
            Document subject = subjectRepository.findById(subjectId);
            if (level.equals(QuestionLevel.EASY.getName()))
                price = subject.getInteger("easy_price") * questionsNo;
            else if (level.equals(QuestionLevel.MID.getName()))
                price = subject.getInteger("mid_price") * questionsNo;
            else
                price = subject.getInteger("hard_price") * questionsNo;
        } else {

            ArrayList<Document> questions = questionRepository.findByIds(
                    questionIds, false
            );

            HashMap<ObjectId, SubjectFilter> subjectFilterHashMap = new HashMap<>();

            for (Document question : questions) {

                ObjectId sId = question.getObjectId("subject_id");
                String l = question.getString("level");

                if (subjectFilterHashMap.containsKey(sId))
                    subjectFilterHashMap.get(sId).add(1, l);
                else
                    subjectFilterHashMap.put(sId, new SubjectFilter(sId, 1, l));
            }

            for (ObjectId sId : subjectFilterHashMap.keySet())
                price += subjectFilterHashMap.get(sId).calc();
        }

        return new PairValue(questionIds, price);
    }

    private static List<Integer> getRandomElement(List<Document> list, int totalItems
    ) throws InvalidFieldsException {

        if (list.size() < totalItems)
            throw new InvalidFieldsException("list size is invalid");

        ArrayList<Integer> selectedIndices = new ArrayList<>();

        if (list.size() == totalItems) {

            for (int i = 0; i < list.size(); i++)
                selectedIndices.add(i);

            Collections.shuffle(selectedIndices);
            return selectedIndices;
        }

        Random rand = new Random();

        if (list.size() - totalItems < totalItems) {

            List<Integer> tmp = getRandomElement(list, list.size() - totalItems);

            for (int i = 0; i < list.size(); i++) {

                if (tmp.contains(i))
                    continue;

                selectedIndices.add(i);
            }

            Collections.shuffle(selectedIndices);

            return selectedIndices;
        }

        while (selectedIndices.size() < totalItems) {

            int randomIndex = rand.nextInt(list.size());
            if (selectedIndices.contains(randomIndex))
                continue;

            selectedIndices.add(randomIndex);
        }

        return selectedIndices;
    }

    static class A {

        Document quiz;
        int reminder;
        Document student;
        long startAt;
        int neededTime;
        boolean needUpdate = false;

        public A(Document quiz, int reminder, Document student, long startAt, int neededTime) {
            this.quiz = quiz;
            this.reminder = reminder;
            this.student = student;
            this.startAt = startAt;
            this.neededTime = neededTime;
        }

        public void setNeedUpdate() {
            needUpdate = true;
        }
    }

    public static class SubjectFilter {

        HashMap<String, Integer> levelsQNo;
        ObjectId objectId;

        public SubjectFilter(ObjectId oId, int qNo, String level) {
            objectId = oId;
            levelsQNo = new HashMap<>();
            levelsQNo.put(level, qNo);
        }

        public void add(int qNo, String level) {
            if (levelsQNo.containsKey(level))
                levelsQNo.put(level, levelsQNo.get(level) + qNo);
            else
                levelsQNo.put(level, qNo);
        }

        int calc() {

            int sum = 0;
            Document subject = subjectRepository.findById(objectId);
            for (String key : levelsQNo.keySet()) {
                if (key.equals(QuestionLevel.EASY.getName()))
                    sum += subject.getInteger("easy_price") * levelsQNo.get(key);
                else if (key.equals(QuestionLevel.MID.getName()))
                    sum += subject.getInteger("mid_price") * levelsQNo.get(key);
                else
                    sum += subject.getInteger("hard_price") * levelsQNo.get(key);
            }

            return sum;
        }

        public int total() {

            int sum = 0;
            for (String key : levelsQNo.keySet())
                sum += levelsQNo.get(key);

            return sum;
        }

        public int easy() {

            int sum = 0;
            for (String key : levelsQNo.keySet()) {
                if (key.equals(QuestionLevel.EASY.getName()))
                    sum += levelsQNo.get(key);
            }

            return sum;

        }

        public int mid() {

            int sum = 0;
            for (String key : levelsQNo.keySet()) {
                if (key.equals(QuestionLevel.MID.getName()))
                    sum += levelsQNo.get(key);
            }

            return sum;
        }

        public int hard() {

            int sum = 0;
            for (String key : levelsQNo.keySet()) {
                if (key.equals(QuestionLevel.HARD.getName()))
                    sum += levelsQNo.get(key);
            }

            return sum;
        }

    }

}
