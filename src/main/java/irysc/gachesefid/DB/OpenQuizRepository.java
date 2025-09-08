package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import irysc.gachesefid.Dto.Report.BuyReport.BuyerInfoDto;
import irysc.gachesefid.Dto.Report.BuyReport.OpenQuizBuyerInfoDto;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;


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
                        Aggregates.match(Filters.gte("students.register_at", System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30)),
                        Aggregates.count()
                )).first()
        ).orElse(new Document("count", 0)).getInteger("count", 0);
    }

    public List<OpenQuizBuyerInfoDto> individualRegistrationsLastMonth() {
        List<OpenQuizBuyerInfoDto> registrations = new ArrayList<>();
        try {
            long last30DaysAgo = System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30;
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(gte("students.register_at", last30DaysAgo)),
                    project(
                            new BasicDBObject("title", 1)
                                    .append("students",
                                            new BasicDBObject("$filter",
                                                    new Document("input", "$students")
                                                            .append("as", "user")
                                                            .append("cond",
                                                                    new Document("$gte", List.of(
                                                                            "$$user.register_at", last30DaysAgo
                                                                    ))
                                                            )
                                            )
                                    )
                    ),
                    unwind("$students"),
                    lookup("user", "students._id", "_id", "userInfo"),
                    unwind("$userInfo"),
                    project(fields(
                            include("title"),
                            computed("refId", "$_id"),
                            computed("registeredAt", "$users.register_at"),
                            computed("userId", "$users._id"),
                            computed("firstname", "$userInfo.first_name"),
                            computed("lastname", "$userInfo.last_name"),
                            computed("nid", "$userInfo.NID"),
                            computed("phone", "$userInfo.phone")
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    registrations.add(
                            objectMapper.readValue(document.toJson(), OpenQuizBuyerInfoDto.class)
                    );
                } catch (JsonProcessingException ignore) {}
            });
        } catch (Exception ignore) {}

        return registrations.stream().sorted(
                Comparator.comparing(
                        BuyerInfoDto::getRegisteredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }
}
