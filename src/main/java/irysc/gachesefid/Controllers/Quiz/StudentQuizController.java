package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.exists;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentQuizController {

    public static String getMyRecp(Common db, ObjectId quizId, ObjectId userId) {

        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);

            Document std = searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class),
                    "_id", userId
            );

            Document transaction = transactionRepository.findOne(
                    and(
                            eq("status", "success"),
                            eq("section", OffCodeSections.GACH_EXAM.getName()),
                            eq("user_id", userId),
                            in("products", quizId),
                            exists("ref_id")
                    ), new BasicDBObject("ref_id", 1)
            );

            JSONObject jsonObject = new JSONObject()
                    .put("paid", std.getInteger("paid"))
                    .put("createdAt", getSolarDate(std.getLong("register_at")))
                    .put("for", GiftController.translateUseFor(OffCodeSections.GACH_EXAM.getName()));

            if(transaction != null)
                jsonObject.put("refId", transaction.get("ref_id"));

            return generateSuccessMsg("data",jsonObject);
        }
        catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
            return generateErr(x.getMessage());
        }

    }

    public static String reviewQuiz(Common db, ObjectId quizId,
                                    ObjectId userId, boolean isStudent
    ) {
        try {
            Document quiz = hasProtectedAccess(db, userId, quizId);
            long curr = System.currentTimeMillis();

            if(isStudent && quiz.getLong("end") > curr)
                return JSON_NOT_ACCESS;

            Document stdDoc = null;

            if(isStudent) {
                stdDoc = searchInDocumentsKeyVal(
                        quiz.getList("students", Document.class),
                        "_id", userId
                );
            }

            int neededTime = new RegularQuizController().calcLen(quiz);
            Document questions =
                    quiz.get("questions", Document.class);

            int qNo = 0;

            if(questions.containsKey("_ids"))
                qNo = questions.getList("_ids", ObjectId.class).size();

            JSONObject quizJSON = new JSONObject()
                    .put("title", quiz.getString("title"))
                    .put("questionsNo", qNo)
                    .put("description", quiz.getOrDefault("description", ""))
                    .put("mode", quiz.getString("mode"))
                    .put("attaches", new JSONArray()
                            .put(new JSONObject()
                                    .put("name", "a.jpg")
                                    .put("link", "https://google.com")
                            )
                            .put(new JSONObject()
                                    .put("name", "b.pdf")
                                    .put("link", "https://varzesh3.com")
                            )
                    )
                    .put("duration", neededTime);

            return returnQuiz(quiz, stdDoc, true, quizJSON);
        }
        catch (Exception x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
            return generateErr(x.getMessage());
        }
    }


    public static String myQuizzes(ObjectId userId,
                                   String generalMode,
                                   String status) {

        if(generalMode != null &&
                !EnumValidatorImp.isValid(generalMode, GeneralKindQuiz.class))
            return JSON_NOT_VALID_PARAMS;

        JSONArray data = new JSONArray();
        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(in("students._id", userId));

        long curr = System.currentTimeMillis();

        if(status != null) {

            if(status.equalsIgnoreCase("finished"))
                filters.add(lt("end", curr));
            else if(status.equalsIgnoreCase("inProgress"))
                filters.add(
                        and(
                                lte("start", curr),
                                gt("end", curr)
                        )
                );
            else
                filters.add(gt("start", curr));
        }

        if(generalMode == null ||
                generalMode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName())
        ) {
            ArrayList<Document> quizzes = iryscQuizRepository.find(and(filters), null);
            QuizAbstract quizAbstract = new RegularQuizController();

            for(Document quiz : quizzes) {

                Document studentDoc = searchInDocumentsKeyVal(
                        quiz.getList("students", Document.class),
                        "_id", userId
                );

                if(studentDoc == null)
                    continue;

                JSONObject jsonObject = quizAbstract.convertDocToJSON(
                        quiz, true, false, true, true
                );

                if(jsonObject.getString("status")
                        .equalsIgnoreCase("inProgress") &&
                        studentDoc.containsKey("start_at") &&
                        studentDoc.get("start_at") != null
                ) {
                    int neededTime = quizAbstract.calcLen(quiz);
                    int untilYetInSecondFormat =
                            (int) ((curr - studentDoc.getLong("start_at")) / 1000);
                    if (untilYetInSecondFormat > neededTime)
                        jsonObject.put("status", "waitForResult");
                }

                data.put(jsonObject);

            }

        }

        return generateSuccessMsg("data", data);
    }

    public static String launch(Common db, ObjectId quizId,
                                ObjectId studentId) {

        try {
            Document doc = hasProtectedAccess(db, studentId, quizId);
            long curr = System.currentTimeMillis();

            if(doc.getLong("start") > curr ||
                    doc.getLong("end") < curr)
                return generateErr("در زمان ارزیابی قرار نداریم.");

            List<Document> students = doc.getList("students", Document.class);

            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            int neededTime = new RegularQuizController().calcLen(doc);
            long startAt;

            if(student.containsKey("start_at") &&
                    student.get("start_at") != null) {

                int untilYetInSecondFormat = (int) ((curr - student.getLong("start_at")) / 1000);
                if (untilYetInSecondFormat > neededTime)
                    return generateErr("شما در این آزمون شرکت کرده اید.");

                startAt = student.getLong("start_at");
            }
            else {
                student.put("start_at", curr);
                startAt = curr;
            }

            student.put("finish_at", curr);

            int delay = Math.max(
                    0,
                    (int)(startAt + neededTime * 1000L - doc.getLong("end")) / 1000
            );

            int reminder = neededTime -
                    (int)((curr - student.getLong("start_at")) / 1000) -
                    delay;

            db.replaceOne(quizId, doc);

            JSONObject quizJSON = new JSONObject()
                    .put("title", doc.getString("title"))
                    .put("id", doc.getObjectId("_id").toString())
                    .put("generalMode", db instanceof IRYSCQuizRepository ? "IRYSC" : "school")
                    .put("questionsNo", doc.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                    .put("description", doc.getOrDefault("description", ""))
                    .put("mode", doc.getString("mode"))
                    .put("attaches", new JSONArray()
                            .put(new JSONObject()
                                    .put("name", "a.jpg")
                                    .put("link", "https://google.com")
                            )
                            .put(new JSONObject()
                                    .put("name", "b.pdf")
                                    .put("link", "https://varzesh3.com")
                            )
                    )
                    .put("refresh", 1) //Math.abs(new Random().nextInt(10)) + 5
                    .put("duration", neededTime)
                    .put("reminder", reminder)
                    .put("isNewPerson", student.getLong("start_at") == curr);

            return returnQuiz(doc, student, false, quizJSON);

        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
            return null;
        }

    }

    public static String storeAnswers(Common db, ObjectId quizId,
                                      ObjectId studentId, JSONArray answers) {
        try {
            Document doc = hasProtectedAccess(db, studentId, quizId);
            long curr = System.currentTimeMillis();

            if(doc.getLong("start") > curr ||
                    doc.getLong("end") < curr)
                return generateErr("در زمان ارزیابی قرار نداریم.");

            List<Document> students = doc.getList("students", Document.class);

            Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            int neededTime = new RegularQuizController().calcLen(doc);

//            if(student.containsKey("start_at") &&
//                    student.get("start_at") != null) {

            long startAt = student.getLong("start_at");
            int delay = Math.max(
                    0,
                    (int)(startAt + neededTime * 1000L - doc.getLong("end")) / 1000
            );

            int reminder = neededTime -
                    (int)((curr - student.getLong("start_at")) / 1000) -
                    delay;

            if(reminder <= 0)
                return generateErr("شما در این آزمون شرکت کرده اید.");

//            int untilYetInSecondFormat = (int) ((curr - student.getLong("start_at")) / 1000);
//            if(untilYetInSecondFormat > neededTime)
//                return generateErr("شما در این آزمون شرکت کرده اید.");

            student.put("finish_at", curr);

            String result = saveStudentAnswers(doc, answers, student, db);
            if(result.contains("nok"))
                return result;
//            Math.abs(new Random().nextInt(3)) + 1
            return generateSuccessMsg("reminder", reminder,
                    new PairValue("refresh", 3)
            );

        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
            return null;
        }
    }

    public static String buy(ObjectId userId, ObjectId packageId,
                             JSONArray ids, JSONArray studentIds,
                             int money, String phone, String mail,
                             String offcode) {

        Document quizPackage = null;
        Document off = null;
        long curr = System.currentTimeMillis();

        if(packageId != null) {
            quizPackage = packageRepository.findById(packageId);
            if(quizPackage == null || quizPackage.getLong("expire_at") < curr)
                return JSON_NOT_VALID_PARAMS;
        }

        if(offcode != null) {

            off = offcodeRepository.findOne(
                    and(
                            exists("code"),
                            eq("code", offcode),
                            or(
                                    and(
                                            exists("user_id"),
                                            eq("user_id", userId)
                                    ),
                                    and(
                                            exists("is_public"),
                                            eq("is_public", true)
                                    )
                            ),
                            or(
                                    exists("used", false),
                                    and(
                                            exists("used"),
                                            eq("used", false)
                                    )
                            ),
                            or(
                                    eq("section", OffCodeSections.ALL.getName()),
                                    eq("section", OffCodeSections.GACH_EXAM.getName())
                            ),
                            nin("students", userId),
                            gt("expire_at", curr)
                    ), null
            );

            if(off == null)
                return JSON_NOT_VALID_PARAMS;
        }

        ArrayList<ObjectId> quizIds = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {

            if (!ObjectId.isValid(ids.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            ObjectId quizId = new ObjectId(ids.getString(i));
            if(quizPackage != null &&
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

        if (quizzes.size() != quizIds.size())
            return JSON_NOT_VALID_PARAMS;

        if(studentIds != null) {
            Document school = schoolRepository.findOne(eq("user_id", userId), new BasicDBObject("_id", 1));
            if(school == null)
                return JSON_NOT_ACCESS;

            ObjectId schoolId = school.getObjectId("_id");
            ArrayList<ObjectId> studentOIds = new ArrayList<>();

            for (int i = 0; i < studentIds.length(); i++) {

                if (!ObjectId.isValid(studentIds.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

                ObjectId studentId = new ObjectId(studentIds.getString(i));
                Document user = userRepository.findById(studentId);
                if(user == null || !user.containsKey("school") ||
                        !user.get("school", Document.class).get("_id").equals(schoolId)
                )
                    return JSON_NOT_ACCESS;

                studentOIds.add(studentId);
            }

            return doBuy(userId, phone, mail, money,
                    quizPackage, off, quizzes, studentOIds
            );
        }

        return doBuy(userId, phone, mail, money,
                quizPackage, off, quizzes, null
        );
    }


    private static String doBuy(ObjectId studentId, String phone,
                                String mail, int money,
                                Document quizPackage,
                                Document off, ArrayList<Document> quizzes,
                                ArrayList<ObjectId> studentIds
    ) {

        long curr = System.currentTimeMillis();
        int totalPrice = 0;

        if(studentIds == null)
            for(Document quiz : quizzes)
                totalPrice += quiz.getInteger("price");
        else
            for(Document quiz : quizzes)
                totalPrice += quiz.getInteger("price") * studentIds.size();

        if(off == null) {
            off = offcodeRepository.findOne(
                    and(
                            exists("code", false),
                            exists("used"),
                            exists("user_id"),
                            eq("user_id", studentId),
                            eq("used", false),
                            or(
                                    eq("section", OffCodeSections.ALL.getName()),
                                    eq("section", OffCodeSections.GACH_EXAM.getName())
                            ),
                            gt("expire_at", curr)
                    ), null
            );
        }

        int packageOff = 0;
        boolean usePackageOff = false;

        if(quizPackage != null) {
            if(quizzes.size() >= quizPackage.getInteger("min_select")) {
                packageOff = quizPackage.getInteger("off_percent");
                usePackageOff = true;
            }
        }

        double offAmount = totalPrice * packageOff / 100.0;
        double shouldPayDouble = totalPrice - offAmount;

        if(studentIds != null) {
            int groupRegistrationOff = irysc.gachesefid.Utility.Utility.getConfig().getInteger("school_off_percent");
            offAmount += shouldPayDouble * groupRegistrationOff / 100.0;
            shouldPayDouble =  totalPrice - offAmount;
        }

        if(off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPayDouble * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPayDouble = totalPrice - offAmount;
        }

        int shouldPay = (int) shouldPayDouble;

        ArrayList<ObjectId> quizIds = new ArrayList<>();
        for(Document quiz : quizzes)
            quizIds.add(quiz.getObjectId("_id"));

        if (shouldPay - money <= 100) {

            int newUserMoney = money;

            if(shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, money);
                Document user = userRepository.findById(studentId);
                user.put("money", newUserMoney);
                userRepository.replaceOne(studentId, user);
            }

            Document finalOff = off;
            boolean finalUsePackageOff = usePackageOff;
            new Thread(() -> {

                if(studentIds != null)
                    new RegularQuizController()
                            .registry(studentIds, phone, mail, quizIds, 0);
                else
                    new RegularQuizController()
                            .registry(studentId, phone, mail, quizIds, 0);

                if (finalOff != null) {

                    BasicDBObject update;

                    if(finalOff.containsKey("is_public") &&
                            finalOff.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                        students.add(studentId);
                        update = new BasicDBObject("students", students);
                    }
                    else
                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.GACH_EXAM.getName())
                                .append("used_for", quizIds);

                    offcodeRepository.updateOne(
                            finalOff.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }

                if(finalUsePackageOff) {
                    quizPackage.put("buyers", quizPackage.getInteger("buyers") + 1);
                    packageRepository.replaceOne(
                            quizPackage.getObjectId("_id"), quizPackage
                    );
                }

                Document doc = new Document("user_id", studentId)
                        .append("amount", 0)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.GACH_EXAM.getName())
                        .append("products", quizIds);

                if(studentIds != null)
                    doc.append("student_ids", studentIds);

                if(finalOff != null)
                    doc.append("off_code", finalOff.getObjectId("_id"));

                transactionRepository.insertOne(doc);
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
                        .append("amount", shouldPay - money)
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", quizIds)
                        .append("section", OffCodeSections.GACH_EXAM.getName())
                        .append("off_code", off == null ? null : off.getObjectId("_id"));

        if(studentIds != null)
            doc.append("student_ids", studentIds);

        if(quizPackage != null)
            doc.put("package_id", quizPackage.getObjectId("_id"));

        return goToPayment(shouldPay - money, doc);
    }

    private static String returnQuiz(Document quiz, Document stdDoc,
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

        ArrayList<PairValue> stdAnswers =
                stdDoc == null ? new ArrayList<>() :
                        Utility.getAnswers(((Binary) stdDoc.getOrDefault("answers", new byte[0])).getData());

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
}
