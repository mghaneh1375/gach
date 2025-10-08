package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdviceRequestDto;
import irysc.gachesefid.Dto.Report.BuyReport.AdviceBuyerInfoDto;
import irysc.gachesefid.Dto.Report.BuyReport.BuyerInfoDto;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.advisorRequestsRepository;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;

public class AdvisorRequestsRepository extends Common {

    public AdvisorRequestsRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("advisor_requests");
    }

    // active_at: منظور از این فیلد اعتبار تا - فعال تا هستش

    public List<AdviceBuyerInfoDto> registrations(Long from, Long to) {
        List<AdviceBuyerInfoDto> registrations = new ArrayList<>();
        List<Bson> filters = new ArrayList<>();
        filters.add(eq("answer", "accept"));

        if(from != null)
            filters.add(gte("created_at", from));
        if(to != null)
            filters.add(lte("created_at", to));

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(filters)),
                            project(
                                    new BasicDBObject("title", 1)
                                            .append("created_at", 1)
                                            .append("user_id", 1)
                                            .append("advisor_id", 1)
                                            .append("price", 1)
                            ),
                            lookup("user", "user_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            lookup("user", "advisor_id", "_id", "advisorInfo"),
                            unwind("$advisorInfo"),
                            project(fields(
                                    include("title"),
                                    computed("refId", "$_id"),
                                    computed("paid", "$price"),
                                    computed("registeredAt", "$created_at"),
                                    computed("user.id", "$user_id"),
                                    computed("user.firstname", "$userInfo.first_name"),
                                    computed("user.lastname", "$userInfo.last_name"),
                                    computed("user.nid", "$userInfo.NID"),
                                    computed("user.phone", "$userInfo.phone"),
                                    computed("user.mail", "$userInfo.mail"),
                                    computed("advisor.id", "$advisor_id"),
                                    computed("advisor.firstname", "$advisorInfo.first_name"),
                                    computed("advisor.lastname", "$advisorInfo.last_name"),
                                    computed("advisorName", new Document("$concat", List.of(
                                            "$advisorInfo.first_name", " ", "$advisorInfo.last_name"
                                    )))
                            ))
                    )
            ).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    registrations.add(
                            objectMapper.readValue(document.toJson(), AdviceBuyerInfoDto.class)
                    );
                } catch (JsonProcessingException ignore) {}
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

    public List<AdviceRequestDto> pendingRequest() {
        List<AdviceRequestDto> requests = new ArrayList<>();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(new ArrayList<>() {{
                                add(eq("answer", "pending"));
                            }})),
                            project(
                                    new BasicDBObject("title", 1)
                                            .append("created_at", 1)
                                            .append("user_id", 1)
                                            .append("advisor_id", 1)
                                            .append("price", 1)
                            ),
                            lookup("user", "user_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            lookup("user", "advisor_id", "_id", "advisorInfo"),
                            unwind("advisorInfo"),
                            project(fields(
                                    computed("planDigest.price", "$price"),
                                    computed("planDigest.title", "$title"),
                                    computed("requestAt", "$created_at"),
                                    computed("user.id", "$user_id"),
                                    computed("user.firstname", "$userInfo.first_name"),
                                    computed("user.lastname", "$userInfo.last_name"),
                                    computed("user.nid", "$userInfo.NID"),
                                    computed("user.phone", "$userInfo.phone"),
                                    computed("user.mail", "$userInfo.mail"),
                                    computed("advisor.id", "$advisor_id"),
                                    computed("advisor.firstname", "$advisorInfo.first_name"),
                                    computed("advisor.lastname", "$advisorInfo.last_name"),
                                    computed("advisor.pic", "$advisorInfo.pic")
                            ))
                    )
            ).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    requests.add(
                            objectMapper.readValue(document.toJson(), AdviceRequestDto.class)
                    );
                } catch (JsonProcessingException ignore) {}
            });
        }
        catch (Exception ignore) {}

        return requests.stream().sorted(
                Comparator.comparing(
                        AdviceRequestDto::getRequestAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }

    public List<AdviceRequestDto> myPendingRequest(ObjectId advisorId) {
        List<AdviceRequestDto> requests = new ArrayList<>();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(new ArrayList<>() {{
                                add(eq("advisor_id", advisorId));
                                add(eq("answer", "pending"));
                            }})),
                            project(
                                    new BasicDBObject("title", 1)
                                            .append("created_at", 1)
                                            .append("user_id", 1)
                                            .append("price", 1)
                            ),
                            lookup("user", "user_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            project(fields(
                                    computed("planDigest.price", "$price"),
                                    computed("planDigest.title", "$title"),
                                    computed("requestAt", "$created_at"),
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
                    requests.add(
                            objectMapper.readValue(document.toJson(), AdviceRequestDto.class)
                    );
                } catch (JsonProcessingException ignore) {}
            });
        }
        catch (Exception ignore) {}

        return requests.stream().sorted(
                Comparator.comparing(
                        AdviceRequestDto::getRequestAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }

    public List<AdviceRequestDto> myLastWeekRequests(ObjectId studentId) {
        List<AdviceRequestDto> requests = new ArrayList<>();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(new ArrayList<>() {{
                                add(eq("user_id", studentId));
                                add(exists("paid_at", false));
                                add(gte("created_at", System.currentTimeMillis() - StaticValues.ONE_WEEK_MIL_SEC));
                            }})),
                            project(
                                    new BasicDBObject("title", 1)
                                            .append("created_at", 1)
                                            .append("user_id", 1)
                                            .append("price", 1)
                                            .append("advisor_id", 1)
                                            .append("answer_at", 1)
                                            .append("status", 1)
                            ),
                            lookup("user", "advisor_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            project(fields(
                                    include("status"),
                                    computed("planDigest.price", "$price"),
                                    computed("planDigest.title", "$title"),
                                    computed("requestAt", "$created_at"),
                                    computed("answerAt", "$answer_at"),
                                    computed("user.id", "$user_id"),
                                    computed("user.firstname", "$userInfo.first_name"),
                                    computed("user.lastname", "$userInfo.last_name"),
                                    computed("user.pic", "$userInfo.pic")
                            ))
                    )
            ).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    requests.add(
                            objectMapper.readValue(document.toJson(), AdviceRequestDto.class)
                    );
                } catch (JsonProcessingException ignore) {}
            });
        }
        catch (Exception ignore) {}

        return requests.stream().sorted(
                Comparator.comparing(
                        AdviceRequestDto::getRequestAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }

    public List<UserDigest> topAdvisors() {

        advisorRequestsRepository.find(
                and(
                        gte("request_at", System.currentTimeMillis() - StaticValues.ONE_MONTH_MIL_SEC),


                )
        );

    }
}
