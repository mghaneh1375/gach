package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.AdminStatsController;
import irysc.gachesefid.Routes.Router;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/admin/stats")
@Validated
public class AdminStatsAPIRoutes extends Router {

    @GetMapping(value = "report")
    @ResponseBody
    public String getStats(){
        return AdminStatsController.stats();
    }

}
