package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.question.QuestionReportDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;

public class QuestionReportRepository extends Common {

    public QuestionReportRepository() {
        init();
    }

    @Override
    void init() {
        table = "question_report";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public List<QuestionReportDto> getList() {
        List<Bson> filters = new ArrayList<>();
        filters.add(gt("unseen_reports_count", 0));

        List<QuestionReportDto> tags = new ArrayList<>();
        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    skip(0),
                    limit(5),
                    project(fields(
                            include("label"),
                            computed("reportsCount","$reports_count"),
                            computed("question","$question_code"),
                            computed("unseenReportsCount",  "$unseen_reports_count")
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    tags.add(
                            objectMapper.readValue(document.toJson(), QuestionReportDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        return tags;
    }
}
