package irysc.gachesefid.Routes.API.Admin.Finance;

import irysc.gachesefid.Controllers.Finance.TransactionController;
import irysc.gachesefid.Routes.Router;
import org.bson.types.ObjectId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController
@RequestMapping(path = "/admin/transaction")
@Validated
public class TransactionAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String get(
            @RequestParam(value = "userId", required = false) ObjectId userId,
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to,
            @RequestParam(value = "useOffCode", required = false) Boolean useOffCode,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "pageIndex") @Min(1) @Max(1000000) int pageIndex
    ) {
        return TransactionController.get(userId, from, to, useOffCode, section, pageIndex);
    }
}
