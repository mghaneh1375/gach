package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Controllers.Config.CityController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/general")
@Validated
public class GeneralAPIRoutes extends Router {

    @GetMapping(value = "/fetchStates")
    @ResponseBody
    public String fetchStates() {
        return CityController.getAll();
    }

    @GetMapping(value = "/fetchSchoolsDigest")
    @ResponseBody
    public String fetchSchoolsDigest() {
        return UserController.fetchSchoolsDigest();
    }
}
