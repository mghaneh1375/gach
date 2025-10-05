package irysc.gachesefid.Service.Question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBatchQuestionServiceResponse {
    private String message;
    private List<String> errors;
}
