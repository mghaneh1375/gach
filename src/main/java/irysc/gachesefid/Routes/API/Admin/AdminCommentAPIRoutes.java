package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.CommentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Controller
@RequestMapping(path = "/api/comment/admin")
@Validated
public class AdminCommentAPIRoutes extends Router {

    @PostMapping(value = "setCommentStatus/{commentId}")
    @ResponseBody
    public String setCommentStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId,
            @RequestParam(value = "status") Boolean status
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return CommentController.setCommentStatus(
                getUser(request).getObjectId("_id"), commentId, status
        );
    }

    @GetMapping(value = "getComments/{refId}/{section}/{pageIndex}")
    @ResponseBody
    public String getComments(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) String section,
            @PathVariable @Min(0) @Max(1000) Integer pageIndex,
            @RequestParam(required = false, value = "status") String status
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommentController.getComments(
                refId, section, pageIndex,
                status, true
        );
    }

    @GetMapping(value = "getCommentsCount/{refId}/{section}")
    @ResponseBody
    public String getCommentsCount(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) String section,
            @RequestParam(required = false, value = "status") String status
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommentController.getCommentsCount(
                refId, section, status, true
        );
    }

    @DeleteMapping(value = "removeComment/{commentId}")
    @ResponseBody
    public String removeComment(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommentController.removeComment(
                null, commentId
        );
    }

}
