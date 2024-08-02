package irysc.gachesefid.Routes.API;


import irysc.gachesefid.Controllers.CommentController;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/comment/public")
@Validated
public class CommentAPIRoutes extends Router {

    @PostMapping(value = "write/{refId}/{section}")
    @ResponseBody
    public String write(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) @NotBlank String section,
            @RequestBody @StrongJSONConstraint(
                    params = {"comment"},
                    paramsType = {String.class}
            ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return CommentController.writeComment(
                getUser(request).getObjectId("_id"), refId, section,
                new JSONObject(jsonStr).getString("comment")
        );
    }

    @DeleteMapping(value = "removeComment/{commentId}")
    @ResponseBody
    public String removeComment(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return CommentController.removeComment(
                getUser(request).getObjectId("_id"),
                commentId
        );
    }


    @GetMapping(value = "getComments/{refId}/{section}/{pageIndex}")
    @ResponseBody
    public String getComments(
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) String section,
            @PathVariable @Min(0) @Max(1000) Integer pageIndex
    ) {
        return CommentController.getComments(
                refId, section, pageIndex,
                null, false,
                null, null, null
        );
    }

    @GetMapping(value = "getCommentsCount/{refId}/{section}")
    @ResponseBody
    public String getCommentsCount(
            @PathVariable @ObjectIdConstraint ObjectId refId,
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) String section
    ) {
        return CommentController.getCommentsCount(
                refId, section, null,
                false, null, null, null
        );
    }

    @GetMapping(value = "getMyComments/{pageIndex}")
    @ResponseBody
    public String getMyComments(
            HttpServletRequest request,
            @PathVariable @Min(0) @Max(1000) Integer pageIndex
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return CommentController.getMyComments(
                getUser(request).getObjectId("_id"), pageIndex
        );
    }

    @GetMapping(value = "getMyCommentsCount")
    @ResponseBody
    public String getMyCommentsCount(HttpServletRequest request
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return CommentController.getMyCommentsCount(getUser(request).getObjectId("_id"));
    }
}
