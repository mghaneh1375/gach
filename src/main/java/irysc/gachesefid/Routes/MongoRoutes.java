package irysc.gachesefid.Routes;

import com.google.common.base.CaseFormat;
import com.mongodb.client.MongoCollection;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.DB.RegularQuizRepository;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.models.Quiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;

@Controller
@RequestMapping(path = "/mongo")
public class MongoRoutes extends Router {

    @GetMapping(value = "/transfer_users")
    @ResponseBody
    public String transferUsers() {

        MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("user");
//        ArrayList<User> users = UserRepository.findAllMysql();

//        for (User user : users) {
//
//            Document document = new Document();
//            for(String key : user.cols.keySet())
//                document.append(key, user.cols.get(key));
//
//            if(document.getInteger("level") > 1) {
//                document.put("level", true);
//                document.put("access", "admin");
//            }
//            else
//                document.put("level", false);
//
//            documentMongoCollection.insertOne(document);
//        }

        return "salam";
    }

    @GetMapping(value = "/transfer_questions")
    @ResponseBody
    public String transferQuestions() {

        MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("question");
        ArrayList<Document> docs = QuestionRepository.findAllMysql();

        for (Document doc : docs) {
            try {
                doc.put("ans", Integer.parseInt(doc.getString("ans")));
                documentMongoCollection.insertOne(doc);
            } catch (Exception x) {
                documentMongoCollection.insertOne(doc);
            }
        }

        return "salam";
    }

    @GetMapping(value = "/transfer_quizes")
    @ResponseBody
    public String transferQuizes() {

        MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("regular_quiz");
        MongoCollection<Document> documentMongoCollection2 = GachesefidApplication.mongoDatabase.getCollection("regular_quiz_result");

        ArrayList<Quiz> quizzes = RegularQuizRepository.findAllMysql();

        for (Quiz quiz : quizzes) {

            Document document = new Document();
            for (String key : quiz.cols.keySet()) {
                String secKey = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key);
                document.append(secKey, quiz.cols.get(key));
            }

            if (!document.containsKey("user_id"))
                continue;

//            Document user = UserRepository.findByMysqlId(document.getInteger("user_id"));
//            if(user == null)
//                continue;

            document.put("start_date", Integer.parseInt(document.getString("start_date")));
            document.put("start_time", Integer.parseInt(document.getString("start_time")));
            document.put("end_date", Integer.parseInt(document.getString("end_date")));
            document.put("end_time", Integer.parseInt(document.getString("end_time")));

//            document.put("user_id", user.getObjectId("_id"));
            ArrayList<ArrayList<Object>> all = RegularQuizRepository.findAllQuizRegistryMysql(document.getInteger("id"));

            document.put("students", all.get(0));
            document.put("ranking", new ArrayList<>());
            document.put("questions", RegularQuizRepository.findQuizQuestions(document.getInteger("id")));
            document.remove("id");
            documentMongoCollection.insertOne(document);

            ObjectId quizId = document.getObjectId("_id");

            for (Object student : all.get(1)) {

                ((Document) student).append("lessons", new ArrayList<>())
                        .append("quiz_id", quizId);

                documentMongoCollection2.insertOne((Document) student);
            }
        }

        return "Sam";
    }
}
