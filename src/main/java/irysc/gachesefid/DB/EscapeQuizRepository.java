package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.Quiz;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.printException;

public class EscapeQuizRepository extends Common {

    public final static String FOLDER = "escape_quizzes";

    public EscapeQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "escape_quiz";
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
