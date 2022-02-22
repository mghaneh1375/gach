package irysc.gachesefid.models;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;

public class User {

    public long id;
    public String username;
    public String password;
    public Role role;
    public boolean isForeign;
    public HashMap<String, Object> cols = new HashMap<>();


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void fillAttrs(ResultSet rs) {

        try {
            ResultSetMetaData rsmd = rs.getMetaData();

            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                cols.put(rsmd.getColumnName(i), rs.getObject(i));
            }

            id = (Long) cols.get("id");
            username = (String) cols.get("username");
            password = (String) cols.get("password");
            role = cols.get("level").equals(0) ? Role.ROLE_CLIENT : Role.ROLE_ADMIN;
        }
        catch (Exception x) {
            x.printStackTrace();
        }

    }

}
