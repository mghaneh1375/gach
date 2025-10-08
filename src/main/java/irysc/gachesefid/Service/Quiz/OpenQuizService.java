package irysc.gachesefid.Service.Quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Quiz.Utility;
import irysc.gachesefid.Dto.Serializer.MongoByteArraySerializer;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Service.MyService;
import irysc.gachesefid.Service.Quiz.model.AddQuestionToQuizResult;
import irysc.gachesefid.entity.QuestionEntity;
import irysc.gachesefid.entity.quiz.QuestionInQuizEntity;
import irysc.gachesefid.entity.quiz.QuizEntity;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.openQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Utility.Utility.batchRowErr;

@Service
public class OpenQuizService extends MyService implements QuizService {

    @Override
    public AddQuestionToQuizResult addQuestionsToQuizAutomatically(
            QuizEntity quizEntity,
            List<QuestionEntity> questionEntities
    ) {
        QuestionInQuizEntity questions = quizEntity.getQuestions();

        List<Double> marks = questions.getMarks() != null ? questions.getMarks() : new ArrayList<>();
        List<ObjectId> ids = questions.getIds() != null ? questions.getIds() : new ArrayList<>();

        HashMap<ObjectId, Integer> allUsed = new HashMap<>();
        AddQuestionToQuizResult addQuestionToQuizResults = new AddQuestionToQuizResult();
        int counter = 0;

        for(QuestionEntity questionEntity : questionEntities) {
            counter++;

            if (questionEntity.getKindQuestion().equalsIgnoreCase(QuestionType.TASHRIHI.getName())) {
                addQuestionToQuizResults.getErrors().add(batchRowErr(counter, "نوع سوال نباید تشریحی باشد"));
                continue;
            }

            questionEntity.incUsed();
            allUsed.put(questionEntity.getId(), questionEntity.getUsed());
            marks.add(questionEntity.getMark());
            ids.add(questionEntity.getId());

            List<WriteModel<Document>> writes = new ArrayList<>();
            for (ObjectId oId : allUsed.keySet()) {
                writes.add(new UpdateOneModel<>(
                        eq("_id", oId),
                        set("used", allUsed.get(oId))
                ));
            }
            if (writes.size() > 0)
                questionRepository.bulkWrite(writes);
        }

        byte[] answersByte;
        if (questions.getAnswers() != null)
            answersByte = questions.getAnswers();
        else
            answersByte = new byte[0];

        for(QuestionEntity questionEntity : questionEntities) {
            answersByte = Utility.addAnswerToByteArr(answersByte, questionEntity.getKindQuestion(),
                    questionEntity.getKindQuestion().equalsIgnoreCase(QuestionType.TEST.getName())
                            ? new PairValue(questionEntity.getChoicesCount(), questionEntity.getAnswer())
                            : questionEntity.getAnswer()
            );
        }

        questions.setAnswers(answersByte);
        questions.setMarks(marks);
        questions.setIds(ids);
        quizEntity.setQuestions(questions);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule()
                .addSerializer(byte[].class, new MongoByteArraySerializer()));

        String json = null;
        try {
            json = mapper.writeValueAsString(questions);
            Document bsonDoc = Document.parse(json);
            bsonDoc.put("_ids", bsonDoc.getList("_ids", Object.class).stream().map(o -> new ObjectId(o.toString())).collect(Collectors.toList()));
            openQuizRepository.updateOneWithClearCache(quizEntity.getId(), set("questions", bsonDoc));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return addQuestionToQuizResults;
    }

    @Override
    public QuizEntity find(ObjectId quizId) {
        Document quiz = openQuizRepository.findById(quizId);
        if(quizId == null)
            throw new InvalidFieldsException("quiz Id is wrong");
        try {
            return simpleMapper.readValue(quiz.toJson(), QuizEntity.class);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            throw new InvalidFieldsException("fail to convert doc to entity");
        }
    }
}
