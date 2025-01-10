package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeacherBioRepository extends Common {

    public TeacherBioRepository() {
        init();
    }

    @Override
    void init() {
        table = "teacher_bio";
        secKey = "name";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
