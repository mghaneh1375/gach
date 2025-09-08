package irysc.gachesefid.Dto.Report.RequestsReport;

import irysc.gachesefid.Enums.AdviceRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdviceRequestFilterDto {
    private Long from;
    private Long to;
    private AdviceRequestStatus status;
}
