package irysc.gachesefid.Service.Quiz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AddQuestionToQuizResult {
    private List<String> errors;

    public AddQuestionToQuizResult() {
        errors = new ArrayList<>();
    }
}
