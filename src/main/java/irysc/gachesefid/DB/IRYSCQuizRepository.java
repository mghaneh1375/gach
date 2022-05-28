package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.Quiz;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.Utility.printException;

public class IRYSCQuizRepository extends Common {

    public final static String FOLDER = "irysc_quizzes";

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<Quiz> findAllMysql() {

        ArrayList<Quiz> quizes = new ArrayList<>();

        try {
            String sql = "select * from regularQuiz where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                Quiz quiz = new Quiz();

                for (int i = 1; i <= rsmd.getColumnCount(); i++)
                    quiz.cols.put(rsmd.getColumnName(i), rs.getObject(i));

                quizes.add(quiz);
            }
        } catch (Exception x) {
        }

        return quizes;
    }

    // todo : after all transfer from mysql to mongo it should be delete
    private static String[] findAllROQ(int quizId, int userId) {

        StringBuilder student_answers = new StringBuilder();
        StringBuilder question_ids = new StringBuilder();

        try {
            String sql = "select result, questionId from ROQ where quizId = ? and uId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);

            ps.setInt(1, quizId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();

            boolean first = true;

            while (rs.next()) {
                if (first) {
                    student_answers.append(rs.getInt(1));
                    question_ids.append(rs.getInt(2));
                    first = false;
                } else {
                    student_answers.append("-").append(rs.getInt(1));
                    question_ids.append("-").append(rs.getInt(2));
                }
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            printException(x);
        }

        return new String[]{student_answers.toString(), question_ids.toString()};
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static String findQuizQuestions(int quizId) {

        StringBuilder ids = new StringBuilder();

        try {
            String sql = "select questionId from regularQOQ where quizId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, quizId);

            ResultSet rs = ps.executeQuery();

            boolean first = true;

            while (rs.next()) {
                if (first) {
                    ids.append(rs.getInt(1));
                    first = false;
                } else {
                    ids.append("-").append(rs.getInt(1));
                }
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            printException(x);
        }

        return ids.toString();
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<ArrayList<Object>> findAllQuizRegistryMysql(int quizId) {

        ArrayList<Object> docs = new ArrayList<>();
        ArrayList<Object> docs2 = new ArrayList<>();

        try {
            String sql = "select timeEntry, endTime, uId from quizRegistry where qId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

//                Document user = userRepository.findByMysqlId(rs.getInt("uId"));
//                if (user == null)
//                    continue;
//
//                docs.add(user.getObjectId("_id"));
//
//                Document newDoc = new Document();
//                String[] answers = findAllROQ(quizId, rs.getInt("uId"));
//                newDoc.put("student_answers", answers[0]);
//                newDoc.put("question_ids", answers[1]);
//                newDoc.put("user_id", user.getObjectId("_id"));
//                newDoc.put("time_entry", rs.getLong("timeEntry"));
//                newDoc.put("end_time", rs.getLong("endTime"));
//
//                docs2.add(newDoc);
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            printException(x);
        }

        ArrayList<ArrayList<Object>> allDocs = new ArrayList<>();
        allDocs.add(docs);
        allDocs.add(docs2);
        return allDocs;
    }

    public IRYSCQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "irysc_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
