package irysc.gachesefid.Routes.API.Ticket;

import irysc.gachesefid.Controllers.Ticket.TicketController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.JSONConstraint;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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


}
