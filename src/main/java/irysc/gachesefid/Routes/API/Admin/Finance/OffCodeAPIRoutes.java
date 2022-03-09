package irysc.gachesefid.Routes.API.Admin.Finance;

import irysc.gachesefid.Controllers.Finance.OffCodeController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.DateValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

@RestController
@RequestMapping(path = "/api/admin/off")
@Validated
public class OffCodeAPIRoutes extends Router {

    @PostMapping(value = "/store")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "type", "expireAt",
                                        "section", "amount"
                                },
                                paramsType = {
                                        String.class, String.class,
                                        String.class, Positive.class
                                },
                                optionals = {"userId"},
                                optionalsType = {String.class}
                        ) String json
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return OffCodeController.store(new JSONObject(json));
    }

    @GetMapping(value = "/offs")
    @ResponseBody
    public String getOffs(HttpServletRequest request,
                          @RequestParam(value = "userId", required = false) ObjectId userId,
                          @RequestParam(value = "used", required = false) Boolean used,
                          @RequestParam(value = "dateUsedSolar", required = false) String dateSolar,
                          @RequestParam(value = "dateUsedGregorian", required = false) String dateGregorian,
                          @RequestParam(value = "dateUsedSolarEndLimit", required = false) String dateSolarEndLimit,
                          @RequestParam(value = "dateUsedGregorianEndLimit", required = false) String dateGregorianEndLimit,
                          @RequestParam(value = "minValue", required = false) Integer minValue,
                          @RequestParam(value = "maxValue", required = false) Integer maxValue,
                          @RequestParam(value = "type", required = false) String type,
                          @RequestParam(value = "expired", required = false) Boolean expired,
                          @RequestParam(value = "section", required = false) String section
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        if (
                (dateSolar != null && !DateValidator.isValid2(dateSolar)) ||
                        (dateGregorian != null && !DateValidator.isValid2(dateGregorian)) ||
                        (dateSolarEndLimit != null && !DateValidator.isValid2(dateSolarEndLimit)) ||
                        (dateGregorianEndLimit != null && !DateValidator.isValid2(dateGregorianEndLimit))
        )
            return null;

        ArrayList<String> dates = new ArrayList<>();

        if (dateSolar != null && dateGregorian == null) {
            String[] splited = dateSolar.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(dateGregorian);

        if (dateSolarEndLimit != null && dateGregorianEndLimit == null) {
            String[] splited = dateSolarEndLimit.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(dateGregorianEndLimit);

        return OffCodeController.offs(userId,
                section, used, expired,
                dates.get(0), dates.get(1),
                minValue, maxValue, type
        );
    }


    @DeleteMapping(value = "/delete/{offCodeId}")
    @ResponseBody
    public String deleteOffCode(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId offCodeId)
            throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        OffCodeController.delete(offCodeId);
        return JSON_OK;
    }

    @DeleteMapping(value = "/deleteByUserId/{userId}")
    @ResponseBody
    public String deleteByUserId(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId userId)
            throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        OffCodeController.deleteByUserId(userId);
        return JSON_OK;
    }

}
