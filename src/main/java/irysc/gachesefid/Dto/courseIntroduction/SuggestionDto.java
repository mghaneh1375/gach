package irysc.gachesefid.Dto.courseIntroduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuggestionDto {
    private String title;
    private String slug;
}
