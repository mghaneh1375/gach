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

import java.util.*;
import java.util.stream.Collectors;

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

    class Taraz {

        private final Document quiz;

        private final ArrayList<QuestionStat> lessonsStat;
        private final ArrayList<QuestionStat> subjectsStat;
        private final List<Document> questionsList;
        private final List<ObjectId> questionIds;

        private final List<Double> marks;
        private final List<Document> students;
        private final ArrayList<QuestionStat> studentsStat;
        private final ArrayList<byte[]> questionStats;
        private ArrayList<Document> studentsData;

        private final HashMap<ObjectId, ObjectId> states;
        private final HashMap<ObjectId, PairValue> usersCities;

        private final HashMap<ObjectId, Integer> cityRanking;
        private final HashMap<Object, Integer> stateRanking;

        private final HashMap<ObjectId, Integer> citySkip;
        private final HashMap<Object, Integer> stateSkip;

        private final HashMap<ObjectId, Double> cityOldT;
        private final HashMap<Object, Double> stateOldT;

        private final ArrayList<Document> rankingList;
        private final List<Document> subjectsGeneralStat;
        private final List<Document> lessonsGeneralStat;

        HashMap<ObjectId, List<TarazRanking>> lessonsTarazRanking = new HashMap<>();
        HashMap<ObjectId, List<TarazRanking>> subjectsTarazRanking = new HashMap<>();
        HashMap<ObjectId, ObjectId> statesDic = new HashMap<>();

        Taraz(Document quiz) {

            this.quiz = quiz;
            Document questions = quiz.get("questions", Document.class);
            marks = questions.getList("marks", Double.class);
            students = quiz.getList("students", Document.class);
            questionIds = questions.getList("_ids", ObjectId.class);

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

            save();
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

        }

        private void calcSubjectMarkSum() {
            for (QuestionStat itr : subjectsStat) {
                for (QuestionStat aStudentsStat : studentsStat)
                    itr.marks.add(
                            (aStudentsStat.subjectMark.get(itr.id) / aStudentsStat.subjectTotalMark.get(itr.id)) * 100.0
                    );
            }
        }

        private void calcLessonMarkSum() {
            for (QuestionStat itr : lessonsStat) {
                for (QuestionStat aStudentsStat : studentsStat)
                    itr.marks.add(
                            (aStudentsStat.lessonMark.get(itr.id) / aStudentsStat.lessonTotalMark.get(itr.id)) * 100.0
                    );
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

        private void fetchUsersData() {

            ArrayList<ObjectId> studentIds = new ArrayList<>();

            for(QuestionStat itr : studentsStat)
                studentIds.add(itr.id);

            studentsData = userRepository.findByIds(
                    studentIds, true
            );

            initTarazRankingLists();

            for(ObjectId subjectId : subjectsTarazRanking.keySet()) {
                List<TarazRanking> allTarazRanking = subjectsTarazRanking.get(subjectId);
                calcStateRanking(allTarazRanking, true, subjectId);
                calcCountryRanking(allTarazRanking, true, subjectId);
                calcCityRanking(allTarazRanking, true, subjectId);
                calcSchoolRanking(allTarazRanking, true, subjectId);
            }

            for(ObjectId lessonId : lessonsTarazRanking.keySet()) {
                List<TarazRanking> allTarazRanking = lessonsTarazRanking.get(lessonId);
                calcStateRanking(allTarazRanking, false, lessonId);
                calcCountryRanking(allTarazRanking, false, lessonId);
                calcCityRanking(allTarazRanking, false, lessonId);
                calcSchoolRanking(allTarazRanking, false, lessonId);
            }

        }

        private void initTarazRankingLists() {

            int k = 0;

            for(QuestionStat itr : studentsStat) {

                ObjectId cityId = studentsData.get(k).get("city", Document.class).getObjectId("_id");
                ObjectId schoolId = studentsData.get(k).get("school", Document.class).getObjectId("_id");
                ObjectId stateId;

                if(statesDic.containsKey(cityId))
                    stateId = statesDic.get(cityId);
                else {
                    stateId = cityRepository.findById(cityId).getObjectId("state_id");
                    statesDic.put(cityId, stateId);
                }

                for(ObjectId oId : itr.subjectTaraz.keySet()) {

                    TarazRanking t = new TarazRanking(
                            schoolId, cityId, stateId,
                            itr.subjectTaraz.get(oId)
                    );

                    if(subjectsTarazRanking.containsKey(oId))
                        subjectsTarazRanking.get(oId).add(t);
                    else
                        subjectsTarazRanking.put(oId, new ArrayList<>() {{
                            add(t);
                        }});
                }

                for(ObjectId oId : itr.lessonTaraz.keySet()) {

                    TarazRanking t = new TarazRanking(
                            schoolId, cityId, stateId,
                            itr.lessonTaraz.get(oId)
                    );

                    if(lessonsTarazRanking.containsKey(oId))
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

            for(TarazRanking t : allTarazRanking) {

                if(t.schoolRank != -1)
                    continue;

                ObjectId wantedSchoolId = t.schoolId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for(TarazRanking ii : allTarazRanking) {
                    if(!ii.schoolId.equals(wantedSchoolId))
                        continue;
                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for(int i = filterSorted.size() - 1; i >= 0; i--) {

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
            for(QuestionStat itr : studentsStat) {
                if(isForSubject)
                    itr.subjectSchoolRanking.put(oId, allTarazRanking.get(k++).schoolRank);
                else
                    itr.lessonSchoolRanking.put(oId, allTarazRanking.get(k++).schoolRank);
            }

        }

        private void calcStateRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for(TarazRanking t : allTarazRanking) {

                if(t.stateRank != -1)
                    continue;

                ObjectId wantedStateId = t.stateId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for(TarazRanking ii : allTarazRanking) {
                    if(!ii.stateId.equals(wantedStateId))
                        continue;
                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for(int i = filterSorted.size() - 1; i >= 0; i--) {

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
            for(QuestionStat itr : studentsStat) {
                if(isForSubject)
                    itr.subjectStateRanking.put(oId, allTarazRanking.get(k++).stateRank);
                else
                    itr.lessonStateRanking.put(oId, allTarazRanking.get(k++).stateRank);
            }

        }

        private void calcCityRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for(TarazRanking t : allTarazRanking) {

                if(t.cityRank != -1)
                    continue;

                ObjectId wantedStateId = t.cityId;

                List<TarazRanking> filterSorted = new ArrayList<>();
                for(TarazRanking ii : allTarazRanking) {

                    if(!ii.cityId.equals(wantedStateId))
                        continue;

                    filterSorted.add(ii);
                }

                filterSorted.sort(Comparator.comparingInt(t2 -> t2.taraz));

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for(int i = filterSorted.size() - 1; i >= 0; i--) {

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
            for(QuestionStat itr : studentsStat) {
                if(isForSubject)
                    itr.subjectCityRanking.put(oId, allTarazRanking.get(k++).cityRank);
                else
                    itr.lessonCityRanking.put(oId, allTarazRanking.get(k++).cityRank);
            }

        }

        private void calcCountryRanking(List<TarazRanking> allTarazRanking, boolean isForSubject, ObjectId oId) {

            for(TarazRanking t : allTarazRanking) {

                if(t.countryRank != -1)
                    continue;

                List<TarazRanking> filterSorted =
                        allTarazRanking.stream()
                                .sorted(Comparator.comparingInt(t2 -> t2.taraz))
                                .collect(Collectors.toList());

                int rank = 0;
                int oldTaraz = -1;
                int skip = 1;

                for(int i = filterSorted.size() - 1; i >= 0; i--) {

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
            for(QuestionStat itr : studentsStat) {
                if(isForSubject)
                    itr.subjectCountryRanking.put(oId, allTarazRanking.get(k++).countryRank);
                else
                    itr.lessonCountryRanking.put(oId, allTarazRanking.get(k++).countryRank);
            }

        }

        private void prepareForCityRanking() {

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
        }

        private void calcSubjectsStats() {
            for(QuestionStat itr : subjectsStat) {
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
            for(QuestionStat itr : lessonsStat) {
                lessonsGeneralStat.add(
                        new Document("avg", itr.mean)
                                .append("max", itr.max)
                                .append("min", itr.min)
                                .append("_id", itr.id)
                                .append("name", itr.name)
                );
            }
        }

        private void save() {

            quiz.put("ranking_list", rankingList);
            quiz.put("report_status", "ready");
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

    void createTaraz(Document quiz) {
        new Taraz(quiz);
    }
}
