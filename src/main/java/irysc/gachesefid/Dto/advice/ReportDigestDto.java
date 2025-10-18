package irysc.gachesefid.Dto.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import irysc.gachesefid.Dto.CommonDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReportDigestDto extends CommonDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String desc;
}
