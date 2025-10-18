package irysc.gachesefid.Dto.dashboard.Advisor;

import com.fasterxml.jackson.annotation.JsonInclude;
import irysc.gachesefid.Dto.dashboard.PublicDashboardStatsDto;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class AdvisorDashboardStatsDto extends PublicDashboardStatsDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer studentsCountForAdvice;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer studentsCountForTeach;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer pendingExamsForPay;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer lastMonthCreatedExams;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer lastMonthSettled;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer pendingSettled;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SettledRequests lastSettledRequest;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AdviceRequestDto> adviceRequests;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CommentsAboutMeDigest> lastComments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ScheduleDigest> inProgressSchedules;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ScheduleDigest> filledSchedules;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TeachRequestDigestDto> teachRequests;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MyCurrStudent> myCurrStudents;
}
