package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CustomQuizRepository extends Common {

    public CustomQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "custom_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
