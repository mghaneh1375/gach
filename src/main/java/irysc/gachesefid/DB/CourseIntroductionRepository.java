package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CourseIntroductionRepository extends Common {

    public CourseIntroductionRepository() {
        init();
    }

    @Override
    void init() {
        table = "course_introduction";
        secKey = "title";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
