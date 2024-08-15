package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Controllers.Advisor.StudentAdviceController;
import irysc.gachesefid.Exception.*;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import java.io.ByteArrayInputStream;
import java.io.File;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;


@Controller
@RequestMapping(path = "/api/advisor/public/")
@Validated
public class StudentAdviceRoutes extends Router {

    @GetMapping(value = "/getMyAdvisors")
    @ResponseBody
    public String getMyAdvisors(HttpServletRequest request
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return StudentAdviceController.getMyAdvisors(getUser(request));
    }

    @PostMapping(value = "payAdvisorPrice/{advisorId}")
    @ResponseBody
    public String payAdvisorPrice(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId advisorId,
                                  @RequestBody(required = false) @StrongJSONConstraint(
                                          params = {},
                                          paramsType = {},
                                          optionals = {
                                                  "off"
                                          },
                                          optionalsType = {
                                                  String.class
                                          }
                                  ) String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        Document user = getUser(request);
        return StudentAdviceController.payAdvisorPrice(
                user.getObjectId("_id"),
                ((Number) user.get("money")).doubleValue(),
                advisorId,
                jsonStr == null || jsonStr.length() == 0 ?
                        new JSONObject() :
                        new JSONObject(jsonStr)
        );
    }

    @GetMapping(value = "getAllAdvisors")
    @ResponseBody
    public String getAllAdvisors(
            @RequestParam(required = false, value = "minAge") Integer minAge,
            @RequestParam(required = false, value = "maxAge") Integer maxAge,
            @RequestParam(required = false, value = "tag") String tag,
            @RequestParam(required = false, value = "minPrice") Integer minPrice,
            @RequestParam(required = false, value = "maxPrice") Integer maxPrice,
            @RequestParam(required = false, value = "minRate") Integer minRate,
            @RequestParam(required = false, value = "maxRate") Integer maxRate,
            @RequestParam(required = false, value = "returnFilters") Boolean returnFilters,
            @RequestParam(required = false, value = "sortBy") String sortBy
    ) {
        return AdvisorController.getAllAdvisors(minAge, maxAge, tag,
                minPrice, maxPrice, minRate, maxRate, returnFilters, sortBy
        );
    }

    @GetMapping(value = "hasOpenRequest")
    @ResponseBody
    public String hasOpenRequest(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        Document user = getUser(request);
        return AdvisorController.hasOpenRequest(
                user.getObjectId("_id"),
                (Number) user.get("money")
        );
    }

    @DeleteMapping(value = "cancelRequest/{reqId}")
    @ResponseBody
    public String cancelRequest(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId reqId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return StudentAdviceController.cancelRequest(getStudentUser(request).getObjectId("_id"), reqId);
    }

    @DeleteMapping(value = "cancel/{advisorId}")
    @ResponseBody
    public String cancel(HttpServletRequest request,
                         @PathVariable @ObjectIdConstraint ObjectId advisorId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.cancel(getStudentUser(request), advisorId);
    }

    @PostMapping(value = "request/{advisorId}/{planId}")
    @ResponseBody
    public String request(HttpServletRequest request,
                          @PathVariable @ObjectIdConstraint ObjectId advisorId,
                          @PathVariable String planId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return AdvisorController.request(getStudentUser(request), advisorId, planId);
    }

    @GetMapping(value = "myRequests")
    @ResponseBody
    public String myRequests(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return StudentAdviceController.myRequests(getStudentUser(request).getObjectId("_id"));
    }

