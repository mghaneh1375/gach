package irysc.gachesefid.Dto.DBMeta.Advisor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdvisorMeeting {
    private ObjectId advisorId; // advisor_id
    private ObjectId studentId; // student_id
    private Long createdAt; // created_at
    private Integer roomId; // room_id
    private String url;
    private Integer advisorSkyId; // advisor_sky_id
    private Integer studentSkyRoomId; // student_sky_id
}
