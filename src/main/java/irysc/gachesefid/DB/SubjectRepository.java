package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.Utility.printException;


public class SubjectRepository extends Common {

    public SubjectRepository() {
        init();
    }

    @Override
    void init() {
        table = "subject";
        secKey = "code";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public static ArrayList<Document> getAllSubjectsByLessonIdFromMySQL(int lessonId) {

        ArrayList<Document> docs = new ArrayList<>();
        long curr = System.currentTimeMillis();

        try {
            String sql = "select id, name, price1, price2, price3 from subject where lessonId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, lessonId);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                docs.add(new Document("name", rs.getString(2))
                        .append("code", String.format("%6d", rs.getInt(1)))
                        .append("created_at", curr)
                        .append("description", "")
                        .append("easy_price", rs.getInt(3))
                        .append("mid_price", rs.getInt(4))
                        .append("hard_price", rs.getInt(5))
                        .append("q_no", 0)
                );

        }
        catch (Exception x) {
            printException(x);
        }

        return docs;
    }

    public static JSONArray getAllSubjectsCompleteFromMySQL() {

        JSONArray jsonArray = new JSONArray();

        try {
            String sql = "select s.id, s.name, s.price1, s.price2, s.price3, l.name, g.name from " +
                    "subject s, lesson l, grade g where s.lessonId = l.id and l.gradeId = g.id";

            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            while (rs.next())
                jsonArray.put(new JSONObject()
                        .put("subject", rs.getString(2))
                        .put("id", String.format("%3d", rs.getInt(1)))
                        .put("easy_price", rs.getInt(3))
                        .put("mid_price", rs.getInt(4))
                        .put("hard_price", rs.getInt(5))
                        .put("lesson", rs.getString(6))
                        .put("grade", rs.getString(7))
                );

        }
        catch (Exception x) {
            printException(x);
        }

        return jsonArray;
    }

    public void clearFormCacheByGradeId(ObjectId gradeId) {

        //todo: img

    }

    public void clearFormCacheByLessonId(ObjectId gradeId) {

        //todo: img

    }
}
