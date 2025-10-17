package irysc.gachesefid.Dto.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.TeachReportTagModeDeserializer;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Models.TeachReportTagMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdviceTagReportDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String label;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer priority;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean visibility;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = TeachReportTagModeDeserializer.class)
    private TeachReportTagMode mode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer unseenReportsCount;
}
