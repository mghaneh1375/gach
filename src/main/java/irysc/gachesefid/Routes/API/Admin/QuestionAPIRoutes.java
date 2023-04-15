package irysc.gachesefid.Routes.API.Admin;

import irysc.gachesefid.Controllers.CommonController;
import irysc.gachesefid.Controllers.Question.QuestionController;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Test.Question.QuestionTestController;
import irysc.gachesefid.Utility.Positive;
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
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.convertPersian;
import static irysc.gachesefid.Utility.Utility.printException;

@Controller
@RequestMapping(path = "/api/admin/question")
@Validated
public class QuestionAPIRoutes extends Router {

    @GetMapping(value = "/subjectQuestions")
    @ResponseBody
    public String subjectQuestions(HttpServletRequest request,
                                   @RequestParam(required = false) Boolean isQuestionNeeded,
                                   @RequestParam(required = false) Integer criticalThresh,
                                   @RequestParam(required = false) ObjectId subjectId,
                                   @RequestParam(required = false) ObjectId lessonId,
                                   @RequestParam(required = false) ObjectId gradeId,
                                   @RequestParam(required = false) String organizationCode
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getPrivilegeUser(request);
        return QuestionController.subjectQuestions(
                isQuestionNeeded, criticalThresh, organizationCode != null && organizationCode.isEmpty() ? null : organizationCode, subjectId, lessonId, gradeId
        );
    }

    @DeleteMapping(value = "remove")
    @ResponseBody
    public String removeQuestions(HttpServletRequest request,
                                  @RequestBody @StrongJSONConstraint(
                                          params = {"items"},
                                          paramsType = {JSONArray.class}
                                  ) @NotBlank String jsonStr
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        PairValue p = CommonController.removeAllReturnDocs(
                questionRepository,
                new JSONObject(jsonStr).getJSONArray("items"),
                or(
                        exists("used", false),
                        eq("used", 0)
                )
        );

        ArrayList<Document> docs = (ArrayList<Document>) p.getValue();
        HashMap<ObjectId, Integer> subjectsQNo = new HashMap<>();

        for (Document doc : docs) {
            Document subject = subjectRepository.findById(doc.getObjectId("subject_id"));
            if (subject == null)
                continue;

            if (!subjectsQNo.containsKey(subject.getObjectId("_id")))
                subjectsQNo.put(subject.getObjectId("_id"), subject.getInteger("q_no") - 1);
            else
                subjectsQNo.put(subject.getObjectId("_id"), subjectsQNo.get(subject.getObjectId("_id")) - 1);

        }

        for (ObjectId subjectId : subjectsQNo.keySet()) {

            Document subject = subjectRepository.findById(subjectId);
            if (subject == null)
                continue;

            subject.put("q_no", subjectsQNo.get(subjectId));
            subjectRepository.updateOne(
                    subjectId,
                    set("q_no", subjectsQNo.get(subjectId))
            );

        }

        return (String) p.getKey();
    }


    @PostMapping(value = "/test")
    @ResponseBody
    public String test(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        if (!DEV_MODE)
            return "not allowed in main server";

        getAdminPrivilegeUserVoid(request);
        String msg;

        try {
            new QuestionTestController();
            msg = "success";
        } catch (Exception x) {
            msg = x.getMessage();
            printException(x);
        }

        return msg;
    }


    @PostMapping(value = "store/{subjectId}")
    @ResponseBody
    public String store(HttpServletRequest request,
                        @PathVariable @ObjectIdConstraint ObjectId subjectId,
                        @RequestPart(value = "questionFile") MultipartFile questionFile,
                        @RequestPart(value = "answerFile", required = false) MultipartFile answerFile,
                        @RequestPart(value = "json") @StrongJSONConstraint(
                                params = {
                                        "level", "authorId",
                                        "neededTime", "answer",
                                        "kindQuestion",
                                        "organizationId",
                                },
                                paramsType = {
                                        QuestionLevel.class, ObjectId.class,
                                        Positive.class, Object.class,
                                        QuestionType.class,
                                        String.class,
                                },
                                optionals = {
                                        "sentencesCount", "telorance",
                                        "choicesCount", "neededLine",
                                        "visibility", "year", "tags"
                                },
                                optionalsType = {
                                        Positive.class, Number.class,
                                        Positive.class, Positive.class,
                                        Boolean.class, Positive.class,
                                        JSONArray.class
                                }
                        ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (questionFile == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return QuestionController.addQuestion(subjectId, questionFile, answerFile, convertPersian(new JSONObject(jsonStr)));
    }

    @PostMapping(value = "edit/{questionId}")
    @ResponseBody
    public String edit(HttpServletRequest request,
                        @PathVariable @ObjectIdConstraint ObjectId questionId,
                        @RequestPart(value = "questionFile", required = false) MultipartFile questionFile,
                        @RequestPart(value = "answerFile", required = false) MultipartFile answerFile,
                        @RequestPart(value = "json") @StrongJSONConstraint(
                                params = {
                                        "level",
                                        "neededTime", "answer",
                                        "kindQuestion", "subjectId",
                                        "organizationId",
                                },
                                paramsType = {
                                        QuestionLevel.class,
                                        Positive.class, Object.class,
                                        QuestionType.class, ObjectId.class,
                                        String.class,
                                },
                                optionals = {
                                        "sentencesCount", "telorance",
                                        "choicesCount", "neededLine",
                                        "visibility", "authorId",
                                        "year", "tags"
                                },
                                optionalsType = {
                                        Positive.class, Number.class,
                                        Positive.class, Positive.class,
                                        Boolean.class, ObjectId.class,
                                        Positive.class, JSONArray.class
                                }
                        ) @NotBlank String jsonStr)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        getAdminPrivilegeUserVoid(request);
        return QuestionController.updateQuestion(questionId, questionFile, answerFile, convertPersian(new JSONObject(jsonStr)));
    }

    @PostMapping(value = "/addBatch")
    @ResponseBody
    public String addBatch(HttpServletRequest request,
                           @RequestBody MultipartFile file)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        getAdminPrivilegeUserVoid(request);
        return QuestionController.addBatch(file);
    }

    @GetMapping
    @ResponseBody
    public String search(HttpServletRequest request,
                         @RequestParam(required = false, value = "isSubjectsNeeded") Boolean isSubjectsNeeded,
                         @RequestParam(required = false, value = "isAuthorsNeeded") Boolean isAuthorsNeeded,
                         @RequestParam(required = false, value = "justUnVisible") Boolean justUnVisible,
                         @RequestParam(required = false, value = "organizationId") String organizationId,
                         @RequestParam(required = false, value = "organizationLike") String organizationLike,
                         @RequestParam(required = false, value = "level") String level,
                         @RequestParam(required = false, value = "kindQuestion") String kindQuestion,
                         @RequestParam(required = false, value = "kindQuestion") String sortBy,
                         @RequestParam(required = false, value = "subjectId") ObjectId subjectId,
                         @RequestParam(required = false, value = "lessonId") ObjectId lessonId,
                         @RequestParam(required = false, value = "questionId") ObjectId questionId,
                         @RequestParam(required = false, value = "quizId") ObjectId quizId,
                         @RequestParam(required = false, value = "authorId") ObjectId authorId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);
        return QuestionController.search(true,
                isSubjectsNeeded == null ? true : isSubjectsNeeded,
                isAuthorsNeeded == null ? true : isAuthorsNeeded,
                justUnVisible, organizationId, organizationLike,
                subjectId, lessonId, questionId, quizId, authorId,
                level, kindQuestion, sortBy
        );
    }

}
