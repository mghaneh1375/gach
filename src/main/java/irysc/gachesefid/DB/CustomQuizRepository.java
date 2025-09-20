package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Report.BuyReport.BuyerInfoDto;
import irysc.gachesefid.Dto.Report.BuyReport.CustomQuizBuyerInfoDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Projections.computed;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;

public class CustomQuizRepository extends Common {

    public CustomQuizRepository() {
        init();
    }

    @Override
    void init() {
        table = "custom_quiz";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public List<CustomQuizBuyerInfoDto> registrations(Long from, Long to) {
        List<CustomQuizBuyerInfoDto> registrations = new ArrayList<>();
        List<Bson> filters = new ArrayList<>();
        filters.add(or(
                eq("status", "paid"),
                eq("status", "finished")
        ));
        if(from != null)
            filters.add(gte("created_at", from));
        if(to != null)
            filters.add(lte("created_at", to));

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(filters)),
                            project(
                                    new BasicDBObject("name", 1)
                                            .append("created_at", 1)
                                            .append("user_id", 1)
                                            .append("price", 1)
                            ),
                            lookup("user", "user_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            project(fields(
                                    computed("title", "$name"),
                                    computed("refId", "$_id"),
                                    computed("paid", "$price"),
                                    computed("registeredAt", "$created_at"),
                                    computed("user.id", "$user_id"),
                                    computed("user.firstname", "$userInfo.first_name"),
                                    computed("user.lastname", "$userInfo.last_name"),
                                    computed("user.nid", "$userInfo.NID"),
                                    computed("user.phone", "$userInfo.phone"),
                                    computed("user.mail", "$userInfo.mail")
                            ))
                    )
            ).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    registrations.add(
                            objectMapper.readValue(document.toJson(), CustomQuizBuyerInfoDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        }
        catch (Exception ignore) {}

        return registrations.stream().sorted(
                Comparator.comparing(
                        BuyerInfoDto::getRegisteredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }
}
