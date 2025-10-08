package irysc.gachesefid.entity.advisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.entity.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdviceTagReportEntity extends MongoEntity {
    private Integer priority;
    private TeachReportTagMode mode;
    private Boolean visibility;
    private String label;
    @JsonProperty("deleted_at")
    private Long deletedAt;
}
