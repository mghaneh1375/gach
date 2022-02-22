package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.Variable;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Utility.StaticValues.USER_DIGEST;

public class TransactionRepository extends Common {

    public TransactionRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("transaction");
    }

    public ObjectId insertOneWithReturnObjectId(Document document) {
        documentMongoCollection.insertOne(document);
        return document.getObjectId("_id");
    }

    public ArrayList<Document> findWithJoin(Bson match,
                                            ObjectId classId,
                                            ObjectId payLinkId,
                                            ObjectId teacherId,
                                            ObjectId studentId,
                                            ObjectId courseId,
                                            ObjectId lessonId,
                                            ObjectId scheduleId,
                                            ObjectId termId,
                                            String scheduleTag,
                                            Bson skip, Bson limit
    ) {

        List<Bson> filters = new ArrayList<>();

        ArrayList<Bson> userFilters = new ArrayList<>();
        userFilters.add(expr(new Document("$eq", Arrays.asList("$_id", "$$myId"))));

        if (studentId != null)
            userFilters.add(expr(new Document("$eq", Arrays.asList("$_id", studentId))));

        filters.add(lookup("user",
                Collections.singletonList(new Variable<>("myId", "$user_id")), Arrays.asList(
                        match(and(userFilters)),
                        project(USER_DIGEST)
                ), "student"));

        ArrayList<Bson> payLinkFilters = new ArrayList<>();
        payLinkFilters.add(expr(new Document("$eq", Arrays.asList("$_id", "$$payLinkId"))));

        if (payLinkId != null)
            payLinkFilters.add(expr(new Document("$eq", Arrays.asList("$_id", payLinkId))));

        filters.add(lookup("pay_link",
                Collections.singletonList(new Variable<>("payLinkId", "$pay_link_id")), Arrays.asList(
                        match(and(payLinkFilters))
                ), "payLink"));

        filters.add(lookup("offcode",
                Collections.singletonList(new Variable<>("offCodeId", "$off_code")), Arrays.asList(
                        match(expr(new Document("$eq", Arrays.asList("$_id", "$$offCodeId")))),
                        project(
                                new BasicDBObject("type", 1).append("amount", 1)
                                        .append("expire_at", 1).append("section", 1)
                        )
                ), "offCode"));

        ArrayList<Bson> classFilters = new ArrayList<>();
        classFilters.add(expr(new Document("$eq", Arrays.asList("$_id", "$$classId"))));

        if (classId != null)
            classFilters.add(expr(new Document("$eq", Arrays.asList("$_id", classId))));

        if (scheduleId != null)
            classFilters.add(expr(new Document("$eq", Arrays.asList("$schedule_id", scheduleId))));

        if (teacherId != null)
            classFilters.add(expr(new Document("$eq", Arrays.asList("$teacher_id", teacherId))));

        if (termId != null)
            classFilters.add(expr(new Document("$eq", Arrays.asList("$term_id", termId))));

        classFilters.add(exists("schedule.0"));
        classFilters.add(exists("term.0"));

        ArrayList<Bson> termFilters = new ArrayList<>();
        termFilters.add(expr(new Document("$eq", Arrays.asList("$_id", "$$termId"))));

        if (courseId != null)
            termFilters.add(expr(new Document("$eq", Arrays.asList("$course._id", courseId))));

        if (lessonId != null)
            termFilters.add(expr(new Document("$eq", Arrays.asList("$lesson._id", lessonId))));

        ArrayList<Bson> scheduleFilters = new ArrayList<>();
        scheduleFilters.add(expr(new Document("$eq", Arrays.asList("$_id", "$$scheduleId"))));

        if (scheduleTag != null)
            scheduleFilters.add(expr(new Document("$eq", Arrays.asList("$tag", scheduleTag))));

        if (match != null)
            filters.add(match);

        filters.add(lookup("class",
                Collections.singletonList(new Variable<>("classId", "$class_id")), Arrays.asList(
                        lookup("term",
                                Collections.singletonList(new Variable<>("termId", "$term_id")), Arrays.asList(
                                        match(and(termFilters)),
                                        project(new BasicDBObject("name", 1).append("_id", 1).append("course", 1).append("lesson", 1))
                                ), "term"),
                        lookup("schedule",
                                Collections.singletonList(new Variable<>("scheduleId", "$schedule_id")), Arrays.asList(
                                        match(and(scheduleFilters)),
                                        project(new BasicDBObject("name", 1).append("tag", 1)
                                                .append("latin_tag", 1))
                                ), "schedule"),
                        lookup("user",
                                Collections.singletonList(new Variable<>("teacherId", "$teacher_id")), Arrays.asList(
                                        match(expr(new Document("$eq", Arrays.asList("$_id", "$$teacherId")))),
                                        project(USER_DIGEST)
                                ), "teacher"),
                        match(and(classFilters))
                ), "class"));

        filters.add(sort(Sorts.descending("created_at")));
        if (skip != null && limit != null) {
            filters.add(skip);
            filters.add(limit);
        }

        filters.add(unwind("$student"));
        filters.add(unwind("$class", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        filters.add(unwind("$payLink", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        ArrayList<Document> docs = new ArrayList<>();
        AggregateIterable<Document> cursor = documentMongoCollection.aggregate(filters);

        for (Document doc : cursor)
            docs.add(doc);

        return docs;
    }
}
