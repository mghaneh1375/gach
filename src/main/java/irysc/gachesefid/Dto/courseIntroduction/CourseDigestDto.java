package irysc.gachesefid.Dto.courseIntroduction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseDigestDto {
    private String title;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String digest;
    private Integer packages;
    private Integer quizzes;
    private Integer advisors;
    private Integer questions;
    private List<SuggestionDto> suggestions;
    private Integer sessions;
}
