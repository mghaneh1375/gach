package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Level.LevelController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/level/admin")
@Validated
public class AdminLevelAPIRoutes extends Router {

    @PostMapping(value = "add")
    @ResponseBody
    public String add(@RequestBody @StrongJSONConstraint(
            params = {
                    "name", "maxPoint",
                    "minPoint", "coin"
            },
            paramsType = {
                    String.class, Positive.class,
                    Positive.class, Number.class
            }
    ) @NotBlank String jsonStr
    ) {
        return LevelController.add(convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "name", "maxPoint",
                            "minPoint", "coin"
                    },
                    paramsType = {
                            String.class, Positive.class,
                            Positive.class, Number.class
                    }
            ) @NotBlank String jsonStr
    ) {
        return LevelController.update(id, convertPersian(new JSONObject(jsonStr)));
    }

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return LevelController.getAll();
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(@PathVariable @ObjectIdConstraint ObjectId id) {
        return LevelController.remove(id);
    }

}
