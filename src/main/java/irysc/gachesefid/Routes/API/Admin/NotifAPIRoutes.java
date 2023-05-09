package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.Notif.NotifController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.NotifVia;
import irysc.gachesefid.Models.Sex;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;

@Controller
@RequestMapping(path = "/api/notifs/manage")
@Validated
public class NotifAPIRoutes extends Router {

    @GetMapping(value = "getAll")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @RequestParam(value = "sendVia") @EnumValidator(enumClazz = NotifVia.class) String sendVia,
                         @RequestParam(required = false, value = "from") Long from,
                         @RequestParam(required = false, value = "to") Long to
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.getAll(sendVia, from, to);
    }


    @GetMapping(value = "getStudents/{id}")
    @ResponseBody
    public String getStudents(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.getStudents(id);
    }

    @GetMapping(value = "get/{id}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.getNotif(id, null);
    }


    @DeleteMapping(value = "remove")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.remove(new JSONObject(jsonStr).getJSONArray("items"));
    }

    @PostMapping(value = "store")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @RequestPart(value = "file", required = false) MultipartFile file,
                        @RequestPart(value = "json") @StrongJSONConstraint(
                                params = {"via", "title", "text", "sendSMS", "sendMail"},
                                paramsType = {
                                        NotifVia.class, String.class, String.class,
                                        Boolean.class, Boolean.class
                                },
                                optionals = {
                                        "cities", "states", "nids", "phones",
                                        "branches", "sex", "schools", "grades",
                                        "quizzes", "packages", "accesses",
                                        "minCoin", "maxCoin", "minMoney",
                                        "maxMoney", "minRank", "maxRank"
                                },
                                optionalsType = {
                                        JSONArray.class, JSONArray.class,
                                        JSONArray.class, JSONArray.class,
                                        JSONArray.class, Sex.class, JSONArray.class,
                                        JSONArray.class, JSONArray.class, JSONArray.class,
                                        JSONArray.class, Number.class, Number.class,
                                        Positive.class, Positive.class, Positive.class,
                                        Positive.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.store(new JSONObject(jsonStr), file);
    }

    @PostMapping(value = "simpleStore")
    @ResponseBody
    public String simpleStore(HttpServletRequest request,
                              @RequestBody @StrongJSONConstraint(
                                      params = {"via", "title", "text", "sendSMS", "sendMail"},
                                      paramsType = {
                                              NotifVia.class, String.class, String.class,
                                              Boolean.class, Boolean.class
                                      },
                                      optionals = {
                                              "cities", "states", "nids", "phones",
                                              "branches", "sex", "schools", "grades",
                                              "quizzes", "packages", "accesses",
                                              "minCoin", "maxCoin", "minMoney",
                                              "maxMoney", "minRank", "maxRank"
                                      },
                                      optionalsType = {
                                              JSONArray.class, JSONArray.class,
                                              JSONArray.class, JSONArray.class,
                                              JSONArray.class, Sex.class, JSONArray.class,
                                              JSONArray.class, JSONArray.class, JSONArray.class,
                                              JSONArray.class, Number.class, Number.class,
                                              Positive.class, Positive.class, Positive.class,
                                              Positive.class
                                      }
                              ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return NotifController.store(new JSONObject(jsonStr), null);
    }

}
