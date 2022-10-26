package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class OpenQuizRepository extends Common {

    public OpenQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "open_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
