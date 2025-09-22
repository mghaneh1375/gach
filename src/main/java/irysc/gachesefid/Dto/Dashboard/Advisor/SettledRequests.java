package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettledRequests {
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String status;
}
