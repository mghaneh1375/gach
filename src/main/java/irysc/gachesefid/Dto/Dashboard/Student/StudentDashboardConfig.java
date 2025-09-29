package irysc.gachesefid.Dto.Dashboard.Student;

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
public class StudentDashboardConfig extends ConfigDto {
    @Builder.Default
    @NotNull
    private Boolean showCurrentKarbargs = true;
    @Builder.Default
    @NotNull
    private Boolean showRequestsStatusForAdvice = true;
    @Builder.Default
    @NotNull
    private Boolean showRequestsStatusForTeach = true;
    @Builder.Default
    @NotNull
    private Boolean showMeeting = true;
    @Builder.Default
    @NotNull
    private Boolean showSuggestionForContent = true;
    @Builder.Default
    @NotNull
    private Boolean showSuggestionForQuiz = true;
    @Builder.Default
    @NotNull
    private Boolean showFutureQuiz = true;
    @Builder.Default
    @NotNull
    private Boolean showMyAdvisor = true;
}
