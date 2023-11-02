package irysc.gachesefid.Routes.API.Quiz;


import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Controllers.Quiz.*;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.GeneralKindQuiz;
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
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.io.File;

import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;

@Controller
@RequestMapping(path = "/api/quiz/public/")
@Validated
public class StudentQuizAPIRoutes extends Router {

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

    @GetMapping(value = "get/{mode}")
    @ResponseBody
    public String get(HttpServletRequest request,
                      @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                      @RequestParam(required = false) String tag,
                      @RequestParam(required = false) Boolean finishedIsNeeded,
                      @RequestParam(required = false) Boolean justRegistrable
    ) {
        Document user = getUserIfLogin(request);
        boolean isAdmin = user != null && Authorization.isAdmin(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) ||
                mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()))
            return QuizController.getRegistrable(
                    mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                            mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) ? escapeQuizRepository :
                            schoolQuizRepository,
                    isAdmin, tag, finishedIsNeeded
            );

//        if(mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
//            return OpenQuizController.getAll(isAdmin, user != null ? user.getObjectId("_id") : null);

        return JSON_NOT_VALID_PARAMS;
    }

    @GetMapping(value = "getFinishedQuizzes")
    @ResponseBody
    public String getFinishedQuizzes() {
        return QuizController.getFinishedQuizzes();
    }

    @GetMapping(value = "getRecpForQuiz/{mode}/{quizId}")
    @ResponseBody
    public String getRecpForQuiz(HttpServletRequest request,
                                 @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.getMyRecpForCustomQuiz(
                    quizId, getUser(request).getObjectId("_id")
            );

        return StudentQuizController.getMyRecp(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()) ?
                                onlineStandQuizRepository :
                                mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                        openQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );
    }


    @GetMapping(value = "reviewQuiz/{mode}/{quizId}")
    @ResponseBody
    public String reviewQuiz(HttpServletRequest request,
                             @PathVariable String mode,
                             @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);
        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if(!EnumValidatorImp.isValid(mode, AllKindQuiz.class) && !mode.equalsIgnoreCase("pdf"))
            return JSON_NOT_VALID_PARAMS;

        if(mode.equalsIgnoreCase("pdf"))
            mode = AllKindQuiz.IRYSC.getName();

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.reviewCustomQuiz(
                    quizId, user.getObjectId("_id")
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()))
            return isAdmin ? QuizController.reviewContentQuiz(quizId) :
                    StudentContentController.reviewFinalQuiz(
                            quizId, user.getObjectId("_id")
                    );

        boolean isStudent = Authorization.isPureStudent(user.getList("accesses", String.class));

        if (mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
            return OnlineStandingController.reviewQuiz(
                    quizId, user.getObjectId("_id"), !isAdmin
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
            return EscapeQuizController.reviewQuiz(
                    quizId, user.getObjectId("_id"), !isAdmin
            );

        return StudentQuizController.reviewQuiz(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()) ?
                                onlineStandQuizRepository :
                                mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                        openQuizRepository : schoolQuizRepository,
                quizId, isAdmin ? null : user.getObjectId("_id"), isStudent
        );

    }


    @PutMapping(value = "rate/{mode}/{quizId}")
    @ResponseBody
    public String rate(HttpServletRequest request,
                       @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                       @PathVariable @ObjectIdConstraint ObjectId quizId,
                       @RequestBody @StrongJSONConstraint(params = {"rate"}, paramsType = {Positive.class}) String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (!mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName())
        )
            return JSON_NOT_VALID_PARAMS;

        JSONObject jsonObject = new JSONObject(jsonStr);
        if (jsonObject.getInt("rate") <= 0 || jsonObject.getInt("rate") > 5)
            return JSON_NOT_VALID_PARAMS;

        return StudentQuizController.rate(selectDB(mode),
                quizId, user.getObjectId("_id"), jsonObject.getInt("rate")
        );

    }

    @GetMapping(value = "launch/{mode}/{quizId}")
    @ResponseBody
    public String launch(HttpServletRequest request,
                         @PathVariable @NotBlank String mode,
                         @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        if(!EnumValidatorImp.isValid(mode, AllKindQuiz.class) &&
                !mode.equalsIgnoreCase("pdf")
        )
            return JSON_NOT_VALID_PARAMS;

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.launchCustom(
                    quizId, getUser(request).getObjectId("_id")
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()))
            return StudentContentController.startFinalQuiz(
                    quizId, getUser(request).getObjectId("_id")
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.ONLINESTANDING.getName()))
            return OnlineStandingController.launch(
                    quizId, getUser(request).getObjectId("_id")
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.ESCAPE.getName()))
            return EscapeQuizController.launch(
                    quizId, getUser(request).getObjectId("_id")
            );

        if(mode.equalsIgnoreCase("pdf"))
            return StudentQuizController.launchPDFQuiz(
                    iryscQuizRepository, quizId,
                    getUser(request).getObjectId("_id")
            );

        return StudentQuizController.launch(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId, getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "getOnlineQuizRankingTable/{quizId}")
    @ResponseBody
    public String getOnlineQuizRankingTable(@PathVariable @ObjectIdConstraint ObjectId quizId) {
        return OnlineStandingController.getOnlineQuizRankingTable(quizId);
    }


    @GetMapping(value = "getMySubmits/{mode}/{quizId}")
    @ResponseBody
    public String getMySubmits(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        return StudentQuizController.getMySubmits(
                mode.equalsIgnoreCase(
                        GeneralKindQuiz.IRYSC.getName()) ? iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName()) ?
                                schoolQuizRepository : null,
                quizId, getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(path = "getMyQuestionPDF/{mode}/{quizId}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getMyQuestionPDF(HttpServletRequest request,
                                                                @PathVariable @EnumValidator(enumClazz = GeneralKindQuiz.class) String mode,
                                                                @PathVariable @ObjectIdConstraint ObjectId quizId)
            throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);
        File f;

        if (mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()))
            f = StudentQuizController.getMyQuestionPDF(iryscQuizRepository, user.getObjectId("_id"), quizId);
        else
            f = StudentQuizController.getMyQuestionPDF(schoolQuizRepository, user.getObjectId("_id"), quizId);

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


    @GetMapping(value = "myQuizzes")
    @ResponseBody
    public String myQuizzes(HttpServletRequest request,
                            @RequestParam(required = false) String mode,
                            @RequestParam(required = false) String status
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.myQuizzes(
                user, mode, status
        );
    }

    @GetMapping(value = "mySchoolQuizzes")
    @ResponseBody
    public String mySchoolQuizzes(HttpServletRequest request,
                                  @RequestParam(value = "forAdvisor") boolean forAdvisor,
                                  @RequestParam(required = false) String status
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.mySchoolQuizzes(
                user, status, forAdvisor
        );
    }

    @GetMapping(value = "myHWs")
    @ResponseBody
    public String myHWs(HttpServletRequest request,
                        @RequestParam(value = "forAdvisor") boolean forAdvisor,
                        @RequestParam(required = false) String status
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.myHWs(
                user.getObjectId("_id"), status, forAdvisor
        );
    }

    @GetMapping(value = "myHW/{id}")
    @ResponseBody
    public String myHW(HttpServletRequest request,
                       @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.myHW(
                user.getObjectId("_id"), id
        );
    }

    @PostMapping(value = "/uploadHW/{hwId}")
    @ResponseBody
    public String uploadHW(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId hwId,
                           @RequestBody MultipartFile file
    ) throws UnAuthException, NotActivateAccountException {

        if (file == null)
            return JSON_NOT_ACCESS;

        Document user = getUserWithOutCheckCompleteness(request);

        return SchoolQuizController.setAnswer(
                hwId, user.getObjectId("_id"),
                file
        );
    }

    @GetMapping(value = "/downloadHW/{hwId}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadHw(HttpServletRequest request,
                                                          @PathVariable @ObjectIdConstraint ObjectId hwId
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);

        PairValue p = SchoolQuizController.downloadHw(
                hwId, user.getObjectId("_id")
        );

        if (p == null)
            return null;

        File f = (File) p.getKey();

        try {
            InputStreamResource file = new InputStreamResource(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(f))
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + p.getValue().toString())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(file);

        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return null;

    }


    @PostMapping(value = "buyAdvisorQuiz/{quizId}")
    @ResponseBody
    public String buyAdvisorQuiz(HttpServletRequest request,
                                 @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException {
        Document user = getUserWithOutCheckCompleteness(request);
        return StudentQuizController.buyAdvisorQuiz(
                user.getObjectId("_id"), quizId, ((Number) user.get("money")).doubleValue()
        );
    }


    @GetMapping(value = "myCustomQuizzes")
    @ResponseBody
    public String myCustomQuizzes(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return StudentQuizController.myCustomQuizzes(
                getUser(request).getObjectId("_id")
        );
    }

    @GetMapping(value = "/getMyAnswerSheet/{mode}/{quizId}")
    @ResponseBody
    public String getMyAnswerSheet(HttpServletRequest request,
                                   @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                   @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.toString()))
            return AdminReportController.getStudentAnswerSheetCustomQuiz(
                    quizId, user.getObjectId("_id")
            );

        return AdminReportController.getStudentAnswerSheet(mode.equalsIgnoreCase("school") ? schoolQuizRepository :
                selectDB(mode), null, quizId, user.getObjectId("_id"));
    }

    @PostMapping(path = "buy")
    @ResponseBody
    public String buy(HttpServletRequest request,
                      @RequestBody @StrongJSONConstraint(
                              params = {"ids"},
                              paramsType = {JSONArray.class},
                              optionals = {"packageId", "code"},
                              optionalsType = {ObjectId.class, String.class}
                      )
                      @NotBlank String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        Document user = getUser(request);

        JSONObject jsonObject = convertPersian(
                new JSONObject(jsonStr)
        );

        return StudentQuizController.buy(
                user.getObjectId("_id"),
                jsonObject.has("packageId") ?
                        new ObjectId(jsonObject.getString("packageId")) : null,
                jsonObject.getJSONArray("ids"), null,
                ((Number) user.get("money")).doubleValue(),
                user.getString("phone"),
                user.getString("mail"),
                user.getString("first_name") + " " + user.getString("last_name"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }


    @DeleteMapping(value = "/leftTeam/{quizId}")
    @ResponseBody
    public String leftTeam(HttpServletRequest request,
                           @PathVariable @ObjectIdConstraint ObjectId quizId
    ) throws UnAuthException, NotActivateAccountException {
        return OnlineStandingController.leftTeam(
                getUserWithOutCheckCompleteness(request).getObjectId("_id"),
                quizId
        );
    }

    @PostMapping(path = "/updateOnlineQuizProfile/{id}")
    @ResponseBody
    public String updateOnlineQuizProfile(HttpServletRequest request,
                                          @PathVariable @ObjectIdConstraint ObjectId id,
                                          @RequestBody @StrongJSONConstraint(
                                                  params = {"teamName"},
                                                  paramsType = {String.class},
                                                  optionals = {"members"},
                                                  optionalsType = {JSONArray.class}
                                          ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException {

        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));

        return StudentQuizController.updateOnlineQuizProfile(
                getUserWithOutCheckCompleteness(request).getObjectId("_id"),
                id, jsonObject.getString("teamName"),
                jsonObject.has("members") ? jsonObject.getJSONArray("members") :
                        new JSONArray()
        );
    }

    @PostMapping(path = "buyOnlineQuiz/{id}")
    @ResponseBody
    public String buyOnlineQuiz(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"teamName"},
                                        paramsType = {String.class},
                                        optionals = {"code", "members"},
                                        optionalsType = {String.class, JSONArray.class}
                                )
                                @NotBlank String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        Document user = getUser(request);

        JSONObject jsonObject = convertPersian(
                new JSONObject(jsonStr)
        );

        return StudentQuizController.buyOnlineQuiz(
                user.getObjectId("_id"),
                id,
                jsonObject.getString("teamName"),
                jsonObject.has("members") ? jsonObject.getJSONArray("members") : new JSONArray(),
                ((Number) user.get("money")).doubleValue(),
                user.getString("phone"),
                user.getString("mail"),
                user.getString("first_name") + " " + user.getString("last_name"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }


    @PostMapping(path = "groupBuy")
    @ResponseBody
    public String groupBuy(HttpServletRequest request,
                           @RequestBody @StrongJSONConstraint(
                                   params = {"ids", "studentIds"},
                                   paramsType = {JSONArray.class, JSONArray.class},
                                   optionals = {"packageId", "code"},
                                   optionalsType = {ObjectId.class, String.class}
                           )
                           @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotAccessException {

        Document user = getSchoolUser(request);

        JSONObject jsonObject = convertPersian(
                new JSONObject(jsonStr)
        );

        return StudentQuizController.buy(
                user.getObjectId("_id"),
                jsonObject.has("packageId") ?
                        new ObjectId(jsonObject.getString("packageId")) : null,
                jsonObject.getJSONArray("ids"),
                jsonObject.getJSONArray("studentIds"),
                ((Number) user.get("money")).doubleValue(),
                user.getString("phone"),
                user.getString("mail"),
                user.getString("first_name") + " " + user.getString("last_name"),
                jsonObject.has("code") ?
                        jsonObject.getString("code") : null
        );
    }

    @PutMapping(value = "/storeEscapeQuizAnswer/{quizId}/{questionId}")
    @ResponseBody
    public String storeAnswers(HttpServletRequest request,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @PathVariable @ObjectIdConstraint ObjectId questionId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"answer"},
                                       paramsType = {Object.class}
                               ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        return EscapeQuizController.storeAnswer(
                quizId, questionId,
                user.getObjectId("_id"), convertPersian(new JSONObject(jsonStr)).get("answer")
        );
    }


    @PutMapping(value = "/storeAnswers/{mode}/{quizId}")
    @ResponseBody
    public String storeAnswers(HttpServletRequest request,
                               @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                               @PathVariable @ObjectIdConstraint ObjectId quizId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"answers"},
                                       paramsType = {JSONArray.class}
                               ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (mode.equalsIgnoreCase(AllKindQuiz.CUSTOM.getName()))
            return StudentQuizController.storeCustomAnswers(
                    quizId, user.getObjectId("_id"),
                    new JSONObject(jsonStr).getJSONArray("answers")
            );

        if (mode.equalsIgnoreCase(AllKindQuiz.CONTENT.getName()))
            return StudentContentController.storeAnswers(
                    quizId, user.getObjectId("_id"),
                    new JSONObject(jsonStr).getJSONArray("answers")
            );

        return StudentQuizController.storeAnswers(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository :
                        mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()) ?
                                openQuizRepository : schoolQuizRepository,
                quizId,
                user.getObjectId("_id"), new JSONObject(jsonStr).getJSONArray("answers")
        );
    }


    @PutMapping(value = "/storeOnlineQuizAnswer/{quizId}/{questionId}")
    @ResponseBody
    public String storeOnlineQuizAnswer(HttpServletRequest request,
                                        @PathVariable @ObjectIdConstraint ObjectId quizId,
                                        @PathVariable @ObjectIdConstraint ObjectId questionId,
                                        @RequestBody @StrongJSONConstraint(
                                                params = {"answer"},
                                                paramsType = {Object.class}
                                        ) @NotBlank String jsonStr
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        return OnlineStandingController.storeAnswer(
                quizId, questionId,
                user.getObjectId("_id"),
                new JSONObject(jsonStr).get("answer")
        );
    }


    @PutMapping(value = "/uploadAnswers/{mode}/{quizId}/{questionId}")
    @ResponseBody
    public String uploadAnswers(HttpServletRequest request,
                                @PathVariable @EnumValidator(enumClazz = AllKindQuiz.class) String mode,
                                @PathVariable @ObjectIdConstraint ObjectId quizId,
                                @PathVariable @ObjectIdConstraint ObjectId questionId,
                                @RequestBody MultipartFile file
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {

        Document user = getUser(request);

        if (!mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()) &&
                !mode.equalsIgnoreCase(AllKindQuiz.SCHOOL.getName())
        )
            return JSON_NOT_VALID_PARAMS;

        return StudentQuizController.uploadAnswers(
                mode.equalsIgnoreCase(GeneralKindQuiz.IRYSC.getName()) ?
                        iryscQuizRepository : schoolQuizRepository,
                quizId, questionId,
                user.getObjectId("_id"), file
        );
    }


    @PostMapping(path = "prepareCustomQuiz")
    @ResponseBody
    public String prepareCustomQuiz(HttpServletRequest request,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {"filters", "name"},
                                            paramsType = {JSONArray.class, String.class}
                                    )
                                    @NotBlank String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {

        JSONObject jsonObject = new JSONObject(jsonStr);

        return StudentQuizController.prepareCustomQuiz(
                getUser(request).getObjectId("_id"),
                jsonObject.getJSONArray("filters"),
                jsonObject.getString("name")
        );
    }

    @PostMapping(path = "payCustomQuiz/{id}")
    @ResponseBody
    public String payCustomQuiz(HttpServletRequest request,
                                @PathVariable @ObjectIdConstraint ObjectId id,
                                @RequestBody(required = false) @StrongJSONConstraint(
                                        params = {}, paramsType = {},
                                        optionals = {"offcode"},
                                        optionalsType = {String.class}
                                ) String jsonStr
    ) throws UnAuthException, NotCompleteAccountException, NotActivateAccountException {
        return StudentQuizController.payCustomQuiz(
                getUser(request),
                id,
                (jsonStr == null || jsonStr.isEmpty()) ?
                        new JSONObject() :
                        new JSONObject(jsonStr)
        );
    }

}
