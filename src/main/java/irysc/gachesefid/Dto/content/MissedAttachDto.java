package irysc.gachesefid.Dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissedAttachDto {
    private String contentTitle;
    private String sessionTitle;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isChunked;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = LongDateSerialization.class)
    private Long uploadedVideoAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> missedAttaches;

}
