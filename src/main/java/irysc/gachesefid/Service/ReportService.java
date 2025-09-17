package irysc.gachesefid.Service;

import irysc.gachesefid.Dto.BuyersReportFilterDto;
import irysc.gachesefid.Dto.Report.BuyReport.ContentBuyerInfoDto;
import irysc.gachesefid.Dto.Report.BuyReport.LastBuyersDto;
import irysc.gachesefid.Dto.Report.BuyReport.OpenQuizBuyerInfoDto;
import irysc.gachesefid.Dto.Report.BuyReport.RegularQuizBuyerInfoDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Enums.BuySection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

import static irysc.gachesefid.Main.GachesefidApplication.*;

@Service
public class ReportService {

    public ResponseEntity<ResponseDto<LastBuyersDto>> buyersReport(@NotNull BuyersReportFilterDto filterDto) {
        return new ResponseEntity<>(
                ResponseDto
                        .builder(LastBuyersDto.class)
                        .status("ok")
                        .data(
                                LastBuyersDto
                                        .builder()
                                        .contentBuyersInfoDto(
                                                filterDto.getSection() == null
                                                        || filterDto.getSection().equals(BuySection.CONTENT)
                                                        || filterDto.getSection().equals(BuySection.ALL)
                                                        ? contentRepository.individualRegistrations(filterDto.getFrom(), filterDto.getTo(), "users", ContentBuyerInfoDto.class)
                                                        : null
                                        )
                                        .openQuizBuyersInfoDto(
                                                filterDto.getSection() == null
                                                        || filterDto.getSection().equals(BuySection.OPEN_QUIZ_EXAM)
                                                        || filterDto.getSection().equals(BuySection.ALL)
                                                        ? openQuizRepository.individualRegistrations(filterDto.getFrom(), filterDto.getTo(), "students", OpenQuizBuyerInfoDto.class)
                                                        : null
                                        )
                                        .regularQuizBuyersInfoDto(
                                                filterDto.getSection() == null
                                                        || filterDto.getSection().equals(BuySection.IRYSC_EXAM)
                                                        || filterDto.getSection().equals(BuySection.ALL)
                                                        ? iryscQuizRepository.individualRegistrations(filterDto.getFrom(), filterDto.getTo(), "students", RegularQuizBuyerInfoDto.class)
                                                        : null
                                        )
                                        .build()
                        )
                        .build(),
                HttpStatus.OK);
    }

//    public ResponseEntity<ResponseDto<List<AdvisorRequestDto>>> adviceRequests() {
////        advisorRequestsRepository.findWithJoinUser("")
//    }

}
