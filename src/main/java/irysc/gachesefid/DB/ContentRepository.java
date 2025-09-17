package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Content.Utility;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Utility.StaticValues.CONTENT_DIGEST;


public class ContentRepository extends Common {

    public static final String FOLDER = "content";

    public ContentRepository() {
        init();
    }

    @Override
    void init() {
        table = "content";
        secKey = "slug";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public Integer getTeacherContentsBuyersSize(ObjectId teacherId) {
        List<Bson> filters = new ArrayList<>() {{
            add(match(eq("teacher_ids", teacherId)));
            add(new BasicDBObject("$group",
                            new BasicDBObject("_id", null)
                                    .append("total_sum", new BasicDBObject("$sum", new BasicDBObject("$size", "$users")))
                    )
            );
        }};

        AggregateIterable<Document> aggregate = documentMongoCollection.aggregate(filters);

        for (Document doc : aggregate)
            return doc.getInteger("total_sum");

        return 0;
    }

    public JSONArray getSuggestion(ObjectId userId, List<Bson> tags) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(
                        and(
                                ne("users._id", userId),
                                or(tags)
                        )
                ),
                Aggregates.addFields(new Field<>("buyersCount",
                        new Document("$size",
                                new Document("$ifNull", Arrays.asList("$users", List.of()))
                        )
                )),
                Aggregates.addFields(new Field<>("rate",
                        new Document("$ifNull", Arrays.asList("$rate", 0)))
                ),
                Aggregates.addFields(new Field<>("rate_count",
                        new Document("$ifNull", Arrays.asList("$rate_count", 0)))
                ),
                Aggregates.addFields(new Field<>("bayesianScore",
                        new Document("$divide", Arrays.asList(
                                new Document("$add", Arrays.asList(
                                        new Document("$multiply", Arrays.asList("$rate", "$rate_count")),
                                        new Document("$multiply", Arrays.asList(3.5, 10))
                                )),
                                new Document("$add", Arrays.asList("$rate_count", 10))
                        ))
                )),
                Aggregates.addFields(new Field<>("recommendationScore",
                        new Document("$multiply", Arrays.asList(
                                "$bayesianScore",
                                new Document("$log", Arrays.asList(
                                        new Document("$add", Arrays.asList("$buyersCount", 1)),
                                        10
                                ))
                        ))
                )),
                Aggregates.sort(Sorts.descending("recommendationScore")),
                Aggregates.limit(1),
                Aggregates.project(CONTENT_DIGEST)
        );

        JSONArray suggestions = new JSONArray();
        documentMongoCollection.aggregate(pipeline).forEach((Consumer<? super Document>) document -> suggestions.put(Utility.convertDigest(document, false)));

        return suggestions;
    }

    public int notChunkedCount() {
        return Optional.ofNullable(
                documentMongoCollection.aggregate(List.of(
                        unwind("$sessions"),
                        Aggregates.match(
                                Filters.or(
                                        Filters.exists("sessions.chunk_at", false),
                                        Filters.eq("sessions.chunk_at", false)
                                )
                        ),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }

    public int countIndividualRegistrationsLastMonth() {
        return Optional.ofNullable(
                documentMongoCollection.aggregate(List.of(
                        unwind("$users"),
                        Aggregates.match(Filters.gte("users.register_at", System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30)),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }

    @Override
    public void cleanRemove(Document doc) {

        if (doc.containsKey("img"))
            FileUtils.removeFile(doc.getString("img"), FOLDER);

        if (doc.containsKey("sessions")) {

            List<Document> sessions = doc.getList("sessions", Document.class);
            for (Document session : sessions)
                removeSession(session);

        }

        deleteOne(doc.getObjectId("_id"));
    }

    public void removeSession(Document session) {

        if (session.containsKey("attaches")) {
            List<String> attaches = session.getList("attaches", String.class);
            for (String attach : attaches)
                FileUtils.removeFile(attach, FOLDER);
        }

        if (session.containsKey("video") && !(Boolean) session.getOrDefault("external_link", false))
            FileUtils.removeFile(session.getString("video"), FOLDER);
    }
}
