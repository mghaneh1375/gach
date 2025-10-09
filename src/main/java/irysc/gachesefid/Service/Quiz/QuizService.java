package irysc.gachesefid.Service.Quiz;

import irysc.gachesefid.Dto.QuizDigestDto;
import irysc.gachesefid.Service.Quiz.model.AddQuestionToQuizResult;
import irysc.gachesefid.entity.QuestionEntity;
import irysc.gachesefid.entity.quiz.QuizEntity;
import org.bson.types.ObjectId;

import java.util.List;

public interface QuizService {

    // guarantee new questions used for adding to quiz
    AddQuestionToQuizResult addQuestionsToQuizAutomatically(
            QuizEntity quizEntity,
            List<QuestionEntity> questionEntities
    );

    QuizEntity find(ObjectId quizId);

    List<QuizDigestDto> digests();
}
