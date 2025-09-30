package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import irysc.gachesefid.Dto.Dashboard.Advisor.MeetingDto;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class PublicDashboardStatsDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer lastMonthMeetings;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer lastMonthKarbargs;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MeetingDto> currMeetings;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NotifDigestDto> lastNotifs;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TicketDigestDto> unSeenTickets;
}
