package irysc.gachesefid.entity.teach;


import com.fasterxml.jackson.annotation.JsonProperty;
import irysc.gachesefid.entity.MongoEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TeachReportEntity extends MongoEntity {
    @JsonProperty("student_id")
    private ObjectId studentId;

    @JsonProperty("teacher_id")
    private ObjectId teacherId;

    @JsonProperty("schedule_id")
    private ObjectId scheduleId;

    @JsonProperty("send_from")
    private String sendFrom; // student, teacher

    private Boolean seen;
    private String desc; // optional

    @JsonProperty("tag_ids")
    private List<ObjectId> tagIds;
}
