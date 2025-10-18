package irysc.gachesefid.Routes.API.Content;

import irysc.gachesefid.Controllers.Content.AdminContentController;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.content.MissedDto;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.content.ContentService;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping(path = "/api/package_content/admin")
@Validated
public class AdminAPIRoutes extends Router {

    @Autowired
    private ContentService contentService;

    @GetMapping(value = "buyers/{id}")
    @ResponseBody
    public String buyers(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException {
        getEditorPrivilegeUserVoid(request);
        return AdminContentController.buyers(id);
    }

    @GetMapping(value = "allContents")
    @ResponseBody
    public String getAllContents(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getWeakAdminPrivilegeUserVoid(request);
        return AdminContentController.getAllContents();
    }


    @PutMapping(value = "force_registry/{id}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items", "paid"},
                                        paramsType = {JSONArray.class, Positive.class},
                                        optionals = {},
                                        optionalsType = {}
                                ) String jsonStr
    ) throws NotAccessException, UnAuthException {
        getEditorPrivilegeUserVoid(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        return AdminContentController.forceRegistry(id, jsonObject.getJSONArray("items"), jsonObject.getInt("paid"));
    }


    @DeleteMapping(value = "forceFire/{id}")
    @ResponseBody
    public String forceFire(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId id,
                            @RequestBody @StrongJSONConstraint(
                                    params = {"items"},
                                    paramsType = {JSONArray.class}
                            ) String jsonStr
    ) throws NotAccessException, UnAuthException {
        getEditorPrivilegeUserVoid(request);
        return AdminContentController.forceFire(id, new JSONObject(jsonStr).getJSONArray("items"));
    }

    @GetMapping(value = "findMissed")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<MissedDto>>> findMissed() {
        return contentService.findMissed();
    }
}
