package irysc.gachesefid.Models;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;

public class Quiz {

    public HashMap<String, Object> cols = new HashMap<>();

    public void fillAttrs(ResultSet rs) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                cols.put(rsmd.getColumnName(i), rs.getObject(i));
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }

    }

}
