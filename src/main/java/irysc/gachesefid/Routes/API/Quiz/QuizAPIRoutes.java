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
import irysc.gachesefid.Models.LaunchMode;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
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
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.io.File;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

//import irysc.gachesefid.Controllers.Python.CVController;


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
                                        "end", "tags", "launchMode",
                                        "capacity", "minusMark",
                                        "backEn", "showResultsAfterCorrection",
                                        "topStudentsGiftCoin",
                                        "topStudentsGiftMoney",
                                        "topStudentsCount",
                                        "paperTheme", "database",
                                        "descAfter", "desc",
                                        "duration", // duration is in min format
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
                                        Positive.class
                                }
                        ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);
        JSONObject jsonObject = Utility.convertPersian(new JSONObject(jsonStr));

        if (mode.equalsIgnoreCase(KindQuiz.REGULAR.getName()) ||
                mode.equalsIgnoreCase(KindQuiz.HYBRID.getName())
        )
            return RegularQuizController.create(
                    user.getObjectId("_id"),
                    jsonObject, mode
            );

        if (mode.equals(KindQuiz.OPEN.getName()))
            return OpenQuizController.create(user.getObjectId("_id"),
                    jsonObject
            );

        if (mode.equals(KindQuiz.TASHRIHI.getName()))
            return TashrihiQuizController.create(user.getObjectId("_id"),
                    jsonObject
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
                         @PathVariable @EnumValidator(enumClazz = KindQuiz.class) @NotBlank String mode
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
    ) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equals(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.get(iryscQuizRepository, isAdmin ? null : "", quizId);

        if(user == null)
            return JSON_NOT_ACCESS;

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

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("items");
        String output;

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            output = CommonController.removeAllFormDocList(iryscQuizRepository, jsonArray, quizId,
                    "questions", gt("start", System.currentTimeMillis())
            );
        else
            output = CommonController.removeAllFormDocList(
                    schoolQuizRepository,
                    jsonArray, quizId, "questions",
                    and(
                            gt("start", System.currentTimeMillis()),
                            eq("created_by", user.getObjectId("_id"))
                    )
            );

        JSONObject jsonObject = new JSONObject(output);
        if (jsonObject.has("doneIds")) {
            JSONArray doneIds = jsonObject.getJSONArray("doneIds");
            for (int i = 0; i < doneIds.length(); i++) {

                Document question = questionRepository.findById(
                        new ObjectId(doneIds.getString(i))
                );

                if (question == null)
                    continue;

                int used = (int) question.getOrDefault("used", 0);

                questionRepository.updateOne(
                        question.getObjectId("_id"),
                        set("used", used - 1)
                );

            }
        }

        return output;
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

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addBatchQuestionsToQuiz(iryscQuizRepository, null, quizId, file);

        return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId, file);

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

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONArray jsonArray = jsonObject.getJSONArray("items");
        double mark = jsonObject.has("mark") ? jsonObject.getNumber("mark").doubleValue() : 3;

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addBatchQuestionsToQuiz(iryscQuizRepository, null, quizId, jsonArray, mark);

        return QuizController.addBatchQuestionsToQuiz(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId, jsonArray, mark);
    }


    @GetMapping(path = "generateQuestionPDF/{mode}/{quizId}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> generateQuestionPDF(HttpServletRequest request,
                                                                   @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                                                   @PathVariable @ObjectIdConstraint ObjectId quizId)
            throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
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
                                       @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                       @PathVariable @NotBlank String organizationCode,
                                       @PathVariable Number mark,
                                       @RequestBody @StrongJSONConstraint(
                                               params = {"items"}, paramsType = {JSONArray.class}
                                       ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        JSONObject jsonObject = new JSONObject(jsonStr);

        JSONArray jsonArray = jsonObject.getJSONArray("items");

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.addQuestionToQuizzes(organizationCode, iryscQuizRepository, null, jsonArray, mark.doubleValue());

        return QuizController.addQuestionToQuizzes(organizationCode, schoolQuizRepository, user.getObjectId("_id"), jsonArray, mark.doubleValue());
    }

    @PutMapping(value = "/updateQuestionMark/{mode}/{quizId}/{questionId}/{mark}")
    @ResponseBody
    public String updateQuestionMark(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId,
                                     @PathVariable @ObjectIdConstraint ObjectId questionId,
                                     @PathVariable Number mark
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getAdminPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.updateQuestionMark(iryscQuizRepository, null, quizId, questionId, mark);

        return QuizController.updateQuestionMark(schoolQuizRepository, user.getObjectId("_id"), quizId, questionId, mark);
    }

    @GetMapping(value = "/fetchQuestions/{mode}/{quizId}")
    @ResponseBody

    public String fetchQuestions(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.fetchQuestions(iryscQuizRepository, null, quizId);

        return QuizController.fetchQuestions(schoolQuizRepository, isAdmin ? null : user.getObjectId("_id"), quizId);
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


    @PostMapping(value = "/correct/{col}/{row}/{qNo}/{eyes}/{choices}/{quizId}/{userId}")
    @ResponseBody
    public String correct(HttpServletRequest request,
                          @PathVariable Integer col,
                          @PathVariable Integer row,
                          @PathVariable Integer qNo,
                          @PathVariable Integer eyes,
                          @PathVariable Integer choices,
                          @PathVariable @ObjectIdConstraint ObjectId quizId,
                          @PathVariable @ObjectIdConstraint ObjectId userId
//                          @RequestBody MultipartFile file
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {
//        getAdminPrivilegeUser(request);
//        return CVController.correct(quizId, userId, col, row, qNo, eyes, choices);
        return "salam";
    }

    @GetMapping(value = "/getDistinctTags")
    @ResponseBody
    public String getDistinctTags() {
        return QuizController.getDistinctTags();
    }

    @GetMapping(value = "/getStudentAnswerSheet/{mode}/{quizId}/{studentId}")
    @ResponseBody
    public String getStudentAnswerSheet(HttpServletRequest request,
                                      @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                      @PathVariable @ObjectIdConstraint ObjectId quizId,
                                      @PathVariable @ObjectIdConstraint ObjectId studentId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getStudentAnswerSheet(
                    iryscQuizRepository, null, quizId, studentId
            );

        return QuizController.getStudentAnswerSheet(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId, studentId
        );
    }

    @GetMapping(value = "/getQuizAnswerSheets/{mode}/{quizId}")
    @ResponseBody
    public String getQuizAnswerSheets(HttpServletRequest request,
                                      @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                      @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getQuizAnswerSheets(
                    iryscQuizRepository, null, quizId
            );

        return QuizController.getQuizAnswerSheets(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId
        );
    }

    @GetMapping(value = "/getQuizAnswerSheet/{mode}/{quizId}")
    @ResponseBody
    public String getQuizAnswerSheet(HttpServletRequest request,
                                     @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                     @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getQuizAnswerSheet(
                    iryscQuizRepository, null, quizId
            );

        return QuizController.getQuizAnswerSheet(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId
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

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

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

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

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
                               @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

//        if (isAdmin && mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.createTaraz(
                    iryscQuizRepository, null, quizId
            );

    }

    @GetMapping(value = "/showRanking/{mode}/{quizId}")
    @ResponseBody
    public String showRanking(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if(mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getRanking(iryscQuizRepository, isAdmin, null, quizId);

        if(user == null)
            return JSON_NOT_ACCESS;

        return QuizController.getRanking(
                schoolQuizRepository,
                isAdmin,
                isAdmin ? null : user.getObjectId("_id"),
                quizId
        );
    }

    @GetMapping(value = "/getStudentStat/{mode}/{quizId}/{userId}")
    @ResponseBody
    public String getStudentStat(HttpServletRequest request,
                              @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                              @PathVariable @ObjectIdConstraint ObjectId quizId,
                              @PathVariable @ObjectIdConstraint ObjectId userId) {

        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if(mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            return QuizController.getStudentStat(iryscQuizRepository, isAdmin ? null : "", quizId, userId);

        if(user == null)
            return JSON_NOT_ACCESS;

        return QuizController.getStudentStat(
                schoolQuizRepository,
                isAdmin ? null : user.getObjectId("_id"),
                quizId,
                userId
        );
    }

    @GetMapping(value = "/stateReport/{quizId}")
    @ResponseBody
    public String getStateReport(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return QuizController.getStateReport(quizId);
    }

    @GetMapping(value = "/cityReport/{quizId}")
    @ResponseBody
    public String cityReport(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return QuizController.getCityReport(quizId);
    }

    @GetMapping(value = "/schoolReport/{quizId}")
    @ResponseBody
    public String getSchoolReport(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return QuizController.getSchoolReport(quizId);
    }

    @GetMapping(value = "/genderReport/{quizId}")
    @ResponseBody
    public String genderReport(HttpServletRequest request,
                                  @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return QuizController.getGenderReport(quizId);
    }

    @GetMapping(value = "/authorReport/{quizId}")
    @ResponseBody
    public String authorReport(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId quizId) {
        return QuizController.getAuthorReport(quizId);
    }

}

