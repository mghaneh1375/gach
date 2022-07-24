package irysc.gachesefid.Routes.API.Quiz;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Quiz.OpenQuizController;
import irysc.gachesefid.Controllers.Quiz.QuizController;
import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Controllers.Quiz.TashrihiQuizController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Models.KindQuiz;
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

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;


@Controller
@RequestMapping(path = "/api/quiz/manage")
@Validated
public class QuizAPIRoutes extends Router {

    @PostMapping(value = "/store/{mode}")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable @EnumValidator(enumClazz = KindQuiz.class) @NotBlank String mode,
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
                                        "end", "tags", "isOnline",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrection",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperTheme", "database",
                                        "descAfter", "desc",
                                        "duration" // duration is in min format
                                },
                                optionalsType = {
                                        Positive.class, Boolean.class,
                                        String.class, Long.class,
                                        Long.class, Long.class,
                                        Long.class, String.class,
                                        Boolean.class, Positive.class,
                                        Boolean.class, Boolean.class,
                                        Boolean.class, Number.class,
                                        Positive.class, Positive.class,
                                        String.class, Boolean.class,
                                        String.class, String.class,
                                        Positive.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        if (mode.equals(KindQuiz.REGULAR.getName()))
            return RegularQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        if (mode.equals(KindQuiz.OPEN.getName()))
            return OpenQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        if (mode.equals(KindQuiz.TASHRIHI.getName()))
            return TashrihiQuizController.create(user.getObjectId("_id"),
                    new JSONObject(jsonStr)
            );

        return "sd";
    }

    @PostMapping(value = "/edit/{quizId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                       @RequestBody @StrongJSONConstraint(
                               params = {},
                               paramsType = {},
                               optionals = {
                                       "title", "price", "permute",
                                       "description", "startRegistry",
                                       "endRegistry", "start",
                                       "end", "tags", "isOnline",
                                       "capacity", "minusMark",
                                       "backEn", "showResultsAfterCorrection",
                                       "topStudentsGiftCoin",
                                       "topStudentsGiftMoney",
                                       "topStudentsCount",
                                       "paperTheme", "database",
                                       "descAfter", "desc",
                                       "duration", // duration is in min format
                                       "visibility"
                               },
                               optionalsType = {
                                       String.class, Positive.class, Boolean.class,
                                       String.class, Long.class,
                                       Long.class, Long.class,
                                       Long.class, String.class,
                                       Boolean.class, Positive.class,
                                       Boolean.class, Boolean.class,
                                       Boolean.class, Number.class,
                                       Positive.class, Positive.class,
                                       String.class, Boolean.class,
                                       String.class, String.class,
                                       Positive.class, Boolean.class
                               }
                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.update(
                isAdmin ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId,
                new JSONObject(jsonStr)
        );
    }

    @GetMapping(value = "getAll/{mode}")
    @ResponseBody
    public String getAll(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = KindQuiz.class
                         ) @NotBlank String mode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin)
            return QuizController.getAll(iryscQuizRepository, null);

        return QuizController.getAll(schoolQuizRepository, user.getObjectId("_id"));
    }

    @GetMapping(value = "/get/{mode}/{quizId}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.get(iryscQuizRepository, null, quizId);

        return QuizController.get(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @PostMapping(value = "/toggleVisibility/{mode}/{quizId}")
    @ResponseBody
    public String toggleVisibility(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.toggleVisibility(iryscQuizRepository, null, quizId);

        return QuizController.toggleVisibility(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId
        );
    }

    @PutMapping(value = "/forceRegistry/{mode}/{quizId}")
    @ResponseBody
    public String forceRegistry(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"items", "paid"},
                                        paramsType = {JSONArray.class, Positive.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONArray jsonArray = jsonObject.getJSONArray("items");
        int paid = jsonObject.getInt("paid");

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.forceRegistry(iryscQuizRepository, null, quizId,
                    jsonArray, paid
            );

        return QuizController.forceRegistry(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                jsonArray, paid
        );
    }

    @DeleteMapping(value = "/forceDeportation/{mode}/{quizId}")
    @ResponseBody
    public String forceDeportation(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(
                                           params = {"items"},
                                           paramsType = JSONArray.class
                                   ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.forceDeportation(iryscQuizRepository, null, quizId,
                    jsonArray);

        return QuizController.forceDeportation(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                jsonArray
        );
    }

    @DeleteMapping(value = "/remove/{mode}")
    @ResponseBody
    public String remove(HttpServletRequest request,
                         @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                         @RequestBody @StrongJSONConstraint(
                                 params = {"items"},
                                 paramsType = {JSONArray.class}
                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.remove(iryscQuizRepository, null, jsonArray);

        return QuizController.remove(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), jsonArray
        );
    }

    @GetMapping(value = "/getParticipants/{mode}/{quizId}")
    @ResponseBody
    public String getParticipants(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId,
                                  @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                  @RequestParam(value = "studentId", required = false) ObjectId studentId,
                                  @RequestParam(value = "isResultsNeeded", required = false) Boolean isResultsNeeded,
                                  @RequestParam(value = "isStudentAnswersNeeded", required = false) Boolean isStudentAnswersNeeded,
                                  @RequestParam(value = "justAbsents", required = false) Boolean justAbsents,
                                  @RequestParam(value = "justPresence", required = false) Boolean justPresence,
                                  @RequestParam(value = "justMarked", required = false) Boolean justMarked,
                                  @RequestParam(value = "justNotMarked", required = false) Boolean justNotMarked
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getParticipants(iryscQuizRepository, null,
                    quizId, studentId, isStudentAnswersNeeded,
                    isResultsNeeded, justMarked,
                    justNotMarked, justAbsents, justPresence
            );

        return QuizController.getParticipants(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId, isStudentAnswersNeeded,
                isResultsNeeded, justMarked,
                justNotMarked, justAbsents, justPresence
        );
    }

    @PutMapping(path = "addAttach/{mode}/{quizId}")
    @ResponseBody
    public String addAttach(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId quizId,
                            @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                            @RequestBody(required = false) MultipartFile file,
                            @RequestBody @NotBlank String title,
                            @RequestBody(required = false) String link
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addAttach(iryscQuizRepository, null, quizId,
                    file, title, link);

        return QuizController.addAttach(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId,
                file, title, link);
    }

    @DeleteMapping(path = "removeAttach/{mode}/{quizId}/{attachId}")
    @ResponseBody
    public String removeAttach(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId attachId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.removeAttach(iryscQuizRepository, null,
                    quizId, attachId);

        return QuizController.removeAttach(schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, attachId);
    }

    @PutMapping(value = "/extend/{mode}/{quizId}")
    @ResponseBody
    public String extend(HttpServletRequest request,
                         @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId,
                         @RequestParam(value = "start", required = false) Long start,
                         @RequestParam(value = "end", required = false) Long end
    ) throws NotActivateAccountException, UnAuthException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.extend(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, start, end);
    }

    @PostMapping(value = "/arrangeQuestions/{mode}/{quizId}")
    @ResponseBody
    public String arrangeQuestions(HttpServletRequest request,
                                   @PathVariable @NotBlank @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId,
                                   @RequestBody @StrongJSONConstraint(params = {"questionIds"},
                                           paramsType = JSONArray.class) @NotBlank String jsonStr)
            throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        return QuizController.arrangeQuestions(
                mode.equals(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository : schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"), quizId, new JSONObject(jsonStr)
        );
    }

    @DeleteMapping(value = "/removeQuestionFromQuiz/{mode}/{quizId}")
    @ResponseBody
    public String removeQuestionFromQuiz(HttpServletRequest request,
                                         @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                         @PathVariable @ObjectIdConstraint ObjectId quizId,
                                         @RequestBody @StrongJSONConstraint(
                                                 params = {"items"}, paramsType = {JSONArray.class}
                                         ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return CommonController.removeAllFormDocList(iryscQuizRepository, jsonArray, quizId,
                    "questions", gt("start", System.currentTimeMillis())
            );

        return CommonController.removeAllFormDocList(schoolQuizRepository,
                jsonArray, quizId, "questions",
                and(
                        gt("start", System.currentTimeMillis()),
                        eq("created_by", user.getObjectId("_id"))
                )
        );

    }

    @PostMapping(value = "/addBatchQuestionsToQuiz/{mode}/{quizId}")
    @ResponseBody
    public String addBatchQuestionsToQuiz(HttpServletRequest request,
                                          @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                                          @RequestBody MultipartFile file
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addBatchQuestionsToQuiz(iryscQuizRepository, null, quizId, file);

        return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, user.getObjectId("_id"), quizId, file);

    }

    @PutMapping(value = "/addBatchQuestionsToQuiz/{mode}/{quizId}")
    @ResponseBody
    public String addBatchQuestionsToQuiz(HttpServletRequest request,
                                          @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                                          @RequestBody @StrongJSONConstraint(
                                                  params = {"items"}, paramsType = {JSONArray.class},
                                                  optionals = {"mark"}, optionalsType = {Positive.class}
                                          ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONArray jsonArray = jsonObject.getJSONArray("items");
        double mark = jsonObject.has("mark") ? jsonObject.getNumber("mark").doubleValue() : 3;

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addBatchQuestionsToQuiz(iryscQuizRepository, null, quizId, jsonArray, mark);

        return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, user.getObjectId("_id"), quizId, jsonArray, mark);
    }

    @GetMapping(value = "/fetchQuestions/{mode}/{quizId}")
    @ResponseBody
    public String fetchQuestions(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.fetchQuestions(iryscQuizRepository, null, quizId);

        return QuizController.fetchQuestions(schoolQuizRepository, user.getObjectId("_id"), quizId);
    }

    @PostMapping(value = "/createPackage")
    @ResponseBody
    public String createPackage(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {
                                                "minSelect", "offPercent", "title",
                                                "gradeId", "lessonId"
                                        },
                                        paramsType = {
                                                Positive.class, Positive.class, String.class,
                                                ObjectId.class, ObjectId.class
                                        },
                                        optionals = {"description"},
                                        optionalsType = {String.class}
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.createPackage(new JSONObject(jsonStr));
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
                                                "gradeId", "lessonId"
                                        },
                                        optionalsType = {
                                                String.class, Positive.class,
                                                Positive.class, String.class,
                                                ObjectId.class, ObjectId.class
                                        }
                                ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        return QuizController.editPackage(packageId, new JSONObject(jsonStr));
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
        return QuizController.addQuizzesToPackage(
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
        return QuizController.removeQuizzesFromPackage(
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
                              @RequestParam(required = false) ObjectId gradeId,
                              @RequestParam(required = false) ObjectId lessonId
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));
        return QuizController.getPackages(isAdmin, gradeId, lessonId);
    }

    @GetMapping(value = "/getPackageQuizzes/{packageId}")
    @ResponseBody
    public String getPackageQuizzes(HttpServletRequest request,
                                    @PathVariable @ObjectIdConstraint ObjectId packageId
    ) {
        Document user = getUserIfLogin(request);
        return QuizController.getPackageQuizzes(
                packageId,
                user != null && Authorization.isAdmin(user.getList("accesses", String.class))
        );
    }
}

