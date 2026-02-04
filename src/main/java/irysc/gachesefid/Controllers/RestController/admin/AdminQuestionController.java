package irysc.gachesefid.Controllers.RestController.admin;

import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Service.Question.QuestionService;
import irysc.gachesefid.Service.Question.model.AddBatchQuestionServiceResponse;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/admin/cv_question/")
public class AdminQuestionController {

    @Autowired
    private QuestionService questionService;

    @PostMapping(
            value = "/cropAndAddQuestionsToQuiz/{quizId}/{quizMode}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseDto<AddBatchQuestionServiceResponse>> cropAndAddQuestionsToQuiz(
            @PathVariable @ObjectIdConstraint ObjectId quizId,
            @PathVariable String quizMode,
            @RequestPart(name = "questionPdf") MultipartFile questionPdf,
            @RequestPart(name = "answerPdf") MultipartFile answerPdf,
            @RequestPart(name = "questionsInfo") MultipartFile questionsInfo
    ) {
        return questionService.cropAndAddQuestionsToQuiz(
                questionPdf, answerPdf, questionsInfo,
                quizId, AllKindQuiz.valueOf(quizMode)
        );
    }
}
