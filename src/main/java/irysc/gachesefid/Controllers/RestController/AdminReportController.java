package irysc.gachesefid.Controllers.RestController;

import irysc.gachesefid.Dto.BuyersReportFilterDto;
import irysc.gachesefid.Dto.Report.BuyReport.LastBuyersDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping(path = "/api/admin/report")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping(value = "buyersReport")
    @ResponseBody
    public ResponseEntity<ResponseDto<LastBuyersDto>> buyersReport(
            @RequestBody @Valid BuyersReportFilterDto filterDto
    ) {
        return reportService.buyersReport(filterDto);
    }

}
