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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

public abstract class QuizAbstract {

    abstract Document registry(Document student, Document quiz, int paid);

    abstract void quit(Document student, Document quiz);

    abstract String buy(Document student, Document quiz);

    abstract JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin);

    class QuestionStat {

        String name;
        ObjectId id;
        ObjectId additionalId;

        List<PairValue> studentAnswers;
        List<Double> marks;

        HashMap<ObjectId, Integer> subjectStateRanking = new HashMap<>();
        HashMap<ObjectId, Integer> subjectCountryRanking = new HashMap<>();
        HashMap<ObjectId, Integer> subjectCityRanking = new HashMap<>();
        HashMap<ObjectId, Integer> subjectSchoolRanking = new HashMap<>();

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
                     List<PairValue> studentAnswers)
        {
            totalMark = 0;
            this.id = id;
            this.name = name;

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

            marks = new ArrayList<>();
        }

        QuestionStat(ObjectId id, String name,
                     ObjectId additionalId) {
            this.id = id;
            this.name = name;
            this.additionalId = additionalId;
            marks = new ArrayList<>();
        }

        short doCorrect(Document question, int idx) {

            double mark = 0.0;
            double qMark = question.getDouble("mark");
            totalMark += qMark;
            short status;

            if(studentAnswers.size() <= idx)
                status = 0;

            else if(question.getString("kind_question").equalsIgnoreCase(
                    QuestionType.TEST.getName()
            )) {

                PairValue pairValue = (PairValue) studentAnswers.get(idx).getValue();

                if(
                        pairValue.getValue().toString().isEmpty() ||
                                pairValue.getValue().toString().equalsIgnoreCase("0")
                )
                    status = 0;

                else if(pairValue.getValue().toString()
                        .equalsIgnoreCase(question.get("answer").toString())
                ) {
                    mark = question.getDouble("mark");
                    status = 1;
                }

                else {
                    mark = -question.getDouble("mark") / (question.getInteger("choices_count") - 1);
                    status = -1;
                }
            }
            else if(question.getString("kind_question").equalsIgnoreCase(
                    QuestionType.SHORT_ANSWER.getName()
            )) {

                if(
                        studentAnswers.get(idx).getValue().toString().isEmpty()
                )
                    status = 0;

                else {
                    double stdAns = (double) studentAnswers.get(idx).getValue();

                    if (question.getDouble("answer") - question.getDouble("telorance") < stdAns &&
                            question.getDouble("answer") + question.getDouble("telorance") > stdAns
                    ) {
                        mark = question.getDouble("mark");
                        status = 1;
                    }
                    else {
                        mark = -question.getDouble("mark");
                        status = -1;
                    }
                }
            }
            else {
                status = 0;
            }

            marks.add(mark);

            ObjectId subjectId = question.getObjectId("subject_id");
            ObjectId lessonId = question.getObjectId("lesson_id");

            if(subjectTotalMark.containsKey(subjectId))
                subjectTotalMark.put(subjectId,
                        subjectTotalMark.get(subjectId) + qMark
                );
            else
                subjectTotalMark.put(subjectId, qMark);

            if(lessonTotalMark.containsKey(lessonId))
                lessonTotalMark.put(lessonId,
                        lessonTotalMark.get(lessonId) + qMark
                );
            else
                lessonTotalMark.put(lessonId, qMark);

            if(subjectMark.containsKey(question.getObjectId("subject_id"))) {
                subjectMark.put(subjectId, subjectMark.get(subjectId) + mark);

                if(status == 0)
                    subjectWhites.put(subjectId, subjectWhites.get(subjectId) + 1);

                else if(status == 1)
                    subjectCorrects.put(subjectId, subjectCorrects.get(subjectId) + 1);

                else
                    subjectIncorrects.put(subjectId, subjectIncorrects.get(subjectId) + 1);
            }
            else {
                subjectMark.put(subjectId, mark);
                subjectWhites.put(subjectId, status == 0 ? 1 : 0);
                subjectCorrects.put(subjectId, status == 1 ? 1 : 0);
                subjectIncorrects.put(subjectId, status == -1 ? 1 : 0);
            }

            if(lessonMark.containsKey(lessonId)) {
                lessonMark.put(lessonId, lessonMark.get(lessonId) + mark);

                if(status == 0)
                    lessonWhites.put(lessonId, lessonWhites.get(lessonId) + 1);

                else if(status == 1)
                    lessonCorrects.put(lessonId, lessonCorrects.get(lessonId) + 1);

                else
                    lessonIncorrects.put(lessonId, lessonIncorrects.get(lessonId) + 1);
            }
            else {
                lessonMark.put(lessonId, mark);
                lessonWhites.put(lessonId, status == 0 ? 1 : 0);
                lessonCorrects.put(lessonId, status == 1 ? 1 : 0);
                lessonIncorrects.put(lessonId, status == -1 ? 1 : 0);
            }

            return status;
        }

        void calculateSD()
        {
            double sum = 0.0, standardDeviation = 0.0;
            int length = marks.size();

            max = -1;
            min = 110;

            for(double num : marks) {
                sum += num;
                if(max < num)
                    max = num;

                if(min > num)
                    min = num;
            }

            mean = sum / length;

            for(double num : marks)
                standardDeviation += Math.pow(num - mean, 2);

            sd = Math.sqrt(standardDeviation/length);
        }

        void calculateTaraz(double mean, double sd,
                            ObjectId oId, boolean isForSubject) {

            double percent =
                    isForSubject ?
                            subjectMark.get(oId) / subjectTotalMark.get(oId) :
                            lessonMark.get(oId) / lessonTotalMark.get(oId);

            percent *= 100;

            double t = 2000.0 * ((percent - mean) / sd) + 5000;

            if(isForSubject) {
                subjectTaraz.put(oId, t);
                subjectPercent.put(oId, percent);
            }
            else {
                lessonTaraz.put(oId, t);
                lessonPercent.put(oId, percent);
            }
        }

        void calculateTotalTaraz() {

            double sum = 0.0;
            for(ObjectId oId : lessonTaraz.keySet())
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

            byte[] out = new byte[10];

            double t = isForSubject ? subjectTaraz.get(oId) : lessonTaraz.get(oId);
            int w = isForSubject ? subjectWhites.get(oId) : lessonWhites.get(oId);
            int c = isForSubject ? subjectCorrects.get(oId) : lessonCorrects.get(oId);
            int ic = isForSubject ? subjectIncorrects.get(oId) : lessonIncorrects.get(oId);
            double p = isForSubject ? subjectPercent.get(oId) : lessonPercent.get(oId);

            byte[] bb = ByteBuffer.allocate(4).putInt((int) t).array();
            out[0] = bb[2];
            out[1] = bb[3];
            out[2] = (byte) w;
            out[3] = (byte) c;
            out[4] = (byte) ic;

            boolean minus = false;
            if(p < 0) {
                minus = true;
                p = -p;
            }

            DecimalFormat df_obj = new DecimalFormat("#.##");
            String[] splited = df_obj.format(p).split("\\.");
            out[5] = (byte) Integer.parseInt(splited[0]);

            if(splited.length == 1)
                out[6] = (byte) (minus ? 0x80 : 0x00);
            else if(minus)
                out[6] = (byte) (Integer.parseInt(splited[1]) + 128);
            else
                out[6] = (byte) Integer.parseInt(splited[1]);

            if(isForSubject) {
                out[7] = (byte) ((int) subjectCountryRanking.get(oId));
                out[8] = (byte) ((int) subjectStateRanking.get(oId));
                out[9] = (byte) ((int) subjectCityRanking.get(oId));
            }
            else {
                out[7] = (byte) 0;
                out[8] = (byte) 0;
                out[9] = (byte) 0;
            }

            return out;
        }

    }

    class TarazRanking {

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

    public static Object[] decode(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);
        int whites = in[2] & 0xff;
        int corrects = in[3] & 0xff;
        int incorrects = in[4] & 0xff;

        int floatSection = (in[6] & 0xff);

        boolean minus = false;

        if(floatSection > 128) {
            minus = true;
            floatSection -= 128;
        }

        double percent = (floatSection < 10) ?
                (in[5] & 0xff) + (floatSection / 10.0) :
                (in[5] & 0xff) + (floatSection / 100.0);

        if(minus)
            percent = -percent;

        int stateRank = 0;
        int cityRank = 0;
        int countryRank = 0;

        if(in.length > 7)
            countryRank = in[7] & 0xff;

        if(in.length > 8)
            stateRank = in[8] & 0xff;

        if(in.length > 9)
            cityRank = in[9] & 0xff;

        return new Object[] {
                taraz, whites, corrects, incorrects, Math.round(percent), countryRank, stateRank, cityRank
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

    public static Object[] decodeFormatGeneral(byte[] in) {

        int taraz = (in[0] & 0xff) * 256 + (in[1] & 0xff);
        int rank = in[2] & 0xff;
        int stateRank = in[3] & 0xff;
        int cityRank = in[4] & 0xff;

        return new Object[] {
                taraz, rank, stateRank, cityRank
        };
    }
}
