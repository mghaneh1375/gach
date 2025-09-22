package irysc.gachesefid.DB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCursor;
import irysc.gachesefid.Dto.Dashboard.Advisor.MeetingDto;
import irysc.gachesefid.Dto.Report.BuyReport.BuyerInfoDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static irysc.gachesefid.Main.GachesefidApplication.objectMapper;
import static irysc.gachesefid.Utility.StaticValues.ONE_HOUR_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.SKY_ROOT_SESSION_DURATION;

public class AdvisorMeetingRepository extends Common {

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
        long skyRoomMeetingLengthMs = ONE_HOUR_MIL_SEC * (SKY_ROOT_SESSION_DURATION / 60);

        try {
            MongoCursor<Document> iterator = documentMongoCollection.aggregate(
                    List.of(
                            match(and(
                                    eq("advisor_id", advisorId),
                                    lte("created_at", curr),
                                    gte("created_at", curr - skyRoomMeetingLengthMs)
                            )),
                            lookup("user", "user_id", "_id", "userInfo"),
                            project(fields(
                                    include("url"),
                                    computed("createdAt", "$created_at"),
                                    computed("student.id", "$user_id"),
                                    computed("student.firstname", "$userInfo.first_name"),
                                    computed("student.lastname", "$userInfo.last_name"),
                                    computed("student.nid", "$userInfo.NID"),
                                    computed("student.phone", "$userInfo.phone"),
                                    computed("student.mail", "$userInfo.mail")
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

        meeting.forEach(meetingDto -> meetingDto.setEndAt(meetingDto.getCreatedAt() + skyRoomMeetingLengthMs));
        return meeting;
    }
}
