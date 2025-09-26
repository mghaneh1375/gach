package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.CommentController;
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
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/comment/manage")
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
    ) throws UnAuthException {
        return CommentController.writeComment(
                getUserId(request), refId, section,
                new JSONObject(jsonStr).getString("comment")
        );
    }

    @DeleteMapping(value = "removeComment/{commentId}")
    @ResponseBody
    public String removeComment(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId commentId
    ) throws UnAuthException {
        return CommentController.removeComment(
                getUserId(request),
                commentId
        );
    }

    @GetMapping(value = "getMyComments")
    @ResponseBody
    public String getMyComments(
            HttpServletRequest request,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "status") String status,
            @RequestParam(required = false, value = "section") String section
    ) throws UnAuthException {
        return CommentController.getMyComments(
                getUserId(request),
                section, from, to, status
        );
    }

    @GetMapping(value = "getCommentsAboutMe")
    @ResponseBody
    public String getCommentsAboutMe(
            HttpServletRequest request,
            @RequestParam(required = false, value = "from") Long from,
            @RequestParam(required = false, value = "to") Long to,
            @RequestParam(required = false, value = "section") String section
    ) throws UnAuthException {
        return CommentController.getCommentsAboutMe(
                getUserId(request),
                section, from, to
        );
    }

    @PutMapping(value = "toggleCommentMarkedStatus/{id}")
    @ResponseBody
    public String toggleCommentMarkedStatus(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException {
        return CommentController.toggleCommentMarkedStatus(
                getUserId(request), id
        );
    }
}
