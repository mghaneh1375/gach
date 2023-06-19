package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.YesOrNo;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.List;

import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;

@Controller
@RequestMapping(path = "/api/advisor/manage/")
@Validated
public class AdvisorAPIRoutes extends Router {

    @PostMapping(value = "requestMeeting/{studentId}")
    @ResponseBody
    public String requestMeeting(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document advisor = getAdvisorUser(request);
        List<ObjectId> students = advisor.getList("students", ObjectId.class);

        if (!students.contains(studentId))
            return JSON_NOT_ACCESS;

        return AdvisorController.requestMeeting(
                advisor.getObjectId("_id"),
                advisor.getString("NID"),
                advisor.getString("first_name") + " " + advisor.getString("last_name"),
                studentId
        );
    }

    @PostMapping(value = "createNewOffer")
    @ResponseBody
    public String createNewOffer(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {
                                                 "price", "title", "videoCalls",
                                                 "visibility",
                                         },
                                         paramsType = {
                                                 Positive.class, String.class, Positive.class,
                                                 Boolean.class
                                         },
                                         optionals = {
                                                 "description", "maxKarbarg", "maxExam",
                                                 "maxChat"
                                         },
                                         optionalsType = {
                                                 String.class, Positive.class, Positive.class,
                                                 Positive.class
                                         }
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.createNewOffer(getAdvisorUser(request).getObjectId("_id"),
                new JSONObject(jsonStr)
        );
    }


    @PutMapping(value = "updateOffer/{id}")
    @ResponseBody
    public String updateOffer(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId id,
                              @RequestBody @StrongJSONConstraint(
                                      params = {
                                              "price", "title", "videoCalls",
                                              "visibility",
                                      },
                                      paramsType = {
                                              Positive.class, String.class, Positive.class,
                                              Boolean.class
                                      },
                                      optionals = {
                                              "description", "maxKarbarg", "maxExam",
                                              "maxChat"
                                      },
                                      optionalsType = {
                                              String.class, Positive.class, Positive.class,
                                              Positive.class
                                      }
                              ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdvisorUser(request);
        return AdvisorController.updateOffer(id, new JSONObject(jsonStr));
    }

    @GetMapping(value = {"getOffers/{advisorId}", "getOffers"})
    @ResponseBody
    public String getOffers(HttpServletRequest request,
                            @PathVariable(required = false) ObjectId advisorId
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        Document user = getUser(request);
        boolean isAdvisor = Authorization.isAdvisor(user.getList("accesses", String.class));
        return AdvisorController.getOffers(isAdvisor ? user.getObjectId("_id") : null, advisorId);
    }

    @PostMapping(value = "toggleStdAcceptance")
    @ResponseBody
    public String toggleStdAcceptance(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.toggleStdAcceptance(getAdvisorUser(request));
    }

    @PostMapping(value = "answerToRequest/{reqId}/{answer}")
    @ResponseBody
    public String answerToRequest(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId reqId,
                                  @PathVariable @EnumValidator(enumClazz = YesOrNo.class) String answer
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.answerToRequest(getAdvisorUser(request), reqId, answer);
    }

    @DeleteMapping(value = "removeStudents")
    @ResponseBody
    public String removeStudents(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = {JSONArray.class}
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.removeStudents(getAdvisorUser(request),
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @GetMapping(value = "myRequests")
    @ResponseBody
    public String myRequests(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        return AdvisorController.myStudentRequests(getAdvisorUser(request).getObjectId("_id"));
    }

}
