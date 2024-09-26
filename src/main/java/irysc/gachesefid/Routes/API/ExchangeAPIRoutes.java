package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Exchange.ExchangeController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Kavenegar.excepctions.HttpException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/exchange/public")
@Validated
public class ExchangeAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return ExchangeController.getAll();
    }

    @PostMapping(value = "getReward/{id}")
    @ResponseBody
    public String getReward(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return ExchangeController.getReward(
                getUser(request), id
        );
    }

}
