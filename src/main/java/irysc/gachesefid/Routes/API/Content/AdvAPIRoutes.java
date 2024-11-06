package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.ContentConfigController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/package_content/adv")
@Validated
public class AdvAPIRoutes extends Router {

    @PostMapping(value = "store")
    @ResponseBody
    public String store(
            @RequestPart(value = "file") MultipartFile file,
            @RequestPart(value = "json") @StrongJSONConstraint(
                    params = {
                            "title", "visibility"
                    },
                    paramsType = {
                            String.class, Boolean.class
                    }
            ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        return ContentConfigController.storeAdv(file, convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "update/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @StrongJSONConstraint(
                    params = {
                            "title", "visibility"
                    },
                    paramsType = {
                            String.class, Boolean.class
                    }
            ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return ContentConfigController.updateAdv(id, convertPersian(new JSONObject(jsonStr)));
    }

    @DeleteMapping(value = "remove/{id}")
    @ResponseBody
    public String remove(@PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {
        return ContentConfigController.removeAdv(id);
    }

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll()
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return ContentConfigController.getAdv();
    }

}
