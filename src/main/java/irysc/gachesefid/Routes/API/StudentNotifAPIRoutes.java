package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Notif.NotifController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;

import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/api/notifs")
@Validated
public class StudentNotifAPIRoutes extends Router {

    @GetMapping(value = "get/{id}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return NotifController.getNotif(id, getUser(request));
    }

    @PutMapping(value = "setSeen/{id}")
    @ResponseBody
    public String setSeen(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException, NotActivateAccountException {
        return NotifController.setSeen(id, getUser(request));
    }

    @GetMapping(value = "myNotifs")
    @ResponseBody
    public String myNotifs(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException {
        return NotifController.myNotifs(getUser(request));
    }

}
