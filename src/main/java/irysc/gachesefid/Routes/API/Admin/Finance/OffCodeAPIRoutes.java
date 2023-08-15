package irysc.gachesefid.Routes.API.Admin.Finance;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Finance.Off.OffCodeController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.DateValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.ne;
import static irysc.gachesefid.Main.GachesefidApplication.offcodeRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

@RestController
@RequestMapping(path = "/api/admin/off")
@Validated
public class OffCodeAPIRoutes extends Router {

    @PostMapping(value = "storeFromShop")
    @ResponseBody
    public String storeFromShop(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "token", "firstName", "lastName",
                                                "email", "phone", "orderId",
                                                "total"
                                        },
                                        paramsType = {
                                                String.class, String.class, String.class,
                                                String.class, String.class, Positive.class,
                                                Object.class
                                        }
                                ) @NotBlank String jsonStr) {
        return OffCodeController.storeFromShop(
                Utility.convertPersian(new JSONObject(jsonStr)),
                request.getRemoteAddr()
        );
    }

    @PostMapping(value = "/storeWithExcel")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestPart MultipartFile file,
                        @RequestPart @StrongJSONConstraint(
                                params = {"expireAt", "type", "amount"
                                },
                                paramsType = {Long.class, String.class,
                                        Positive.class
                                },
                                optionals = {
                                        "section", "code"
                                },
                                optionalsType = {
                                        String.class, String.class
                                }

                        ) @NotBlank String json
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        JSONObject jsonObject = new JSONObject(json);

        getAdminPrivilegeUserVoid(request);
        return OffCodeController.store(file,
                jsonObject.getString("code"),
                jsonObject.getString("type"),
                jsonObject.getInt("amount"),
                jsonObject.getLong("expireAt"),
                jsonObject.has("section") ?
                        jsonObject.getString("section") :
                        OffCodeSections.ALL.getName());
    }


    @PutMapping(value = "/update/{id}")
    @ResponseBody
    public String update(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id,
                         @RequestBody @StrongJSONConstraint(
                                 params = {}, paramsType = {},
                                 optionals = {
                                         "type", "expireAt", "amount",
                                         "section", "code",
                                 },
                                 optionalsType = {
                                         String.class, Long.class, Positive.class,
                                         String.class, String.class
                                 }
                         ) String json
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return OffCodeController.update(id, new JSONObject(json));
    }


    @PutMapping(value = "/store")
    @ResponseBody
    public String storeJSONArr(HttpServletRequest request,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"expireAt",
                                               "type", "amount"
                                       },
                                       paramsType = {Long.class,
                                               String.class, Positive.class
                                       },
                                       optionals = {
                                               "items", "section",
                                               "code", "isPublic"
                                       },
                                       optionalsType = {
                                               JSONArray.class, String.class,
                                               String.class, Boolean.class
                                       }

                               ) @NotBlank String json
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return OffCodeController.store(
                Utility.convertPersian(new JSONObject(json))
        );
    }

    @GetMapping(value = "/offs")
    @ResponseBody
    public String getOffs(HttpServletRequest request,
                          @RequestParam(value = "userId", required = false) ObjectId userId,
                          @RequestParam(value = "used", required = false) Boolean used,
                          @RequestParam(value = "createdAt", required = false) Long createdAt,
                          @RequestParam(value = "createdAtEndLimit", required = false) Long createdAtEndLimit,
                          @RequestParam(value = "usedAt", required = false) Long usedAt,
                          @RequestParam(value = "usedAtEndLimit", required = false) Long usedAtEndLimit,
                          @RequestParam(value = "expiredAt", required = false) Long expiredAt,
                          @RequestParam(value = "expiredAtEndLimit", required = false) Long expiredAtEndLimit,
                          @RequestParam(value = "minValue", required = false) Integer minValue,
                          @RequestParam(value = "maxValue", required = false) Integer maxValue,
                          @RequestParam(value = "type", required = false) String type,
                          @RequestParam(value = "withCode", required = false) String withCode,
                          @RequestParam(value = "code", required = false) String code,
                          @RequestParam(value = "isPublic", required = false) Boolean isPublic,
                          @RequestParam(value = "hasExpired", required = false) Boolean expired,
                          @RequestParam(value = "section", required = false) String section
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        return OffCodeController.offs(userId,
                section, used, expired,
                createdAt, createdAtEndLimit,
                expiredAt, expiredAtEndLimit,
                usedAt, usedAtEndLimit,
                minValue, maxValue, type, isPublic,
                code, withCode
        );
    }


    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String deleteOffCode(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items"},
                                        paramsType = {JSONArray.class}
                                ) @NotBlank String jsonStr)
            throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommonController.removeAll(offcodeRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                ne("used", true)
        );
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
