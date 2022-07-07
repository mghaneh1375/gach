package irysc.gachesefid.Routes.API.Ticket;

import irysc.gachesefid.Controllers.Ticket.TicketController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@RestController
@RequestMapping(path = "/api/ticket/")
@Validated
public class StudentTicketAPIRouter extends Router {


    @PostMapping(value = "/submit")
    @ResponseBody
    public String submit(HttpServletRequest request,
                         @RequestBody @JSONConstraint(
                                 params = {
                                         "title", "description"
                                 },
                                 optionals = {
                                         "section", "priority",
                                         "userId"
                                 }) String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        JSONObject jsonObject = new JSONObject(jsonStr);

        Document user = getUser(request);
        return TicketController.insert(
                user.getList("accesses", String.class),
                user.getObjectId("_id"),
                jsonObject
        );
    }


    @PutMapping(value = "/setAnswer/{requestId}")
    @ResponseBody
    public String setAnswer(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId requestId,
                            @RequestBody @JSONConstraint(params = {"answer"}) String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return TicketController.setRequestAnswerUser(
                requestId, getUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getString("answer")
        );
    }

    @PutMapping(value = "/addFileToRequest/{requestId}")
    @ResponseBody
    public String addFileToRequest(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId requestId,
                                   @RequestBody MultipartFile file)
            throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getUser(request);

        return TicketController.addFileToRequest(
                user.getList("accesses", String.class),
                user.getObjectId("_id"),
                requestId, file);
    }

    @PostMapping(value = "/sendRequest/{requestId}")
    @ResponseBody
    public String sendRequest(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId requestId)
            throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return TicketController.sendRequest(
                getUser(request).getObjectId("_id"),
                requestId
        );
    }

}
