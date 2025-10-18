package irysc.gachesefid.Dto.dashboard.Student;

import com.fasterxml.jackson.annotation.JsonInclude;
import irysc.gachesefid.Dto.dashboard.Advisor.AdviceRequestDto;
import irysc.gachesefid.Dto.dashboard.Advisor.ScheduleDigest;
import irysc.gachesefid.Dto.dashboard.PublicDashboardStatsDto;
import irysc.gachesefid.Dto.QuizDigestDto;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

import java.util.List;

@Data
@SuperBuilder
public class DashboardStatsDto extends PublicDashboardStatsDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object coin;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long passedQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer userOpenQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long activeQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer activeAdvisors;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer activeTeachers;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer tutorialsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SuggestedContentDto> tutorialsSuggestion;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer yourTutorialsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer registrableQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<JSONObject> registrableQuizzesSuggestion;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object gradeRank;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object rank;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double coinToMoneyExchange;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object money;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer schools;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer students;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer questions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MyAdvisorDigestDto> myAdvisors;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ScheduleDigest> currentSchedules;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuizDigestDto> futureQuizzes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AdviceRequestDto> adviceRequests;
}
