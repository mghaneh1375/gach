package irysc.gachesefid.Routes.API.QuestionReport;

import irysc.gachesefid.Controllers.QuestionReport.QuestionReportController;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;


@Controller
@RequestMapping(path = "/api/questionReport/manage")
@Validated
public class AdminQuestionReportAPIRoutes extends Router {

    @GetMapping(value = "getReports/{tagId}")
    @ResponseBody
    public String getReports(@PathVariable @ObjectIdConstraint ObjectId tagId
    ) {
        return QuestionReportController.getReports(tagId);
    }

    @GetMapping(value = "getAllTags")
    @ResponseBody
    public String getAllTags() {
        return QuestionReportController.getAllTags(true);
    }

    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(@RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) String jsonStr
    ) {
        return QuestionReportController.remove(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PutMapping(value = "setSeen/{tagId}")
    @ResponseBody
    public String setSeen(@PathVariable @ObjectIdConstraint ObjectId tagId,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"items"},
                                  paramsType = {JSONArray.class}
                          ) String jsonStr
    ) {
        return QuestionReportController.setSeen(tagId, new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(value = "addTag")
    @ResponseBody
    public String addTag(@RequestBody @StrongJSONConstraint(
                                 params = {"label", "priority", "visibility", "canHasDesc"},
                                 paramsType = {
                                         String.class, Positive.class, Boolean.class, Boolean.class
                                 }
                         ) @NotBlank String jsonStr
    ) {
        return QuestionReportController.create(Utility.convertPersian(new JSONObject(jsonStr)));
    }

    @PostMapping(value = "editTag/{id}")
    @ResponseBody
    public String editTag(@PathVariable @ObjectIdConstraint ObjectId id,
                          @RequestBody @StrongJSONConstraint(
                                  params = {"label", "priority", "canHasDesc", "visibility"},
                                  paramsType = {
                                          String.class, Positive.class, Boolean.class, Boolean.class
                                  }
                          ) @NotBlank String jsonStr
    ) {
        return QuestionReportController.edit(id, Utility.convertPersian(new JSONObject(jsonStr)));
    }

}
