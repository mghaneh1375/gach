package irysc.gachesefid.entity.quiz;

import irysc.gachesefid.entity.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuizEntity extends MongoEntity {
    private QuestionInQuizEntity questions;
}
