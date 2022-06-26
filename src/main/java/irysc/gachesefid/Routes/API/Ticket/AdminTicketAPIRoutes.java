package irysc.gachesefid.Routes.API.Ticket;

import irysc.gachesefid.Controllers.Ticket.TicketController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@RestController
@RequestMapping(path="/api/admin/ticket/")
@Validated
public class AdminTicketAPIRoutes extends Router {

    @GetMapping(value = "/getRequests")
    @ResponseBody
    public String getRequests(HttpServletRequest request,
                              @RequestParam(value = "sendDateSolar", required = false) String sendDateSolar,
                              @RequestParam(value = "answerDateSolar", required = false) String answerDateSolar,
                              @RequestParam(value = "sendDateSolarEndLimit", required = false) String sendDateSolarEndLimit,
                              @RequestParam(value = "answerDateSolarEndLimit", required = false) String answerDateSolarEndLimit,
                              @RequestParam(value = "answered", required = false) Boolean answered,
                              @RequestParam(value = "isForTeacher", required = false) Boolean isForTeacher,
                              @RequestParam(value = "startByAdmin", required = false) Boolean startByAdmin,
                              @RequestParam(value = "section", required = false) String section,
                              @RequestParam(value = "searchInArchive", required = false) Boolean searchInArchive,
                              @RequestParam(value = "finished", required = false) Boolean finished,
                              @RequestParam(value = "finisher", required = false) ObjectId finisher,
                              @RequestParam(value = "studentId", required = false) ObjectId studentId,
                              @RequestParam(value = "id", required = false) ObjectId id
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        ArrayList<String> dates = Utility.checkDatesConstriant(
                sendDateSolar, answerDateSolar,
                sendDateSolarEndLimit, answerDateSolarEndLimit
        );

        if(dates == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);

        return TicketController.getRequests(
                searchInArchive, answered, finished,
                finisher, id, studentId,
                dates.get(0), dates.get(1), dates.get(2), dates.get(3),
                isForTeacher, startByAdmin, section
        );
    }

    @GetMapping(value = "/getRequest/{ticketId}")
    @ResponseBody
    public String getRequest(HttpServletRequest request,
                             @PathVariable @ObjectIdConstraint ObjectId ticketId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return TicketController.getRequest(ticketId);
    }


}