package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeetingDto {
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long endAt;
    private String url;
    private UserDigest student;
}
