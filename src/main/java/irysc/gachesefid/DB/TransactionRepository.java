package irysc.gachesefid.DB;

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
import static irysc.gachesefid.Utility.StaticValues.USER_DIGEST;

public class TransactionRepository extends Common {

    public TransactionRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("transaction");
    }


    public AggregateIterable<Document> all(Bson match) {

        List<Bson> filters = new ArrayList<>();

        if(match != null)
            filters.add(match);

        filters.add(sort(Sorts.descending("created_at")));

        filters.add(lookup("user",
                Collections.singletonList(new Variable<>("myId", "$user_id")), Arrays.asList(
                        match(expr(new Document("$eq", Arrays.asList("$_id", "$$myId")))),
                        project(USER_DIGEST)
                ), "user"));

        filters.add(unwind("$user", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        return documentMongoCollection.aggregate(filters);
    }

}
