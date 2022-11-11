package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.OpenQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class RegularQuizController extends QuizAbstract {

    // endRegistry consider as optional field

    // topStudentsGiftCoin or topStudentsGiftMoney or topStudentsCount
    // are optional and can inherit from config
//"duration",

    private final static String[] mandatoryFields = {
            "startRegistry", "start", "price",
            "end", "launchMode", "showResultsAfterCorrection",
            "showResultsAfterCorrectionNotLoginUsers"
    };

    private final static String[] forbiddenFields = {
            "paperTheme", "database"
    };

    public static String create(ObjectId userId, JSONObject jsonObject, String mode) {

        try {

            Utility.checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", "regular");
            Document newDoc = QuizController.store(userId, jsonObject);
            iryscQuizRepository.insertOne(newDoc);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "quiz", new RegularQuizController()
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

    @Override
    JSONObject convertDocToJSON(Document quiz, boolean isDigest,
                                boolean isAdmin, boolean afterBuy, boolean isDescNeeded) {

        JSONObject jsonObject = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("start", quiz.getLong("start"))
                .put("end", quiz.getLong("end"))
                .put("generalMode", AllKindQuiz.IRYSC.getName())
                .put("mode", quiz.getString("mode"))
                .put("launchMode", quiz.getString("launch_mode"))
                .put("tags", quiz.getList("tags", String.class))
                .put("reportStatus", quiz.getOrDefault("report_status", "not_ready"))
                .put("id", quiz.getObjectId("_id").toString());

        int questionsCount = 0;
        try {
            questionsCount = quiz.get("questions", Document.class)
                    .getList("_ids", ObjectId.class).size();
        } catch (Exception ignore) {
        }

        if (afterBuy) {
            long curr = System.currentTimeMillis();

            if (quiz.getLong("end") < curr) {
                boolean canSeeResult = quiz.getBoolean("show_results_after_correction") &&
                        quiz.containsKey("report_status") &&
                        quiz.getString("report_status").equalsIgnoreCase("ready");

                if(canSeeResult)
                    jsonObject.put("status", "finished")
                            .put("questionsCount", questionsCount);
                else
                    jsonObject.put("status", "waitForResult")
                            .put("questionsCount", questionsCount);
            }
            else if (quiz.getLong("start") <= curr &&
                    quiz.getLong("end") > curr
            ) {
                jsonObject
                        .put("status", "inProgress")
                        .put("duration", calcLen(quiz))
                        .put("questionsCount", questionsCount);
            }
            else
                jsonObject.put("status", "notStart");

        } else
            jsonObject.put("startRegistry", quiz.getLong("start_registry"))
                    .put("endRegistry", quiz.getOrDefault("end_registry", ""))
                    .put("price", quiz.get("price"));

        if (isAdmin) {
            jsonObject
                    .put("studentsCount", quiz.getInteger("registered"))
                    .put("visibility", quiz.getBoolean("visibility"))
                    .put("questionsCount", questionsCount)
                    .put("capacity", quiz.getInteger("capacity"));
        }

        if (quiz.containsKey("capacity"))
            jsonObject.put("reminder", Math.max(quiz.getInteger("capacity") - quiz.getInteger("registered"), 0));

        if(!isDigest || isDescNeeded)
            jsonObject
                    .put("description", quiz.getOrDefault("description", ""));

        if (!isDigest) {
            jsonObject
                    .put("topStudentsCount", quiz.getInteger("top_students_count"))
                    .put("showResultsAfterCorrection", quiz.getBoolean("show_results_after_correction"));

            if(isAdmin) {
                JSONArray attaches = new JSONArray();
                if(quiz.containsKey("attaches")) {
                    for (String attach : quiz.getList("attaches", String.class))
                        attaches.put(STATICS_SERVER + IRYSCQuizRepository.FOLDER + "/" + attach);
                }

                jsonObject.put("lenMode", quiz.containsKey("duration") ? "custom" : "question");
                if(quiz.containsKey("duration"))
                    jsonObject.put("len", quiz.getInteger("duration"));

                jsonObject.put("minusMark", quiz.getOrDefault("minus_mark", false));
                jsonObject.put("backEn", quiz.getOrDefault("back_en", false));
                jsonObject.put("permute", quiz.getOrDefault("permute", false));

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

    @Override
    public List<Document> registry(ObjectId studentId, String phone,
                                   String mail, List<ObjectId> quizIds,
                                   int paid
    ) {

        ArrayList<Document> added = new ArrayList<>();

        for (ObjectId quizId : quizIds) {

            try {
                Document quiz = iryscQuizRepository.findById(quizId);
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
                        .append("answers", new byte[0]);

                if ((boolean) quiz.getOrDefault("permute", false))
                    stdDoc.put("question_indices", new ArrayList<>());

                students.add(stdDoc);
                added.add(stdDoc);
                quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);

                iryscQuizRepository.replaceOne(
                        quizId, quiz
                );

                //todo : send notif
            } catch (Exception ignore) {}
        }

        return added;
    }

    public List<Document> registry(List<ObjectId> studentIds, String phone,
                                   String mail, List<ObjectId> quizIds,
                                   int paid
    ) {

        ArrayList<Document> added = new ArrayList<>();

        for (ObjectId quizId : quizIds) {

            try {
                Document quiz = iryscQuizRepository.findById(quizId);
                List<Document> students = quiz.getList("students", Document.class);

                for(ObjectId studentId : studentIds) {
                    if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                            students, "_id", studentId
                    ) != -1)
                        continue;

                    Document stdDoc = new Document("_id", studentId)
                            .append("paid", paid / quizIds.size())
                            .append("register_at", System.currentTimeMillis())
                            .append("finish_at", null)
                            .append("start_at", null)
                            .append("answers", new byte[0]);

                    if ((boolean) quiz.getOrDefault("permute", false))
                        stdDoc.put("question_indices", new ArrayList<>());

                    students.add(stdDoc);
                    added.add(stdDoc);
                    quiz.put("registered", (int) quiz.getOrDefault("registered", 0) + 1);
                }

                iryscQuizRepository.replaceOne(
                        quizId, quiz
                );

                //todo : send notif
            } catch (Exception ignore) {}
        }

        return added;
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

    static class Taraz {

        private Document quiz;

        private ArrayList<QuestionStat> lessonsStat;
        private ArrayList<QuestionStat> subjectsStat;
        private List<Document> questionsList;
        private List<ObjectId> questionIds;

        private List<Double> marks;
        private List<Document> students;
        private ArrayList<QuestionStat> studentsStat;
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

//            marks = questions.getList("marks", Double.class);

            students = quiz.getList("students", Document.class);
            questionIds = questions.getList("_ids", ObjectId.class);

            marks = new ArrayList<>();
            for(int i = 0; i < questionIds.size(); i++)
                marks.add(3.0);

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

            for (QuestionStat aStudentsStat : studentsStat)
                aStudentsStat.calculateTotalTaraz();

            studentsStat.sort(QuestionStat::compareTo);

            fetchUsersData();
            saveStudentsStats();

            prepareForCityRanking();
            calcCityRanking();

            calcSubjectsStats();
            calcLessonsStats();

            save(db);
            if(db instanceof IRYSCQuizRepository)
                storeInRankingTable();
        }

        public ArrayList<Document> lessonsStatOutput;
        public ArrayList<Document> subjectsStatOutput;

        Taraz(
                ArrayList<Document> questions, 
                ObjectId userId,
                ArrayList<PairValue> studentAnswers
        ) {

            lessonsStat = new ArrayList<>();
            subjectsStat = new ArrayList<>();
            questionsList = new ArrayList<>();
            studentsStat = new ArrayList<>();
            questionStats = new ArrayList<>();

            fetchQuestions(questions);

            studentsStat.add(new QuestionStat(
                    userId, "", studentAnswers
            ));

            doCorrectStudents();
            calcSubjectMarkSum();
            calcLessonMarkSum();

            calcSubjectsStandardDeviationAndTaraz();
            calcLessonsStandardDeviationAndTaraz();

            saveStudentStats();
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
        }

        private void fetchQuestions(ArrayList<Document> questions) {

            for (Document question : questions) {

                Document tmp = Document.parse(question.toJson());
                tmp.put("mark", 3.0);

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
        }

        private void initStudentStats() {
            for (Document student : students) {
                studentsStat.add(new QuestionStat(
                        student.getObjectId("_id"), "",
                        Utility.getAnswers(
                                student.get("answers", Binary.class).getData()
                        )
                ));
            }
        }

        private void doCorrectStudents() {

            int idx = 0;
            for (Document question : questionsList) {

                short corrects = 0, incorrects = 0, whites = 0;
                short status;

                for (QuestionStat aStudentsStat : studentsStat) {
                    status = aStudentsStat.doCorrect(question, idx);
                    if (status == 0)
                        whites++;
                    else if (status == 1)
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
        }

        private void calcSubjectMarkSum() {
            for (QuestionStat itr : subjectsStat) {
                for (QuestionStat aStudentsStat : studentsStat) {
                    itr.marks.add(
                            (aStudentsStat.subjectMark.get(itr.id) / aStudentsStat.subjectTotalMark.get(itr.id)) * 100.0
                    );
                }
            }
        }

        private void calcLessonMarkSum() {
            for (QuestionStat itr : lessonsStat) {
                for (QuestionStat aStudentsStat : studentsStat) {
                    itr.marks.add(
                            (aStudentsStat.lessonMark.get(itr.id) / aStudentsStat.lessonTotalMark.get(itr.id)) * 100.0
                    );
                }
            }
        }

        private void calcSubjectsStandardDeviationAndTaraz() {
            for (QuestionStat itr : subjectsStat) {
                itr.calculateSD();
                for (QuestionStat aStudentsStat : studentsStat)
                    aStudentsStat.calculateTaraz(
                            itr.mean, itr.sd,
                            itr.id, true
                    );
            }
        }

        private void calcLessonsStandardDeviationAndTaraz() {
            for (QuestionStat itr : lessonsStat) {
                itr.calculateSD();
                for (QuestionStat aStudentsStat : studentsStat)
                    aStudentsStat.calculateTaraz(
                            itr.mean, itr.sd,
                            itr.id, false
                    );
            }
        }

        private void saveStudentsStats() {

            for (QuestionStat aStudentsStat : studentsStat) {

                Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        students, "_id", aStudentsStat.id
                );

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
            }

        }


        private void saveStudentStats() {


            lessonsStatOutput = new ArrayList<>();

            for (QuestionStat itr : lessonsStat) {

                lessonsStatOutput.add(new Document
                        ("stat", studentsStat.get(0).encodeCustomQuiz(itr.id, false))
                        .append("name", itr.name)
                        .append("_id", itr.id)
                );

            }

            subjectsStatOutput = new ArrayList<>();

            for (QuestionStat itr : subjectsStat) {

                subjectsStatOutput.add(new Document
                        ("stat", studentsStat.get(0).encodeCustomQuiz(itr.id, true))
                        .append("name", itr.name)
                        .append("_id", itr.id)
                );

            }

        }


        private void fetchUsersData() {

            ArrayList<ObjectId> studentIds = new ArrayList<>();

            for (QuestionStat itr : studentsStat)
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

            for (QuestionStat itr : studentsStat) {

                ObjectId cityId;
                ObjectId schoolId;

                if(!studentsData.get(k).containsKey("city") ||
                        studentsData.get(k).get("city") == null
                )
                    cityId = unknownCity;
                else
                    cityId = studentsData.get(k).get("city", Document.class).getObjectId("_id");

                if(!studentsData.get(k).containsKey("school") ||
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
            for (QuestionStat itr : studentsStat) {
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
            for (QuestionStat itr : studentsStat) {
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
            for (QuestionStat itr : studentsStat) {
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
            for (QuestionStat itr : studentsStat) {
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

                rankingList.add(
                        new Document("_id", aStudentsStat.id)
                                .append("stat", encodeFormatGeneral(
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
            for (QuestionStat itr : subjectsStat) {
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
            for (QuestionStat itr : lessonsStat) {
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

            if(db instanceof OpenQuizRepository)
                quiz.put("last_build_at", System.currentTimeMillis());

            db.replaceOne(
                    quiz.getObjectId("_id"), quiz
            );

        }

        private void storeInRankingTable() {

            ArrayList<ObjectId> userIds = new ArrayList<>();

            for (QuestionStat aStudentsStat : studentsStat) {
                userIds.add(aStudentsStat.id);
            }

            ArrayList<Document> tarazRankingList = tarazRepository.findPreserveOrderWitNull("user_id", userIds);
            int idx = 0;
            ObjectId quizId = this.quiz.getObjectId("_id");

            List<BasicDBObject> updates = new ArrayList<>();
            boolean needUpdateIRYSCRank = false;
            List<WriteModel<Document>> writes = new ArrayList<>();
            ArrayList<ObjectId> gradesNeedUpdate = new ArrayList<>();

            for(Document doc : tarazRankingList) {

                BasicDBObject update = new BasicDBObject();

                if(!doc.containsKey("cum_sum_last_five"))
                    doc.put("cum_sum_last_five", 0);

                if(!doc.containsKey("cum_sum_last_three"))
                    doc.put("cum_sum_last_three", 0);

                ObjectId gradeId;

                if(!studentsData.get(idx).containsKey("grade") ||
                        studentsData.get(idx).get("grade") == null
                )
                    gradeId = null;
                else
                    gradeId = studentsData.get(idx).get("grade", Document.class).getObjectId("_id");

                if(!doc.containsKey("grade_id") && gradeId != null) {
                    doc.put("grade_id", gradeId);
                    update.append("grade_id", gradeId);
                }

                List<Document> quizzes = doc.containsKey("quizzes") ?
                        doc.getList("quizzes", Document.class) : new ArrayList<>();

                Document q = searchInDocumentsKeyVal(
                        quizzes, "_id", quizId
                );

                if(q == null) {
                    quizzes.add(new Document("_id", quizId)
                            .append("taraz", (int)studentsStat.get(idx).taraz)
                            .append("start", this.quiz.getLong("start"))
                    );
                }
                else
                    q.put("taraz", (int) studentsStat.get(idx).taraz);

                quizzes.sort((o1, o2) ->
                        Long.compare(o2.getLong("start"),
                                o1.getLong("start")));

                update.append("quizzes", quizzes);

                int index = searchInDocumentsKeyValIdx(
                        quizzes, "_id", quizId
                );

                if(index < 5) {
                    int cumSum = 0;
                    for(int i = 0; i < Math.min(5, quizzes.size()); i++)
                        cumSum += quizzes.get(i).getInteger("taraz");

                    doc.put("cum_sum_last_five", cumSum);
                    update.append("cum_sum_last_five", cumSum);
                    needUpdateIRYSCRank = true;
                }

                if(index < 3) {
                    int cumSum = 0;
                    for(int i = 0; i < Math.min(3, quizzes.size()); i++)
                        cumSum += quizzes.get(i).getInteger("taraz");

                    doc.put("cum_sum_last_three", cumSum);
                    update.append("cum_sum_last_three", cumSum);
                    if(gradeId != null &&
                            !gradesNeedUpdate.contains(gradeId))
                        gradesNeedUpdate.add(gradeId);
                }

                updates.add(update);
                idx++;
            }

            List<Document> allTaraz;

            if(needUpdateIRYSCRank) {

                allTaraz = tarazRepository.find(null, null);
                for (Document tarazRanking : tarazRankingList) {
                    int index = searchInDocumentsKeyValIdx(
                            allTaraz, "_id", tarazRanking.getObjectId("_id")
                    );

                    if(index != -1)
                        allTaraz.set(index, tarazRanking);
                    else
                        allTaraz.add(tarazRanking);
                }

                allTaraz.sort((o1, o2) ->
                        Integer.compare(o2.getInteger("cum_sum_last_five"),
                                o1.getInteger("cum_sum_last_five")));


                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for (Document document : allTaraz) {

                    if (oldTaraz != document.getInteger("cum_sum_last_five")) {
                        rank += skip;
                        skip = 1;
                    } else
                        skip++;

                    int index = searchInDocumentsKeyValIdx(
                            tarazRankingList, "_id", document.getObjectId("_id")
                    );

                    if(index == -1)
                        writes.add(
                                new UpdateOneModel<>(
                                        eq("_id", document.getObjectId("_id")),
                                        set("rank", rank)
                                )
                        );
                    else {
                        updates.get(index).append("rank", rank);
                        writes.add(
                                new UpdateOneModel<>(
                                        eq("user_id", userIds.get(index)),
                                        new BasicDBObject("$set", updates.get(index)),
                                        new UpdateOptions().upsert(true)
                                )
                        );
                    }

                    oldTaraz = document.getInteger("cum_sum_last_five");
                }
            }

            if(gradesNeedUpdate.size() > 0) {

                for(ObjectId gradeId : gradesNeedUpdate) {

                    allTaraz = tarazRepository.find(eq("grade_id", gradeId), null);

                    for (Document tarazRanking : tarazRankingList) {

                        if(
                                gradeId == null || tarazRanking == null ||
                                !tarazRanking.containsKey("grade_id") ||
                                tarazRanking.get("grade_id") == null ||
                                !tarazRanking.get("grade_id").equals(gradeId))
                            continue;

                        int index = searchInDocumentsKeyValIdx(
                                allTaraz, "_id", tarazRanking.getObjectId("_id")
                        );

                        if(index != -1)
                            allTaraz.set(index, tarazRanking);
                        else
                            allTaraz.add(tarazRanking);
                    }

                    allTaraz.sort((o1, o2) ->
                            Integer.compare(o2.getInteger("cum_sum_last_three"),
                                    o1.getInteger("cum_sum_last_three")));


                    int rank = 0;
                    int oldTaraz = -1;
                    int skip = 1;

                    for (Document document : allTaraz) {

                        if (oldTaraz != document.getInteger("cum_sum_last_three")) {
                            rank += skip;
                            skip = 1;
                        } else
                            skip++;

                        int index = searchInDocumentsKeyValIdx(
                                tarazRankingList, "_id", document.getObjectId("_id")
                        );

                        if(index == -1)
                            writes.add(
                                    new UpdateOneModel<>(
                                            eq("_id", document.getObjectId("_id")),
                                            set("grade_rank", rank)
                                    )
                            );
                        else {
                            updates.get(index).append("grade_rank", rank);
                            writes.add(
                                    new UpdateOneModel<>(
                                            eq("user_id", userIds.get(index)),
                                            new BasicDBObject("$set", updates.get(index)),
                                            new UpdateOptions().upsert(true)
                                    )
                            );
                        }

                        oldTaraz = document.getInteger("cum_sum_last_three");
                    }

                }
            }

            tarazRepository.bulkWrite(writes);
        }

    }

    void createTaraz(Document quiz) {
        new Taraz(quiz, iryscQuizRepository);
    }
}
