package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Finance.Utilities;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

public class RegularQuizController extends QuizAbstract {

    // endRegistry consider as optional field

    // topStudentsGiftCoin or topStudentsGiftMoney or topStudentsCount
    // are optional and can inherit from config
//"duration",

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "launchMode", "showResultsAfterCorrection",
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database"
    };

    public static String create(ObjectId userId, JSONObject jsonObject, String mode) {

        try {

            Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", mode);
            Document newDoc = QuizController.store(userId, jsonObject);
            iryscQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz", new RegularQuizController()
                            .convertDocToJSON(newDoc, false, true)
            );

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(
                    e.getMessage()
            );
        }

    }

    public static String delete(ObjectId quizId, ObjectId userId) {

        Document quiz = iryscQuizRepository.findOneAndDelete(and(
                eq("_id", quizId),
                eq("mode", KindQuiz.REGULAR.getName())
        ));

        if (quiz == null)
            return JSON_NOT_VALID;

        iryscQuizRepository.cleanRemove(quiz);

        return JSON_OK;
    }

    public static String createKarname(ObjectId userId, String access, ObjectId quizId) {

//        Document quiz = regularQuizRepository.findById(quizId);
//
//        if(quiz == null ||
//                (!quiz.getObjectId("user_id").equals(userId) && !access.equals("admin")))
//            return JSON_NOT_ACCESS;
//
//        String[] questionIdsStr = quiz.getString("questions").split("-");
//        int[] questionIds = new int[questionIdsStr.length];
//
//        for(int i = 0; i < questionIdsStr.length; i++)
//            questionIds[i] = Integer.parseInt(questionIdsStr[i]);
//
//        HashMap<Integer, Question> questionsInfo = QuestionRepository.getAnswers(questionIds);
//        ArrayList<Document> studentResults = RegularQuizResultRepository.getRegularQuizResults(quizId);
//
//        for(Document studentResult : studentResults) {
//
//            String[] studentAnswersStr = studentResult.getString("student_answers").split("-");
//            String[] studentQuestionIdsStr = studentResult.getString("question_ids").split("-");
//
//            if(studentAnswersStr.length != studentQuestionIdsStr.length) {
//                System.out.println("heyyy corrupt");
//                continue;
//            }
//
//            for(int i = 0; i < studentAnswersStr.length; i++) {
//                questionsInfo.get(Integer.parseInt(studentQuestionIdsStr[i]));
//            }
//
//        }

        return JSON_OK;
    }

    public static String myQuizes(ObjectId userId) {
        return null;
    }

    public static String myPassedQuizes(ObjectId userId) {
        return null;
    }

    @Override
    public String buy(Document user, Document quiz) {

        if (quiz.containsKey("end_registry") &&
                quiz.getLong("end_registry") < System.currentTimeMillis())
            return irysc.gachesefid.Utility.Utility.generateErr(
                    "زمان ثبت نام در آزمون موردنظر گذشته است."
            );

        ObjectId userId = user.getObjectId("_id");

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                quiz.getList("students", Document.class), "_id", userId
        ) != -1)
            return irysc.gachesefid.Utility.Utility.generateErr(
                    "شما در آزمون موردنظر ثبت نام کرده اید."
            );

        PairValue p = Utilities.calcPrice(quiz.getInteger("price"),
                user.getInteger("money"), userId, OffCodeSections.GACH_EXAM.getName()
        );

        int price = (int) p.getKey();

        if (price <= 100) {

            new Thread(() -> {

                registry(user, quiz, 0);

                if (p.getValue() != null) {

                    PairValue offcode = (PairValue) p.getValue();
                    int finalAmount = (int) offcode.getValue();

                    BasicDBObject update = new BasicDBObject("used", true)
                            .append("used_at", System.currentTimeMillis())
                            .append("used_section", OffCodeSections.GACH_EXAM.getName())
                            .append("used_for", quiz.getObjectId("_id"))
                            .append("amount", finalAmount);

                    Document off = offcodeRepository.findOneAndUpdate(
                            (ObjectId) offcode.getKey(),
                            new BasicDBObject("$set", update)
                    );

                    if (off.getInteger("amount") != finalAmount) {

                        Document newDoc = new Document("type", off.getString("type"))
                                .append("amount", off.getInteger("amount") - finalAmount)
                                .append("expire_at", off.getInteger("expire_at"))
                                .append("section", off.getString("section"))
                                .append("used", false)
                                .append("created_at", System.currentTimeMillis())
                                .append("user_id", userId);

                        offcodeRepository.insertOne(newDoc);
                    }
                }

                transactionRepository.insertOne(
                        new Document("user_id", userId)
                                .append("amount", 0)
                                .append("created_at", System.currentTimeMillis())
                                .append("status", "success")
                                .append("for", OffCodeSections.GACH_EXAM.getName())
                                .append("off_code", p.getValue() == null ? null : ((PairValue) p.getValue()).getKey())
                );

            }).start();

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success"
            );
        }

        ObjectId transactionId = transactionRepository.insertOneWithReturnObjectId(
                new Document("user_id", userId)
                        .append("amount", price)
                        .append("created_at", System.currentTimeMillis())
                        .append("status", "init")
                        .append("for", OffCodeSections.GACH_EXAM.getName())
                        .append("off_code", p.getValue() == null ? null : ((PairValue) p.getValue()).getKey())
        );

        return Utilities.goToPayment(price, userId, transactionId);
    }

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("start", quiz.getLong("start"))
                .put("end", quiz.getLong("end"))
                .put("startRegistry", quiz.getLong("start_registry"))
                .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                .put("price", quiz.getInteger("price"))
                .put("generalMode", "IRYSC")
                .put("mode", quiz.getString("mode"))
                .put("launchMode", quiz.getString("launch_mode"))
                .put("tags", quiz.getList("tags", String.class))
                .put("id", quiz.getObjectId("_id").toString());

        if (isAdmin)
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("capacity", quiz.getInteger("capacity"));

        try {
            jsonObject
                    .put("questionsCount", quiz.get("questions", Document.class)
                            .getList("_ids", ObjectId.class).size());
        } catch (Exception x) {
            jsonObject.put("questionsCount", 0);
        }

        if (quiz.containsKey("capacity"))
            jsonObject.put("reminder", Math.max(quiz.getInteger("capacity") - quiz.getInteger("registered"), 0));

        if (!isDigest) {
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""))
                    .put("topStudentsCount", quiz.getInteger("top_students_count"))
                    .put("showResultsAfterCorrection", quiz.getBoolean("show_results_after_correction"));
        }

        return jsonObject;
    }

    @Override
    Document registry(Document student, Document quiz, int paid) {

        List<Document> students = quiz.getList("students", Document.class);

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        ) != -1)
            return null;

        Document stdDoc = new Document("_id", student.getObjectId("_id"))
                .append("paid", paid)
                .append("register_at", System.currentTimeMillis())
                .append("finish_at", null)
                .append("start_at", null)
                .append("answers", new byte[0]);

        if ((boolean) quiz.getOrDefault("permute", false))
            stdDoc.put("question_indices", new ArrayList<>());

        students.add(stdDoc);

        return stdDoc;
        //todo : send notif
    }

    @Override
    void quit(Document student, Document quiz) {

        List<Document> students = quiz.getList("students", Document.class);
        int idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        );

        if (idx == -1)
            return;

        students.remove(idx);

        // todo: send notif
    }

    void createTaraz(Document quiz) {

        Document questions = quiz.get("questions", Document.class);
        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);
        List<Double> marks = questions.getList("marks", Double.class);
        List<Document> students = quiz.getList("students", Document.class);

        List<Document> questionsList = new ArrayList<>();
        ArrayList<QuestionStat> lessonsStat = new ArrayList<>();
        ArrayList<QuestionStat> subjectsStat = new ArrayList<>();

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

            for (QuestionStat itr : subjectsStat) {
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
                        new QuestionStat(
                                subjectId, subject.getString("name"), lessonId
                        )
                );

                tmp.put("lesson_id", lessonId);

                boolean isLessonAdded = false;

                for (QuestionStat itr : lessonsStat) {
                    if (itr.equals(lessonId)) {
                        isLessonAdded = true;
                        break;
                    }
                }

                if (!isLessonAdded)
                    lessonsStat.add(
                            new QuestionStat(lessonId,
                                    lesson.getString("name"))
                    );
            }

            questionsList.add(tmp);
        }

        ArrayList<QuestionStat> studentsStat = new ArrayList<>();

        for (Document student : students) {

            studentsStat.add(new QuestionStat(
                    student.getObjectId("_id"), "",
                    Utility.getAnswers(
                            student.get("answers", Binary.class).getData()
                    )
            ));
        }

        ArrayList<byte[]> questionStats = new ArrayList<>();

        int idx = 0;
        for (Document question : questionsList) {

            short corrects = 0, incorrects = 0, whites = 0;
            short status;

            for (QuestionStat aStudentsStat : studentsStat) {
                status = aStudentsStat.doCorrect(question, idx);
                if(status == 0)
                    whites++;
                else if(status == 1)
                    corrects++;
                else
                    incorrects++;
            }

            byte[] tmp = new byte[3];
            tmp[0] = (byte) whites;
            tmp[1] = (byte) corrects;
            tmp[2] = (byte) incorrects;
            questionStats.add(tmp);
            idx++;
        }

        for (QuestionStat itr : subjectsStat) {

            for (QuestionStat aStudentsStat : studentsStat)
                itr.marks.add(
                        (aStudentsStat.subjectMark.get(itr.id) / aStudentsStat.subjectTotalMark.get(itr.id)) * 100.0
                );

        }

        for (QuestionStat itr : lessonsStat) {

            for (QuestionStat aStudentsStat : studentsStat)
                itr.marks.add(
                        (aStudentsStat.lessonMark.get(itr.id) / aStudentsStat.lessonTotalMark.get(itr.id)) * 100.0
                );

        }

        for (QuestionStat itr : subjectsStat) {

            itr.calculateSD();

            for (QuestionStat aStudentsStat : studentsStat)
                aStudentsStat.calculateTaraz(
                        itr.mean, itr.sd,
                        itr.id, true
                );

        }

        for (QuestionStat itr : lessonsStat) {

            itr.calculateSD();

            for (QuestionStat aStudentsStat : studentsStat)
                aStudentsStat.calculateTaraz(
                        itr.mean, itr.sd,
                        itr.id, false
                );

        }

        k = 0;

        for (QuestionStat aStudentsStat : studentsStat) {
            aStudentsStat.calculateTotalTaraz();
            Document student = students.get(k);
            ArrayList<Document> lessonsStats = new ArrayList<>();

            for (QuestionStat itr : lessonsStat) {

                lessonsStats.add(new Document
                        ("stat", aStudentsStat.encode(itr.id, false))
                        .append("name", itr.name)
                        .append("_id", itr.id)
                );

            }

            student.put("lessons", lessonsStats);

            ArrayList<Document> subjectStats = new ArrayList<>();

            for (QuestionStat itr : subjectsStat) {

                subjectStats.add(new Document
                        ("stat", aStudentsStat.encode(itr.id, true))
                        .append("name", itr.name)
                        .append("_id", itr.id)
                );

            }

            student.put("subjects", subjectStats);
            k++;
        }

        studentsStat.sort(QuestionStat::compareTo);

        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for(QuestionStat itr : studentsStat)
            studentIds.add(itr.id);

        ArrayList<Document> studentsData = userRepository.findByIds(
                studentIds, true
        );

        HashMap<ObjectId, ObjectId> states = new HashMap<>();
        HashMap<ObjectId, PairValue> usersCities = new HashMap<>();

        HashMap<ObjectId, Integer> cityRanking = new HashMap<>();
        HashMap<Object, Integer> stateRanking = new HashMap<>();

        HashMap<ObjectId, Integer> citySkip = new HashMap<>();
        HashMap<Object, Integer> stateSkip = new HashMap<>();

        HashMap<ObjectId, Double> cityOldT = new HashMap<>();
        HashMap<Object, Double> stateOldT = new HashMap<>();

        for(Document itr : studentsData) {

            ObjectId cityId = itr.get("city", Document.class).getObjectId("_id");
            ObjectId stateId;

            if(states.containsKey(cityId))
                stateId = states.get(cityId);
            else {
                Document city = cityRepository.findById(cityId);
                stateId = city.getObjectId("state_id");
                states.put(cityId, stateId);
            }

            if(
                    !stateRanking.containsKey(stateId)
            ) {
                stateRanking.put(stateId, 0);
                stateOldT.put(stateId, -1.0);
                stateSkip.put(stateId, 1);
            }

            if(
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

        int rank = 0;
        int skip = 1;
        double oldTaraz = -1;

        quiz.put("report_status", "ready");

        ArrayList<Document> rankingList = new ArrayList<>();

        for (QuestionStat aStudentsStat : studentsStat) {

            PairValue p = usersCities.get(aStudentsStat.id);

            ObjectId stateId = (ObjectId) p.getValue();
            ObjectId cityId = (ObjectId) p.getKey();
            double currTaraz = aStudentsStat.taraz;

            if (oldTaraz != currTaraz) {
                rank += skip;
                skip = 1;
            } else
                skip++;

            if(stateOldT.get(stateId) != currTaraz) {
                stateRanking.put(stateId, stateRanking.get(stateId) + stateSkip.get(stateId));
                stateSkip.put(stateId, 1);
            }
            else
                stateSkip.put(stateId, stateSkip.get(stateId) + 1);

            if(cityOldT.get(cityId) != currTaraz) {
                cityRanking.put(cityId, cityRanking.get(cityId) + citySkip.get(cityId));
                citySkip.put(cityId, 1);
            }
            else
                citySkip.put(cityId, citySkip.get(cityId) + 1);

            rankingList.add(
                    new Document("_id", aStudentsStat.id)
                    .append("stat", encodeFormatGeneral(
                            (int)currTaraz, rank, stateRanking.get(stateId), cityRanking.get(cityId)
                    ))
            );
            stateRanking.put(stateId, stateRanking.get(stateId) + 1);
            cityRanking.put(cityId, cityRanking.get(cityId) + 1);

            oldTaraz = currTaraz;
            stateOldT.put(stateId, currTaraz);
            cityOldT.put(cityId, currTaraz);
        }

        quiz.put("ranking_list", rankingList);

        List<Document> subjectsGeneralStat = new ArrayList<>();

        for(QuestionStat itr : subjectsStat) {
            subjectsGeneralStat.add(
                    new Document("avg", itr.mean)
                        .append("max", itr.max)
                        .append("min", itr.min)
                        .append("_id", itr.id)
                        .append("name", itr.name)
            );
        }

        List<Document> lessonsGeneralStat = new ArrayList<>();

        for(QuestionStat itr : lessonsStat) {
            lessonsGeneralStat.add(
                    new Document("avg", itr.mean)
                            .append("max", itr.max)
                            .append("min", itr.min)
                            .append("_id", itr.id)
                            .append("name", itr.name)
            );
        }

        quiz.put("general_stat",
                new Document("lessons", lessonsGeneralStat)
                    .append("subjects", subjectsGeneralStat)
        );

        quiz.put("question_stat", questionStats);

        iryscQuizRepository.replaceOne(
                quiz.getObjectId("_id"), quiz
        );
    }
}
