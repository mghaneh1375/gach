package irysc.gachesefid.DB;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.in;

public class QuestionRepository {

    static MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("question");

    public static HashMap<Integer, Question> getAnswers(int[] ids) {

        FindIterable<Document> docs = documentMongoCollection.find(in("id", ids)).projection(new BasicDBObject("ans", 1).
                append("id", 1).append("lesson_id", 1).append("lesson_name", 1));

        HashMap<Integer, Question> answers = new HashMap<>();

        for(Document doc : docs)
            answers.put(doc.getInteger("id"),
                    new Question(doc.getInteger("id"),
                            doc.getInteger("ans"),
                            doc.getInteger("lesson_id"),
                            doc.getString("lesson_name")
                    ));

        return answers;
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<Document> findAllMysql() {

        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select * from question where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                Document document = new Document();

                for (int i = 1; i <= rsmd.getColumnCount(); i++)
                    document.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rsmd.getColumnName(i)), rs.getObject(i));

                HashMap<String, Object> tmp = findAllLessonMysql(document.getInteger("s_id"));
                for (String key : tmp.keySet())
                    document.put(key, tmp.get(key));

                document.remove("s_id");
                output.add(document);
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return output;
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static HashMap<String, Object> findAllLessonMysql(int sId) {

        HashMap<String, Object> output = new HashMap<>();

        try {
            String sql = "select s.name, l.id, l.name, g.id, g.name from subject s, lesson l, grade g where s.id = ? and s.lessonId = l.id and l.gradeId = g.id";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, sId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                output.put("subject_name", rs.getString(1));
                output.put("subject_id", sId);
                output.put("lesson_name", rs.getString(3));
                output.put("lesson_id", rs.getInt(2));
                output.put("grade_name", rs.getString(5));
                output.put("grade_id", rs.getString(4));
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return output;
    }
}
