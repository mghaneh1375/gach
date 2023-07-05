package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdviseExamTagRepository extends Common {

    public AdviseExamTagRepository() {
        init();
    }

    @Override
    void init() {
        table = "advise_exam_tag";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
