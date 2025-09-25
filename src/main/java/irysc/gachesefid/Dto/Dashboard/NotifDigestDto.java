package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifDigestDto {
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String title;
}
