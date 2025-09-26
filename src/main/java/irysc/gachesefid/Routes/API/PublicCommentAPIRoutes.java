package irysc.gachesefid.Routes.API;

import irysc.gachesefid.Controllers.CommentController;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Controller
@RequestMapping(path = "/api/comment/public")
@Validated
public class PublicCommentAPIRoutes extends Router {

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

    @GetMapping(value = "getTopComments/{section}")
    @ResponseBody
    public String getTopComments(
            @PathVariable @EnumValidator(enumClazz = CommentSection.class) String section
    ) {
        return CommentController.getTopComments(section);
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

    @GetMapping(value = "getTeacherMarkedComments/{teacherId}")
    @ResponseBody
    public String getTeacherMarkedComments(
            @PathVariable @ObjectIdConstraint ObjectId teacherId
    ) {
        return CommentController.getTeacherMarkedComments(
                teacherId
        );
    }
}
