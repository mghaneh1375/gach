package irysc.gachesefid.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardStatsDto {
    public Integer pendingChunks;
    public Integer pendingTickets;
    public Integer pendingUpgradeLevelRequests;
    public Integer pendingSettleRequests;
    public Integer pendingRequestForAdvisorAnswer;
    public Integer pendingRequestForStudentPay;
    public Integer lastMonthKarbargs;
    public Integer lastMonthMeetings;
    public Integer lastMonthSettled;
    public Integer lastMonthOpenQuizRegistry;
    public Integer lastMonthCustomQuizRegistry;
    public Integer lastMonthContentBuyCount;
    public Integer lastMonthTutorialCount;
    public Integer lastMonthTeachReportsCount;
}
