package irysc.gachesefid.Dto.dashboard.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import irysc.gachesefid.Dto.dashboard.Advisor.AdviceRequestDto;
import irysc.gachesefid.Dto.dashboard.Advisor.ReportProblemDigestDto;
import irysc.gachesefid.Dto.dashboard.PublicDashboardStatsDto;
import irysc.gachesefid.Dto.dashboard.Student.SuggestedContentDto;
import irysc.gachesefid.Dto.advice.TopAdvisors;
import irysc.gachesefid.Dto.question.QuestionReportDto;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class AdminDashboardStatsDto extends PublicDashboardStatsDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    public Integer lastMonthAdviceReportsCount;
    public Integer pendingComments;
    public Integer activeTeachers;
    public Integer activeAdvisors;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AdviceRequestDto> adviceRequests;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ReportProblemDigestDto> problemReports;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionReportDto> questionReports;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TopAdvisors> topAdvisors;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SuggestedContentDto> topLastWeekBestSeller;

}
