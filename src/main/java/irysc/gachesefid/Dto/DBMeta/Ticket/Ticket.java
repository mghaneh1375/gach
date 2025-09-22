package irysc.gachesefid.Dto.DBMeta.Ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    private String title;
    private Long createdAt; // created_at
    private Boolean isForTeacher; // is_for_teacher
    private Boolean startByAdmin; // start_by_admin
    private List<Chat> chats;
    private String status; // init,
    private String priority; // avg,
    private String section; // advisor, quiz
    private ObjectId userId; // user_id
    private ObjectId advisorId; // optional - advisor_id
    private ObjectId refId; // optional - ref_id
    private String additional; // optional - additional
}
