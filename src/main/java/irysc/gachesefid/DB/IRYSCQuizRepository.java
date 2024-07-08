package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.List;

public class IRYSCQuizRepository extends Common {

    public final static String FOLDER = "irysc_quizzes";

    public IRYSCQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "irysc_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    @Override
    public void cleanRemove(Document doc) {

        if(!doc.containsKey("attaches"))
            return;

        List<String> attaches = doc.getList("attaches", String.class);
        for(String attach : attaches)
            FileUtils.removeFile(attach, FOLDER);

    }
}
