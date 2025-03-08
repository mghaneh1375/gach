package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static java.lang.Double.isNaN;

public abstract class QuizAbstract {

    abstract List<Document> registry(ObjectId studentId, String phone,
                                     String mail, List<ObjectId> quizIds,
                                     int paid, ObjectId transactionId, String stdName);

    public static int calcLenStatic(Document quiz) {
        return doCalcLen(quiz);
    }

    private static int doCalcLen(Document quiz) {
        if (quiz.containsKey("duration"))
            return quiz.getInteger("duration") * 60;

        if (quiz.containsKey("duration_sum"))
            return quiz.getInteger("duration_sum");

        if (!quiz.containsKey("questions"))
            return 0;

        Document questions = quiz.get("questions", Document.class);

        if (!questions.containsKey("_ids"))
            return 0;

        List<ObjectId> questionIds = questions.getList("_ids", ObjectId.class);
        ArrayList<Document> questionsDoc = questionRepository.findByIds(questionIds, false);

        if (questionsDoc == null || questionsDoc.size() == 0)
            return 0;

        int total = 0;
        for (Document question : questionsDoc)
            total += question.getInteger("needed_time");

        quiz.put("duration_sum", total);
        return total;
    }

    int calcLen(Document quiz) {
        return doCalcLen(quiz);
    }

    abstract void quit(Document student, Document quiz);

