package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Finance.AdminSettlementController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.SettledStatus;
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
@RequestMapping(path = "/api/admin/settled")
@Validated
public class AdminSettledAPIRoutes extends Router {

    @PutMapping(value = "changeSettlementRequestStatus/{id}")
    @ResponseBody
    public String changeSettlementRequestStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {"status"},
                    paramsType = {SettledStatus.class},
                    optionals = {"desc"},
                    optionalsType = {String.class}
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminSettlementController.changeSettlementRequestStatus(
                id, new JSONObject(jsonStr)
        );
    }

    @GetMapping(value = "getSettledRequests")
    @ResponseBody
    public String getSettledRequests(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "createdFrom", required = false) Long createdFrom,
            @RequestParam(value = "createdTo", required = false) Long createdTo,
            @RequestParam(value = "answerFrom", required = false) Long answerFrom,
            @RequestParam(value = "answerTo", required = false) Long answerTo
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return AdminSettlementController.getSettledRequests(
                status, createdFrom, createdTo, answerFrom, answerTo
        );
    }

    @PostMapping(value = "createSettlementRequest/{userId}/{refId}")
    @ResponseBody
    public String createSettlementRequest(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId userId,
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @RequestBody @StrongJSONConstraint(
                    params = {"section", "amount"},
                    paramsType = {
                            String.class, Positive.class
                    },
                    optionals = {"desc"},
                    optionalsType = {String.class}
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        return AdminSettlementController.createSettlementRequest(
                jsonObject.getString("section"), userId,
                jsonObject.getInt("amount"), refId,
                jsonObject.has("desc") ? jsonObject.getString("desc") : null
        );
    }

}
