package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.types.ObjectId;


public class SubjectRepository extends Common {

    public SubjectRepository() {
        init();
    }

    @Override
    void init() {
        table = "subject";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public void clearFormCacheByGradeId(ObjectId gradeId) {

        //todo: img

    }

    public void clearFormCacheByLessonId(ObjectId gradeId) {

        //todo: img

    }
}
