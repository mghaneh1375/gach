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
    @NotNull
    private Long from;
    @NotNull
    private Long to;
}
