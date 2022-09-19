package irysc.gachesefid.Routes;

import irysc.gachesefid.DB.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Models.Quiz;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Main.GachesefidApplication.*;

@Controller
@RequestMapping(path = "/mongo")
public class MongoRoutes extends Router {

    @GetMapping(value = "/transfer_users")
    @ResponseBody
    public String transferUsers() {

        ArrayList<Quiz> users = UserRepository.findAllMysql();
        ArrayList<String> accesses = new ArrayList<>(){
            {add(Access.STUDENT.getName());}
        };

        Document config = Utility.getConfig();
        Document avatar = avatarRepository.findById(config.getObjectId("default_avatar"));

        long curr = System.currentTimeMillis();

        for (Quiz user : users) {

            try {

                Document city = cityRepository.findOne(eq("mysql_id", Integer.parseInt(user.cols.get("cityId").toString())), null);
                if(city == null)
                    continue;

                Document school = schoolRepository.findOne(eq("mysql_id", Integer.parseInt(user.cols.get("sId").toString())), null);
                if(school == null)
                    continue;

                Document document = new Document("level", false)
                        .append("accesses", accesses).append("status", "active");

                document.put("first_name", user.cols.get("firstName"));
                document.put("last_name", user.cols.get("lastName"));
                document.put("NID", user.cols.get("NID"));
                document.put("phone", user.cols.get("phoneNum"));
                document.put("money", Integer.parseInt(user.cols.get("money").toString()));
                document.put("sex", user.cols.get("sex").toString().equals("1") ? Sex.MALE.getName() : Sex.FEMALE.getName());
                document.put("student_id", Utility.getRandIntForStudentId(Utility.getToday("/").substring(0, 6).replace("/", "")));
                document.put("events", new ArrayList<>());
                document.put("invitation_code", Utility.randomString(8));
                document.put("created_at", curr);
                document.put("avatar_id", avatar.getObjectId("_id"));
                document.put("pic", avatar.getString("file"));
                document.put("password", user.cols.get("password"));
                document.put("coin", config.getDouble("init_coin"));
                document.put("branches", new ArrayList<>());
                document.put("city", new Document("_id", city.getObjectId("_id"))
                        .append("name", city.getString("name"))
                );
                document.put("school", new Document("_id", school.getObjectId("_id"))
                        .append("name", school.getString("name"))
                );
                document.put("mysql_id", Integer.parseInt(user.cols.get("id").toString()));

                if (!user.cols.get("email").toString().isEmpty())
                    document.put("mail", user.cols.get("email").toString());

                userRepository.insertOne(document);
            }
            catch (Exception x) {
                x.printStackTrace();
            }
        }

        return "salam";
    }

