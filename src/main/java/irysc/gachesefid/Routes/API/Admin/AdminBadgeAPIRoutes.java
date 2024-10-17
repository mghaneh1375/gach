package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Badge.BadgeController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/badge/admin")
@Validated
public class AdminBadgeAPIRoutes extends Router {

    @PostMapping(
            value = "add",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public String add(
            @RequestPart(name = "locked") MultipartFile locked,
            @RequestPart(name = "unlocked") MultipartFile unlocked,
            @RequestPart(name = "json") @StrongJSONConstraint(
                    params = {
                            "name", "actions",
                            "priority", "award"
                    },
                    paramsType = {
                            String.class, JSONArray.class,
                            Positive.class, Number.class
                    }
            ) @NotBlank String jsonStr
    ) {
        if(locked == null || unlocked == null) return JSON_NOT_VALID_PARAMS;
        return BadgeController.add(locked, unlocked, convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(
            value = "update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestPart(name = "locked", required = false) MultipartFile locked,
            @RequestPart(name = "unlocked", required = false) MultipartFile unlocked,
            @RequestPart(name = "json") @StrongJSONConstraint(
                    params = {
                            "name", "actions",
                            "priority", "award"
                    },
                    paramsType = {
                            String.class, JSONArray.class,
                            Positive.class, Number.class
                    }
            ) @NotBlank String jsonStr
    ) {
        return BadgeController.update(id, locked, unlocked, convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return BadgeController.remove(id);
    }

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return BadgeController.getAll(null);
    }
}
