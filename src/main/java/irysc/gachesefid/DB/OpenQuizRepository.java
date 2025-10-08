package irysc.gachesefid.DB;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;


public class OpenQuizRepository extends Common {

    public OpenQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "open_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public int countIndividualRegistrationsLastMonth() {
        return Optional.ofNullable(
                documentMongoCollection.aggregate(List.of(
                        Aggregates.unwind("$students"),
                        Aggregates.match(Filters.gte("students.register_at", System.currentTimeMillis() - ONE_MONTH_MIL_SEC)),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }
}
