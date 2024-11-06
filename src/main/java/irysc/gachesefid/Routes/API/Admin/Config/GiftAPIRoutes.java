package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Models.GiftType;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
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

import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/admin/config/gift")
@Validated
public class GiftAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String getAll(
            @RequestParam(value = "useFor", required = false) String useFor
    ) {
        return GiftController.getAll(useFor);
    }

    @GetMapping(value = "/getConfig")
    @ResponseBody
    public String getConfig() {
        return GiftController.getConfig();
    }

    @PutMapping(value = "/updateConfig")
    @ResponseBody
    public String updateConfig(
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
    ) {
        return GiftController.updateConfig(new JSONObject(jsonStr));
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) @NotBlank String jsonStr
    ) {
        return GiftController.removeAll(
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            @RequestBody @StrongJSONConstraint(
                    params = {"type", "amount", "isForSite", "count", "prob", "priority"},
                    paramsType = {GiftType.class, Number.class, Boolean.class, Positive.class, Number.class, Positive.class},
                    optionals = {"useFor", "offCodeType", "expireAt"},
                    optionalsType = {OffCodeSections.class, OffCodeTypes.class, Long.class}
            ) @NotBlank String str
    ) {
        return GiftController.store(null, new JSONObject(str));
    }

    @PostMapping(value = "edit/{giftId}")
    @ResponseBody
    public String edit(
            @PathVariable @ObjectIdConstraint ObjectId giftId,
            @RequestBody @StrongJSONConstraint(
                    params = {"type", "amount", "isForSite", "count", "prob", "priority"},
                    paramsType = {GiftType.class, Number.class, Boolean.class, Positive.class, Number.class, Positive.class},
                    optionals = {"useFor", "offCodeType", "expireAt"},
                    optionalsType = {OffCodeSections.class, OffCodeTypes.class, Long.class}
            ) @NotBlank String str
    ) {
        return GiftController.store(giftId, new JSONObject(str));
    }


    @GetMapping(value = "report")
    @ResponseBody
    public String report(
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to,
            @RequestParam(value = "giftId", required = false) ObjectId giftId,
            @RequestParam(value = "repeat", required = false) String repeat,
            @RequestParam(value = "userId", required = false) ObjectId userId
    ) {
        return GiftController.report(from, to, giftId, repeat, userId);
    }


//    @PostMapping(value = "edit/{authorId}")
//    @ResponseBody
//    public String edit(
//                       @PathVariable @ObjectIdConstraint ObjectId authorId,
//                       @RequestBody @JSONConstraint(
//                               params = {"name"},
//                               optionals = {"tag"}
//                       ) @NotBlank String str
//    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
//        return UserController.editAuthor(authorId, new JSONObject(str));
//    }
}
