package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Exchange.ExchangeController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/exchange/admin")
@Validated
public class AdminExchangeAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(
            HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ExchangeController.getAll();
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
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
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ExchangeController.store(
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return ExchangeController.remove(id);
    }

}
