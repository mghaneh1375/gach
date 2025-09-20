package irysc.gachesefid.Dto.Report.BuyReport;

import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Enums.BuySection;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdviceBuyerInfoDto extends BuyerInfoDto {
    @Builder.Default
    private BuySection refSection = BuySection.ADVISOR;
    private UserDigest advisor;
    private String advisorName;
}
