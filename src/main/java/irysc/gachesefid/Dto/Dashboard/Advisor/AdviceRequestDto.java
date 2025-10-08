package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest user;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest advisor;
    private AdvicePlanDigest planDigest;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = LongDateSerialization.class)
    private Long answerAt;
}
