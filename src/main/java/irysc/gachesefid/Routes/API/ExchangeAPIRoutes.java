package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Exchange.ExchangeController;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/api/exchange/public")
@Validated
public class ExchangeAPIRoutes {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return ExchangeController.getAll();
    }

}
