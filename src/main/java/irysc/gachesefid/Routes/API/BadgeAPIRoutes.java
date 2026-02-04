package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Badge.BadgeController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/badge/public")
@Validated
public class BadgeAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(
            HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return BadgeController.getAll(getUserId(request));
    }
}
