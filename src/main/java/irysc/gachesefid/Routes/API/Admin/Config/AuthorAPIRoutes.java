package irysc.gachesefid.Routes.API.Admin.Config;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Config.AuthorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GradeSchool;
import irysc.gachesefid.Models.KindSchool;
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

import javax.servlet.http.HttpServletRequest;
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
    public String getAll(HttpServletRequest request,
                         @RequestParam(value = "tag", required = false) String tag
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.getAuthors(tag);
    }

    @GetMapping(value = "/getAuthorsKeyVals")
    @ResponseBody
    public String getAuthorsKeyVals(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.getAuthorsKeyVals();
    }

    @GetMapping(value = "/getTransactions/{authorId}")
    @ResponseBody
    public String getTransactions(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId authorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.getAuthorTransactions(authorId);
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return CommonController.removeAll(authorRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                null
        );
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String addAuthor(HttpServletRequest request,
                            @RequestBody @JSONConstraint(
                                    params = {"name"},
                                    optionals = {"tag"}
                            ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.addAuthor(new JSONObject(str));
    }

    @PostMapping(value = "edit/{authorId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId authorId,
                       @RequestBody @JSONConstraint(
                               params = {"name"},
                               optionals = {"tag"}
                       ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.editAuthor(authorId, new JSONObject(str));
    }

    @PostMapping(value = "addTransaction/{authorId}")
    @ResponseBody
    public String addTransaction(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId authorId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"payAt", "pay"},
                                         paramsType = {Long.class, Positive.class},
                                         optionals = {"description"},
                                         optionalsType = {String.class}
                                 ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return AuthorController.addAuthorTransaction(authorId, new JSONObject(str));
    }

    @DeleteMapping(value = "/removeTransactions/{authorId}")
    @ResponseBody
    public String removeTransactions(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId authorId,
                                     @RequestBody @StrongJSONConstraint(
                                             params = {"items"},
                                             paramsType = {JSONArray.class}
                                     ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return CommonController.removeAllFormDocList(authorRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                authorId, "transactions", null
        );
    }

    @GetMapping(value = "/getLastTransaction/{authorId}")
    @ResponseBody
    public String getLastTransaction(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId authorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);

        Document doc = authorRepository.findById(authorId);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        return AuthorController.returnLastAuthorTransaction(
                doc.containsKey("transactions") ?
                        doc.getList("transactions", Document.class) :
                        new ArrayList<>(), null
        );
    }
}
