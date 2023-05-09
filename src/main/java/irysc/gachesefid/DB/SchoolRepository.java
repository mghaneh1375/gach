package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.GradeSchool;
import irysc.gachesefid.Models.KindSchool;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.cityRepository;
import static irysc.gachesefid.Utility.Utility.printException;

public class SchoolRepository extends Common {

    public SchoolRepository() {
        init();
    }

    @Override
    void init() {
        table = "school";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public static ArrayList<Document> fetchAllFromMysql() {

        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select id, name, level, kind, cityId, address from school where 1";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Document city = cityRepository.findOne(eq("mysql_id", rs.getInt(5)), new BasicDBObject("_id", 1).append("name", 1));
                Document doc = new Document("name", rs.getString(2))
                        .append("mysql_id", rs.getInt(1))
                        .append("city_id", city.getObjectId("_id"))
                        .append("city_name", city.getString("name"))
                        .append("address", rs.getString(6));

                switch (rs.getInt(3)) {
                    case 0:
                        doc.append("grade", GradeSchool.MOTEVASETEAVAL.getName());
                        break;
                    case 1:
                        doc.append("grade", GradeSchool.MOTEVASETEDOVOM.getName());
                        break;
                    case 2:
                        doc.append("grade", GradeSchool.DABESTAN.getName());
                        break;
                }

                switch (rs.getInt(4)) {
                    case 1:
                        doc.append("kind", KindSchool.SAMPAD.getName());
                        break;
                    case 2:
                        doc.append("kind", KindSchool.GHEYR.getName());
                        break;
                    case 3:
                        doc.append("kind", KindSchool.NEMONE.getName());
                        break;
                    case 4:
                        doc.append("kind", KindSchool.SHAHED.getName());
                        break;
                    case 5:
                        doc.append("kind", KindSchool.SAYER.getName());
                        break;
                    case 6:
                        doc.append("kind", KindSchool.HEYAT.getName());
                        break;
                    case 7:
                        doc.append("kind", KindSchool.DOLATI.getName());
                        break;
                }

                output.add(doc);
            }
        }
        catch (Exception x) {
            printException(x);
        }

        return output;

    }
}
