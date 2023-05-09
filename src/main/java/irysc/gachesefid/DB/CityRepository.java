package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.Utility.printException;

public class CityRepository extends Common {

    public CityRepository() {
        init();
    }

    @Override
    void init() {
        table = "city";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public static ArrayList<Document> fetchAllByStateIdFromMysql(int id) {

        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select id, name from city where stateId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                output.add(new Document("name", rs.getString(2))
                        .append("mysql_id", rs.getInt(1))
                );
        }
        catch (Exception x) {
            printException(x);
        }

        return output;

    }
}
