package irysc.gachesefid.Dto.dashboard.Advisor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.UserDigest;
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
public class ScheduleDigest {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest user;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<UserDigest> advisors;
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String weekStartAt;
}