    abstract JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin,
                                         boolean afterBuy, boolean isDescNeeded
    );

    static class TashrihiQuestionStat {

        String name;
        ObjectId id;
        ObjectId additionalId;

        List<Document> studentAnswers;
        List<Double> marks;

        HashMap<ObjectId, Integer> subjectStateRanking;
        HashMap<ObjectId, Integer> subjectCountryRanking;
        HashMap<ObjectId, Integer> subjectCityRanking;
        HashMap<ObjectId, Integer> subjectSchoolRanking;

        HashMap<ObjectId, Integer> lessonStateRanking;
        HashMap<ObjectId, Integer> lessonCountryRanking;
        HashMap<ObjectId, Integer> lessonCityRanking;
        HashMap<ObjectId, Integer> lessonSchoolRanking;

        HashMap<ObjectId, Double> subjectMark;
        HashMap<ObjectId, Double> lessonMark;

        HashMap<ObjectId, Double> subjectTotalMark;
        HashMap<ObjectId, Double> lessonTotalMark;

        HashMap<ObjectId, Double> subjectPercent;
        HashMap<ObjectId, Double> lessonPercent;

        HashMap<ObjectId, Integer> subjectTotalQuestions;
        HashMap<ObjectId, Integer> lessonTotalQuestions;

        HashMap<ObjectId, Double> lessonTaraz;
        HashMap<ObjectId, Double> subjectTaraz;

        double sd;
        double mean;
        double taraz;
        double totalMark;
        double max;
        double min;


        TashrihiQuestionStat(ObjectId id, String name) {
            this.id = id;
            this.name = name;
            marks = new ArrayList<>();
        }

        TashrihiQuestionStat(ObjectId id, String name, ObjectId additionalId) {
            this.id = id;
            this.name = name;
            this.additionalId = additionalId;
            marks = new ArrayList<>();
        }

        TashrihiQuestionStat(ObjectId id, String name,
                             List<Document> studentAnswers) {
            totalMark = 0;
            this.id = id;
            this.name = name;

            this.studentAnswers = studentAnswers;

            subjectMark = new HashMap<>();
            lessonMark = new HashMap<>();

            subjectTotalMark = new HashMap<>();
            lessonTotalMark = new HashMap<>();

            subjectTaraz = new HashMap<>();
            lessonTaraz = new HashMap<>();

            subjectPercent = new HashMap<>();
            lessonPercent = new HashMap<>();

            subjectStateRanking = new HashMap<>();
            subjectCountryRanking = new HashMap<>();
            subjectCityRanking = new HashMap<>();
            subjectSchoolRanking = new HashMap<>();

            lessonStateRanking = new HashMap<>();
            lessonCountryRanking = new HashMap<>();
            lessonCityRanking = new HashMap<>();
            lessonSchoolRanking = new HashMap<>();

            subjectTotalQuestions = new HashMap<>();
            lessonTotalQuestions = new HashMap<>();

            marks = new ArrayList<>();
        }

        void doCorrect(Document question) {

            double qMark = question.getDouble("mark");
            totalMark += qMark;

            Document ans = Utility.searchInDocumentsKeyVal(
                    studentAnswers, "question_id", question.getObjectId("_id")
            );

            double mark = (ans != null && ans.containsKey("mark")) ? ans.getDouble("mark") : 0.0;
            marks.add(mark);

            ObjectId subjectId = question.getObjectId("subject_id");
            ObjectId lessonId = question.getObjectId("lesson_id");

            if (subjectTotalMark.containsKey(subjectId))
                subjectTotalMark.put(subjectId,
                        subjectTotalMark.get(subjectId) + qMark
                );
            else
                subjectTotalMark.put(subjectId, qMark);

            if (lessonTotalMark.containsKey(lessonId))
                lessonTotalMark.put(lessonId,
                        lessonTotalMark.get(lessonId) + qMark
                );
            else
                lessonTotalMark.put(lessonId, qMark);

            if (subjectMark.containsKey(question.getObjectId("subject_id")))
                subjectMark.put(subjectId, subjectMark.get(subjectId) + mark);
            else
                subjectMark.put(subjectId, mark);

            if (subjectTotalQuestions.containsKey(question.getObjectId("subject_id")))
                subjectTotalQuestions.put(subjectId, subjectTotalQuestions.get(subjectId) + 1);
            else
                subjectTotalQuestions.put(subjectId, 1);

            if (lessonMark.containsKey(lessonId))
                lessonMark.put(lessonId, lessonMark.get(lessonId) + mark);
            else
                lessonMark.put(lessonId, mark);

            if (lessonTotalQuestions.containsKey(lessonId))
                lessonTotalQuestions.put(lessonId, lessonTotalQuestions.get(lessonId) + 1);
            else
                lessonTotalQuestions.put(lessonId, 1);
        }

        void calculateSD() {

            double sum = 0.0, standardDeviation = 0.0;
            int length = marks.size();

            max = 0;
            min = Integer.MAX_VALUE;

            for (double num : marks) {

                sum += num;
                if (max < num)
                    max = num;

                if (min > num)
                    min = num;
            }

            mean = sum / length;

            for (double num : marks)
                standardDeviation += Math.pow(num - mean, 2);

            sd = Math.sqrt(standardDeviation / length);
        }

        void calculateTaraz(double mean, double sd,
                            ObjectId oId, boolean isForSubject) {

            double percent =
                    isForSubject ?
                            subjectMark.containsKey(oId) ?
                            subjectMark.get(oId) / subjectTotalMark.get(oId) : 0 :
                            lessonMark.containsKey(oId) ?
                            lessonMark.get(oId) / lessonTotalMark.get(oId) : 0;

            percent *= 100;

            double t = sd == 0 ? 5000 : 2000.0 * ((percent - mean) / sd) + 5000;

            if (isForSubject) {
                subjectTaraz.put(oId, t);
                subjectPercent.put(oId, percent);
            } else {
                lessonTaraz.put(oId, t);
                lessonPercent.put(oId, percent);
            }
        }

        void calculateTotalTaraz() {

            double sum = 0.0;
            for (ObjectId oId : lessonTaraz.keySet()) {
                sum += lessonTaraz.get(oId);
            }

            taraz = sum / lessonTaraz.size();

        }

        @Override
        public boolean equals(Object o) {
            return id.equals(o);
        }

        int compareTo(TashrihiQuestionStat q) {
            return Double.compare(q.taraz, taraz);
        }

        byte[] encode(ObjectId oId, boolean isForSubject) {

            byte[] out = new byte[13];

            double t = 0;
            if (isForSubject && subjectTaraz != null && subjectTaraz.containsKey(oId))
                t = subjectTaraz.get(oId);
            else if (!isForSubject && lessonTaraz != null && lessonTaraz.containsKey(oId))
                t = lessonTaraz.get(oId);

            fillByteArrWithDouble(isForSubject ? subjectPercent.get(oId) : lessonPercent.get(oId), out, 2);

            byte[] bb = ByteBuffer.allocate(4).putInt((int) t).array();
            out[0] = bb[2];
            out[1] = bb[3];

            if (isForSubject) {
                out[4] = (byte) ((int) subjectCountryRanking.get(oId));
                out[5] = (byte) ((int) subjectStateRanking.get(oId));
                out[6] = (byte) ((int) subjectCityRanking.get(oId));
                out[7] = (byte) ((int) subjectSchoolRanking.get(oId));
                out[12] = (byte) (subjectTotalQuestions.containsKey(oId) ? (int) subjectTotalQuestions.get(oId) : 0);
            } else {
                out[4] = (byte) ((int) lessonCountryRanking.get(oId));
                out[5] = (byte) ((int) lessonStateRanking.get(oId));
                out[6] = (byte) ((int) lessonCityRanking.get(oId));
                out[7] = (byte) ((int) lessonSchoolRanking.get(oId));
                out[12] = (byte) (lessonTotalQuestions.containsKey(oId) ? (int) lessonTotalQuestions.get(oId) : 0);
            }

            fillByteArrWithDouble(isForSubject ?
                    subjectMark.containsKey(oId) ? subjectMark.get(oId) : 0 :
                    lessonMark.containsKey(oId) ? lessonMark.get(oId) : 0,
                    out, 8
            );
            fillByteArrWithDouble(isForSubject ?
                    subjectTotalMark.containsKey(oId) ? subjectTotalMark.get(oId) : 0 :
                    lessonTotalMark.containsKey(oId) ? lessonTotalMark.get(oId) : 0,
                    out, 10
            );

            return out;
        }

    }

    static void fillByteArrWithDouble(double p, byte[] out, int startIdx) {

        DecimalFormat df_obj = new DecimalFormat("#.##");
        String[] splited = df_obj.format(p).split("\\.");

        out[startIdx] = (byte) Integer.parseInt(splited[0]);

        if (splited.length == 1)
            out[startIdx + 1] = (byte) (0x00);
        else
            out[startIdx + 1] = (byte) Integer.parseInt(splited[1]);

    }

    public static class QuestionStat {

        String name;
        ObjectId id;
        ObjectId additionalId;

        List<PairValue> studentAnswers;
        List<Double> marks;

        HashMap<ObjectId, Integer> subjectStateRanking;
        HashMap<ObjectId, Integer> subjectCountryRanking;
        HashMap<ObjectId, Integer> subjectCityRanking;
        HashMap<ObjectId, Integer> subjectSchoolRanking;

        HashMap<ObjectId, Integer> lessonStateRanking;
        HashMap<ObjectId, Integer> lessonCountryRanking;
        HashMap<ObjectId, Integer> lessonCityRanking;
        HashMap<ObjectId, Integer> lessonSchoolRanking;

        HashMap<ObjectId, Integer> subjectWhites;
        HashMap<ObjectId, Integer> subjectCorrects;
        HashMap<ObjectId, Integer> subjectIncorrects;

        HashMap<ObjectId, Integer> lessonWhites;
        HashMap<ObjectId, Integer> lessonCorrects;
        HashMap<ObjectId, Integer> lessonIncorrects;

        HashMap<ObjectId, Double> subjectMark;
        HashMap<ObjectId, Double> lessonMark;

        HashMap<ObjectId, Double> subjectTotalMark;
        HashMap<ObjectId, Double> lessonTotalMark;

        HashMap<ObjectId, Double> subjectPercent;
        HashMap<ObjectId, Double> lessonPercent;

        HashMap<ObjectId, Double> lessonTaraz;
        HashMap<ObjectId, Double> subjectTaraz;
        boolean hasMinusMark;

        double sd;
        double mean;
        double taraz;
        double totalMark;
        double max;
        double min;

        QuestionStat(ObjectId id, String name) {
            this.id = id;
            this.name = name;
            marks = new ArrayList<>();
        }

        QuestionStat(ObjectId id, String name,
                     List<PairValue> studentAnswers,
                     boolean hasMinusMark) {

            totalMark = 0;
            this.id = id;
            this.name = name;

            this.hasMinusMark = hasMinusMark;
            this.studentAnswers = studentAnswers;

            subjectWhites = new HashMap<>();
            lessonWhites = new HashMap<>();

            subjectCorrects = new HashMap<>();
            lessonCorrects = new HashMap<>();

            subjectIncorrects = new HashMap<>();
            lessonIncorrects = new HashMap<>();

            subjectMark = new HashMap<>();
            lessonMark = new HashMap<>();

            subjectTotalMark = new HashMap<>();
            lessonTotalMark = new HashMap<>();

            subjectTaraz = new HashMap<>();
            lessonTaraz = new HashMap<>();

            subjectPercent = new HashMap<>();
            lessonPercent = new HashMap<>();

            subjectStateRanking = new HashMap<>();
            subjectCountryRanking = new HashMap<>();
            subjectCityRanking = new HashMap<>();
            subjectSchoolRanking = new HashMap<>();

            lessonStateRanking = new HashMap<>();
            lessonCountryRanking = new HashMap<>();
            lessonCityRanking = new HashMap<>();
            lessonSchoolRanking = new HashMap<>();

            marks = new ArrayList<>();
        }

        QuestionStat(ObjectId id, String name,
                     ObjectId additionalId) {
            this.id = id;
            this.name = name;
            this.additionalId = additionalId;
            marks = new ArrayList<>();
        }

        short doCorrectMultiSentence(Document question, int idx) {
            if(studentAnswers.size() <= idx)
                return (short)0;
            String stdAns = studentAnswers.get(idx).getValue().toString();
            String answer = question.getString("answer");
            double qMark = question.getDouble("mark");
            PairValue p = getStdMarkInMultiSentenceQuestion(answer, stdAns, question.getDouble("mark"));

            updateStats((Double) p.getKey(), question, qMark, (short) p.getValue());
            return (short) p.getValue();
        }

        public static PairValue getStdMarkInMultiSentenceQuestion(String answer, String stdAns, double mark) {
            int t = answer.length();
            int c = 0;
            int inc = 0;

            for (int z = 0; z < t; z++) {
                if (z >= stdAns.length() || stdAns.charAt(z) == '_')
                    continue;

                if (stdAns.charAt(z) == answer.charAt(z))
                    c++;
                else
                    inc++;
            }

            short status = (short) (c == t ? 1 : inc + c == 0 ? 0 : -1);
            double thisMark;
            if (c == t)
                thisMark = mark;
            else {
                int p = -10 * inc;
                if (c > 1 && t <= 4) {
                    if (c == 2)
                        p += 20;
                    else if (c == 3)
                        p += 50;
                } else if (c > 1) {
                    if (c == 2) {
//                        p += 10;
                        p += 20;
                    }
                    else if (c == 3) {
//                        p += 30;
                        p += 40;
                    }
                    else if (c == 4)
                        p += 60;
                }

                thisMark = (p / 100.0) * mark;
            }

            DecimalFormat df = new DecimalFormat("#.##");
            thisMark = Double.parseDouble(df.format(thisMark));

            return new PairValue(thisMark, status);
        }

        short doCorrect(Document question, int idx) {

            double mark = 0.0;
            double qMark = question.getDouble("mark");
            totalMark += qMark;
            short status;

            try {
                if (studentAnswers.size() <= idx)
                    status = 0;

                else if (question.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(
                        QuestionType.TEST.getName()
                )) {

                    PairValue pairValue = (PairValue) studentAnswers.get(idx).getValue();

                    if (
                            pairValue.getValue().toString().isEmpty() ||
                                    pairValue.getValue().toString().equalsIgnoreCase("0")
                    )
                        status = 0;

                    else if (pairValue.getValue().toString()
                            .equalsIgnoreCase(question.get("answer").toString())
                    ) {
                        mark = question.getDouble("mark");
                        status = 1;
                    } else {

                        if (hasMinusMark)
                            mark = -question.getDouble("mark") / (question.getInteger("choices_count") - 1);
                        else
                            mark = 0;

                        status = -1;
                    }
                } else if (question.getOrDefault("kind_question", "test").toString().equalsIgnoreCase(
                        QuestionType.SHORT_ANSWER.getName()
                )) {

                    if (
                            studentAnswers.get(idx).getValue().toString().isEmpty() ||
                                    isNaN(((Number) studentAnswers.get(idx).getValue()).doubleValue())
                    )
                        status = 0;

                    else {
                        double stdAns = ((Number) studentAnswers.get(idx).getValue()).doubleValue();
                        double qAns = ((Number) question.get("answer")).doubleValue();

                        if (qAns - question.getDouble("telorance") < stdAns &&
                                qAns + question.getDouble("telorance") > stdAns
                        ) {
                            mark = question.getDouble("mark");
                            status = 1;
                        } else {

                            if (hasMinusMark)
                                mark = -question.getDouble("mark");
                            else
                                mark = 0;

                            status = -1;
                        }
                    }
                } else
                    status = 0;

                updateStats(mark, question, qMark, status);
                return status;
            }
            catch (Exception x) {
                System.out.println(x.getMessage());
            }

            return 0;
        }

        private void updateStats(double mark, Document question, double qMark, short status) {

            marks.add(mark);

            ObjectId subjectId = question.getObjectId("subject_id");
            ObjectId lessonId = question.getObjectId("lesson_id");

            if (subjectTotalMark.containsKey(subjectId))
                subjectTotalMark.put(subjectId,
                        subjectTotalMark.get(subjectId) + qMark
                );
            else
                subjectTotalMark.put(subjectId, qMark);

            if (lessonTotalMark.containsKey(lessonId))
                lessonTotalMark.put(lessonId,
                        lessonTotalMark.get(lessonId) + qMark
                );
            else
                lessonTotalMark.put(lessonId, qMark);

            if (subjectMark.containsKey(question.getObjectId("subject_id"))) {
                subjectMark.put(subjectId, subjectMark.get(subjectId) + mark);

                if (status == 0)
                    subjectWhites.put(subjectId, subjectWhites.get(subjectId) + 1);

                else if (status == 1)
                    subjectCorrects.put(subjectId, subjectCorrects.get(subjectId) + 1);

                else
                    subjectIncorrects.put(subjectId, subjectIncorrects.get(subjectId) + 1);
            } else {
                subjectMark.put(subjectId, mark);
                subjectWhites.put(subjectId, status == 0 ? 1 : 0);
                subjectCorrects.put(subjectId, status == 1 ? 1 : 0);
                subjectIncorrects.put(subjectId, status == -1 ? 1 : 0);
            }

            if (lessonMark.containsKey(lessonId)) {
                lessonMark.put(lessonId, lessonMark.get(lessonId) + mark);

                if (status == 0)
                    lessonWhites.put(lessonId, lessonWhites.get(lessonId) + 1);

                else if (status == 1)
                    lessonCorrects.put(lessonId, lessonCorrects.get(lessonId) + 1);

                else
                    lessonIncorrects.put(lessonId, lessonIncorrects.get(lessonId) + 1);
            } else {
                lessonMark.put(lessonId, mark);
                lessonWhites.put(lessonId, status == 0 ? 1 : 0);
                lessonCorrects.put(lessonId, status == 1 ? 1 : 0);
                lessonIncorrects.put(lessonId, status == -1 ? 1 : 0);
            }

        }

        void calculateSD() {
            double sum = 0.0, standardDeviation = 0.0;
            int length = marks.size();

            max = -1;
            min = 110;

            for (double num : marks) {
                sum += num;
                if (max < num)
                    max = num;

                if (min > num)
                    min = num;
            }

            mean = sum / length;

            for (double num : marks)
                standardDeviation += Math.pow(num - mean, 2);

            sd = Math.sqrt(standardDeviation / length);
        }

        void calculateTaraz(double mean, double sd,
                            ObjectId oId, boolean isForSubject) {

            double percent = 0;
            try {
                percent = isForSubject ?
                        subjectTotalMark.get(oId) == 0 ? 0 : subjectMark.get(oId) / subjectTotalMark.get(oId) :
                        lessonTotalMark.get(oId) == 0 ? 0 : lessonMark.get(oId) / lessonTotalMark.get(oId);
            }
            catch (Exception ignore) {}

            percent *= 100;

            double t = sd == 0 ? 5000 : 2000.0 * ((percent - mean) / sd) + 5000;

            if (isForSubject) {
                subjectTaraz.put(oId, t);
                subjectPercent.put(oId, percent);
            } else {
                lessonTaraz.put(oId, t);
                lessonPercent.put(oId, percent);
            }
        }

        void calculateTotalTaraz() {

            double sum = 0.0;
            for (ObjectId oId : lessonTaraz.keySet())
                sum += lessonTaraz.get(oId);

            taraz = sum / lessonTaraz.size();

        }

        @Override
        public boolean equals(Object o) {
            return id.equals(o);
        }

        int compareTo(QuestionStat q) {
            return Double.compare(q.taraz, taraz);
        }

        byte[] encode(ObjectId oId, boolean isForSubject) {

            byte[] out = new byte[11];

            double t = 0;
            if (isForSubject && subjectTaraz != null && subjectTaraz.containsKey(oId))
                t = subjectTaraz.get(oId);
            else if (!isForSubject && lessonTaraz != null && lessonTaraz.containsKey(oId))
                t = lessonTaraz.get(oId);

            Integer w = isForSubject ? subjectWhites.get(oId) : lessonWhites.get(oId);
            Integer c = isForSubject ? subjectCorrects.get(oId) : lessonCorrects.get(oId);
            Integer ic = isForSubject ? subjectIncorrects.get(oId) : lessonIncorrects.get(oId);
            Double p = isForSubject ? subjectPercent.get(oId) : lessonPercent.get(oId);

            byte[] bb = ByteBuffer.allocate(4).putInt((int) t).array();
            out[0] = bb[2];
            out[1] = bb[3];
            out[2] = w == null ? (byte)0 : (byte) w.intValue();
            out[3] = c == null ? (byte)0 : (byte) c.intValue();
            out[4] = ic == null ? (byte)0 : (byte) ic.intValue();

            if(p == null)
                p = 0.0;

            boolean minus = false;
            if (p < 0) {
                minus = true;
                p = -p;
            }

            DecimalFormat df_obj = new DecimalFormat("#.##");
            String[] splited = df_obj.format(p).split("\\.");
            out[5] = (byte) Integer.parseInt(splited[0]);

            if (splited.length == 1)
                out[6] = (byte) (minus ? 0x80 : 0x00);
            else if (minus)
                out[6] = (byte) (Integer.parseInt(splited[1]) + 128);
            else
                out[6] = (byte) Integer.parseInt(splited[1]);

            if (isForSubject) {
                out[7] = (byte) ((int) subjectCountryRanking.get(oId));
                out[8] = (byte) ((int) subjectStateRanking.get(oId));
                out[9] = (byte) ((int) subjectCityRanking.get(oId));
                out[10] = (byte) ((int) subjectSchoolRanking.get(oId));
            } else {
                out[7] = (byte) ((int) lessonCountryRanking.get(oId));
                out[8] = (byte) ((int) lessonStateRanking.get(oId));
                out[9] = (byte) ((int) lessonCityRanking.get(oId));
                out[10] = (byte) ((int) lessonSchoolRanking.get(oId));
            }

            return out;
        }

        byte[] encodeCustomQuiz(ObjectId oId, boolean isForSubject) {

            byte[] out = new byte[11];

            int w = isForSubject ? subjectWhites.get(oId) : lessonWhites.get(oId);
            int c = isForSubject ? subjectCorrects.get(oId) : lessonCorrects.get(oId);
            int ic = isForSubject ? subjectIncorrects.get(oId) : lessonIncorrects.get(oId);
            double p = isForSubject ? subjectPercent.get(oId) : lessonPercent.get(oId);

            out[0] = (byte) w;
            out[1] = (byte) c;
            out[2] = (byte) ic;

            boolean minus = false;
            if (p < 0) {
                minus = true;
                p = -p;
            }

            DecimalFormat df_obj = new DecimalFormat("#.##");
            String[] splited = df_obj.format(p).split("\\.");
            out[3] = (byte) Integer.parseInt(splited[0]);

            if (splited.length == 1)
                out[4] = (byte) (minus ? 0x80 : 0x00);
            else if (minus)
                out[4] = (byte) (Integer.parseInt(splited[1]) + 128);
            else
                out[4] = (byte) Integer.parseInt(splited[1]);

            return out;
        }

    }

    static class TarazRanking {

        ObjectId schoolId;
        ObjectId cityId;
        ObjectId stateId;

        int schoolRank = -1;
        int stateRank = -1;
        int countryRank = -1;
        int cityRank = -1;

        int taraz;

        public TarazRanking(ObjectId schoolId, ObjectId cityId,
                            ObjectId stateId, double taraz
        ) {
            this.schoolId = schoolId;
            this.cityId = cityId;
            this.stateId = stateId;
            this.taraz = (int) taraz;
        }
    }

    public static Object[] decodeTashrihi(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);
        int floatSection = (in[3] & 0xff);

        double percent = (floatSection < 10) ?
                (in[2] & 0xff) + (floatSection / 10.0) :
                (in[2] & 0xff) + (floatSection / 100.0);

        int countryRank = in[4] & 0xff;
        int stateRank = in[5] & 0xff;
        int cityRank = in[6] & 0xff;
        int schoolRank = in[7] & 0xff;

        floatSection = (in[9] & 0xff);
        double mark = (floatSection < 10) ?
                (in[8] & 0xff) + (floatSection / 10.0) :
                (in[8] & 0xff) + (floatSection / 100.0);

        floatSection = (in[11] & 0xff);
        double totalMark = (floatSection < 10) ?
                (in[10] & 0xff) + (floatSection / 10.0) :
                (in[10] & 0xff) + (floatSection / 100.0);

        int totalQuestions = in[12] & 0xff;

        return new Object[]{
                taraz, Math.round(percent), countryRank, stateRank, cityRank, schoolRank, mark, totalMark, totalQuestions
        };
    }

    public static Object[] decode(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);

        int whites = in[2] & 0xff;
        int corrects = in[3] & 0xff;
        int incorrects = in[4] & 0xff;

        int floatSection = (in[6] & 0xff);

        boolean minus = false;

        if (floatSection >= 128) {
            minus = true;
            floatSection -= 128;
        }

        double percent = (floatSection < 10) ?
                (in[5] & 0xff) + (floatSection / 10.0) :
                (in[5] & 0xff) + (floatSection / 100.0);

        if (minus)
            percent = -percent;

        int countryRank = in[7] & 0xff;
        int stateRank = in[8] & 0xff;
        int cityRank = in[9] & 0xff;
        int schoolRank = in[10] & 0xff;

        return new Object[]{
                taraz, whites, corrects, incorrects, Math.round(percent), countryRank, stateRank, cityRank, schoolRank
        };
    }

    public static Object[] decodeCustomQuiz(byte[] in) {

        int whites = in[0] & 0xff;
        int corrects = in[1] & 0xff;
        int incorrects = in[2] & 0xff;

        int floatSection = (in[4] & 0xff);

        boolean minus = false;

        if (floatSection >= 128) {
            minus = true;
            floatSection -= 128;
        }

        double percent = (floatSection < 10) ?
                (in[3] & 0xff) + (floatSection / 10.0) :
                (in[3] & 0xff) + (floatSection / 100.0);

        if (minus)
            percent = -percent;

        return new Object[]{
                whites, corrects, incorrects, Math.round(percent)
        };
    }

    public static byte[] encodeFormatGeneral(int taraz, int rank,
                                             int cityRank, int stateRank
    ) {

        byte[] out = new byte[5];

        byte[] bb = ByteBuffer.allocate(4).putInt(taraz).array();
        out[0] = bb[2];
        out[1] = bb[3];
        out[2] = (byte) rank;
        out[3] = (byte) stateRank;
        out[4] = (byte) cityRank;

        return out;
    }

    public static byte[] encodeFormatGeneralTashrihi(double totalMark, int taraz, int rank,
                                                     int cityRank, int stateRank
    ) {

        byte[] out = new byte[13];

        byte[] bb = ByteBuffer.allocate(4).putInt(taraz).array();
        out[0] = bb[2];
        out[1] = bb[3];
        out[2] = (byte) rank;
        out[3] = (byte) stateRank;
        out[4] = (byte) cityRank;

        long lng = Double.doubleToLongBits(totalMark);
        for (int i = 0; i < 8; i++) out[i + 5] = (byte) ((lng >> ((7 - i) * 8)) & 0xff);

        return out;
    }

    public static Object[] decodeFormatGeneralTashrihi(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);
        int rank = in[2] & 0xff;
        int stateRank = in[3] & 0xff;
        int cityRank = in[4] & 0xff;

        byte[] tmp = new byte[8];
        System.arraycopy(in, 5, tmp, 0, 8);
        double mark = ByteBuffer.wrap(tmp).getDouble();

        return new Object[]{
                taraz, rank, stateRank, cityRank, mark
        };
    }

    public static Object[] decodeFormatGeneral(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);
        int rank = in[2] & 0xff;
        int stateRank = in[3] & 0xff;
        int cityRank = in[4] & 0xff;

        return new Object[]{
                taraz, rank, stateRank, cityRank
        };
    }
}
