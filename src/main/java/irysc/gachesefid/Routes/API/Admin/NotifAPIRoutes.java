package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Notif.NotifController;
import irysc.gachesefid.Models.NotifVia;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/notifs/manage")
@Validated
public class NotifAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(
            @RequestParam(value = "sendVia") @EnumValidator(enumClazz = NotifVia.class) String sendVia,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to
    ) {
        return NotifController.getAll(sendVia, from, to);
    }


    @GetMapping(value = "getStudents/{id}")
    @ResponseBody
    public String getStudents(@PathVariable @ObjectIdConstraint ObjectId id) {
        return NotifController.getStudents(id);
    }

    @GetMapping(value = "get/{id}")
    @ResponseBody
    public String get(@PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return NotifController.getNotif(id, null);
    }


    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(@RequestBody @StrongJSONConstraint(
            params = {"items"},
            paramsType = {JSONArray.class}
    ) String jsonStr
    ) {
        return NotifController.remove(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "list", required = false) MultipartFile mailList,
            @RequestPart(value = "json") @StrongJSONConstraint(
                    params = {"via", "title", "text", "sendSMS", "sendMail"},
                    paramsType = {
                            NotifVia.class, String.class, String.class,
                            Boolean.class, Boolean.class
                    },
                    optionals = {
                            "cities", "states", "nids", "phones",
                            "branches", "sex", "schools", "grades",
                            "quizzes", "packages", "accesses",
                            "minCoin", "maxCoin", "minMoney",
                            "maxMoney", "minRank", "maxRank"
                    },
                    optionalsType = {
                            JSONArray.class, JSONArray.class,
                            JSONArray.class, JSONArray.class,
                            JSONArray.class, Sex.class, JSONArray.class,
                            JSONArray.class, JSONArray.class, JSONArray.class,
                            JSONArray.class, Number.class, Number.class,
                            Positive.class, Positive.class, Positive.class,
                            Positive.class
                    }
            ) @NotBlank String jsonStr
    ) {
        return NotifController.store(new JSONObject(jsonStr), file, mailList);
    }

    @PostMapping(value = "simpleStore")
    @ResponseBody
    public String simpleStore(@RequestBody @StrongJSONConstraint(
            params = {"via", "title", "text", "sendSMS", "sendMail"},
            paramsType = {
                    NotifVia.class, String.class, String.class,
                    Boolean.class, Boolean.class
            },
            optionals = {
                    "cities", "states", "nids", "phones",
                    "branches", "sex", "schools", "grades",
                    "quizzes", "packages", "accesses",
                    "minCoin", "maxCoin", "minMoney",
                    "maxMoney", "minRank", "maxRank"
            },
            optionalsType = {
                    JSONArray.class, JSONArray.class,
                    JSONArray.class, JSONArray.class,
                    JSONArray.class, Sex.class, JSONArray.class,
                    JSONArray.class, JSONArray.class, JSONArray.class,
                    JSONArray.class, Number.class, Number.class,
                    Positive.class, Positive.class, Positive.class,
                    Positive.class
            }
    ) @NotBlank String jsonStr
    ) {
        return NotifController.store(new JSONObject(jsonStr), null, null);
    }
}
