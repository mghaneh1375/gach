package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.Quiz;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

public class SchoolQuizRepository extends Common {

    public SchoolQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "school_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
