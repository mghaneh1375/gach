package irysc.gachesefid.Dto.Dashboard.Advisor;

import irysc.gachesefid.Dto.Dashboard.NotifDigestDto;
import irysc.gachesefid.Dto.Dashboard.TicketDigestDto;
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
    private Integer lastMonthKarbargs;
    private List<MeetingDto> futureMeetings;
    private List<TicketDigestDto> unSeenTickets;
    private SettledRequests lastSettledRequest;
    private List<AdviceRequestDto> adviceRequests;
    private List<NotifDigestDto> lastNotifs;
}
