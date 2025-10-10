package irysc.gachesefid.Dto.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdvisorGeneralInfoDto extends AdvisorDigestInfoDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String adviceBio;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String adviceVideoLink;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalStudentsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer rateCount;
}
