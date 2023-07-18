package irysc.gachesefid.Routes.API.Ticket;

import irysc.gachesefid.Controllers.Ticket.TicketController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.TicketPriority;
import irysc.gachesefid.Models.TicketSection;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@RestController
@RequestMapping(path = "/api/ticket/")
@Validated
public class StudentTicketAPIRouter extends Router {


    @GetMapping(value = "/getMyRequests")
    @ResponseBody
    public String getMyRequests(HttpServletRequest request,
                                @RequestParam(value = "sendDateSolar", required = false) Long sendDateSolar,
                                @RequestParam(value = "answerDateSolar", required = false) Long answerDateSolar,
                                @RequestParam(value = "sendDateSolarEndLimit", required = false) Long sendDateSolarEndLimit,
                                @RequestParam(value = "answerDateSolarEndLimit", required = false) Long answerDateSolarEndLimit,
                                @RequestParam(value = "section", required = false) String section,
                                @RequestParam(value = "advisorId", required = false) ObjectId advisorId,
                                @RequestParam(value = "studentId", required = false) ObjectId studentId,
                                @RequestParam(value = "priority", required = false) String priority,
                                @RequestParam(value = "status", required = false) String status
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);
        boolean isAdvisor = Authorization.isAdvisor(user.getList("accesses", String.class));

        return TicketController.getRequests(
                null, status,
                null, null,
                isAdvisor && studentId != null ? studentId : user.getObjectId("_id"),
                null, isAdvisor && studentId != null ? user.getObjectId("_id") : advisorId,
                sendDateSolar, answerDateSolar, sendDateSolarEndLimit, answerDateSolarEndLimit,
                null, null, section, priority, true,
                Authorization.isAdvisor(user.getList("accesses", String.class))
        );
    }


    @PostMapping(value = "/submit")
    @ResponseBody
    public String submit(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {
                                         "title", "description"
                                 },
                                 paramsType = {String.class, String.class},
                                 optionals = {
                                         "section", "priority",
                                         "userId", "refId", "additional",
                                         "advisorId"
                                 },
                                 optionalsType = {
                                         TicketSection.class, TicketPriority.class,
                                         ObjectId.class, ObjectId.class, String.class,
                                         ObjectId.class
                                 }
                         ) String jsonStr
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

        Document user = getUser(request);

        if(Authorization.isAdvisor(user.getList("accesses", String.class)))
            return TicketController.setRequestAnswer(
                    requestId, user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        return TicketController.setRequestAnswerUser(
                requestId, user.getObjectId("_id"),
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
