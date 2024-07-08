package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.HashMap;

import static com.mongodb.client.model.Filters.in;

public class QuestionRepository extends Common {

    public static final String FOLDER = "questions";

    public QuestionRepository() {
        init();
    }

    public HashMap<Integer, Question> getAnswers(int[] ids) {

        FindIterable<Document> docs = documentMongoCollection.find(in("id", ids)).projection(new BasicDBObject("ans", 1).
                append("id", 1).append("lesson_id", 1).append("lesson_name", 1));

        HashMap<Integer, Question> answers = new HashMap<>();

        for(Document doc : docs)
            answers.put(doc.getInteger("id"),
                    new Question(doc.getInteger("id"),
                            doc.getInteger("ans"),
                            doc.getInteger("lesson_id"),
                            doc.getString("lesson_name")
                    ));

        return answers;
    }

    @Override
    void init() {
        table = "question";
        secKey = "organization_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    @Override
    public void cleanReject(Document doc) {

    }

    @Override
    public void cleanRemove(Document doc) {
        FileUtils.removeFile(doc.getString("question_file"), QuestionRepository.FOLDER);
        if(doc.containsKey("answer_file"))
            FileUtils.removeFile(doc.getString("answer_file"), QuestionRepository.FOLDER);
    }
}
