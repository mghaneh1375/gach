package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;

public class TeachScheduleRepository extends Common{
    @Override
    void init() {
        table = "teach_schedule";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public TeachScheduleRepository() {
        init();
    }

    public Integer getStudentsSize(ObjectId teacherId) {
        List<Bson> filters = new ArrayList<>() {{
            add(match(and(eq("user_id", teacherId), exists("students"))));
            add(new BasicDBObject("$group",
                            new BasicDBObject("_id", null)
                                    .append("total_sum", new BasicDBObject("$sum", new BasicDBObject("$size", "$students")))
                    )
            );
        }};

        AggregateIterable<Document> aggregate = documentMongoCollection.aggregate(filters);
        for (Document doc : aggregate)
            return doc.getInteger("total_sum");

        return 0;
    }
}
