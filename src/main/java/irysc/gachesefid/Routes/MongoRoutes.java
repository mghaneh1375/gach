package irysc.gachesefid.Routes;

import com.google.common.base.CaseFormat;
import com.mongodb.client.MongoCollection;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.Quiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.*;

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

    @GetMapping(value = "/transfer_basics")
    @ResponseBody
    public String transferBasics() {

        HashMap<PairValue, ArrayList<PairValue>> grades = GradeRepository.getGradeWithLessonsFromMySQL();
        int counter = 0;

        for(PairValue grade : grades.keySet()) {

            List<Document> lessonsDoc = new ArrayList<>();
            List<PairValue> lessons = grades.get(grade);

            for(PairValue lesson : lessons) {
                counter++;
                lessonsDoc.add(
                        new Document("name", lesson.getValue())
                            .append("_id", new ObjectId())
                            .append("mysql_id", lesson.getKey())
                );
            }

            gradeRepository.insertOne(
                    new Document("name", grade.getValue())
                    .append("lessons", lessonsDoc)
            );
        }
        System.out.println(counter);

        return "ok";
    }

    @GetMapping(value = "/transfer_subjects")
    @ResponseBody
    public String transferSubjects() {

        ArrayList<Document> docs = gradeRepository.find(null, null);
        for(Document doc : docs) {

            if(!doc.containsKey("lessons"))
                continue;

            Document gradeDoc = new Document(
                    "_id", doc.getObjectId("_id")
            ).append("name", doc.getString("name"));

            List<Document> lessons = doc.getList("lessons", Document.class);

            for(Document lesson : lessons) {

                if(!lesson.containsKey("mysql_id"))
                    continue;

                Document lessonDoc = new Document(
                        "_id", lesson.getObjectId("_id")
                ).append("name", lesson.getString("name"));

                ArrayList<Document> subjects = SubjectRepository.getAllSubjectsByLessonIdFromMySQL(
                        lesson.getInteger("mysql_id")
                );

                for(Document subject : subjects) {
                    subject.put("lesson", lessonDoc);
                    subject.put("grade", gradeDoc);
                    subjectRepository.insertOne(subject);
                }
            }

        }

        return "ok";
    }

    @GetMapping(value = "/transfer_questions")
    @ResponseBody
    public String transferQuestions() {

        ArrayList<Document> docs = subjectRepository.find(null, null);
        for(Document doc : docs) {

            if(!doc.containsKey("code"))
                continue;

            int code = Integer.parseInt(doc.getString("code").replace(" ", ""));
            if(code > 10000)
                continue;

            ArrayList<Document> questions = QuestionRepository.findAllMysql(
                    code
            );

            for(Document question : questions) {
                question.put("subject_id", doc.getObjectId("_id"));
                questionRepository.insertOne(question);
            }

        }

        return "ok";
    }

    @GetMapping(value = "/transfer_authors")
    @ResponseBody
    public String transferAuthors() {

        ArrayList<Document> docs = UserRepository.fetchAuthorsFromMySQL();

        for(Document doc : docs)
            authorRepository.insertOne(doc);

        return "ok";
    }

    @GetMapping(value = "/transfer_quizes")
    @ResponseBody
    public String transferQuizes() {

        MongoCollection<Document> documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("regular_quiz");
        MongoCollection<Document> documentMongoCollection2 = GachesefidApplication.mongoDatabase.getCollection("regular_quiz_result");

//        ArrayList<Quiz> quizzes = RegularQuizRepository.findAllMysql();

//        for (Quiz quiz : quizzes) {
//
//            Document document = new Document();
//            for (String key : quiz.cols.keySet()) {
//                String secKey = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key);
//                document.append(secKey, quiz.cols.get(key));
//            }
//
//            if (!document.containsKey("user_id"))
//                continue;
//
////            Document user = UserRepository.findByMysqlId(document.getInteger("user_id"));
////            if(user == null)
////                continue;
//
//            document.put("start_date", Integer.parseInt(document.getString("start_date")));
//            document.put("start_time", Integer.parseInt(document.getString("start_time")));
//            document.put("end_date", Integer.parseInt(document.getString("end_date")));
//            document.put("end_time", Integer.parseInt(document.getString("end_time")));
//
////            document.put("user_id", user.getObjectId("_id"));
//            ArrayList<ArrayList<Object>> all = RegularQuizRepository.findAllQuizRegistryMysql(document.getInteger("id"));
//
//            document.put("students", all.get(0));
//            document.put("ranking", new ArrayList<>());
//            document.put("questions", RegularQuizRepository.findQuizQuestions(document.getInteger("id")));
//            document.remove("id");
//            documentMongoCollection.insertOne(document);
//
//            ObjectId quizId = document.getObjectId("_id");
//
//            for (Object student : all.get(1)) {
//
//                ((Document) student).append("lessons", new ArrayList<>())
//                        .append("quiz_id", quizId);
//
//                documentMongoCollection2.insertOne((Document) student);
//            }
//        }

        return "Sam";
    }

    @GetMapping(value = "/transfer_states")
    @ResponseBody
    public String transferStates() {

        ArrayList<Document> docs = StateRepository.fetchAllFromMysql();
        for(Document doc : docs) {

            int stateMysqlId = doc.getInteger("id");
            doc.remove("id");
            ObjectId oId = stateRepository.insertOneWithReturnId(doc);

            ArrayList<Document> cities = CityRepository.fetchAllByStateIdFromMysql(stateMysqlId);
            for(Document city : cities) {
                city.append("state_id", oId);
                cityRepository.insertOne(city);
            }

        }

        return "salam";
    }

    @GetMapping(value = "/transfer_schools")
    @ResponseBody
    public String transferSchools() {

        ArrayList<Document> docs = SchoolRepository.fetchAllFromMysql();
        for(Document doc : docs)
            schoolRepository.insertOne(doc);

        return "salam";
    }
}
