package irysc.gachesefid.Routes.API.Quiz;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Quiz.*;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.*;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.EnumValidatorImp;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.io.File;

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.generateErr;


@Controller
@RequestMapping(path = "/api/quiz/manage")
@Validated
public class QuizAPIRoutes extends Router {

    private boolean isIRYSCQuiz(String mode) {
        return mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName());
    }

    private Common selectDB(String mode) {
        return mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                openQuizRepository :
                mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()) ?
                        onlineStandQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) ?
                                escapeQuizRepository :
                                mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()) ?
                                        contentQuizRepository : iryscQuizRepository;
    }

    @PostMapping(value = "calQuestionsAgain/{quizId}")
    @ResponseBody
    public void calQuestionsAgain(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        QuizController.calcQuestionsAgain(quizId);
    }

    @PostMapping(value = "/createFromIRYSCQuiz/{quizId}")
    @ResponseBody
    public String createFromIRYSCQuiz(HttpServletRequest request,
                                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return OpenQuizController.createFromIRYSCQuiz(quizId);
    }

    @PostMapping(value = "/copy/{mode}/{quizId}")
    @ResponseBody
    public String copy(
            HttpServletRequest request,
            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
            @PathVariable @ObjectIdConstraint ObjectId quizId,
            @RequestBody @StrongJSONConstraint(
                    params = {"title"},
                    paramsType = {String.class},
                    optionals = {
                            "start", "end", "startRegistry", "endRegistry",
                            "description"
                    },
                    optionalsType = {
                            Long.class, Long.class, Long.class, Long.class,
                            String.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        if (!mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName())
        )
            return JSON_NOT_ACCESS;

        Document user = getAdminPrivilegeUser(request);
        if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return OpenQuizController.copy(user.getObjectId("_id"), quizId, new JSONObject(jsonStr));

        return RegularQuizController.copy(
                user.getObjectId("_id"), quizId, new JSONObject(jsonStr)
        );
    }

    @PostMapping(value = "/store/{mode}")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) @NotBlank String mode,
                        @RequestBody @StrongJSONConstraint(
                                params = {
                                        "title"
                                },
                                paramsType = {
                                        String.class
                                },
                                optionals = {
                                        "price", "permute",
                                        "description", "startRegistry",
                                        "endRegistry", "start",
                                        "end", "tags", "launchMode",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrection",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperTheme", "database",
                                        "descAfter", "desc",
                                        "duration", // duration is in min format
                                        "showResultsAfterCorrectionNotLoginUsers",
                                        "isRegistrable", "isUploadable",
                                        "kind", "isQRNeeded", "priority",
                                        "payByStudent", "perTeam", "maxTeams",
                                        "maxTry", "shouldComplete"
                                },
                                optionalsType = {
                                        Positive.class, Boolean.class,
                                        String.class, Long.class,
                                        Long.class, Long.class,
                                        Long.class, JSONArray.class,
                                        LaunchMode.class, Positive.class,
                                        Boolean.class, Boolean.class,
                                        Boolean.class, Number.class,
                                        Positive.class, Positive.class,
                                        String.class, Boolean.class,
                                        String.class, String.class,
                                        Positive.class, Boolean.class,
                                        Boolean.class, Boolean.class,
                                        String.class, Boolean.class,
                                        Positive.class, Boolean.class,
                                        Positive.class, Positive.class,
                                        Positive.class, Boolean.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        if (jsonObject.has("kind") &&
                !EnumValidatorImp.isValid(jsonObject.getString("kind"), KindQuiz.class) &&
                !jsonObject.getString("kind").equalsIgnoreCase("regularWithPDF")
        )
            return JSON_NOT_VALID_PARAMS;

        Document user = getQuizUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin) {

            if (mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
                return OnlineStandingController.create(user.getObjectId("_id"), jsonObject);

            if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
                return EscapeQuizController.create(user.getObjectId("_id"), jsonObject);

            if (mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName())) {

                if (jsonObject.has("kind") &&
                        jsonObject.getString("kind").equalsIgnoreCase(KindQuiz.TASHRIHI.getName())
                )
                    return TashrihiQuizController.create(
                            user.getObjectId("_id"),
                            jsonObject, mode
                    );

                return RegularQuizController.create(
                        user.getObjectId("_id"),
                        jsonObject, mode, false,
                        jsonObject.has("kind") && jsonObject.getString("kind").equalsIgnoreCase("regularWithPDF")
                );

            }


            if (mode.equals(AllKindQuiz.OPEN.getName()))
                return OpenQuizController.create(user.getObjectId("_id"),
                        jsonObject
                );

            if (mode.equals(AllKindQuiz.CONTENT.getName()))
                return ContentQuizController.create(user.getObjectId("_id"),
                        jsonObject
                );

        }

        if (mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName())) {
            return RegularQuizController.create(
                    user.getObjectId("_id"),
                    jsonObject, mode,
                    !isAdmin && Authorization.isAdvisor(user.getList("accesses", String.class)),
                    jsonObject.has("kind") && jsonObject.getString("kind").equalsIgnoreCase("regularWithPDF")
            );
        }

        return JSON_NOT_VALID_PARAMS;
    }

    @PutMapping(value = "setPDFQuizQuestions/{quizId}/{qNo}")
    @ResponseBody
    public String setPDFQuizQuestions(HttpServletRequest request,
                                      @PathVariable @ObjectIdConstraint ObjectId quizId,
                                      @PathVariable @Min(1) @Max(100) int qNo,
                                      @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.setPDFQuizQuestions(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, qNo, file,
                isAdmin ? null : user.getObjectId("_id")
        );
    }

    @PutMapping(value = "setPDFQuizInfo/{quizId}")
    @ResponseBody
    public String setPDFQuizInfo(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"info"},
                                         paramsType = {JSONArray.class}
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.setPDFQuizInfo(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id"), new JSONObject(jsonStr).getJSONArray("info")
        );
    }

    @GetMapping(value = "getGradesAndBranches/{quizId}")
    @ResponseBody
    public String getGradesAndBranches(HttpServletRequest request,
                                       @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.getGradesAndBranches(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @GetMapping(value = "getPDFQuizInfo/{quizId}")
    @ResponseBody
    public String getPDFQuizInfo(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.getPDFQuizInfo(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @PutMapping(value = "setPDFQuizSubjectsAndChoicesCount/{quizId}")
    @ResponseBody
    public String setPDFQuizSubjectsAndChoicesCount(HttpServletRequest request,
                                                    @PathVariable @ObjectIdConstraint ObjectId quizId,
                                                    @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.setPDFQuizSubjectsAndChoicesCount(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, file, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @GetMapping(value = "getPDFQuizAnswerSheet/{quizId}")
    @ResponseBody
    public String getPDFQuizAnswerSheet(HttpServletRequest request,
                                        @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.getPDFQuizAnswerSheet(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @PutMapping(value = "setPDFQuizAnswerSheet/{quizId}")
    @ResponseBody
    public String setPDFQuizAnswerSheet(HttpServletRequest request,
                                        @PathVariable @ObjectIdConstraint ObjectId quizId,
                                        @RequestBody @StrongJSONConstraint(
                                                params = {
                                                        "answers"
                                                },
                                                paramsType = {
                                                        JSONArray.class
                                                }
                                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.setPDFQuizAnswerSheet(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id"),
                new JSONObject(jsonStr).getJSONArray("answers")
        );
    }

    @GetMapping(value = "getPDFQuizQuestions/{quizId}")
    @ResponseBody
    public String getQuizQuestions(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.getPDFQuizQuestions(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @GetMapping(value = "getPDFQuizSubjects/{quizId}")
    @ResponseBody
    public String getPDFQuizSubjects(HttpServletRequest request,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.getPDFQuizSubjects(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id")
        );
    }

    @PostMapping(value = "/edit/{mode}/{quizId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) @NotBlank String mode,
                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                       @RequestBody @StrongJSONConstraint(
                               params = {},
                               paramsType = {},
                               optionals = {
                                       "title", "price", "permute",
                                       "description", "startRegistry",
                                       "endRegistry", "start",
                                       "end", "tags", "launchMode",
                                       "capacity", "minusMark",
                                       "backEn", "showResultsAfterCorrection",
                                       "topStudentsGiftCoin",
                                       "topStudentsGiftMoney",
                                       "topStudentsCount",
                                       "paperTheme", "database",
                                       "descAfter", "desc",
                                       "duration", // duration is in min format
                                       "visibility",
                                       "showResultsAfterCorrectionNotLoginUsers",
                                       "isRegistrable", "isUploadable",
                                       "kind", "isQRNeeded",
                                       "priority", "payByStudent",
                                       "perTeam", "maxTeams",
                                       "maxTry", "shouldComplete"
                               },
                               optionalsType = {
                                       String.class, Positive.class, Boolean.class,
                                       String.class, Long.class,
                                       Long.class, Long.class,
                                       Long.class, JSONArray.class,
                                       LaunchMode.class, Positive.class,
                                       Boolean.class, Boolean.class,
                                       Boolean.class, Number.class,
                                       Positive.class, Positive.class,
                                       String.class, Boolean.class,
                                       String.class, String.class,
                                       Positive.class, Boolean.class,
                                       Boolean.class, Boolean.class,
                                       Boolean.class, String.class,
                                       Boolean.class, Positive.class,
                                       Boolean.class, Positive.class,
                                       Positive.class, Positive.class, Boolean.class
                               }
                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        if (jsonObject.has("kind") && !EnumValidatorImp.isValid(jsonObject.getString("kind"), KindQuiz.class) &&
                !jsonObject.getString("kind").equalsIgnoreCase("regularWithPDF")
        )
            return JSON_NOT_VALID_PARAMS;

        Document user = getQuizUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));
        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.update(
                    selectDB(mode), null, quizId,
                    jsonObject, false
            );


        return QuizController.update(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId,
                Utility.convertPersian(new JSONObject(jsonStr)),
                !isAdmin && Authorization.isAdvisor(user.getList("accesses", String.class))
        );
    }

    @GetMapping(value = "getAll/{mode}")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) @NotBlank String mode,
                         @RequestParam(required = false, value = "name") String name,
                         @RequestParam(required = false, value = "kind") String kind,
                         @RequestParam(required = false, value = "startDateSolar") Long startDateSolar,
                         @RequestParam(required = false, value = "startDateSolarEndLimit") Long startDateSolarEndLimit,
                         @RequestParam(required = false, value = "startRegistryDateSolar") Long startRegistryDateSolar,
                         @RequestParam(required = false, value = "startRegistrySolarEndLimit") Long startRegistrySolarEndLimit
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin) {

            if (mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()))
                return QuizController.getAll(iryscQuizRepository, null,
                        name, startDateSolar, startDateSolarEndLimit,
                        startRegistryDateSolar, startRegistrySolarEndLimit,
                        kind
                );

            if (mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
                return QuizController.getAll(onlineStandQuizRepository, null,
                        name, startDateSolar, startDateSolarEndLimit,
                        startRegistryDateSolar, startRegistrySolarEndLimit,
                        kind
                );

            if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
                return QuizController.getAll(escapeQuizRepository, null,
                        name, startDateSolar, startDateSolarEndLimit,
                        startRegistryDateSolar, startRegistrySolarEndLimit,
                        kind
                );

            if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
                return QuizController.getAll(openQuizRepository, null,
                        name, null, null, null, null,
                        kind
                );

            if (mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()))
                return QuizController.getAll(contentQuizRepository, null,
                        name, null, null, null, null, null
                );

        }

        if (!mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.HW.getName())
        )
            return JSON_NOT_VALID_PARAMS;

        return QuizController.getAll(
                mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()) ? schoolQuizRepository : hwRepository,
                user.getObjectId("_id"),
                name, startDateSolar, startDateSolarEndLimit,
                startRegistryDateSolar, startRegistrySolarEndLimit, kind
        );
    }

    @GetMapping(value = "/get/{mode}/{quizId}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isIRYSCQuiz(mode))
            return QuizController.get(selectDB(mode), isAdmin ? null : "", quizId);

        if (user == null)
            return JSON_NOT_ACCESS;

        return QuizController.get(
                mode.equals(AllKindQuiz.HW.getName()) ? hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }


    @GetMapping(value = "/getLog/{mode}/{quizId}")
    @ResponseBody
    public String getLog(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUserVoid(request);

        if (!mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName())
        )
            return JSON_NOT_VALID_PARAMS;

        return QuizController.getLog(
                mode.equals(AllKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                quizId
        );
    }


    @PostMapping(value = "/toggleVisibility/{mode}/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.toggleVisibility(selectDB(mode), null, quizId);

        return QuizController.toggleVisibility(
                mode.equalsIgnoreCase(AllKindQuiz.HW.getName()) ?
                        hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @DeleteMapping(value = "/removeMember/{quizId}/{currMainMember}")
    @ResponseBody
    public String removeMember(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId currMainMember,
                               @RequestBody @StrongJSONConstraint(
                                       params = {
                                               "items"
                                       },
                                       paramsType = {
                                               JSONArray.class
                                       }
                               ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);
        boolean isAdmin = Authorization.isEditor(user.getList("accesses", String.class));

        return OnlineStandingController.removeMember(isAdmin ? null : user.getObjectId("_id"),
                quizId, currMainMember, new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PutMapping(value = "/onlineStandingAddMember/{quizId}/{currMainMember}")
    @ResponseBody
    public String onlineStandingAddMember(HttpServletRequest request,
                                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                                          @PathVariable @ObjectIdConstraint ObjectId currMainMember,
                                          @RequestBody @StrongJSONConstraint(
                                                  params = {
                                                          "items"
                                                  },
                                                  paramsType = {
                                                          JSONArray.class
                                                  }
                                          ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);
        boolean isAdmin = Authorization.isEditor(user.getList("accesses", String.class));

        return OnlineStandingController.addMember(isAdmin ? null : user.getObjectId("_id"),
                quizId, currMainMember, new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PutMapping(value = "/changeMainMember/{quizId}/{currMainMember}")
    @ResponseBody
    public String changeMainMember(HttpServletRequest request,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @PathVariable @ObjectIdConstraint ObjectId currMainMember,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {
                                                   "items"
                                           },
                                           paramsType = {
                                                   JSONArray.class
                                           }
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getEditorPrivilegeUserVoid(request);

        JSONObject jsonObject = new JSONObject(jsonStr);
        if (jsonObject.getJSONArray("items").length() != 1)
            return generateErr("لطفا تنها یک دانش آموز را انتخاب کنید");

        String id = jsonObject.getJSONArray("items").getString(0);
        if (!ObjectId.isValid(id))
            return JSON_NOT_VALID_PARAMS;

        return OnlineStandingController.changeMainMember(quizId, currMainMember, new ObjectId(id));
    }

    @PutMapping(value = "/onlineStandingForceRegistry/{quizId}")
    @ResponseBody
    public String onlineStandingForceRegistry(HttpServletRequest request,
                                              @PathVariable @ObjectIdConstraint ObjectId quizId,
                                              @RequestBody @StrongJSONConstraint(
                                                      params = {"items", "paid", "teamName"},
                                                      paramsType = {JSONArray.class, Positive.class, String.class}
                                              ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getEditorPrivilegeUserVoid(request);

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));

        return OnlineStandingController.forceRegistry(quizId, jsonObject.getJSONArray("items"),
                jsonObject.getInt("paid"), jsonObject.getString("teamName")
        );
    }

    @PutMapping(value = "/forceRegistry/{mode}/{quizId}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items", "paid"},
                                        paramsType = {JSONArray.class, Positive.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user;

        try {
            user = getQuizUser(request);
        } catch (Exception x) {
            user = getEditorPrivilegeUser(request);
        }

        boolean isAdmin = Authorization.isEditor(user.getList("accesses", String.class));

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));
        JSONArray jsonArray = jsonObject.getJSONArray("items");
        int paid = jsonObject.getInt("paid");

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.forceRegistry(
                    selectDB(mode), null, quizId,
                    jsonArray, paid, false
            );

        return QuizController.forceRegistry(
                mode.equalsIgnoreCase(AllKindQuiz.HW.getName()) ? hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                jsonArray, paid, !isAdmin && Authorization.isAdvisor(user.getList("accesses", String.class))
        );
    }

    @DeleteMapping(value = "/forceDeportation/{mode}/{quizId}")
    @ResponseBody
    public String forceDeportation(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"items"},
                                           paramsType = JSONArray.class
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user;

        try {
            user = getQuizUser(request);
        } catch (Exception x) {
            user = getEditorPrivilegeUser(request);
        }

        boolean isAdmin = Authorization.isEditor(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.forceDeportation(selectDB(mode), null, quizId,
                    jsonArray);

        return QuizController.forceDeportation(
                mode.equalsIgnoreCase(AllKindQuiz.HW.getName()) ? hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                jsonArray
        );
    }

    @DeleteMapping(value = "/remove/{mode}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.remove(selectDB(mode), null, jsonArray);

        if (mode.equals(AllKindQuiz.SCHOOL.getName()))
            return QuizController.remove(schoolQuizRepository,
                    isAdmin ? null : user.getObjectId("_id"), jsonArray
            );

        return JSON_NOT_VALID_PARAMS;
    }


    @PostMapping(path = "finalizeQuizResult/{mode}/{quizId}")
    @ResponseBody
    public String finalizeQuizResult(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.finalizeQuizResult(quizId, selectDB(mode));
    }


    @PutMapping(path = "setCorrectorByStudentMode/{mode}/{quizId}/{correctorId}")
    @ResponseBody
    public String setCorrectorByStudentMode(HttpServletRequest request,
                                            @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                            @PathVariable @ObjectIdConstraint ObjectId quizId,
                                            @PathVariable @ObjectIdConstraint ObjectId correctorId,
                                            @RequestBody @StrongJSONConstraint(
                                                    params = {},
                                                    paramsType = {},
                                                    optionals = {"students"},
                                                    optionalsType = {JSONArray.class}
                                            ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        JSONArray jsonArray;

        if (jsonStr == null || jsonStr.isEmpty())
            jsonArray = new JSONArray();
        else {
            JSONObject jsonObject = new JSONObject(jsonStr);
            if (jsonObject.has("students"))
                jsonArray = jsonObject.getJSONArray("students");
            else
                jsonArray = new JSONArray();
        }

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return TashrihiQuizController.setCorrectorByStudentMode(iryscQuizRepository, null, quizId,
                    correctorId, jsonArray);

        return TashrihiQuizController.setCorrectorByStudentMode(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, correctorId,
                jsonArray
        );

    }


    @PutMapping(path = "setCorrectorByQuestionMode/{mode}/{quizId}/{correctorId}")
    @ResponseBody
    public String setCorrectorByQuestionMode(HttpServletRequest request,
                                             @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                             @PathVariable @ObjectIdConstraint ObjectId quizId,
                                             @PathVariable @ObjectIdConstraint ObjectId correctorId,
                                             @RequestBody @StrongJSONConstraint(
                                                     params = {},
                                                     paramsType = {},
                                                     optionals = {"questions"},
                                                     optionalsType = {JSONArray.class}
                                             ) String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        JSONArray jsonArray;

        if (jsonStr == null || jsonStr.isEmpty())
            jsonArray = new JSONArray();
        else {
            JSONObject jsonObject = new JSONObject(jsonStr);
            if (jsonObject.has("questions"))
                jsonArray = jsonObject.getJSONArray("questions");
            else
                jsonArray = new JSONArray();
        }

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return TashrihiQuizController.setCorrectorByQuestionMode(iryscQuizRepository, null, quizId,
                    correctorId, jsonArray);

        return TashrihiQuizController.setCorrectorByQuestionMode(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, correctorId,
                jsonArray
        );

    }

    @PutMapping(value = "resetEscapeQuiz/{quizId}/{stdId}")
    @ResponseBody
    public String resetEscapeQuiz(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                  @PathVariable @ObjectIdConstraint ObjectId stdId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return EscapeQuizController.reset(quizId, stdId);
    }


    @GetMapping(value = "/getParticipants/{mode}/{quizId}")
    @ResponseBody
    public String getParticipants(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                  @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                  @RequestParam(value = "studentId", required = false) ObjectId studentId,
                                  @RequestParam(value = "justAbsents", required = false) Boolean justAbsents,
                                  @RequestParam(value = "justPresence", required = false) Boolean justPresence,
                                  @RequestParam(value = "justMarked", required = false) Boolean justMarked,
                                  @RequestParam(value = "justNotMarked", required = false) Boolean justNotMarked
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin &&
                mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName())
        ) {
            return OnlineStandingController.getParticipants(
                    onlineStandQuizRepository, null,
                    quizId, studentId, justAbsents, justPresence
            );
        }

        if (isAdmin &&
                mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName())
        )
            return EscapeQuizController.getParticipants(quizId);

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())
        ))
            return QuizController.getParticipants(
                    selectDB(mode), null,
                    quizId, studentId, justMarked,
                    justNotMarked, justAbsents, justPresence
            );


        if (mode.equalsIgnoreCase(AllKindQuiz.HW.getName()))
            return SchoolQuizController.getParticipants(
                    isAdmin ? null : user.getObjectId("_id"),
                    quizId, justMarked,
                    justNotMarked, justAbsents, justPresence
            );

        return QuizController.getParticipants(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId, justMarked,
                justNotMarked, justAbsents, justPresence
        );
    }

    @GetMapping(value = "getEscapeQuizGift/{quizId}")
    @ResponseBody
    public String getEscapeQuizGift(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return EscapeQuizController.gifts(quizId);
    }

    @DeleteMapping(value = "removeEscapeQuizGift/{quizId}/{rank}")
    @ResponseBody
    public String removeEscapeQuizGift(HttpServletRequest request,
                                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                                       @PathVariable @Min(1) int rank
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return EscapeQuizController.removeGift(quizId, rank);
    }


    @PostMapping(value = "addEscapeQuizGift/{quizId}")
    @ResponseBody
    public String addEscapeQuizGift(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId quizId,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {"type", "rank"},
                                            paramsType = {GiftType.class, Positive.class},
                                            optionals = {
                                                    "useFor", "offCodeType",
                                                    "expireAt", "description",
                                                    "amount",
                                            },
                                            optionalsType = {
                                                    OffCodeSections.class, OffCodeTypes.class,
                                                    Long.class, String.class, Number.class,
                                            }
                                    ) @NotBlank String str
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return EscapeQuizController.addGift(quizId, new JSONObject(str));
    }

    @PutMapping(path = "addAttach/{mode}/{quizId}")
    @ResponseBody
    public String addAttach(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId quizId,
                            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                            @RequestBody(required = false) MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.addAttach(selectDB(mode), null, quizId, file);

        return QuizController.addAttach(
                mode.equalsIgnoreCase(AllKindQuiz.HW.getName()) ? hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, file
        );
    }

    @DeleteMapping(path = "removeAttach/{mode}/{quizId}")
    @ResponseBody
    public String removeAttach(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @RequestParam(name = "attach") @NotBlank String attach
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        return QuizController.removeAttach(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository :
                                mode.equalsIgnoreCase(AllKindQuiz.HW.getName()) ? hwRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, attach
        );
    }

    @PutMapping(value = "/arrangeQuestions/{mode}/{quizId}")
    @ResponseBody
    public String arrangeQuestions(HttpServletRequest request,
                                   @PathVariable @NotBlank @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(params = {"questionIds"},
                                           paramsType = JSONArray.class) @NotBlank String jsonStr)
            throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getQuizUser(request);
        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
            return QuizController.arrangeEscapeQuizQuestions(quizId, new JSONObject(jsonStr));

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.arrangeQuestions(
                    selectDB(mode), null, quizId, new JSONObject(jsonStr)
            );

        return QuizController.arrangeQuestions(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, new JSONObject(jsonStr)
        );
    }

    @DeleteMapping(value = "/removeQuestionFromQuiz/{mode}/{quizId}")
    @ResponseBody
    public String removeQuestionFromQuiz(HttpServletRequest request,
                                         @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                         @PathVariable @ObjectIdConstraint ObjectId quizId,
                                         @RequestBody @StrongJSONConstraint(
                                                 params = {"items"}, paramsType = {JSONArray.class}
                                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && isIRYSCQuiz(mode)) {
            if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
                return EscapeQuizController.removeQuestions(quizId, jsonArray);

            return QuizController.removeQuestions(selectDB(mode), quizId, jsonArray);
        }

        return QuizController.removeQuestions(
                schoolQuizRepository, quizId, jsonArray
        );
    }

    @PostMapping(value = "/addBatchQuestionsToQuiz/{mode}/{quizId}")
    @ResponseBody
    public String addBatchQuestionsToQuiz(HttpServletRequest request,
                                          @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                                          @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.addBatchQuestionsToQuiz(selectDB(mode), null, quizId, file);

        if (mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()))
            return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId, file);

        return JSON_NOT_VALID_PARAMS;
    }

    @PutMapping(value = "/addBatchQuestionsToQuiz/{mode}/{quizId}")
    @ResponseBody
    public String addBatchQuestionsToQuiz(HttpServletRequest request,
                                          @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                                          @RequestBody @StrongJSONConstraint(
                                                  params = {"items"}, paramsType = {JSONArray.class},
                                                  optionals = {"mark"}, optionalsType = {Positive.class}
                                          ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONArray jsonArray = jsonObject.getJSONArray("items");
        double mark = jsonObject.has("mark") ? jsonObject.getNumber("mark").doubleValue() : 3;

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.addBatchQuestionsToQuiz(selectDB(mode), null, quizId, jsonArray, mark);

        if (mode.equalsIgnoreCase(GeneralKindQuiz.SCHOOL.getName()))
            return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId, jsonArray, mark);

        return JSON_NOT_VALID_PARAMS;
    }


    @GetMapping(path = "generateQuestionPDF/{mode}/{quizId}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> generateQuestionPDF(HttpServletRequest request,
                                                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                                                   @PathVariable @ObjectIdConstraint ObjectId quizId)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));
        File f;
        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            f = QuizController.generateQuestionPDF(iryscQuizRepository, null, quizId);
        else
            f = QuizController.generateQuestionPDF(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId);

        if (f == null)
            return null;

        try {
            InputStreamResource file = new InputStreamResource(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(f))
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_2.pdf")
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(file);
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return null;
    }

    @PutMapping(value = "/addQuestionToQuizzes/{mode}/{organizationCode}/{mark}")
    @ResponseBody
    public String addQuestionToQuizzes(HttpServletRequest request,
                                       @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                       @PathVariable @NotBlank String organizationCode,
                                       @PathVariable Number mark,
                                       @RequestBody @StrongJSONConstraint(
                                               params = {"items"}, paramsType = {JSONArray.class}
                                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getQuizUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONArray jsonArray = jsonObject.getJSONArray("items");

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName())
        ))
            return QuizController.addQuestionToQuizzes(organizationCode,
                    mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) ? escapeQuizRepository : iryscQuizRepository,
                    null, jsonArray, mark.doubleValue()
            );

        if (mode.equalsIgnoreCase(GeneralKindQuiz.SCHOOL.getName()))
            return QuizController.addQuestionToQuizzes(organizationCode, schoolQuizRepository, user.getObjectId("_id"), jsonArray, mark.doubleValue());

        return JSON_NOT_VALID_PARAMS;
    }

    @PutMapping(value = "/updateQuestionMark/{mode}/{quizId}/{questionId}/{mark}")
    @ResponseBody
    public String updateQuestionMark(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId,
                                     @PathVariable @ObjectIdConstraint ObjectId questionId,
                                     @PathVariable Number mark,
                                     @RequestParam(required = false, value = "canUpload") String canUpload
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode))
            return QuizController.updateQuestionMark(selectDB(mode), null, quizId, questionId, mark, canUpload);

        return QuizController.updateQuestionMark(schoolQuizRepository, user.getObjectId("_id"), quizId, questionId, mark, canUpload);
    }


    @GetMapping(value = "/getCorrectors/{mode}/{quizId}")
    @ResponseBody
    public String getCorrectors(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())
        ))
            return TashrihiQuizController.correctors(
                    mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                            openQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()) ?
                                    contentQuizRepository : iryscQuizRepository, null,
                    quizId
            );

        return TashrihiQuizController.correctors(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId);
    }


    @GetMapping(value = "/getCorrector/{mode}/{quizId}/{correctorId}")
    @ResponseBody
    public String getCorrector(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId correctorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())
        ))
            return TashrihiQuizController.corrector(
                    mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                            openQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()) ?
                                    contentQuizRepository : iryscQuizRepository, null,
                    quizId, correctorId
            );

        return TashrihiQuizController.corrector(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId, correctorId);
    }


    @PostMapping(value = "/addCorrector/{mode}/{quizId}/{NID}")
    @ResponseBody
    public String addCorrector(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @NotBlank String NID
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())
        ))
            return TashrihiQuizController.addCorrector(
                    mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                            openQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()) ?
                                    contentQuizRepository : iryscQuizRepository, null,
                    quizId, NID
            );

        return TashrihiQuizController.addCorrector(
                schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId,
                NID
        );
    }

    @DeleteMapping(value = "/removeCorrectors/{mode}/{quizId}")
    @ResponseBody
    public String removeCorrectors(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"items"},
                                           paramsType = {JSONArray.class}
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && (
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ||
                        mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName())
        ))
            return TashrihiQuizController.removeCorrectors(
                    mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                            openQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()) ?
                                    contentQuizRepository : iryscQuizRepository, null,
                    quizId, new JSONObject(jsonStr).getJSONArray("items")
            );

        return TashrihiQuizController.removeCorrectors(
                schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId,
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }


    @GetMapping(value = "/fetchQuestions/{mode}/{quizId}")
    @ResponseBody
    public String fetchQuestions(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode)) {

            if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
                return EscapeQuizController.fetchQuestions(quizId);

            return QuizController.fetchQuestions(
                    selectDB(mode), null, quizId
            );
        }

        return QuizController.fetchQuestions(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId);
    }

    @PostMapping(value = "/createPackage")
    @ResponseBody
    public String createPackage(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "minSelect", "offPercent",
                                                "title", "gradeId", "priority"
                                        },
                                        paramsType = {
                                                Positive.class, Positive.class,
                                                String.class, ObjectId.class, Positive.class
                                        },
                                        optionals = {"description", "lessonId"},
                                        optionalsType = {String.class, ObjectId.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageController.createPackage(Utility.convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "/updatePackage/{packageId}")
    @ResponseBody
    public String updatePackage(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId packageId,
                                @RequestBody @StrongJSONConstraint(
                                        params = {},
                                        paramsType = {},
                                        optionals = {
                                                "description", "minSelect",
                                                "offPercent", "title",
                                                "gradeId", "lessonId",
                                                "priority"
                                        },
                                        optionalsType = {
                                                String.class, Positive.class,
                                                Positive.class, String.class,
                                                ObjectId.class, ObjectId.class,
                                                Positive.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageController.editPackage(packageId, Utility.convertPersian(new JSONObject(jsonStr)));
    }

    @PutMapping(value = "/addQuizzesToPackage/{packageId}")
    @ResponseBody
    public String addQuizzesToPackage(HttpServletRequest request,
                                      @PathVariable @ObjectIdConstraint ObjectId packageId,
                                      @RequestBody @StrongJSONConstraint(
                                              params = {"ids"},
                                              paramsType = {JSONArray.class}
                                      ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageController.addQuizzesToPackage(
                packageId,
                new JSONObject(jsonStr).getJSONArray("ids")
        );
    }

    @DeleteMapping(value = "/removeQuizzesFromPackage/{packageId}")
    @ResponseBody
    public String removeQuizzesFromPackage(HttpServletRequest request,
                                           @PathVariable @ObjectIdConstraint ObjectId packageId,
                                           @RequestBody @StrongJSONConstraint(
                                                   params = {"ids"},
                                                   paramsType = {JSONArray.class}
                                           ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return PackageController.removeQuizzesFromPackage(
                packageId,
                new JSONObject(jsonStr).getJSONArray("ids")
        );
    }

    @DeleteMapping(value = "/removePackages")
    @ResponseBody
    public String removePackages(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"items"},
                                         paramsType = {JSONArray.class}
                                 ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return CommonController.removeAll(packageRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                null
        );
    }

    @GetMapping(value = "/getPackages")
    @ResponseBody
    public String getPackages(HttpServletRequest request,
                              @RequestParam(required = false, value = "quizId") ObjectId quizId,
                              @RequestParam(required = false, value = "gradeId") ObjectId gradeId,
                              @RequestParam(required = false, value = "lessonId") ObjectId lessonId
    ) {
        Document user = getUserIfLogin(request);

        return PackageController.getPackages(
                user == null ? null : user.getList("accesses", String.class),
                user == null ? null : user.getObjectId("_id"),
                gradeId, lessonId, null, quizId
        );
    }


    @GetMapping(value = "/getPackage/{id}")
    @ResponseBody
    public String getPackages(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        Document user = getUserIfLogin(request);

        return PackageController.getPackages(
                user == null ? null : user.getList("accesses", String.class),
                user == null ? null : user.getObjectId("_id"),
                null, null, id, null
        );
    }

    @GetMapping(value = "/getPackagesDigest")
    @ResponseBody
    public String getPackagesDigest(HttpServletRequest request,
                                    @RequestParam(required = false) ObjectId gradeId,
                                    @RequestParam(required = false) ObjectId lessonId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);

        return PackageController.getPackagesDigest(
                gradeId, lessonId
        );
    }

    @GetMapping(value = "/getPackageQuizzes/{packageId}")
    @ResponseBody
    public String getPackageQuizzes(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId packageId
    ) {
        Document user = getUserIfLogin(request);
        return PackageController.getPackageQuizzes(
                packageId,
                user != null && Authorization.isWeakAdmin(user.getList("accesses", String.class))
        );
    }


    @GetMapping(value = "/getDistinctTags")
    @ResponseBody
    public String getDistinctTags() {
        return QuizController.getDistinctTags();
    }

    @GetMapping(value = "/getStudentAnswerSheet/{mode}/{quizId}/{studentId}")
    @ResponseBody
    public String getStudentAnswerSheet(HttpServletRequest request,
                                        @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                        @PathVariable @ObjectIdConstraint ObjectId quizId,
                                        @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return AdminReportController.getStudentAnswerSheetCustomQuiz(
                    quizId, studentId
            );

        Authorization.hasAccessToThisStudent(studentId, user.getObjectId("_id"));

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return AdminReportController.getStudentAnswerSheet(
                    iryscQuizRepository, null, quizId, studentId
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return AdminReportController.getStudentAnswerSheet(
                    openQuizRepository, null, quizId, studentId
            );

        return AdminReportController.getStudentAnswerSheet(
                schoolQuizRepository,
                null, quizId, studentId
        );
    }

    @GetMapping(value = "/getQuizAnswerSheets/{mode}/{quizId}")
    @ResponseBody
    public String getQuizAnswerSheets(HttpServletRequest request,
                                      @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                      @PathVariable @ObjectIdConstraint ObjectId quizId,
                                      @RequestParam(required = false, value = "studentId") ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        if (isAdmin && isIRYSCQuiz(mode))
            return AdminReportController.getQuizAnswerSheets(
                    selectDB(mode), null, quizId, studentId
            );

        return AdminReportController.getQuizAnswerSheets(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId
        );
    }

    @GetMapping(value = "/getQuizAnswerSheet/{mode}/{quizId}")
    @ResponseBody
    public String getQuizAnswerSheet(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isWeakAdmin(user.getList("accesses", String.class));

        return QuizController.getQuizAnswerSheet(
                selectDB(mode),
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @PostMapping(value = "/setQuizAnswerSheet/{mode}/{quizId}/{userId}")
    @ResponseBody
    public String setQuizAnswerSheet(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId,
                                     @PathVariable @ObjectIdConstraint ObjectId userId,
                                     @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.setQuizAnswerSheet(
                    iryscQuizRepository, null, quizId, userId, file
            );

        return QuizController.setQuizAnswerSheet(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, userId, file
        );
    }

    @PutMapping(value = "/storeAnswers/{mode}/{quizId}/{userId}")
    @ResponseBody
    public String storeAnswers(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId userId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"answers"},
                                       paramsType = {JSONArray.class}
                               ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.storeAnswers(
                    iryscQuizRepository, null, quizId,
                    userId, new JSONObject(jsonStr).getJSONArray("answers")
            );

        return QuizController.storeAnswers(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, userId, new JSONObject(jsonStr).getJSONArray("answers")
        );
    }

    @PutMapping(value = "/createTaraz/{mode}/{quizId}")
    @ResponseBody
    public String createTaraz(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        return QuizController.createTaraz(
                mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ? openQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }


    @PutMapping(value = "/resetStudentQuizEntryTime/{mode}/{quizId}/{userId}")
    @ResponseBody
    public String resetStudentQuizEntryTime(HttpServletRequest request,
                                            @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                            @PathVariable @ObjectIdConstraint ObjectId quizId,
                                            @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isContent(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.resetStudentQuizEntryTime(
                    iryscQuizRepository, null, quizId,
                    userId
            );

        if (isAdmin && mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            return QuizController.resetStudentQuizEntryTime(
                    openQuizRepository, null, quizId,
                    userId
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()))
            return QuizController.resetStudentQuizEntryTime(
                    schoolQuizRepository,
                    isAdmin ? null : user.getObjectId("_id"),
                    quizId, userId
            );

        return JSON_NOT_VALID_PARAMS;
    }


    @GetMapping(value = "rates/{mode}/{id}")
    @ResponseBody
    public String rates(HttpServletRequest request,
                        @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                        @PathVariable @ObjectIdConstraint ObjectId id
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUser(request);
        return QuizController.rates(mode.equalsIgnoreCase(
                GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                openQuizRepository, id
        );
    }
}

