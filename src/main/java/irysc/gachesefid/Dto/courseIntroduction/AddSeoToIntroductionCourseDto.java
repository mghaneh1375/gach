package irysc.gachesefid.Dto.courseIntroduction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddSeoToIntroductionCourseDto {
    @NotBlank
    @Size(max = 100)
    private String key;
    @NotBlank
    @Size(max = 5000)
    private String value;
}