    @GetMapping(value = "/transfer_roq")
    @ResponseBody
    public String transferROQ() {

        ArrayList<Document> quizzes = iryscQuizRepository.find(exists("mysql_id"), null);
        for(Document quiz : quizzes) {

            Document questions = quiz.get("questions", Document.class);

            ArrayList<PairValue> pairValues = irysc.gachesefid.Controllers.Quiz.Utility.getAnswers(
                    ((Binary)questions.getOrDefault("answers", new byte[0])).getData()
            );

            ArrayList<Document> students = new ArrayList<>();
            ArrayList<Document> docs = IRYSCQuizRepository.findAllQuizRegistryMysql(quiz.getInteger("mysql_id"));

            for(Document doc : docs) {

                List<Object> answers = doc.getList("answers", Object.class);
                if(pairValues.size() != answers.size())
                    continue;

                int idx = -1;
                ArrayList<PairValue> stdAnswers = new ArrayList<>();

                try {
                    boolean allow = true;

                    for (PairValue p : pairValues) {

                        idx++;
                        Object stdAns = answers.get(idx);
                        Object stdAnsAfterFilter = null;

                        String type = p.getKey().toString();
                        if (type.equalsIgnoreCase(QuestionType.TEST.getName())) {

                            double d = (double) stdAns;
                            int s = (int) d;

                            PairValue pp = (PairValue) p.getValue();
                            if(s > (int)pp.getKey() || s < 0) {
                                allow = false;
                                break;
                            }

                            stdAnsAfterFilter = new PairValue(
                                    pp.getKey(),
                                    s
                            );
                        }
                        else if (type.equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
                            stdAnsAfterFilter = stdAns;

                        if(stdAnsAfterFilter != null)
                            stdAnswers.add(new PairValue(p.getKey(), stdAnsAfterFilter));
                    }

                    if(allow) {
                        doc.put("answers", irysc.gachesefid.Controllers.Quiz.Utility.getStdAnswersByteArr(stdAnswers));
                        students.add(doc);
                    }
                }
                catch (Exception x) {
                    System.out.println(x.getMessage());
                    x.printStackTrace();
                    return "nooo";
                }

            }

            quiz.put("students", students);
            quiz.put("registered", students.size());
            iryscQuizRepository.replaceOne(
                    quiz.getObjectId("_id"),
                    quiz
            );
        }

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

    @GetMapping(value = "/transfer_quizzes")
    @ResponseBody
    public String transferQuizzes() {

        ArrayList<Quiz> quizzes = IRYSCQuizRepository.findAllMysql();

        ObjectId createdBy = new ObjectId("62ac6228301bf06072cab5f5");
        long createdAt = System.currentTimeMillis();

        for (Quiz quiz : quizzes) {

            try {
                int qNos = Integer.parseInt(quiz.cols.get("qNos").toString());
                int quizId = Integer.parseInt(quiz.cols.get("id").toString());
                if(quizId < 106)
                    continue;

                PairValue p = QuestionRepository
                        .findOrganizationIdByQuiz(quizId);

                ArrayList<String> questionsCodes = (ArrayList<String>) p.getKey();
                if (questionsCodes.size() != qNos)
                    continue;

                ArrayList<Document> questions = new ArrayList<>();

                for(String code : questionsCodes) {
                    Document question = questionRepository.findOne(
                            eq("organization_id", code), null
                    );
                    if(question == null)
                        continue;

                    questions.add(question);
                }

                if(questions.size() != qNos)
                    continue;

                Document document = new Document();

                String s = Utility.convertStringToDate(quiz.cols.get("startDate").toString(), "-");
                String[] splited = s.split("-");
                long start = Utility.getTimestamp(JalaliCalendar.jalaliToGregorian(
                        new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                        .format("-")
                );

                String e = Utility.convertStringToDate(quiz.cols.get("endDate").toString(), "-");
                splited = e.split("-");
                long end = Utility.getTimestamp(JalaliCalendar.jalaliToGregorian(
                        new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                        .format("-")
                );

                String sR = Utility.convertStringToDate(quiz.cols.get("startReg").toString(), "-");
                splited = sR.split("-");
                long startRegistry = Utility.getTimestamp(JalaliCalendar.jalaliToGregorian(
                        new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                        .format("-")
                );

                String eR = Utility.convertStringToDate(quiz.cols.get("endReg").toString(), "-");
                splited = eR.split("-");
                long endRegistry = Utility.getTimestamp(JalaliCalendar.jalaliToGregorian(
                        new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                        .format("-")
                );

                document.put("start", start);
                document.put("end", end);
                document.put("start_registry", startRegistry);
                document.put("end_registry", endRegistry);
                document.put("price", Integer.parseInt(quiz.cols.get("price").toString()));
                document.put("title", quiz.cols.get("name"));
                document.put("description", "");
                document.put("launch_mode", "online");
                document.put("show_results_after_correction", true);
                document.put("permute", false);
                document.put("visibility", true);
                document.put("back_en", true);
                document.put("mode", "regular");
                document.put("minus_mark", true);
                document.put("mysql_id", quizId);
                document.put("tags", new ArrayList<>());
                document.put("created_by", createdBy);
                document.put("created_at", createdAt);
                document.put("top_students_count", Integer.parseInt(quiz.cols.get("ranking").toString()));


                ArrayList<Double> marks = (ArrayList<Double>) p.getValue();
                Document questionsDoc = new Document();
                questionsDoc.put("marks", marks);

                ArrayList<ObjectId> ids = new ArrayList<>();

                for(Document question : questions)
                    ids.add(question.getObjectId("_id"));

                questionsDoc.put("_ids", ids);
                byte[] answers = irysc.gachesefid.Controllers.Quiz.Utility.getAnswersByteArr(ids);
                questionsDoc.put("answers", answers);

                document.put("questions", questionsDoc);
                document.put("students", new ArrayList<>());

                iryscQuizRepository.insertOne(document);
            }
            catch (Exception x) {
                x.printStackTrace();
            }
        }

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
