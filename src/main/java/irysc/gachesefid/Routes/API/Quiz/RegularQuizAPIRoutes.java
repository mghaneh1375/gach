package irysc.gachesefid.Routes.API.Quiz;

import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Utility.StaticValues.*;

@Controller
@RequestMapping(path="/api/regularQuiz")
@Validated
public class RegularQuizAPIRoutes extends Router {

}
