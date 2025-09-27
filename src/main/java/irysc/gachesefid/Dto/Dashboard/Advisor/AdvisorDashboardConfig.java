package irysc.gachesefid.Dto.Dashboard.Advisor;

import irysc.gachesefid.Dto.Dashboard.ConfigDto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Validated
public class AdvisorDashboardConfig extends ConfigDto {
    @Builder.Default
    @NotNull
    private Boolean showLastSettleRequest = true;
    @Builder.Default
    @NotNull
    private Boolean showInProgressKarbargs = true;
    @Builder.Default
    @NotNull
    private Boolean showFilledKarbargs = true;
    @Builder.Default
    @NotNull
    private Boolean showMyLastComments = true;
    @Builder.Default
    @NotNull
    private Boolean showIncomingRequestsForAdvice = true;
    @Builder.Default
    @NotNull
    private Boolean showIncomingRequestsForTeach = true;
    @Builder.Default
    @NotNull
    private Boolean showMeeting = true;
    @Builder.Default
    @NotNull
    private Boolean showLastUserReportsAboutMe = true;
}
