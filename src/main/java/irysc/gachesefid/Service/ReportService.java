package irysc.gachesefid.Service;

import irysc.gachesefid.Dto.BuyersReportFilterDto;
import irysc.gachesefid.Dto.Report.BuyReport.*;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Enums.BuySection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

import java.util.HashMap;

import static irysc.gachesefid.Main.GachesefidApplication.*;

@Service
public class ReportService {

    public ResponseEntity<ResponseDto<LastBuyersDto>> buyersReport(@NotNull BuyersReportFilterDto filterDto) {
        LastBuyersDto lastBuyersDto = LastBuyersDto
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
                .customQuizBuyersInfoDto(
                        filterDto.getSection() == null
                                || filterDto.getSection().equals(BuySection.CUSTOM_QUIZ)
                                || filterDto.getSection().equals(BuySection.ALL)
                                ? customQuizRepository.registrations(filterDto.getFrom(), filterDto.getTo())
                                : null
                )
                .adviceBuyersInfoDto(
                        filterDto.getSection() == null
                                || filterDto.getSection().equals(BuySection.ADVISOR)
                                || filterDto.getSection().equals(BuySection.ALL)
                                ? advisorRequestsRepository.registrations(filterDto.getFrom(), filterDto.getTo())
                                : null
                )
                .build();

        HashMap<String, Integer> stats = new HashMap<>();
        if(lastBuyersDto.getAdviceBuyersInfoDto() != null)
            stats.put("مشاوره", lastBuyersDto.getAdviceBuyersInfoDto().stream().mapToInt(BuyerInfoDto::getPaid).sum());

        if(lastBuyersDto.getContentBuyersInfoDto() != null)
            stats.put("دورههای آموزشی", lastBuyersDto.getContentBuyersInfoDto().stream().mapToInt(BuyerInfoDto::getPaid).sum());

        if(lastBuyersDto.getRegularQuizBuyersInfoDto() != null)
            stats.put("آزمونهای آیریسک", lastBuyersDto.getRegularQuizBuyersInfoDto().stream().mapToInt(BuyerInfoDto::getPaid).sum());

        if(lastBuyersDto.getCustomQuizBuyersInfoDto() != null)
            stats.put("آزمونهای شخصی", lastBuyersDto.getCustomQuizBuyersInfoDto().stream().mapToInt(BuyerInfoDto::getPaid).sum());

        if(lastBuyersDto.getOpenQuizBuyersInfoDto() != null)
            stats.put("آزمونهای باز", lastBuyersDto.getOpenQuizBuyersInfoDto().stream().mapToInt(BuyerInfoDto::getPaid).sum());

        lastBuyersDto.setStats(stats);
        return new ResponseEntity<>(
                ResponseDto
                        .builder(LastBuyersDto.class)
                        .status("ok")
                        .data(lastBuyersDto)
                        .build(),
                HttpStatus.OK);
    }

//    public ResponseEntity<ResponseDto<List<AdvisorRequestDto>>> adviceRequests() {
////        advisorRequestsRepository.findWithJoinUser("")
//    }

}
