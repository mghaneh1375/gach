package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import irysc.gachesefid.Dto.dashboard.Advisor.TeachRequestDigestDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;
import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;

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
                        Aggregates.match(Filters.gte("students.created_at", System.currentTimeMillis() - ONE_MONTH_MIL_SEC)),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }

    public List<TeachRequestDigestDto> getTeachPendingRequests(ObjectId advisorId) {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", advisorId));
            add(exists("requests.0", true));
            add(eq("requests.status", "pending"));
        }};
        List<TeachRequestDigestDto> teachRequests = new ArrayList<>();
        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    project(
                            new BasicDBObject("title", 1)
                                    .append("teach_mode", 1)
                                    .append("price", 1)
                                    .append("length", 1)
                                    .append("min_cap", 1)
                                    .append("max_cap", 1)
                                    .append("start_at", 1)
                                    .append("start_date", 1)
                                    .append("end_date", 1)
                                    .append("end_registration", 1)
                                    .append("requests", new BasicDBObject("$filter",
                                            new Document("input", "$requests")
                                                    .append("as", "req")
                                                    .append("cond", new Document("$and", List.of(
                                                            new Document()
                                                                    .append("$eq", List.of("$$req.status", "pending"))
                                                    )))
                                    ))
                                    .append("students", 1)
                    ),
                    unwind("$requests"),
                    lookup("user", "requests._id", "_id", "userInfo"),
                    unwind("$userInfo"),
                    project(fields(
                            computed("id", "$_id"),
                            include("title"),
                            computed("teachMode", "$teach_mode"),
                            include("price"),
                            include("length"),
                            computed("minCap", "$min_cap"),
                            computed("maxCap", "$maxCap"),
                            computed("startAt", "$start_at"),
                            computed("startDate", "$start_date"),
                            computed("endDate", "$end_date"),
                            computed("endRegistration", "$end_registration"),
                            computed("newRequesters.id", "$userInfo._id"),
                            computed("newRequesters.createdAt", "$requests.created_at"),
                            computed("newRequesters.firstname", "$userInfo.first_name"),
                            computed("newRequesters.lastname", "$userInfo.last_name"),
                            computed("studentsCount", new Document("$size", "$students"))
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    teachRequests.add(
                            objectMapper.readValue(document.toJson(), TeachRequestDigestDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        }
        catch (Exception ignore) {}

        return teachRequests;
    }

}
