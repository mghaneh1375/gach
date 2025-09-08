package irysc.gachesefid.Service;

import irysc.gachesefid.DB.AdvisorRequestsRepository;
import irysc.gachesefid.Dto.BuyersReportFilterDto;
import irysc.gachesefid.Dto.Report.BuyReport.LastBuyersDto;
import irysc.gachesefid.Dto.Report.RequestsReport.AdvisorRequestDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Enums.BuySection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.*;

@Service
public class ReportService {

    public ResponseEntity<ResponseDto<LastBuyersDto>> buyersReport(BuyersReportFilterDto filterDto) {
        return new ResponseEntity<>(
                ResponseDto
                        .builder(LastBuyersDto.class)
                        .status("ok")
                        .data(
                                LastBuyersDto
                                        .builder()
                                        .contentBuyersInfoDto(
                                                filterDto == null || filterDto.getSection() == null
                                                        || filterDto.getSection().equals(BuySection.CONTENT)
                                                        || filterDto.getSection().equals(BuySection.ALL)
                                                        ? contentRepository.individualRegistrationsLastMonth()
                                                        : null
                                        )
                                        .openQuizBuyersInfoDto(
                                                filterDto == null || filterDto.getSection() == null
                                                        || filterDto.getSection().equals(BuySection.OPEN_QUIZ_EXAM)
                                                        || filterDto.getSection().equals(BuySection.ALL)
                                                        ? openQuizRepository.individualRegistrationsLastMonth()
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
