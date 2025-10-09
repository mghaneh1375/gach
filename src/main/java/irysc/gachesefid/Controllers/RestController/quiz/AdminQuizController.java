package irysc.gachesefid.Controllers.RestController.quiz;

import irysc.gachesefid.Dto.QuizDigestDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.Quiz.IryscQuizService;
import irysc.gachesefid.Service.Quiz.OpenQuizService;
import irysc.gachesefid.Validator.EnumValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/api/admin/quiz")
public class AdminQuizController extends Router {

    @Autowired
    private OpenQuizService openQuizService;

    @Autowired
    private IryscQuizService iryscQuizService;

    @GetMapping("/digests")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<QuizDigestDto>>> digests(
            @RequestParam(name = "mode") @NotBlank @EnumValidator(enumClazz = AllKindQuiz.class) String mode
    ) {
        List<QuizDigestDto> digestDtos = null;
        if(mode.equalsIgnoreCase(AllKindQuiz.OPEN.getName()))
            digestDtos = openQuizService.digests();
        else if(mode.equalsIgnoreCase(AllKindQuiz.IRYSC.getName()))
            digestDtos = iryscQuizService.digests();

        return new ResponseEntity<>(
                ResponseDto
                        .builderList(QuizDigestDto.class)
                        .data(digestDtos)
                        .status("ok")
                        .build(),
                HttpStatus.OK
        );
    }

}
