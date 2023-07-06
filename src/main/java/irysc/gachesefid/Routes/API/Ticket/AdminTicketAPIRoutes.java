package irysc.gachesefid.Routes.API.Ticket;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Ticket.TicketController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static irysc.gachesefid.Main.GachesefidApplication.ticketRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@RestController
@RequestMapping(path = "/api/admin/ticket/")
@Validated
public class AdminTicketAPIRoutes extends Router {

    @GetMapping(value = "/getRequests")
    @ResponseBody
    public String getRequests(HttpServletRequest request,
                              @RequestParam(value = "sendDateSolar", required = false) Long sendDateSolar,
                              @RequestParam(value = "answerDateSolar", required = false) Long answerDateSolar,
                              @RequestParam(value = "sendDateSolarEndLimit", required = false) Long sendDateSolarEndLimit,
                              @RequestParam(value = "answerDateSolarEndLimit", required = false) Long answerDateSolarEndLimit,
                              @RequestParam(value = "isForTeacher", required = false) Boolean isForTeacher,
                              @RequestParam(value = "startByAdmin", required = false) Boolean startByAdmin,
                              @RequestParam(value = "section", required = false) String section,
                              @RequestParam(value = "priority", required = false) String priority,
                              @RequestParam(value = "searchInArchive", required = false) Boolean searchInArchive,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "finisher", required = false) ObjectId finisher,
                              @RequestParam(value = "studentId", required = false) ObjectId studentId,
                              @RequestParam(value = "refId", required = false) ObjectId refId,
                              @RequestParam(value = "id", required = false) ObjectId id
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

//        ArrayList<String> dates = Utility.checkDatesConstriant(
//                sendDateSolar, answerDateSolar,
//                sendDateSolarEndLimit, answerDateSolarEndLimit
//        );
//
//        if (dates == null)
//            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);

        return TicketController.getRequests(
                searchInArchive, status,
                finisher, id, studentId, refId,
//                dates.get(0), dates.get(1), dates.get(2), dates.get(3),
                sendDateSolar, answerDateSolar, sendDateSolarEndLimit, answerDateSolarEndLimit,
                isForTeacher, startByAdmin, section, priority, false, false
        );
    }

    @GetMapping(value = "/getRequest/{ticketId}")
    @ResponseBody
    public String getRequest(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId ticketId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        Document user = getUser(request);
        return TicketController.getRequest(ticketId,
                Authorization.isAdmin(user.getList("accesses", String.class)) ? null
                        : user.getObjectId("_id")
        );
    }

    @DeleteMapping(value = "/remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
//        getAdminPrivilegeUserVoid(request);
        Document user = getUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return CommonController.removeAll(ticketRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                isAdmin ? null : or(
                        eq("user_id", user.getObjectId("_id")),
                        eq("advisor_id", user.getObjectId("_id"))
                )
        );
    }

    @PostMapping(value = "/rejectRequests")
    @ResponseBody
    public String finishRequests(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = "items",
                                         paramsType = {JSONArray.class}
                                 ) @NotBlank String jsonStr)
            throws UnAuthException, NotActivateAccountException, NotAccessException {
        return TicketController.rejectRequests(
                getPrivilegeUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }
}
