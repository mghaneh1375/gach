package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/advisor/manage/")
@Validated
public class AdvisorAPIRoutes extends Router {


    @PostMapping(value = "toggleStdAcceptance")
    @ResponseBody
    public String toggleStdAcceptance(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.toggleStdAcceptance(getAdvisorUser(request));
    }



}
