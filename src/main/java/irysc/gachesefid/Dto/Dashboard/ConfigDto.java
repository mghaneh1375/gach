package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConfigDto {
    private ObjectId _id;
    private ObjectId userId;
    @Builder.Default
    @NotNull
    private Boolean showLastTickets = true;
    @Builder.Default
    @NotNull
    private Boolean showDashboard = true;
}
