package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Dashboard.Advisor.MeetingDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;
import static irysc.gachesefid.Utility.StaticValues.ONE_HOUR_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.SKY_ROOT_SESSION_DURATION;

public class AdvisorMeetingRepository extends Common {

    private static final long SKY_ROOM_MEETING_LENGTH_MS = ONE_HOUR_MIL_SEC * (SKY_ROOT_SESSION_DURATION / 60);

    public AdvisorMeetingRepository() {
        init();
    }

    @Override
    void init() {
        table = "advisor_meeting";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public List<MeetingDto> fetchAdvisorCurrentMeetings(ObjectId advisorId) {
        List<MeetingDto> meeting = new ArrayList<>();
        long curr = System.currentTimeMillis();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(
                                    eq("advisor_id", advisorId),
                                    lte("created_at", curr),
                                    gte("created_at", curr - SKY_ROOM_MEETING_LENGTH_MS)
                            )),
                            lookup("user", "student_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            project(fields(
                                    include("url"),
                                    computed("createdAt", "$created_at"),
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
                    meeting.add(
                            objectMapper.readValue(document.toJson(), MeetingDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        meeting.forEach(meetingDto -> meetingDto.setEndAt(meetingDto.getCreatedAt() + SKY_ROOM_MEETING_LENGTH_MS));
        return meeting;
    }

    public List<MeetingDto> fetchStudentCurrentMeetings(ObjectId studentId) {
        List<MeetingDto> meeting = new ArrayList<>();
        long curr = System.currentTimeMillis();

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(
                                    eq("student_id", studentId),
                                    lte("created_at", curr),
                                    gte("created_at", curr - SKY_ROOM_MEETING_LENGTH_MS)
                            )),
                            lookup("user", "advisor_id", "_id", "userInfo"),
                            unwind("$userInfo"),
                            project(fields(
                                    include("url"),
                                    computed("createdAt", "$created_at"),
                                    computed("user.id", "$user_id"),
                                    computed("user.firstname", "$userInfo.first_name"),
                                    computed("user.lastname", "$userInfo.last_name"),
                                    computed("user.pic", "$userInfo.pic")
                            ))
                    )
            ).iterator();
            iterator.forEachRemaining(document -> {
                try {
                    meeting.add(
                            objectMapper.readValue(document.toJson(), MeetingDto.class)
                    );
                } catch (JsonProcessingException ignore) {
                    System.out.println(ignore.getMessage());
                }
            });
        } catch (Exception ignore) {
            System.out.println(ignore.getMessage());
        }

        meeting.forEach(meetingDto -> meetingDto.setEndAt(meetingDto.getCreatedAt() + SKY_ROOM_MEETING_LENGTH_MS));
        return meeting;
    }
}
