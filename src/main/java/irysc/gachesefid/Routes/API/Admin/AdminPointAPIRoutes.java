package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Models.Action;
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
@RequestMapping(path = "/api/point/admin")
@Validated
public class AdminPointAPIRoutes extends Router {

    @PostMapping(value = "add")
    @ResponseBody
    public String add(
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "action", "point",
                    },
                    paramsType = {
                            Action.class, Positive.class
                    }
            ) @NotBlank String jsonStr
    ) {
        return PointController.add(convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "action", "point",
                    },
                    paramsType = {
                            Action.class, Positive.class
                    }
            ) @NotBlank String jsonStr
    ) {
        return PointController.update(id, convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return PointController.remove(id);
    }

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll() {
        return PointController.getAll();
    }

    @GetMapping(value = "getActions")
    @ResponseBody
    public String getActions() {
        return PointController.getActions();
    }

}
