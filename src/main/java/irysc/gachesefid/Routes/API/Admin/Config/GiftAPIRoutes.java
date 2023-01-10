package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/admin/config/gift")
@Validated
public class GiftAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(value = "useFor", required = false) String useFor
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.getAll(useFor);
    }

    @GetMapping(value = "/getConfig")
    @ResponseBody
    public String getConfig(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.getConfig();
    }

    @PutMapping(value = "/updateConfig")
    @ResponseBody
    public String updateConfig(HttpServletRequest request,
                               @RequestBody @StrongJSONConstraint(
                                       params = {},
                                       paramsType = {},
                                       optionals = {
                                               "coinForSecondTime",
                                               "coinForThirdTime",
                                               "coinForForthTime",
                                               "coinForFifthTime",
                                               "maxWebGiftSlot",
                                               "maxAppGiftSlot",
                                               "appGiftDays",
                                               "webGiftDays",
                                       },
                                       optionalsType = {
                                               Number.class, Number.class,
                                               Number.class, Number.class,
                                               Positive.class, Positive.class,
                                               JSONArray.class, JSONArray.class
                                       }
                               ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.updateConfig(new JSONObject(jsonStr));
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return GiftController.removeAll(
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(HttpServletRequest request,
                            @RequestBody @StrongJSONConstraint(
                                    params = {"type", "amount", "isForSite", "count", "prob", "priority"},
                                    paramsType = {GiftType.class, Number.class, Boolean.class, Positive.class, Number.class, Positive.class},
                                    optionals = {"useFor", "offCodeType", "expireAt"},
                                    optionalsType = {OffCodeSections.class, OffCodeTypes.class, Long.class}
                            ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.store(null, new JSONObject(str));
    }

    @PostMapping(value = "edit/{giftId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                        @PathVariable @ObjectIdConstraint ObjectId giftId,
                        @RequestBody @StrongJSONConstraint(
                                params = {"type", "amount", "isForSite", "count", "prob", "priority"},
                                paramsType = {GiftType.class, Number.class, Boolean.class, Positive.class, Number.class, Positive.class},
                                optionals = {"useFor", "offCodeType", "expireAt"},
                                optionalsType = {OffCodeSections.class, OffCodeTypes.class, Long.class}
                        ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.store(giftId, new JSONObject(str));
    }


    @GetMapping(value = "report")
    @ResponseBody
    public String report(HttpServletRequest request,
                         @RequestParam(value = "from", required = false) Long from,
                         @RequestParam(value = "to", required = false) Long to,
                         @RequestParam(value = "giftId", required = false) ObjectId giftId,
                         @RequestParam(value = "repeat", required = false) String repeat,
                         @RequestParam(value = "userId", required = false) ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return GiftController.report(from, to, giftId, repeat, userId);
    }



//    @PostMapping(value = "edit/{authorId}")
//    @ResponseBody
//    public String edit(HttpServletRequest request,
//                       @PathVariable @ObjectIdConstraint ObjectId authorId,
//                       @RequestBody @JSONConstraint(
//                               params = {"name"},
//                               optionals = {"tag"}
//                       ) @NotBlank String str
//    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
//        getAdminPrivilegeUser(request);
//        return UserController.editAuthor(authorId, new JSONObject(str));
//    }
}
