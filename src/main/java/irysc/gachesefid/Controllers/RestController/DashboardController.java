package irysc.gachesefid.Controllers.RestController;

import irysc.gachesefid.Dto.Dashboard.DashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.Dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Validated
@RestController
@RequestMapping(path = "/api/dashboard")
public class DashboardController extends Router {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping(value = "getMySummary")
    @ResponseBody
    public ResponseEntity<ResponseDto<DashboardStatsDto>> stats(
            HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return dashboardService.dashboardInfo(
                getUser(request)
        );
    }

}
