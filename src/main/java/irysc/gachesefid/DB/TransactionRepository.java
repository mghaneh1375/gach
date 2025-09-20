package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.Variable;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.expr;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Utility.StaticValues.USER_DIGEST;

public class TransactionRepository extends Common {

    private static final Integer PAGE_SIZE = 20;

    public TransactionRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("transaction");
    }


    public AggregateIterable<Document> all(Bson match, int pageIndex) {

        List<Bson> filters = new ArrayList<>();
        if (match != null)
            filters.add(match);

        filters.add(sort(Sorts.descending("created_at")));
        filters.add(skip((pageIndex - 1) * PAGE_SIZE));
        filters.add(limit(PAGE_SIZE));
        filters.add(lookup("user",
                Collections.singletonList(new Variable<>("myId", "$user_id")), Arrays.asList(
                        match(expr(new Document("$eq", Arrays.asList("$_id", "$$myId")))),
                        project(USER_DIGEST)
                ), "user"));
        filters.add(unwind("$user", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        filters.add(lookup("advisor_requests", "products", "_id", "advisorReqRef"));

        filters.add(lookup("irysc_quiz", "products", "_id", "iryscQuizRef"));
        filters.add(lookup("open_quiz", "products", "_id", "openQuizRef"));
        filters.add(lookup("content", "products", "_id", "contentRef"));
//        filters.add(unwind("$refTitle", new UnwindOptions().preserveNullAndEmptyArrays(true)));
        filters.add(project(fields(
                include("created_at"),
                include("ref_id"),
                include("off_code"),
                include("section"),
                include("amount"),
                include("account_money"),
                computed("user", "$user"),
                computed("openQuizRef", new Document("$map", new Document()
                        .append("input", "$openQuizRef")
                        .append("as", "item")
                        .append("in", new Document()
                                .append("name", "$$item.title")
                        )
                )),
                computed("contentRef", new Document("$map", new Document()
                        .append("input", "$contentRef")
                        .append("as", "item")
                        .append("in", new Document()
                                .append("name", "$$item.title")
                        )
                )),
                computed("iryscQuizRef", new Document("$map", new Document()
                        .append("input", "$iryscQuizRef")
                        .append("as", "item")
                        .append("in", new Document()
                                .append("name", "$$item.title")
                        )
                )),
                computed("advisorReqRef", new Document("$map", new Document()
                        .append("input", "$advisorReqRef")
                        .append("as", "item")
                        .append("in", new Document()
                                .append("advisor_id", "$$item.advisor_id")
                        )
                ))
        )));
//        filters.add(lookup("content",
//                Collections.singletonList(new Variable<>("contentId", "$products")), Arrays.asList(
//                        match(expr(new Document("$eq", Arrays.asList("$_id", "$$contentId")))),
//                        project(new BasicDBObject("title", 1))
//                ), "refTitle"));
//        filters.add(unwind("$refTitle", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        return documentMongoCollection.aggregate(filters);
    }

}
