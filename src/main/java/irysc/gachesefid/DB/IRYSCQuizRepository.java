package irysc.gachesefid.DB;

import com.mongodb.client.DistinctIterable;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.Quiz;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.printException;

public class IRYSCQuizRepository extends Common {

    public final static String FOLDER = "irysc_quizzes";

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<Quiz> findAllMysql() {

        ArrayList<Quiz> quizzes = new ArrayList<>();

        try {
            String sql = "select r.*, (select count(*) from regularqoq where quizId = r.id) as qNos from regularquiz r where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                Quiz quiz = new Quiz();

                for (int i = 1; i <= rsmd.getColumnCount(); i++)
                    quiz.cols.put(rsmd.getColumnName(i), rs.getObject(i));

                quizzes.add(quiz);
            }
        } catch (Exception x) {
        }

        return quizzes;
    }

    // todo : after all transfer from mysql to mongo it should be delete
    private static ArrayList<Object> findAllROQ(int quizId, int userId) {

        ArrayList<Object> answers = new ArrayList<>();

        try {
            String sql = "select r.result from roq r, regularqoq q where q.quizId = r.quizId and r.questionId = q.questionId and r.quizId = ? and r.uId = ? order by q.qNo asc";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);

            ps.setInt(1, quizId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next())
                 answers.add(rs.getObject(1));

        } catch (Exception x) {
            System.out.println(x.getMessage());
            printException(x);
        }

        return answers;
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
    public static ArrayList<Document> findAllQuizRegistryMysql(int quizId) {

        ArrayList<Document> docs2 = new ArrayList<>();

        try {
            String sql = "select timeEntry, uId from quizregistry where qId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                Document user = userRepository.findOne(eq("mysql_id", rs.getInt("uId")), null);
                if (user == null)
                    continue;

                Document newDoc = new Document();

                newDoc.put("answers", findAllROQ(quizId, rs.getInt("uId")));
                newDoc.put("_id", user.getObjectId("_id"));
                newDoc.put("paid", 0);
                newDoc.put("register_at", System.currentTimeMillis());
                newDoc.put("start_at", rs.getLong("timeEntry"));
                newDoc.put("finish_at", rs.getLong("timeEntry"));

                docs2.add(newDoc);
            }
        } catch (Exception x) {
            System.out.println(x.getMessage());
            printException(x);
        }

        return docs2;
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
