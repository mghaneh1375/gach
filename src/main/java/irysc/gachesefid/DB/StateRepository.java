package irysc.gachesefid.DB;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;

import static irysc.gachesefid.Utility.Utility.printException;

public class StateRepository extends Common {

    public StateRepository() {
        init();
    }

    @Override
    void init() {
        table = "state";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public static ArrayList<Document> fetchAllFromMysql() {

        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select id, name from state where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                output.add(new Document("name", rs.getString(2))
                        .append("id", rs.getInt(1))
                );
        }
        catch (Exception x) {
            printException(x);
        }

        return output;

    }
}
