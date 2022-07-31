package irysc.gachesefid.DB;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Main.GachesefidApplication.authorRepository;
import static irysc.gachesefid.Utility.Utility.printException;

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

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<Document> findAllMysql(int subjectId) {

        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select q.*, (select count(*) from regularqoq where questionId = q.id) as used from question q, soq s where s.qId = q.id and s.sId = ?";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, subjectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    Document document = new Document();

                    for (int i = 1; i <= rsmd.getColumnCount(); i++)
                        document.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rsmd.getColumnName(i)), rs.getObject(i));

                    document.remove("s_id");
                    document.put("visibility", document.getInteger("status") == 1);
                    document.remove("status");

                    document.put("answer", document.get("ans"));
                    document.remove("ans");

                    document.put("answer_file", document.get("ans_file"));
                    document.remove("ans_file");

                    document.put("used", Integer.parseInt(document.get("used").toString()));

                    document.put("kind_question",
                            Integer.parseInt(document.get("kind_q").toString()) == 1 ? QuestionType.TEST.getName() :
                                    Integer.parseInt(document.get("kind_q").toString()) == 0 ? QuestionType.SHORT_ANSWER.getName() :
                                            QuestionType.MULTI_SENTENCE.getName()
                    );

                    document.remove("kind_q");

                    if(document.getString("kind_question").equalsIgnoreCase(QuestionType.MULTI_SENTENCE.getName())) {
                        document.put("answer", document.get("answer").toString().replace("2", "0"));
                        document.put("sentences_count", document.get("answer").toString().length());
                    }
                    else {
                        if(document.getString("kind_question").equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
                            document.put("answer", Double.parseDouble(document.get("answer").toString()));
                        else
                            document.put("answer", Integer.parseInt(document.get("answer").toString()));
                    }

                    document.put("level", document.getInteger("level") == 1 ?
                            "easy" :
                            document.getInteger("level") == 2 ? "mid" : "hard");

                    if(!document.getString("kind_question").equalsIgnoreCase(QuestionType.SHORT_ANSWER.getName()))
                        document.remove("telorance");

                    if(!document.getString("kind_question").equalsIgnoreCase(QuestionType.TEST.getName()))
                        document.remove("choices_count");

                    Document author = authorRepository.findBySecKey(document.getInteger("author"));
                    if(author == null)
                        document.put("author", "آیریسک");
                    else
                        document.put("author", author.getString("first_name") + " " + author.getString("last_name"));

                    document.remove("id");
                    output.add(document);
                }
                catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return output;
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static HashMap<String, Object> findAllLessonMysql(int sId) {

        HashMap<String, Object> output = new HashMap<>();

        try {
            String sql = "select s.name, l.id, l.name, g.id, g.name from subject s, lesson l, grade g where s.id = ? and s.lessonId = l.id and l.gradeId = g.id";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ps.setInt(1, sId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                output.put("subject_name", rs.getString(1));
                output.put("subject_id", sId);
                output.put("lesson_name", rs.getString(3));
                output.put("lesson_id", rs.getInt(2));
                output.put("grade_name", rs.getString(5));
                output.put("grade_id", rs.getString(4));
            }
        }
        catch (Exception x) {
            printException(x);
        }

        return output;
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
