package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.OpenQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Excel;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Quiz.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdminReportController {

    public static String getStudentStatCustomQuiz(ObjectId quizId, ObjectId userId) {

        Document doc = customQuizRepository.findOne(
                and(
                        eq("_id", quizId),
                        eq("user_id", userId),
                        ne("status", "wait")
                ), null
        );

        if(doc == null || !doc.containsKey("start_at") ||
                doc.get("start_at") == null)
            return JSON_NOT_ACCESS;

        int neededTime = doc.getInteger("duration");
        long curr = System.currentTimeMillis();

        int untilYetInSecondFormat =
                (int) ((curr - doc.getLong("start_at")) / 1000);

        int reminder = neededTime - untilYetInSecondFormat;

        if (reminder > 0)
            return generateErr("زمان مرور آزمون هنوز فرانرسیده است.");

        if(!doc.getString("status").equalsIgnoreCase("finished")) {
            doc.put("status", "finished");

            ArrayList<PairValue> studentAnswers = doc.containsKey("student_answers") ?
                    Utility.getAnswers(
                            doc.get("student_answers", Binary.class).getData()
                    ) : new ArrayList<>();

            ArrayList<Document> questions = questionRepository.findByIds(
                    doc.getList("questions", ObjectId.class), true
            );

            if(questions == null)
                return JSON_NOT_UNKNOWN;

            RegularQuizController.Taraz t  = new RegularQuizController.Taraz(
                    questions, userId, studentAnswers
            );

            doc.put("lessons", t.lessonsStatOutput);
            doc.put("subjects", t.subjectsStatOutput);

            customQuizRepository.replaceOne(quizId, doc);

            Utilities.updateQuestionsStatWithByteArr(
                    questions, t.questionStats
            );

            doc = customQuizRepository.findById(quizId);
        }

        Document config = getConfig();

        JSONObject data = new JSONObject();
        JSONArray lessons = new JSONArray();

        int totalCorrect = 0;
        for (Document lesson : doc.getList("lessons", Document.class)) {

            Object[] stats = QuizAbstract.decodeCustomQuiz(lesson.get("stat", Binary.class).getData());
            totalCorrect += (int) stats[2];

            JSONObject jsonObject = new JSONObject()
                    .put("name", lesson.getString("name"))
                    .put("whites", stats[0])
                    .put("corrects", stats[1])
                    .put("incorrects", stats[2])
                    .put("total", (int) stats[0] + (int) stats[1] + (int) stats[2]);

            lessons.put(jsonObject);
        }

        JSONArray subjects = new JSONArray();
        for (Document subject : doc.getList("subjects", Document.class)) {

            Object[] stats = QuizAbstract.decodeCustomQuiz(subject.get("stat", Binary.class).getData());

            JSONObject jsonObject = new JSONObject()
                    .put("name", subject.getString("name"))
                    .put("whites", stats[0])
                    .put("corrects", stats[1])
                    .put("incorrects", stats[2])
                    .put("total", (int) stats[0] + (int) stats[1] + (int) stats[2]);

            subjects.put(jsonObject);
        }

        data.put("lessons", lessons);
        data.put("subjects", subjects);
        data.put("totalCorrects", totalCorrect);

        data.put("quizName", doc.getString("name"));

        if (config.containsKey("taraz_levels")) {
            List<Document> levels = config.getList("taraz_levels", Document.class);
            levels.sort(Comparator.comparing(o -> o.getInteger("priority")));

            JSONArray conditions = new JSONArray();
            for (int i = levels.size() - 1; i >= 0; i--) {
                Document level = levels.get(i);
                conditions.put(new JSONObject()
                        .put("min", level.getInteger("min"))
                        .put("max", level.getInteger("max"))
                        .put("color", level.getString("color")));
            }
            data.put("conditions", conditions);
        }

        data.put("student", new JSONObject().put("id", userId.toString()));
        return generateSuccessMsg(
                "data", data
        );
    }

    public static String getStudentStat(Common db, Object user,
                                        ObjectId quizId, ObjectId studentId,
                                        boolean userIsNotLogin) {

        try {

            Document studentDoc = userRepository.findById(studentId);

            if (studentDoc == null)
                return JSON_NOT_VALID_ID;

            boolean isIRYSCQuiz = db instanceof IRYSCQuizRepository;

            if(userIsNotLogin && !isIRYSCQuiz)
                return JSON_NOT_ACCESS;

            Document quiz = isIRYSCQuiz ?
                    hasPublicAccess(db, user, quizId) :
                    hasProtectedAccess(db, user == null ? null : (ObjectId) user, quizId);

            if(user != null &&
                    !(boolean)quiz.getOrDefault("show_results_after_correction", true))
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            if(userIsNotLogin &&
                    !(boolean)quiz.getOrDefault("show_results_after_correction_not_login_users", false))
                return generateErr("برای رویت نتایج باید وارد سامانه شوید.");

            List<Document> students = quiz.getList("students", Document.class);

            Document student = searchInDocumentsKeyVal(
                    students, "_id", studentId
            );

            if (student == null)
                return JSON_NOT_VALID_ID;

            long curr = System.currentTimeMillis();

            if(user != null && db instanceof OpenQuizRepository) {

                Document userDocInQuiz = searchInDocumentsKeyVal(
                        students, "_id", user
                );

                if(userDocInQuiz == null)
                    return JSON_NOT_ACCESS;

                int neededTime = new RegularQuizController().calcLen(quiz);
                long startAt = userDocInQuiz.getLong("start_at");

                int reminder = neededTime -
                        (int) ((curr - startAt) / 1000);

                if(reminder > 0)
                    return generateErr("هنوز زمان مشاهده نتایج فرا نرسیده است.");

                if(quiz.getLong("last_build_at") == null ||
                        quiz.getLong("last_build_at") < quiz.getLong("last_finished_at")
                ) {
                    new RegularQuizController.Taraz(quiz, openQuizRepository);
                    openQuizRepository.clearFromCache(quiz.getObjectId("_id"));
                    quiz = openQuizRepository.findById(quizId);

                    students = quiz.getList("students", Document.class);

                    student = searchInDocumentsKeyVal(
                            students, "_id", studentId
                    );

                }
            }

            Document config = getConfig();

            if (
                    !quiz.containsKey("general_stat") ||
                            !quiz.containsKey("ranking_list")
//                            ||
//                            !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");


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

            int totalCorrect = 0;
            for (Document doc : student.getList("lessons", Document.class)) {

                Document generalStat = searchInDocumentsKeyVal(
                        lessonsGeneralStats, "_id", doc.getObjectId("_id")
                );

                Object[] stats = QuizAbstract.decode(doc.get("stat", Binary.class).getData());
                totalCorrect += (int) stats[2];

                JSONObject jsonObject = new JSONObject()
                        .put("name", doc.getString("name"))
                        .put("taraz", stats[0])
                        .put("whites", stats[1])
                        .put("corrects", stats[2])
                        .put("incorrects", stats[3])
                        .put("total", (int) stats[1] + (int) stats[2] + (int) stats[3])
                        .put("percent", stats[4])
                        .put("countryRank", stats[5])
                        .put("stateRank", stats[6])
                        .put("cityRank", stats[7])
                        .put("schoolRank", stats[8])
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
                        .put("countryRank", stats[5])
                        .put("stateRank", stats[6])
                        .put("cityRank", stats[7])
                        .put("schoolRank", stats[8])
                        .put("total", (int) stats[1] + (int) stats[2] + (int) stats[3])
                        .put("avg", df_obj.format(generalStat.getDouble("avg")))
                        .put("max", df_obj.format(generalStat.getDouble("max")))
                        .put("min", df_obj.format(generalStat.getDouble("min")));

                subjects.put(jsonObject);
            }

            data.put("lessons", lessons);
            data.put("subjects", subjects);
            data.put("totalCorrects", totalCorrect);
            data.put("totalQuizzes", iryscQuizRepository.count(
                    and(
                            in("students._id", studentId),
                            lt("start", curr)
                    )
            ));

            Object[] stat = QuizAbstract.decodeFormatGeneral(
                    studentGeneralStat.get("stat", Binary.class).getData()
            );

            JSONObject jsonObject = new JSONObject()
                    .put("taraz", stat[0])
                    .put("cityRank", stat[3])
                    .put("stateRank", stat[2])
                    .put("countryRank", stat[1]);

            data.put("rank", jsonObject);
            data.put("quizName", quiz.getString("title"));

            if (config.containsKey("taraz_levels")) {
                List<Document> levels = config.getList("taraz_levels", Document.class);
                levels.sort(Comparator.comparing(o -> o.getInteger("priority")));

                JSONArray conditions = new JSONArray();
                for (int i = levels.size() - 1; i >= 0; i--) {
                    Document level = levels.get(i);
                    conditions.put(new JSONObject()
                            .put("min", level.getInteger("min"))
                            .put("max", level.getInteger("max"))
                            .put("color", level.getString("color")));
                }
                data.put("conditions", conditions);
            }

            irysc.gachesefid.Utility.Utility.fillJSONWithUser(data, studentDoc);

            return generateSuccessMsg(
                    "data", data
            );

        } catch (InvalidFieldsException e) {
            return JSON_NOT_ACCESS;
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
            fillWithAnswerSheetData(answersJsonArray, null, pairValues, marks);
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

    public static String getStudentAnswerSheetCustomQuiz(ObjectId quizId, ObjectId studentId) {

        Document doc = customQuizRepository.findOne(
                and(
                        eq("_id", quizId),
                        eq("user_id", studentId),
                        ne("status", "wait")
                ), null
        );

        if(doc == null || !doc.containsKey("start_at") ||
                doc.get("start_at") == null)
            return JSON_NOT_ACCESS;

        int neededTime = doc.getInteger("duration");
        long curr = System.currentTimeMillis();

        int untilYetInSecondFormat =
                (int) ((curr - doc.getLong("start_at")) / 1000);

        int reminder = neededTime - untilYetInSecondFormat;

        if (reminder > 0)
            return generateErr("زمان مرور آزمون هنوز فرانرسیده است.");

        ArrayList<PairValue> pairValues = Utility.getAnswers(
                ((Binary) doc.getOrDefault("answers", new byte[0])).getData()
        );

        ArrayList<Double> marks = new ArrayList<>();
        for(int i = 0; i < doc.getList("questions", ObjectId.class).size(); i++)
            marks.add(3.0);

        JSONArray answersJsonArray = new JSONArray();
        fillWithAnswerSheetData(answersJsonArray, null, pairValues, marks);
        Binary binary = (Binary) doc.getOrDefault("student_answers", null);
        ArrayList<PairValue> stdAnswers = binary == null ? new ArrayList<>() : Utility.getAnswers(binary.getData());

        for (int i = 0; i < pairValues.size(); i++) {

            if (i >= stdAnswers.size())
                answersJsonArray.getJSONObject(i).put("studentAns", "");
            else {
                if (pairValues.get(i).getKey().toString().equalsIgnoreCase(QuestionType.TEST.getName()))
                    answersJsonArray.getJSONObject(i).put("studentAns", ((PairValue) stdAnswers.get(i).getValue()).getValue());
                else
                    answersJsonArray.getJSONObject(i).put("studentAns", stdAnswers.get(i).getValue());
            }
        }

        return generateSuccessMsg("data", answersJsonArray);
    }

    public static String getStudentAnswerSheet(Common db, ObjectId userId,
                                               ObjectId quizId, ObjectId studentId) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            if(!(boolean)doc.getOrDefault("show_results_after_correction", true))
                return generateErr("زمان رویت نتایج فرانرسیده است.");

            List<Document> students = doc.getList("students", Document.class);

            Document student = searchInDocumentsKeyVal(
                    students, "_id", studentId
            );
            if (student == null)
                return JSON_NOT_ACCESS;

            Document questions = doc.get("questions", Document.class);
            List<Double> marks = questions.getList("marks", Double.class);
            ArrayList<PairValue> pairValues = Utility.getAnswers(((Binary) questions.getOrDefault("answers", new byte[0])).getData());
            List<Binary> questionStats = null;
            if (doc.containsKey("question_stat")) {
                questionStats = doc.getList("question_stat", Binary.class);
                if (questionStats.size() != pairValues.size())
                    questionStats = null;
            }

            JSONArray answersJsonArray = new JSONArray();
            fillWithAnswerSheetData(answersJsonArray, questionStats, pairValues, marks);
            ArrayList<PairValue> stdAnswers = Utility.getAnswers(((Binary) student.getOrDefault("answers", new byte[0])).getData());

            for (int i = 0; i < pairValues.size(); i++) {

                if (i >= stdAnswers.size())
                    answersJsonArray.getJSONObject(i).put("studentAns", "");
                else {
                    if (pairValues.get(i).getKey().toString().equalsIgnoreCase(QuestionType.TEST.getName()))
                        answersJsonArray.getJSONObject(i).put("studentAns", ((PairValue) stdAnswers.get(i).getValue()).getValue());
                    else
                        answersJsonArray.getJSONObject(i).put("studentAns", stdAnswers.get(i).getValue());
                }
            }

            return generateSuccessMsg("data", answersJsonArray);

        } catch (Exception x) {
            return null;
        }
    }

    //todo : other accesses
    public static String getStateReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (!quiz.containsKey("ranking_list"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> stateTaraz = new HashMap<>();
        HashMap<ObjectId, ObjectId> citiesState = new HashMap<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for (Document itr : rankingList) {
            studentIds.add(itr.getObjectId("_id"));
        }

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for (Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            ObjectId cityId = studentsInfo.get(k)
                    .get("city", Document.class).getObjectId("_id");

            ObjectId stateId;

            if (citiesState.containsKey(cityId))
                stateId = citiesState.get(cityId);
            else {
                Document city = cityRepository.findById(cityId);
                stateId = city.getObjectId("state_id");
                citiesState.put(cityId, stateId);
            }

            ArrayList<Integer> tmp;
            if (stateTaraz.containsKey(stateId))
                tmp = stateTaraz.get(stateId);
            else
                tmp = new ArrayList<>();

            tmp.add((Integer) stats[0]);
            stateTaraz.put(stateId, tmp);
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for (ObjectId stateId : stateTaraz.keySet()) {

            Document state = stateRepository.findById(stateId);
            ArrayList<Integer> allTaraz = stateTaraz.get(stateId);

            int sum = 0;
            for (int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", state.getString("name"))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for (int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getCityReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (!quiz.containsKey("ranking_list"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> cityTaraz = new HashMap<>();
        HashMap<ObjectId, String> cities = new HashMap<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for (Document itr : rankingList)
            studentIds.add(itr.getObjectId("_id"));

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for (Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            Document city = studentsInfo.get(k)
                    .get("city", Document.class);

            ObjectId cityId = city.getObjectId("_id");

            if (!cities.containsKey(cityId))
                cities.put(cityId, city.getString("name"));

            ArrayList<Integer> tmp;
            if (cityTaraz.containsKey(cityId))
                tmp = cityTaraz.get(cityId);
            else
                tmp = new ArrayList<>();

            tmp.add((Integer) stats[0]);
            cityTaraz.put(cityId, tmp);
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for (ObjectId cityId : cityTaraz.keySet()) {

            ArrayList<Integer> allTaraz = cityTaraz.get(cityId);

            int sum = 0;
            for (int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", cities.get(cityId))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for (int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getSchoolReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (!quiz.containsKey("ranking_list"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> schoolTaraz = new HashMap<>();
        HashMap<ObjectId, String> schools = new HashMap<>();

        ArrayList<Integer> unknownTaraz = new ArrayList<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for (Document itr : rankingList) {
            studentIds.add(itr.getObjectId("_id"));
        }

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for (Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());
            Document school = studentsInfo.get(k)
                    .get("school", Document.class);

            if(school == null)
                unknownTaraz.add((Integer) stats[0]);
            else {

                ObjectId schoolId = school.getObjectId("_id");

                if (!schools.containsKey(schoolId))
                    schools.put(schoolId, school.getString("name"));

                ArrayList<Integer> tmp;
                if (schoolTaraz.containsKey(schoolId))
                    tmp = schoolTaraz.get(schoolId);
                else
                    tmp = new ArrayList<>();

                tmp.add((Integer) stats[0]);
                schoolTaraz.put(schoolId, tmp);
            }
            k++;
        }

        List<JSONObject> list = new ArrayList<>();
        for (ObjectId schoolId : schoolTaraz.keySet()) {

            ArrayList<Integer> allTaraz = schoolTaraz.get(schoolId);

            int sum = 0;
            for (int itr : allTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", schools.get(schoolId))
                    .put("count", allTaraz.size())
                    .put("avg", sum / allTaraz.size())
            );
        }

        if(unknownTaraz.size() > 0) {

            int sum = 0;
            for (int itr : unknownTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", "نامشخص")
                    .put("count", unknownTaraz.size())
                    .put("avg", sum / unknownTaraz.size())
            );
        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for (int i = list.size() - 1; i >= 0; i--)
            data.put(list.get(i).put("rank", k++));

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String getGenderReport(ObjectId quizId) {


        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        if (!quiz.containsKey("ranking_list"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();

        ArrayList<Integer> maleTaraz = new ArrayList<>();
        ArrayList<Integer> femaleTaraz = new ArrayList<>();
        ArrayList<Integer> unknownTaraz = new ArrayList<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for (Document itr : rankingList)
            studentIds.add(itr.getObjectId("_id"));

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for (Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());

            if(!studentsInfo.get(k).containsKey("sex") ||
                    studentsInfo.get(k).get("sex") == null
            )
                unknownTaraz.add((int) stats[0]);
            else if (studentsInfo.get(k).getString("sex").equalsIgnoreCase("male"))
                maleTaraz.add((int) stats[0]);
            else
                femaleTaraz.add((int) stats[0]);

            k++;
        }

        List<JSONObject> list = new ArrayList<>();

        int sum = 0;
        for (int itr : maleTaraz)
            sum += itr;

        list.add(new JSONObject()
                .put("label", "آقا")
                .put("count", maleTaraz.size())
                .put("avg", maleTaraz.size() == 0 ? 0 : sum / maleTaraz.size())
        );

        sum = 0;
        for (int itr : femaleTaraz)
            sum += itr;

        list.add(new JSONObject()
                .put("label", "خانم")
                .put("count", femaleTaraz.size())
                .put("avg", femaleTaraz.size() == 0 ? 0 : sum / femaleTaraz.size())
        );

        if(unknownTaraz.size() > 0) {

            sum = 0;
            for (int itr : unknownTaraz)
                sum += itr;

            list.add(new JSONObject()
                    .put("label", "نامشخص")
                    .put("count", unknownTaraz.size())
                    .put("avg", sum / unknownTaraz.size())
            );

        }

        list.sort(Comparator.comparingDouble(o -> o.getDouble("avg")));

        k = 1;
        for (int i = list.size() - 1; i >= 0; i--)
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

        if (questions == null)
            return JSON_NOT_UNKNOWN;

        List<Binary> questionStats = null;

        if (quiz.containsKey("question_stat")) {
            questionStats = quiz.getList("question_stat", Binary.class);

            if (questionStats.size() != questions.size())
                questionStats = null;
        }

        int k = 0;

        for (Document itr : questions) {

            String author = itr.getString("author");

            ArrayList<Integer> tmp;
            if (authorPercents.containsKey(author))
                tmp = authorPercents.get(author);
            else
                tmp = new ArrayList<>();

            if (questionStats != null) {
                byte[] stats = questionStats.get(k).getData();
                int percent = (stats[1] * 100) / (stats[0] + stats[1] + stats[2]);
                tmp.add(percent);
            } else
                tmp.add(0);

            authorPercents.put(author, tmp);
            k++;
        }

        for (String author : authorPercents.keySet()) {

            ArrayList<Integer> allPercent = authorPercents.get(author);

            int sum = 0;
            for (int itr : allPercent) {
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

    private static ArrayList<Document> filterStudentsWithAccess(ArrayList<ObjectId> students,
                                                                Document user) {

        List<String> accesses = user.getList("accesses", String.class);

        boolean isAdmin = Authorization.isAdmin(accesses);
        ArrayList<ObjectId> studentsIdAfterFilter = new ArrayList<>();

        if(isAdmin)
            studentsIdAfterFilter = students;
        else {

            if (Authorization.isSchool(accesses)) {

                if(!user.containsKey("students"))
                    return null;

                List<ObjectId> myStudentsId = user.getList("students", ObjectId.class);
                for (ObjectId std : students) {

                    if (!myStudentsId.contains(std))
                        continue;

                    studentsIdAfterFilter.add(std);
                }
            }
        }

        return userRepository.findByIds(
                studentsIdAfterFilter, true
        );
    }

    public static String getParticipantReport(ObjectId quizId, Document user) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null)
            return JSON_NOT_VALID_ID;

        JSONArray data = new JSONArray();
        List<Document> students = quiz.getList("students", Document.class);
        ArrayList<ObjectId> studentsIdBeforeFilter = new ArrayList<>();

        for (Document student : students)
            studentsIdBeforeFilter.add(student.getObjectId("_id"));

        ArrayList<Document> studentsInfo = filterStudentsWithAccess(
                studentsIdBeforeFilter,
                user
        );

        if(studentsInfo == null || studentsInfo.size() == 0)
            return JSON_NOT_ACCESS;

        int i = 0;
        for (Document student : studentsInfo) {
            JSONObject jsonObject = new JSONObject()
                    .put("startAt", students.get(i).containsKey("start_at") && students.get(i).get("start_at") != null ?
                            getSolarDate(students.get(i).getLong("start_at")) : "")
                    .put("finishAt", students.get(i).containsKey("finish_at") && students.get(i).get("finish_at") != null ?
                            getSolarDate(students.get(i).getLong("finish_at")) : "")
                    .put("name", student.getString("first_name") + " " + student.getString("last_name"))
                    .put("id", student.getObjectId("_id"));

            data.put(jsonObject);
            i++;
        }

        return generateSuccessMsg(
                "data", data
        );

    }

    public static String A1(Common db, ObjectId userId,
                            ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            long curr = System.currentTimeMillis();

            if (quiz.getLong("end") > curr || !quiz.containsKey("report_status") ||
                    !quiz.getString("report_status").equals("ready"))
                return JSON_NOT_ACCESS;

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
            if (quiz.containsKey("question_stat")) {
                questionStats = quiz.getList("question_stat", Binary.class);
                if (questionStats.size() != questionsMark.size())
                    questionStats = null;
            }

            JSONArray questionsJSONArr = Utilities.convertList(
                    questionsList, true, true, true, true, false
            );

            if (questionStats != null) {
                for (i = 0; i < questionsJSONArr.length(); i++) {

                    JSONObject jsonObject = questionsJSONArr.getJSONObject(i);

                    byte[] bytes = questionStats.get(i).getData();
                    jsonObject
                            .put("oldCorrect", bytes[1] & 0xff)
                            .put("oldIncorrect", bytes[2] & 0xff)
                            .put("oldWhite", bytes[0] & 0xff);
                }
            }

            return generateSuccessMsg("data", questionsJSONArr);

        } catch (Exception x) {
            System.out.println(x.getMessage());
            return null;
        }
    }


    public static String getKarnameReport(Common db, boolean isAdmin,
                                                       ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, isAdmin ? null : userId, quizId);

            if (
                    !quiz.containsKey("report_status") ||
                            !quiz.containsKey("ranking_list") ||
                            !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("زمان رویت نتایج هنوز فرانرسیده است.");

            JSONArray jsonArray = new JSONArray();
            ArrayList<ObjectId> userIds = new ArrayList<>();

            for (Document doc : quiz.getList("ranking_list", Document.class))
                userIds.add(doc.getObjectId("_id"));

            ArrayList<Document> studentsInfo = userRepository.findByIds(
                    userIds, true
            );

            List<Document> studentResults = quiz.getList("students", Document.class);

            HashMap<ObjectId, String> stateNames = new HashMap<>();
            int k = 0;

            for (Document doc : quiz.getList("ranking_list", Document.class)) {

                Document studentResult = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        studentResults, "_id", doc.getObjectId("_id")
                );
                if(studentResult == null)
                    continue;

                Object[] stat = QuizAbstract.decodeFormatGeneral(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("name", studentsInfo.get(k).getString("first_name") + " " + studentsInfo.get(k).getString("last_name"))
                        .put("taraz", stat[0])
                        .put("cityRank", stat[3])
                        .put("stateRank", stat[2])
                        .put("rank", stat[1]);

                JSONArray lessonsStats = new JSONArray();

                for(Document lessonStat : studentResult.getList("lessons", Document.class)) {
                    Object[] lessonStats = QuizAbstract.decode(lessonStat.get("stat", Binary.class).getData());
                    lessonsStats.put(
                            new JSONObject()
                                    .put("name", lessonStat.getString("name"))
                                    .put("percent", lessonStats[4])
                    );
                }

                jsonObject.put("lessonsStats", lessonsStats);

                if(!studentsInfo.get(k).containsKey("city") ||
                        studentsInfo.get(k).get("city") == null) {
                    jsonObject.put("state", "نامشخص");
                    jsonObject.put("city", "نامشخص");
                }
                else {

                    ObjectId cityId = studentsInfo.get(k).get("city", Document.class).getObjectId("_id");

                    if (stateNames.containsKey(cityId))
                        jsonObject.put("state", stateNames.get(cityId));
                    else {
                        Document city = cityRepository.findById(cityId);
                        Document state = stateRepository.findById(city.getObjectId("state_id"));
                        stateNames.put(cityId, state.getString("name"));
                        jsonObject.put("state", stateNames.get(cityId));
                    }

                    jsonObject.put("city", studentsInfo.get(k).get("city", Document.class).getString("name"));
                }

                if(
                        !studentsInfo.get(k).containsKey("school") ||
                                studentsInfo.get(k).get("school") == null
                )
                    jsonObject.put("school", "آیریسک");
                else
                    jsonObject.put("school", studentsInfo.get(k).get("school", Document.class).getString("name"));

                jsonArray.put(jsonObject);
                k++;
            }

            return generateSuccessMsg(
                    "data", jsonArray
            );

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

}
