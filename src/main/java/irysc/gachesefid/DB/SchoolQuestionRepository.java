package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class SchoolQuestionRepository extends Common {

    public final static String FOLDER = "school_question";

    public SchoolQuestionRepository() {
        init();
    }

    @Override
    void init() {
        table = "school_question";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