    @GetMapping(value = {"myLifeStyle", "myLifeStyle/{studentId}"})
    @ResponseBody
    public String myLifeStyle(HttpServletRequest request,
                              @PathVariable(required = false) String studentId
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {
        Document result = getUserWithAdvisorAccess(request, true, studentId);
        return StudentAdviceController.myLifeStyle(result.get("user", Document.class).getObjectId("_id"));
    }

    @PutMapping(value = "setMyExamInLifeStyle")
    @ResponseBody
    public String setMyExamInLifeStyle(HttpServletRequest request,
                                       @RequestBody @StrongJSONConstraint(
                                               params = {
                                                       "exams"
                                               },
                                               paramsType = {
                                                       JSONArray.class
                                               }
                                       ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return StudentAdviceController.setMyExamInLifeStyle(getStudentUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getJSONArray("exams")
        );
    }

    @PutMapping(value = "addItemToMyLifeStyle")
    @ResponseBody
    public String addItemToMyLifeStyle(HttpServletRequest request,
                                       @RequestBody @StrongJSONConstraint(
                                               params = {
                                                       "tag", "duration",
                                                       "day"
                                               },
                                               paramsType = {
                                                       ObjectId.class, Positive.class,
                                                       String.class
                                               },
                                               optionals = {
                                                       "startAt"
                                               },
                                               optionalsType = {
                                                       String.class
                                               }
                                       ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return StudentAdviceController.addItemToMyLifeStyle(getStudentUser(request).getObjectId("_id"), new JSONObject(jsonStr));
    }


    @DeleteMapping(value = "removeItemFromMyLifeStyle")
    @ResponseBody
    public String removeItemFromMyLifeStyle(HttpServletRequest request,
                                            @RequestBody @StrongJSONConstraint(
                                                    params = {
                                                            "tag", "day"
                                                    },
                                                    paramsType = {
                                                            String.class, String.class
                                                    }
                                            ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {
        return StudentAdviceController.removeItemFromMyLifeStyle(getStudentUser(request).getObjectId("_id"), new JSONObject(jsonStr));
    }


    @PutMapping(value = "rate/{advisorId}")
    @ResponseBody
    public String rate(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId advisorId,
                       @RequestBody @StrongJSONConstraint(
                               params = {"rate"},
                               paramsType = {Positive.class}
                       ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, NotAccessException {

        Document user = getStudentUser(request);
        if (!user.containsKey("my_advisors"))
            return JSON_NOT_ACCESS;

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        int rate = jsonObject.getInt("rate");
        if (rate < 1 || rate > 5)
            return JSON_NOT_VALID_PARAMS;

        if(!user.getList("my_advisors", ObjectId.class).contains(advisorId))
            return JSON_NOT_ACCESS;

        return StudentAdviceController.rate(
                user.getObjectId("_id"), advisorId,
                rate
        );
    }


    @GetMapping(value = "getMySchedules")
    @ResponseBody
    public String getMySchedules(HttpServletRequest request,
                                      @RequestParam(value = "notReturnPassed", required = false) Boolean notReturnPassed
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return AdvisorController.getStudentSchedules(
                null, getUser(request).getObjectId("_id"), notReturnPassed
        );
    }

    @GetMapping(value = "getMySchedulesDigest")
    @ResponseBody
    public String getMySchedulesDigest(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return AdvisorController.getStudentSchedulesDigest(
                getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "getMyLessonsInSchedule/{id}")
    @ResponseBody
    public String getMyLessonsInSchedule(HttpServletRequest request,
                                         @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        ObjectId studentId = getUser(request).getObjectId("_id");

        return AdvisorController.lessonsInSchedule(
                studentId, id, false
        );
    }


    @GetMapping(value = "getMyCurrentRoom")
    @ResponseBody
    public String getMyCurrentRoom(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentAdviceController.getMyCurrentRoom(getStudentUser(request).getObjectId("_id"));
    }

    @GetMapping(value = "getAdvisorTags")
    @ResponseBody
    public String getAdvisorTags() {
        return generateSuccessMsg("data", userRepository.distinctTagsWithFilter(
                and(
                        eq("accesses", "advisor"),
                        exists("tags")
                ), "tags")
        );
    }

    @GetMapping(value = "exportPDF/{id}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> exportPDF(HttpServletRequest request,
                                                         @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        boolean isAdvisor = Authorization.isAdvisor(user.getList("accesses", String.class));
        ObjectId userId = user.getObjectId("_id");

        File f = AdvisorController.exportPDF(id, isAdvisor ? userId : null, isAdvisor ? null : userId);
        if (f == null)
            return null;

        try {
            InputStreamResource file = new InputStreamResource(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(f))
            );

            f.delete();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_2.pdf")
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(file);
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return null;
    }

    @PostMapping(value = "setDoneTime/{id}/{itemId}")
    @ResponseBody
    public String setDoneTime(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId id,
                              @PathVariable @ObjectIdConstraint ObjectId itemId,
                              @RequestBody @StrongJSONConstraint(
                                      params = {
                                              "fullDone",
                                      },
                                      paramsType = {
                                              Boolean.class
                                      },
                                      optionals = {
                                            "duration", "additional"
                                      },
                                      optionalsType = {
                                              Positive.class, Positive.class
                                      }
                              ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        return StudentAdviceController.setDoneTime(
                getUser(request).getObjectId("_id"), id, itemId,
                Utility.convertPersian(new JSONObject(jsonStr))
        );
    }

    @PostMapping(value = "notifyAdvisorForSchedule/{id}")
    @ResponseBody
    public String notifyAdvisorForSchedule(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {

        Document user = getUser(request);

        return StudentAdviceController.notifyAdvisorForSchedule(
                id,
                user.getString("first_name") + " " + user.getString("last_name"),
                user.getObjectId("_id")
        );
    }

}
