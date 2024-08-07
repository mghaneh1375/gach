package irysc.gachesefid.Routes.API.Advice;

import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Controllers.Advisor.StudentAdviceController;
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
import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.convertPersian;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.Utility.*;

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

        if (!Authorization.hasAccessToThisStudent(studentId, advisor.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        return AdvisorController.requestMeeting(
                advisor.getObjectId("_id"),
                advisor.getString("NID"),
                advisor.getString("first_name") + " " + advisor.getString("last_name"),
                studentId
        );
    }

    @GetMapping(value = "getMyCurrentRoomForAdvisor")
    @ResponseBody
    public String getMyCurrentRoomForAdvisor(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.getMyCurrentRoomForAdvisor(getAdvisorUser(request).getObjectId("_id"));
    }

    @GetMapping(value = "getMyCurrentRoomForAdvisorForSpecificStudent/{studentId}")
    @ResponseBody
    public String getMyCurrentRoomForAdvisor(HttpServletRequest request,
                                             @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document advisor = getAdvisorUser(request);

        if (!Authorization.hasAccessToThisStudent(studentId, advisor.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        return AdvisorController.getMyCurrentRoomForAdvisor(
                advisor.getObjectId("_id"), studentId
        );
    }

    @GetMapping(value = "getStudentDigest/{studentId}")
    @ResponseBody
    public String getStudentDigest(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document advisor = getAdvisorUser(request);
        if (!Authorization.hasAccessToThisStudent(studentId, advisor.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        return AdvisorController.getStudentDigest(advisor.getObjectId("_id"), studentId);
    }

    @GetMapping(value = "getStudentsDigest")
    @ResponseBody
    public String getStudentsDigest(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document advisor = getAdvisorUser(request);
        return AdvisorController.getStudentsDigest(advisor);
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
                                                 "maxChat", "videoLink"
                                         },
                                         optionalsType = {
                                                 String.class, Positive.class, Positive.class,
                                                 Positive.class, String.class
                                         }
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.createNewOffer(getAdvisorUser(request).getObjectId("_id"),
                convertPersian(new JSONObject(jsonStr))
        );
    }

    @DeleteMapping(value = "removeOffers")
    @ResponseBody
    public String removeOffers(HttpServletRequest request,
                               @RequestBody @StrongJSONConstraint(
                                       params = {
                                               "items"
                                       },
                                       paramsType = {
                                               JSONArray.class
                                       }
                               ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.removeOffers(getAdvisorUser(request).getObjectId("_id"),
                new JSONObject(jsonStr).getJSONArray("items")
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
                                              "maxChat", "videoLink"
                                      },
                                      optionalsType = {
                                              String.class, Positive.class, Positive.class,
                                              Positive.class, String.class
                                      }
                              ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdvisorUser(request);
        return AdvisorController.updateOffer(id, convertPersian(new JSONObject(jsonStr)));
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

    @PostMapping(value = "copy/{scheduleId}")
    @ResponseBody
    public String copy(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId scheduleId,
                       @RequestBody @StrongJSONConstraint(
                               params = {
                                       "scheduleFor", "users"
                               },
                               paramsType = {
                                       Positive.class, JSONArray.class,

                               }
                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));

        return AdvisorController.copy(
                getAdvisorUser(request).getObjectId("_id"),
                scheduleId, jsonObject.getJSONArray("users"),
                jsonObject.getInt("scheduleFor")
        );

    }


    @PutMapping(value = "setScheduleDesc/{id}")
    @ResponseBody
    public String setScheduleDesc(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId id,
                                  @RequestBody @StrongJSONConstraint(
                                          params = {"description"},
                                          paramsType = {String.class}
                                  ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.setScheduleDesc(
                getAdvisorUser(request).getObjectId("_id"), id,
                new JSONObject(jsonStr).getString("description")
        );
    }

    @PutMapping(value = "/updateScheduleItem/{id}")
    @ResponseBody
    public String updateScheduleItem(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId id,
                                     @RequestBody @StrongJSONConstraint(
                                             params = {
                                                     "tag", "duration"
                                             },
                                             paramsType = {
                                                     ObjectId.class, Positive.class
                                             },
                                             optionals = {
                                                     "startAt", "description",
                                                     "additional"
                                             },
                                             optionalsType = {
                                                     String.class, String.class,
                                                     Positive.class
                                             }
                                     ) @NotBlank String jsonStr) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.updateScheduleItem(
                getAdvisorUser(request).getObjectId("_id"),
                id, convertPersian(new JSONObject(jsonStr))
        );
    }

    @PutMapping(value = "addItemToSchedule/{userId}")
    @ResponseBody
    public String addItemToSchedule(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId userId,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {
                                                    "tag", "duration",
                                                    "day", "lessonId",
                                            },
                                            paramsType = {
                                                    ObjectId.class, Positive.class,
                                                    String.class, ObjectId.class
                                            },
                                            optionals = {
                                                    "startAt", "description",
                                                    "scheduleFor", "id",
                                                    "additional"
                                            },
                                            optionalsType = {
                                                    String.class, String.class,
                                                    Integer.class, ObjectId.class,
                                                    Positive.class
                                            }
                                    ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
        return AdvisorController.addItemToSchedule(
                getAdvisorUser(request).getObjectId("_id"),
                userId,
                convertPersian(new JSONObject(jsonStr))
        );
    }

    @DeleteMapping(value = "removeItemFromSchedule/{userId}/{id}")
    @ResponseBody
    public String removeItemFromSchedule(HttpServletRequest request,
                                         @PathVariable @ObjectIdConstraint ObjectId userId,
                                         @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.removeItemFromSchedule(
                getAdvisorUser(request).getObjectId("_id"),
                userId, id
        );
    }

    @DeleteMapping(value = "removeSchedule/{id}")
    @ResponseBody
    public String removeSchedule(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return AdvisorController.removeSchedule(
                getAdvisorUser(request).getObjectId("_id"),
                id
        );
    }

//    @PutMapping(value = "updateItem/{userId}/{id}")
//    @ResponseBody
//    public String updateItem(HttpServletRequest request,
//                             @PathVariable @ObjectIdConstraint ObjectId userId,
//                             @PathVariable @ObjectIdConstraint ObjectId id,
//                             @RequestBody @StrongJSONConstraint(
//                                     params = {
//                                             "tag", "duration",
//                                             "subjectId",
//                                     },
//                                     paramsType = {
//                                             ObjectId.class, Positive.class,
//                                             ObjectId.class
//                                     },
//                                     optionals = {
//                                             "startAt", "description"
//                                     },
//                                     optionalsType = {
//                                             String.class, String.class
//                                     }
//                             ) @NotBlank String jsonStr
//    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
//        return AdvisorController.updateItem(
//                getAdvisorUser(request).getObjectId("_id"),
//                userId, id, convertPersian(new JSONObject(jsonStr))
//        );
//    }


    @GetMapping(value = "getStudentSchedules/{userId}")
    @ResponseBody
    public String getStudentSchedules(HttpServletRequest request,
                                      @PathVariable @ObjectIdConstraint ObjectId userId,
                                      @RequestParam(value = "notReturnPassed", required = false) Boolean notReturnPassed
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        ObjectId advisorId = getAdvisorUser(request).getObjectId("_id");

        if (!Authorization.hasAccessToThisStudent(userId, advisorId))
            return JSON_NOT_ACCESS;

        return AdvisorController.getStudentSchedules(
                advisorId, userId, notReturnPassed
        );
    }


    @GetMapping(value = "getStudentSchedulesDigest/{userId}")
    @ResponseBody
    public String getStudentSchedulesDigest(HttpServletRequest request,
                                            @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        ObjectId advisorId = getAdvisorUser(request).getObjectId("_id");

        if (!Authorization.hasAccessToThisStudent(userId, advisorId))
            return JSON_NOT_ACCESS;

        return AdvisorController.getStudentSchedulesDigest(userId);
    }

    @GetMapping(value = "getAdvisorTags/{id}")
    @ResponseBody
    public String getAdvisorTags(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestParam(value = "mode") String mode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);
        Document advisor = userRepository.findById(id);

        if (advisor == null)
            return JSON_NOT_VALID_ID;

        List<String> tags = (List<String>) advisor.getOrDefault(
                mode.equals("teach") ? "teach_tags" : "tags",
                new ArrayList<Document>()
        );
        JSONArray jsonArray = new JSONArray();

        for (String tag : tags)
            jsonArray.put(tag);

        return generateSuccessMsg("data", jsonArray);
    }


    @PutMapping(value = "addAdvisorTag/{id}")
    @ResponseBody
    public String addAdvisorTag(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestParam(value = "mode") String mode,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"tags"},
                                        paramsType = {JSONArray.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);
        Document advisor = userRepository.findById(id);

        if (advisor == null)
            return JSON_NOT_VALID_ID;

        List<String> tags = new ArrayList<>();
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("tags");

        for (int i = 0; i < jsonArray.length(); i++) {
            String tag = jsonArray.getString(i);
            if (tags.contains(tag))
                continue;
            tags.add(tag);
        }

        advisor.put(mode.equals("teach") ? "teach_tags" : "tags", tags);
        userRepository.replaceOne(id, advisor);
        return JSON_OK;
    }

    @DeleteMapping(value = "removeAdvisorTag/{id}")
    @ResponseBody
    public String removeAdvisorTag(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestParam(value = "mode") String mode,
            @RequestBody @StrongJSONConstraint(
                    params = {"tag"},
                    paramsType = {String.class}
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        Document advisor = userRepository.findById(id);

        if (advisor == null)
            return JSON_NOT_VALID_ID;

        List<String> tags = (List<String>) advisor.getOrDefault(
                mode.equals("teach") ? "teach_tags" : "tags",
                new ArrayList<Document>()
        );
        String tag = new JSONObject(jsonStr).getString("tag");

        tags.remove(tag);
        advisor.put(mode.equals("teach") ? "teach_tags" : "tags", tags);

        userRepository.replaceOne(id, advisor);
        return JSON_OK;
    }

    @GetMapping(value = "lessonsInSchedule/{id}")
    @ResponseBody
    public String lessonsInSchedule(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        ObjectId advisorId = getAdvisorUser(request).getObjectId("_id");

        return AdvisorController.lessonsInSchedule(
                advisorId, id, true
        );
    }

    @GetMapping(value = "progress/{userId}")
    @ResponseBody
    public String progress(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId userId,
                           @RequestParam(required = false, value = "start") Long start,
                           @RequestParam(required = false, value = "end") Long end
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        ObjectId advisorId = getAdvisorUser(request).getObjectId("_id");

        if (!Authorization.hasAccessToThisStudent(userId, advisorId))
            return JSON_NOT_ACCESS;

        return AdvisorController.progress(
                userId, null, start, end
        );
    }


    @GetMapping(value = {"getStudentSchedule/{userId}/{scheduleFor}", "getStudentSchedule/{id}"})
    @ResponseBody
    public String getStudentSchedules(HttpServletRequest request,
                                      @PathVariable(required = false) String userId,
                                      @PathVariable(required = false) Integer scheduleFor,
                                      @PathVariable(required = false) String id
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if (
                ((userId == null) != (scheduleFor == null)) ||
                        ((userId == null) == (id == null))
        )
            return JSON_NOT_VALID_PARAMS;

        if (userId != null && !ObjectId.isValid(userId))
            return JSON_NOT_VALID_PARAMS;

        if (id != null && !ObjectId.isValid(id))
            return JSON_NOT_VALID_PARAMS;

        if (scheduleFor != null && (scheduleFor < 0 || scheduleFor > 4))
            return JSON_NOT_VALID_PARAMS;

        Document user = getUser(request);

        boolean isAdvisor = Authorization.isAdvisor(user.getList("accesses", String.class));
        ObjectId advisorId = null;

        if (isAdvisor) {
            advisorId = user.getObjectId("_id");
            if (userId != null && !Authorization.hasAccessToThisStudent(new ObjectId(userId), advisorId))
                return JSON_NOT_ACCESS;
        } else
            userId = user.getObjectId("_id").toString();

        return StudentAdviceController.mySchedule(
                advisorId, userId != null ? new ObjectId(userId) : null,
                scheduleFor, id != null ? new ObjectId(id) : null
        );
    }


    @PostMapping(value = "notifyStudentForSchedule/{id}")
    @ResponseBody
    public String notifyStudentForSchedule(HttpServletRequest request,
                                           @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getAdvisorUser(request);

        return AdvisorController.notifyStudentForSchedule(
                id,
                user.getString("first_name") + " " + user.getString("last_name"),
                user.getObjectId("_id")
        );
    }

}
