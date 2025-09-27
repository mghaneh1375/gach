package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAboutMeDigestDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private UserDigest reporter;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String title;
}
