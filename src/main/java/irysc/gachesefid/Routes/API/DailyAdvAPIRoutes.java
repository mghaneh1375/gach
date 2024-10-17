package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.DailyAdv.DailyAdvController;
import irysc.gachesefid.Exception.NotActivateAccountException;

import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.HttpReqRespUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;

@Controller
@RequestMapping(path = "/api/daily_adv/public")
@Validated
public class DailyAdvAPIRoutes extends Router {

    @Value("${front_ip}")
    private String frontIP;

    @GetMapping("canReqForAdv")
    @ResponseBody
    public String canReqForAdv(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return DailyAdvController.canReqForAdv(getUserId(request));
    }

    @GetMapping("getRandAdv")
    @ResponseBody
    public String getRandAdv(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return DailyAdvController.getRandAdv(getUserId(request));
    }

    @PostMapping("giveMyPointForAdv")
    @ResponseBody
    public String giveMyPointForAdv(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        if (!HttpReqRespUtils.getClientIpAddressIfServletRequestExist(request).equals(frontIP))
            return JSON_NOT_ACCESS;
        return DailyAdvController.giveMyPointForAdv(getUserId(request));
    }

}
