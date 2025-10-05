package irysc.gachesefid.Service.Question.model;

import irysc.gachesefid.entity.QuestionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBatchQuestionResult {
    private List<QuestionEntity> insertedItems;
    private List<String> errors;
}
