package irysc.gachesefid.Routes.API.Admin.Finance;

import irysc.gachesefid.Controllers.Finance.TransactionController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.bson.types.ObjectId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/api/admin/transaction")
@Validated
public class TransactionAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @RequestParam(value = "userId", required = false) ObjectId userId,
                      @RequestParam(value = "from", required = false) Long from,
                      @RequestParam(value = "to", required = false) Long to,
                      @RequestParam(value = "useOffCode", required = false) Boolean useOffCode,
                      @RequestParam(value = "section", required = false) String section
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return TransactionController.get(userId, from, to, useOffCode, section);
    }
}
