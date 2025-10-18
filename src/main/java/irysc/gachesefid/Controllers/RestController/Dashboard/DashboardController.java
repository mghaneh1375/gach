package irysc.gachesefid.Controllers.RestController.Dashboard;

import irysc.gachesefid.Dto.dashboard.Student.DashboardStatsDto;
import irysc.gachesefid.Dto.dashboard.Student.StudentDashboardConfig;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.dashboard.ConfigDashboardService;
import irysc.gachesefid.Service.dashboard.StudentDashboardService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping(path = "/api/dashboard")
public class DashboardController extends Router {

    @Autowired
    private StudentDashboardService studentDashboardService;
    @Autowired
    private ConfigDashboardService configDashboardService;

    @GetMapping(value = "getMySummary")
    @ResponseBody
    public ResponseEntity<ResponseDto<DashboardStatsDto>> stats(
            HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return studentDashboardService.getStudentDashboard(
                getUser(request)
        );
    }

    @GetMapping(value = "getConfig")
    @ResponseBody
    public ResponseEntity<ResponseDto<StudentDashboardConfig>> getConfig(
            HttpServletRequest request
    ) throws UnAuthException {
        return configDashboardService.getConfig(getUserId(request), StudentDashboardConfig.class);
    }

    @PutMapping(value = "setConfig")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setConfig(
            HttpServletRequest request,
            @RequestBody @NonNull @Valid StudentDashboardConfig config
    ) throws UnAuthException {
        configDashboardService.setStudentConfig(getUserId(request), config);
    }
}
