package irysc.gachesefid.DB;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import static irysc.gachesefid.Utility.Utility.printException;


public class GradeRepository extends Common {

    public GradeRepository() {
        init();
    }

    @Override
    void init() {
        table = "grade";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public static HashMap<PairValue, ArrayList<PairValue>> getGradeWithLessonsFromMySQL() {

        HashMap<PairValue, ArrayList<PairValue>> grades = new HashMap<>();

        try {
            String sql = "select id, name from grade where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                int id = rs.getInt(1);
                String name = rs.getString(2);
                PairValue p = new PairValue(id, name);

                sql = "select id, name from lesson where gradeId = ?";
                ps = GachesefidApplication.con.prepareStatement(sql);
                ps.setInt(1, id);
                ResultSet rs2 = ps.executeQuery();
                ArrayList<PairValue> lessons = new ArrayList<>();

                while (rs2.next())
                    lessons.add(new PairValue(rs2.getInt(1), rs2.getString(2)));

                grades.put(p, lessons);
            }
        }
        catch (Exception x) {
            printException(x);
        }

        return grades;
    }
}
