package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.Level.LevelController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Routes.Router;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

public class LevelAPIRoutes extends Router {

    @GetMapping(value = "getMyCurrLevel")
    @ResponseBody
    public String getMyCurrLevel(
            HttpServletRequest request
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return LevelController.getMyCurrLevel(getUser(request).getObjectId("_id"));
    }

}
