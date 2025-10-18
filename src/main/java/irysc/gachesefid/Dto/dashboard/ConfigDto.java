package irysc.gachesefid.Dto.dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.minidev.json.annotate.JsonIgnore;
import org.bson.types.ObjectId;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Validated
public class ConfigDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonIgnore
    private ObjectId _id;
    @JsonIgnore
    private ObjectId userId;
    @Builder.Default
    @NotNull
    private Boolean showLastTickets = true;
    @Builder.Default
    @NotNull
    private Boolean showLastNotifs = true;
    @Builder.Default
    @NotNull
    private Boolean showDashboard = true;
}
