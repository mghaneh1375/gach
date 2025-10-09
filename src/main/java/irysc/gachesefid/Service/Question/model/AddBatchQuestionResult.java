package irysc.gachesefid.Service.Question.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionEntity> insertedItems;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> errors;
}
