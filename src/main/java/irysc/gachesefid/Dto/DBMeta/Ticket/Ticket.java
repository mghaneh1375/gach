package irysc.gachesefid.Dto.DBMeta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    private String title;
    private Long createdAt; // created_at
    private Boolean isForTeacher; // is_for_teacher
    private ObjectId advisorId; // optional - advisor_id
    private ObjectId refId; // optional - ref_id
}
