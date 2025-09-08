package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Data
@Builder
public class DashboardStatsDto {
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
    private String tutorialsSuggestion;
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
}
