package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.JustDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCurrStudent {
    private UserDigest student;
    @JsonSerialize(using = JustDateSerialization.class)
    private Long startAt;
    @JsonSerialize(using = JustDateSerialization.class)
    private Long endAt;
}
