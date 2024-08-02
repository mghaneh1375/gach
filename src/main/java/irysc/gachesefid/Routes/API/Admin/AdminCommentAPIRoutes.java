package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.CommentController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
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

    @PutMapping(value = "setCommentStatus/{commentId}")
    @ResponseBody
    public String setCommentStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId,
            @RequestParam(value = "status") Boolean status
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        return CommentController.setCommentStatus(
                getEditorPrivilegeUser(request).getObjectId("_id"), commentId, status
        );
    }

    @PutMapping(value = "toggleTopStatus/{commentId}")
    @ResponseBody
    public String toggleTopStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getEditorPrivilegeUserVoid(request);
        return CommentController.toggleTopStatus(commentId);
    }

    @GetMapping(value = "getComments")
    @ResponseBody
    public String getComments(
            HttpServletRequest request,
            @RequestParam(required = false, value = "refId") ObjectId refId,
            @RequestParam(required = false, value = "section") String section,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "status") String status,
            @RequestParam(required = false, value = "justTop") Boolean justTop,
            @RequestParam(value = "pageIndex") @Min(0) @Max(1000) Integer pageIndex
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommentController.getComments(
                refId, section, pageIndex,
                status, true, from, to, justTop
        );
    }

    @GetMapping(value = "getCommentsCount")
    @ResponseBody
    public String getCommentsCount(
            HttpServletRequest request,
            @RequestParam(required = false, value = "refId") ObjectId refId,
            @RequestParam(required = false, value = "section") String section,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "status") String status,
            @RequestParam(required = false, value = "justTop") Boolean justTop
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return CommentController.getCommentsCount(
                refId, section, status, true,
                from, to, justTop
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
