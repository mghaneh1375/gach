package irysc.gachesefid.Dto.Dashboard.Student;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.JustDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAdvisorDigestDto {
    private UserDigest advisor;
    @JsonSerialize(using = JustDateSerialization.class)
    private Long startAt;
    @JsonSerialize(using = JustDateSerialization.class)
    private Long endAt;
    private Integer rate;
}
