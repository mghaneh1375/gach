package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
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
    private UserDigest userDigest;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long joinAt;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long expireAt;
}
