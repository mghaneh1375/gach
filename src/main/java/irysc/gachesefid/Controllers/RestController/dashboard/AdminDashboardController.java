package irysc.gachesefid.Controllers.RestController.dashboard;

import irysc.gachesefid.Dto.dashboard.admin.AdminDashboardConfig;
import irysc.gachesefid.Dto.dashboard.admin.AdminDashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.dashboard.ConfigDashboardService;
import irysc.gachesefid.Service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping(path = "/admin/dashboard")
public class AdminDashboardController extends Router {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ConfigDashboardService configDashboardService;

    @GetMapping(value = "getInfo")
    @ResponseBody
    public ResponseEntity<ResponseDto<AdminDashboardStatsDto>> getInfo(
            HttpServletRequest request
    ) throws UnAuthException {
        return dashboardService.adminDashboardInfo(getUserId(request));
    }

    @GetMapping(value = "getConfig")
    @ResponseBody
    public ResponseEntity<ResponseDto<AdminDashboardConfig>> getConfig(
            HttpServletRequest request
    ) throws UnAuthException {
        return configDashboardService.getConfig(getUserId(request), AdminDashboardConfig.class);
    }

    @PutMapping(value = "setConfig")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setConfig(
            HttpServletRequest request,
            @RequestBody @Valid AdminDashboardConfig config
    ) throws UnAuthException {
        configDashboardService.setAdminConfig(getUserId(request), config);
    }
}
