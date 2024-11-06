package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Config.AuthorController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.JSONConstraint;
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
import java.util.ArrayList;

import static irysc.gachesefid.Main.GachesefidApplication.authorRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;

@Controller
@RequestMapping(path = "/api/admin/config/author")
@Validated
public class AuthorAPIRoutes extends Router {

    @GetMapping(value = "/get")
    @ResponseBody
    public String getAll(
            @RequestParam(value = "tag", required = false) String tag
    ) {
        return AuthorController.getAuthors(tag);
    }

    @GetMapping(value = "/getAuthorsKeyVals")
    @ResponseBody
    public String getAuthorsKeyVals() {
        return AuthorController.getAuthorsKeyVals();
    }

    @GetMapping(value = "/getTransactions/{authorId}")
    @ResponseBody
    public String getTransactions(
            @PathVariable @ObjectIdConstraint ObjectId authorId
    ) {
        return AuthorController.getAuthorTransactions(authorId);
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) @NotBlank String jsonStr
    ) {
        return CommonController.removeAll(authorRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                null
        );
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String addAuthor(
            @RequestBody @JSONConstraint(
                    params = {"name"},
                    optionals = {"tag"}
            ) @NotBlank String str
    ) {
        return AuthorController.addAuthor(new JSONObject(str));
    }

    @PostMapping(value = "edit/{authorId}")
    @ResponseBody
    public String edit(
            @PathVariable @ObjectIdConstraint ObjectId authorId,
            @RequestBody @JSONConstraint(
                    params = {"name"},
                    optionals = {"tag"}
            ) @NotBlank String str
    ) {
        return AuthorController.editAuthor(authorId, new JSONObject(str));
    }

    @PostMapping(value = "addTransaction/{authorId}")
    @ResponseBody
    public String addTransaction(
            @PathVariable @ObjectIdConstraint ObjectId authorId,
            @RequestBody @StrongJSONConstraint(
                    params = {"payAt", "pay"},
                    paramsType = {Long.class, Positive.class},
                    optionals = {"description"},
                    optionalsType = {String.class}
            ) @NotBlank String str
    ) {
        return AuthorController.addAuthorTransaction(authorId, new JSONObject(str));
    }

    @DeleteMapping(value = "/removeTransactions/{authorId}")
    @ResponseBody
    public String removeTransactions(
            @PathVariable @ObjectIdConstraint ObjectId authorId,
            @RequestBody @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) @NotBlank String jsonStr
    ) {
        return CommonController.removeAllFormDocList(authorRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                authorId, "transactions", null
        );
    }

    @GetMapping(value = "/getLastTransaction/{authorId}")
    @ResponseBody
    public String getLastTransaction(
            @PathVariable @ObjectIdConstraint ObjectId authorId
    ) {
        Document doc = authorRepository.findById(authorId);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        return AuthorController.returnLastAuthorTransaction(
                doc.containsKey("transactions") ?
                        doc.getList("transactions", Document.class) :
                        new ArrayList<>(), null
        );
    }
}
