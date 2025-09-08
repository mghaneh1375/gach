package irysc.gachesefid.Dto;

import irysc.gachesefid.Enums.BuySection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class BuyersReportFilterDto {
    @NotNull
    private BuySection section;
//    private Long from;
//    private Long to;
}
