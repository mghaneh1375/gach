package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Variable;
import irysc.gachesefid.Dto.advice.AdviceTagReportDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;

public class AdviceTagReportRepository extends Common {

    @Override
    void init() {
        table = "advice_tag_report";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public AdviceTagReportRepository() {
        init();
    }

    public List<AdviceTagReportDto> getList(String mode, boolean isAdmin) {

        List<Bson> filters = new ArrayList<>();
        filters.add(exists("deleted_at", false));
        if (!isAdmin)
            filters.add(eq("visibility", true));

        if (mode != null)
            filters.add(eq("mode", mode));

        List<AdviceTagReportDto> tags = new ArrayList<>();
        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    isAdmin
                            ? lookup(
                            "advice_report",
                            List.of(
                                    new Variable<>("tagId", "$_id")
                            ),
                            List.of(
                                    match(
                                            expr(new Document("$in", Arrays.asList("$$tagId", "$tag_ids")))
                                    ),
                                    group(
                                            null,
                                            sum("unseenCount",
                                                    new Document("$cond",
                                                            Arrays.asList(
                                                                    new Document("$ifNull", Arrays.asList("$unseen", false)),
                                                                    1, 0
                                                            )
                                                    )
                                            )
                                    )
                            ),
                            "reportStats"
                    ) : skip(0),
                    project(
                            isAdmin
                                    ? fields(
                                    include("label", "visibility", "priority", "mode"),
                                    computed("id", "$_id"),
                                    computed("createdAt", "$created_at"),
                                    computed("unseenReportsCount", new Document("$size", "$reportStats.unseenCount"))
                            ) : fields(
                                    include("label"),
                                    computed("id", "$_id"),
                                    computed("createdAt", "$created_at")
                            )
                    )
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    tags.add(

                            objectMapper.readValue(document.toJson(), AdviceTagReportDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        return tags;
    }
}
