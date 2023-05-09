package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

public class RegularQuizResultRepository {

    static MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("regular_quiz_result");

    public static ArrayList<Document> getRegularQuizResults(ObjectId quizId) {

        FindIterable<Document> cursor = documentMongoCollection.find(eq("quiz_id", quizId))
                .projection(new BasicDBObject("_id", 1).append("student_answers", 1).append("user_id", 1)
                .append("question_ids", 1));

        ArrayList<Document> docs = new ArrayList<>();
        for(Document doc : cursor)
            docs.add(doc);

        return docs;
    }
}
