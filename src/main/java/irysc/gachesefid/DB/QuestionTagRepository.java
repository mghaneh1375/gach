package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class QuestionTagRepository extends Common {


    public QuestionTagRepository() {
        init();
    }

    @Override
    void init() {
        table = "question_tag";
        secKey = "code";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
