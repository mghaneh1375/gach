package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Dashboard.Advisor.ReportProblemDigestDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;
import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;

public class TeachReportRepository extends Common {

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("teach_report");
    }

    public TeachReportRepository() {
        init();
    }

    public List<ReportProblemDigestDto> getLastReports() {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("seen", false));
            add(gte("created_at", System.currentTimeMillis() - ONE_MONTH_MIL_SEC));
        }};
        List<ReportProblemDigestDto> reports = new ArrayList<>();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    skip(0),
                    limit(5),
                    lookup("user", "student_id", "_id", "userInfo"),
                    unwind("$userInfo"),
                    lookup("user", "teacher_id", "_id", "teacherInfo"),
                    unwind("$teacherInfo"),
                    lookup("teach_schedule", "schedule_id", "_id", "scheduleInfo"),
                    unwind("$scheduleInfo"),
                    lookup("teach_tag_report", "tag_ids", "_id", "tagInfo"),
                    project(fields(
                            computed("id", "$_id"),
                            computed("createdAt", "$created_at"),
                            computed("section", "teach"),
                            include("desc"),
                            computed("sendFrom", "$send_from"),
                            computed("reporter.id",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "student")))
                                                    .append("then", "$userInfo._id")
                                                    .append("else", "$teacherInfo._id")
                                    )
                            ),
                            computed("reporter.firstname",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "student")))
                                                    .append("then", "$userInfo.first_name")
                                                    .append("else", "$teacherInfo.first_name")
                                    )
                            ),
                            computed("reporter.lastname",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "student")))
                                                    .append("then", "$userInfo.last_name")
                                                    .append("else", "$teacherInfo.last_name")
                                    )
                            ),
                            computed("reportAbout.id",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "teacher")))
                                                    .append("then", "$userInfo._id")
                                                    .append("else", "$teacherInfo._id")
                                    )
                            ),
                            computed("reportAbout.firstname",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "teacher")))
                                                    .append("then", "$userInfo.first_name")
                                                    .append("else", "$teacherInfo.first_name")
                                    )
                            ),
                            computed("reportAbout.lastname",
                                    new Document("$cond",
                                            new Document("if", new Document("$eq", Arrays.asList("$send_from", "teacher")))
                                                    .append("then", "$userInfo.last_name")
                                                    .append("else", "$teacherInfo.last_name")
                                    )
                            ),
                            computed("ref", "$scheduleInfo.title"),
                            computed("tags",
                                    new Document("$cond",
                                            new Document("if",
                                                    new Document("$and", Arrays.asList(
                                                            new Document("$exists", Arrays.asList("$tag_ids", true)),
                                                            new Document("$ne", Arrays.asList("$tag_ids", null))
                                                    ))
                                            )
                                                    .append("then",
                                                            new Document("$map",
                                                                    new Document("input", "$tagInfo")
                                                                            .append("as", "tag")
                                                                            .append("in", "$$tag.label")
                                                            )
                                                    )
                                                    .append("else", List.of())
                                    )
                            )
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    reports.add(
                            objectMapper.readValue(document.toJson(), ReportProblemDigestDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        return reports;
    }
}
