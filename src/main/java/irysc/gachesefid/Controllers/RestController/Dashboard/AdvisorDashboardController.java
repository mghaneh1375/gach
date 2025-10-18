package irysc.gachesefid.Controllers.RestController.Dashboard;


import irysc.gachesefid.Dto.dashboard.Advisor.AdvisorDashboardConfig;
import irysc.gachesefid.Dto.dashboard.Advisor.AdvisorDashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.dashboard.AdvisorDashboardService;
import irysc.gachesefid.Service.dashboard.ConfigDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping(path = "/api/advisor/dashboard")
public class AdvisorDashboardController extends Router {

    @Autowired
    private AdvisorDashboardService advisorDashboardService;

    @Autowired
    private ConfigDashboardService configDashboardService;

    @GetMapping(value = "getInfo")
    @ResponseBody
    public ResponseEntity<ResponseDto<AdvisorDashboardStatsDto>> getInfo(
            HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return advisorDashboardService.advisorDashboardInfo(getUser(request));
    }

    @GetMapping(value = "getConfig")
    @ResponseBody
    public ResponseEntity<ResponseDto<AdvisorDashboardConfig>> getConfig(
            HttpServletRequest request
    ) throws UnAuthException {
        return configDashboardService.getConfig(getUserId(request), AdvisorDashboardConfig.class);
    }

    @PutMapping(value = "setConfig")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setConfig(
            HttpServletRequest request,
            @RequestBody @Valid AdvisorDashboardConfig config
    ) throws UnAuthException {
        configDashboardService.setAdvisorConfig(getUserId(request), config);
    }
}
