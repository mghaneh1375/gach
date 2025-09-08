package irysc.gachesefid.Dto.Dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdvisorDashboardStatsDto {
    private Integer studentsCountForAdvice;
    private Integer studentsCountForTeach;
    private Integer pendingExamsForPay;
    private Integer lastMonthCreatedExams;
    private Integer lastMonthMeetings;
    private Integer lastMonthkarbargs;
}
