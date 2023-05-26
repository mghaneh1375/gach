package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

public class EscapeQuizQuestionRepository extends Common {

    public static final String FOLDER = "escape_quiz_questions";

    public EscapeQuizQuestionRepository() {
        init();
    }

    @Override
    void init() {
        table = "escape_quiz_questions";
        secKey = "organization_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    @Override
    public void cleanReject(Document doc) {

    }

    @Override
    public void cleanRemove(Document doc) {
        FileUtils.removeFile(doc.getString("question_file"), EscapeQuizQuestionRepository.FOLDER);
        if(doc.containsKey("answer_file"))
            FileUtils.removeFile(doc.getString("answer_file"), EscapeQuizQuestionRepository.FOLDER);
    }
}
