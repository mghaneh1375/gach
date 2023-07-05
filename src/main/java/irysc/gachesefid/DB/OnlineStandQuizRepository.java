package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.List;

public class OnlineStandQuizRepository extends Common {

    public final static String FOLDER = "online_standing_quizzes";

    public OnlineStandQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "online_standing";
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
