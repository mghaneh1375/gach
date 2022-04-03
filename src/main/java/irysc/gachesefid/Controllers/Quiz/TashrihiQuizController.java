package irysc.gachesefid.Controllers.Quiz;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class TashrihiQuizController extends QuizAbstract {

    @Override
    void registry(Document student, Document quiz, int paid) {

        List<Document> students = quiz.getList("students", Document.class);

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                students, "_id", student.getObjectId("_id")
        ) != -1)
            return;

        Document stdDoc = new Document("_id", student.getObjectId("_id"))
                .append("paid", paid)
                .append("register_at", System.currentTimeMillis())
                .append("finish_at", null)
                .append("start_at", null)
                .append("answers", new ArrayList<>())
                .append("all_marked", false);

        if(quiz.getBoolean("permute"))
            stdDoc.put("question_indices", new ArrayList<>());

        students.add(stdDoc);


    }

    @Override
    void quit(Document student, Document quiz) {

    }

    @Override
    String buy(Document student, Document quiz) {
        return null;
    }


}
