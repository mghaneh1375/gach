package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Config.TarazLevelController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.configRepository;

@Controller
@RequestMapping(path = "/api/admin/config/tarazLevel")
@Validated
public class TarazLevelAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String get() {
        return TarazLevelController.getAll();
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document config = Utility.getConfig();
        return CommonController.removeAllFormDocList(configRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                config.getObjectId("_id"), "taraz_levels",
                null
        );
    }

    @PostMapping(value = "create")
    @ResponseBody
    public String create(
            @RequestBody @StrongJSONConstraint(
                    params = {"min", "max", "color", "priority"},
                    paramsType = {
                            Positive.class, Positive.class,
                            String.class, Positive.class
                    }
            ) @NotBlank String str
    ) {
        return TarazLevelController.add(new JSONObject(str));
    }

    @PutMapping(value = "edit/{id}")
    @ResponseBody
    public String edit(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {"min", "max", "color", "priority"},
                    paramsType = {
                            Positive.class, Positive.class,
                            String.class, Positive.class
                    }
            ) @NotBlank String str
    ) {
        return TarazLevelController.edit(id, new JSONObject(str));
    }
}
