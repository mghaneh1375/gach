package irysc.gachesefid.Dto.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerialization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifDigestDto {
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String title;
    @JsonSerialize(using = ObjectIdSerialization.class)
    private ObjectId id;
}
