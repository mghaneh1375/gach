package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import irysc.gachesefid.Dto.Dashboard.Advisor.ScheduleDigest;
import irysc.gachesefid.Main.GachesefidApplication;
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
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;
import static irysc.gachesefid.Service.Advice.ScheduleUtils.getFormattedDate;
import static irysc.gachesefid.Utility.StaticValues.ONE_WEEK_MIL_SEC;

public class ScheduleRepository extends Common {

    public ScheduleRepository() {
        init();
    }

    @Override
    void init() {
        table = "schedule";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public List<ScheduleDigest> getInProgressSchedulesDigest(ObjectId advisorId) {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("advisors", advisorId));
            add(gte("week_start_at_int", getFormattedDate(System.currentTimeMillis() - ONE_WEEK_MIL_SEC)));
            add(or(
                    exists("ready_for_use", false),
                    eq("ready_for_use", false)
            ));
        }};
        return findSchedules(filters);
    }

    public List<ScheduleDigest> getInProgressSchedulesDigestForStudent(ObjectId studentId) {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", studentId));
            add(gte("week_start_at_int", getFormattedDate(System.currentTimeMillis() - ONE_WEEK_MIL_SEC)));
            add(exists("ready_for_use", true));
            add(eq("ready_for_use", true));
        }};
        List<ScheduleDigest> schedules = new ArrayList<>();
        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    project(
                            new BasicDBObject("week_start_at", 1)
                                    .append("advisors", 1)
                    ),
                    unwind("$advisors"),
                    lookup("user", "advisors", "_id", "advisorInfo"),
                    unwind("$advisorInfo"),
                    group(
                            "$_id",
                            Accumulators.first("weekStartAt", "$week_start_at"),
                            Accumulators.push("advisors", new Document("id", "$advisorInfo._id")
                                    .append("firstname", "$advisorInfo.first_name")
                                    .append("lastname", "$advisorInfo.last_name"))
                    ),
                    project(fields(
                            include("weekStartAt", "advisors"),
                            computed("id", "$_id")
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    schedules.add(
                            objectMapper.readValue(document.toJson(), ScheduleDigest.class)
                    );
                } catch (JsonProcessingException ignore) {}
            });
        }
        catch (Exception ignore) {}

        return schedules.stream().sorted(
                Comparator.comparing(
                        ScheduleDigest::getWeekStartAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }

    public List<ScheduleDigest> getDoneSchedulesDigest(ObjectId advisorId) {
        ArrayList<Bson> filters = new ArrayList<>() {{
            add(eq("advisors", advisorId));
            add(gte("week_start_at_int", getFormattedDate(System.currentTimeMillis() - ONE_WEEK_MIL_SEC)));
            add(exists("ready_for_evaluate"));
            add(eq("ready_for_evaluate", true));
        }};
        return findSchedules(filters);
    }

    private List<ScheduleDigest> findSchedules(ArrayList<Bson> filters) {
        List<ScheduleDigest> schedules = new ArrayList<>();
        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(List.of(
                    match(and(filters)),
                    project(
                            new BasicDBObject("week_start_at", 1)
                                    .append("user_id", 1)
                    ),
                    lookup("user", "user_id", "_id", "userInfo"),
                    unwind("$userInfo"),
                    project(fields(
                            computed("weekStartAt", "$week_start_at"),
                            computed("id", "$_id"),
                            computed("user.id", "$user_id"),
                            computed("user.firstname", "$userInfo.first_name"),
                            computed("user.lastname", "$userInfo.last_name")
                    ))
            )).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    schedules.add(
                            objectMapper.readValue(document.toJson(), ScheduleDigest.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        }
        catch (Exception ignore) {}

        return schedules.stream().sorted(
                Comparator.comparing(
                        ScheduleDigest::getWeekStartAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        ).collect(Collectors.toList());
    }
}
