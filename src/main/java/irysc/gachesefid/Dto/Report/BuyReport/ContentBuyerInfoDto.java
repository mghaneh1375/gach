package irysc.gachesefid.Dto.Report.BuyReport;

import irysc.gachesefid.Enums.BuySection;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ContentBuyerInfoDto extends BuyerInfoDto {

    @Builder.Default
    BuySection refSection = BuySection.CONTENT;
}
