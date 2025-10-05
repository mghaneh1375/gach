package irysc.gachesefid.Service.Quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import irysc.gachesefid.Service.MyService;
import irysc.gachesefid.Service.Quiz.model.AddQuestionToQuizResult;
import irysc.gachesefid.entity.QuestionEntity;
import irysc.gachesefid.entity.quiz.QuizEntity;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;

@Service
public class IryscQuizService extends MyService implements QuizService {

    @Override
    public AddQuestionToQuizResult addQuestionsToQuizAutomatically(
            QuizEntity quizEntity,
            List<QuestionEntity> questionEntities
    ) {

        return null;
    }

    @Override
    public QuizEntity find(ObjectId quizId) {
        Document quiz = iryscQuizRepository.findById(quizId);
        try {
            return mapper.readValue(quiz.toJson(), QuizEntity.class);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
