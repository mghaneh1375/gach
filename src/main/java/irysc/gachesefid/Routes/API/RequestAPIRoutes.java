package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Controllers.RequestController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/request")
@Validated
public class RequestAPIRoutes extends Router {

    @GetMapping(path = "/getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(required = false, value = "isPending") Boolean isPending,
                         @RequestParam(required = false, value = "isFinished") Boolean isFinished,
                         @RequestParam(value = "searchInArchive", required = false) Boolean searchInArchive,
                         @RequestParam(required = false, value = "type") String type,
                         @RequestParam(required = false, value = "userId") ObjectId userId,
                         @RequestParam(value = "sendDate", required = false) String sendDate,
                         @RequestParam(value = "answerDate", required = false) String answerDate,
                         @RequestParam(value = "sendDateEndLimit", required = false) String sendDateEndLimit,
                         @RequestParam(value = "answerDateEndLimit", required = false) String answerDateEndLimit
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        ArrayList<String> dates = Utility.checkDatesConstriant(
                sendDate, answerDate,
                sendDateEndLimit, answerDateEndLimit);

        if (dates == null)
            return JSON_NOT_VALID_PARAMS;

        getEditorPrivilegeUserVoid(request);
        return RequestController.getAll(isPending, isFinished, searchInArchive,
                type, userId, sendDate, answerDate, sendDateEndLimit, answerDateEndLimit);
    }

    @PutMapping(path = "/setRequestAnswer/{requestId}")
    @ResponseBody
    public String setRequestAnswer(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId requestId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"answer"},
                                           paramsType = {Boolean.class},
                                           optionals = {"description"},
                                           optionalsType = {String.class}
                                   ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getEditorPrivilegeUserVoid(request);
        JSONObject jsonObject = new JSONObject(jsonStr);
        return RequestController.setRequestAnswer(requestId,
                jsonObject.getBoolean("answer"),
                jsonObject.has("description") ? jsonObject.getString("description") : "");
    }
}
