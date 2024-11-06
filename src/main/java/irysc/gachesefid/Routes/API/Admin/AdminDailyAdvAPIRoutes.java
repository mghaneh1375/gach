package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.DailyAdv.DailyAdvController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Controller
@RequestMapping(path = "/api/daily_adv/admin")
@Validated
public class AdminDailyAdvAPIRoutes extends Router {

    @GetMapping("getAll")
    @ResponseBody
    public String getAll(
    ) {
        return DailyAdvController.getAll();
    }

    @PostMapping("add")
    @ResponseBody
    public String add(
            @RequestParam(value = "expireAt") Long expireAt,
            @RequestParam(value = "title") @NotBlank @Size(min = 2, max = 100) String title,
            @RequestBody MultipartFile file
    ) {
        return DailyAdvController.add(file, title, expireAt);
    }

    @DeleteMapping("remove/{id}")
    @ResponseBody
    public String delete(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return DailyAdvController.delete(id);
    }

}
