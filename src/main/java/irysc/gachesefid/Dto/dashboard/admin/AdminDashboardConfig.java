package irysc.gachesefid.Dto.dashboard.admin;

import irysc.gachesefid.Dto.dashboard.ConfigDto;
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
public class AdminDashboardConfig extends ConfigDto {
    @Builder.Default
    @NotNull
    private Boolean showLastSettleRequest = true;
    @Builder.Default
    @NotNull
    private Boolean showIncomingRequestsForAdvice = true;
    @Builder.Default
    @NotNull
    private Boolean showIncomingRequestsForTeach = true;
    @Builder.Default
    @NotNull
    private Boolean showLastUserReports = true;
    @Builder.Default
    @NotNull
    private Boolean showMeetings = true;
    @Builder.Default
    @NotNull
    private Boolean showTopAdvisors = true;
    @Builder.Default
    @NotNull
    private Boolean showTopTeachers = true;
    @Builder.Default
    @NotNull
    private Boolean showTopLastWeekBestSellerContents = true;
}
