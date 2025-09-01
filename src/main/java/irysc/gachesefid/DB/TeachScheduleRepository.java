package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public int countIndividualRegistrationsLastMonth() {
        return Optional.ofNullable(
                documentMongoCollection.aggregate(List.of(
                        Aggregates.unwind("$students"),
                        Aggregates.match(Filters.gte("students.created_at", System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30)),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }
}
