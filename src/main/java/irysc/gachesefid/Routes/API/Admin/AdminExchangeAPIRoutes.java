package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Exchange.ExchangeController;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/exchange/admin")
@Validated
public class AdminExchangeAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return ExchangeController.getAll();
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(@RequestBody @StrongJSONConstraint(
            params = {
                    "neededCoin"
            },
            paramsType = {
                    Number.class
            },
            optionals = {
                    "section", "money",
                    "offCodeAmount", "isPercent"
            },
            optionalsType = {
                    OffCodeSections.class, Positive.class,
                    Positive.class, Boolean.class
            }
    ) @NotBlank String jsonStr
    ) {
        return ExchangeController.store(
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return ExchangeController.remove(id);
    }

}
