package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionType;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public static String getStudentStat(Common db, Object user,
                                        ObjectId quizId, ObjectId studentId) {

        try {

            Document studentDoc = userRepository.findById(studentId);

            if (studentDoc == null)
                return JSON_NOT_VALID_ID;

            Document quiz = hasPublicAccess(db, user, quizId);
            long curr = System.currentTimeMillis();
            Document config = getConfig();

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

            Object[] stat = QuizAbstract.decodeFormatGeneral(studentGeneralStat.get("stat", Binary.class).getData());

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

    public static String getStudentAnswerSheet(Common db, ObjectId userId,
                                               ObjectId quizId, ObjectId studentId) {
        try {
            Document doc = hasAccess(db, userId, quizId);
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
            System.out.println(x.getMessage());
            return null;
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

        JSONArray data = new JSONArray();

        HashMap<ObjectId, ArrayList<Integer>> schoolTaraz = new HashMap<>();
        HashMap<ObjectId, String> schools = new HashMap<>();

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

        JSONArray data = new JSONArray();

        ArrayList<Integer> maleTaraz = new ArrayList<>();
        ArrayList<Integer> femaleTaraz = new ArrayList<>();

        List<Document> rankingList = quiz.getList("ranking_list", Document.class);
        ArrayList<ObjectId> studentIds = new ArrayList<>();

        for (Document itr : rankingList)
            studentIds.add(itr.getObjectId("_id"));

        ArrayList<Document> studentsInfo = userRepository.findByIds(studentIds, true);

        int k = 0;

        for (Document itr : rankingList) {

            Object[] stats = QuizAbstract.decodeFormatGeneral(itr.get("stat", Binary.class).getData());

            if (studentsInfo.get(k).getString("sex").equalsIgnoreCase("male"))
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

        List<Binary> questionStats = quiz.getList("question_stat", Binary.class);

        if (questionStats.size() != questions.size())
            return JSON_NOT_UNKNOWN;

        int k = 0;

        for (Document itr : questions) {

            String author = itr.getString("author");
            byte[] stats = questionStats.get(k).getData();
            int percent = (stats[1] * 100) / (stats[0] + stats[1] + stats[2]);

            ArrayList<Integer> tmp;
            if (authorPercents.containsKey(author))
                tmp = authorPercents.get(author);
            else
                tmp = new ArrayList<>();

            tmp.add(percent);
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
}
