package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class QuestionReportRepository extends Common {

    public QuestionReportRepository() {
        init();
    }

    @Override
    void init() {
        table = "question_report";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
