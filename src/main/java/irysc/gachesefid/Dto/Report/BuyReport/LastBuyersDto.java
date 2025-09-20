package irysc.gachesefid.Dto.Report.BuyReport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LastBuyersDto {
    private HashMap<String, Integer> stats;
    private List<ContentBuyerInfoDto> contentBuyersInfoDto;
    private List<OpenQuizBuyerInfoDto> openQuizBuyersInfoDto;
    private List<RegularQuizBuyerInfoDto> regularQuizBuyersInfoDto;
    private List<CustomQuizBuyerInfoDto> customQuizBuyersInfoDto;
    private List<AdviceBuyerInfoDto> adviceBuyersInfoDto;
}
