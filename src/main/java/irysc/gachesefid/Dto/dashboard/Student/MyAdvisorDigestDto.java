package irysc.gachesefid.Dto.dashboard.Student;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.AgeSerializer;
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
    private Double rate;
    private Integer stdCount;
    @JsonSerialize(using = AgeSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long age;
}
