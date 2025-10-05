package irysc.gachesefid.entity.quiz;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionInQuizEntity {
    private List<Double> marks;
    @JsonProperty("_ids")
    private List<ObjectId> ids;
    private byte[] answers;
}
