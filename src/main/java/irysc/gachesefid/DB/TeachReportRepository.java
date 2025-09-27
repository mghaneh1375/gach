package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Dashboard.Advisor.ReportAboutMeDigestDto;
import irysc.gachesefid.Dto.Dashboard.Advisor.TeachRequestDigestDto;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Projections.computed;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;

public class TeachReportRepository extends Common {

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("teach_report");
    }

    public TeachReportRepository() {
        init();
    }

    public List<ReportAboutMeDigestDto> getLastReportsAboutMe(ObjectId advisorId) {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("teacher_id", advisorId));
            add(eq("send_from", "student"));
            add(gte("created_at", System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30));
        }};
        List<ReportAboutMeDigestDto> reports = new ArrayList<>();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    project(
                            new BasicDBObject("created_at", 1)
                                    .append("tag_ids", 1)
                                    .append("desc", 1)
                                    .append("schedule_id", 1)
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
                            computed("newRequesters.firstname", "$userInfo.first_name"),
                            computed("newRequesters.lastname", "$userInfo.last_name"),
                            computed("studentsCount", new Document("$size", "$students"))
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    reports.add(
                            objectMapper.readValue(document.toJson(), ReportAboutMeDigestDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        }
        catch (Exception ignore) {}

        return reports;
    }
}
