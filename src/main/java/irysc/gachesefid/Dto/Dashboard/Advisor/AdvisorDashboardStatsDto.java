package irysc.gachesefid.Dto.Dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdvisorDashboardStatsDto {
    private Integer studentsCountForAdvice;
    private Integer studentsCountForTeach;
    private Integer pendingExamsForPay;
    private Integer lastMonthCreatedExams;
    private Integer lastMonthMeetings;
    private Integer lastMonthkarbargs;
    private List<String> futureMeetings;
    private List<TicketDigestDto> unSeenTickets;
}
