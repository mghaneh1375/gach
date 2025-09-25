package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.MongoNumberLongDeserializer;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdviceRequestDto {
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonDeserialize(using = MongoNumberLongDeserializer.class)
    private Long requestAt;
    private UserDigest student;
    private AdvicePlanDigest planDigest;
}
