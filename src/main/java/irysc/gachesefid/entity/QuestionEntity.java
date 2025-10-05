package irysc.gachesefid.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class QuestionEntity extends MongoEntity {
    @JsonProperty(value = "kind_question")
    private String kindQuestion;
    private Object answer;
    @Builder.Default
    private Boolean visibility = true;
    @JsonProperty(value = "answer_file")
    private String answerFile;
    @JsonProperty(value = "question_file")
    private String questionFile;
    private String author;
    @JsonProperty(value = "is_public")
    @Builder.Default
    private Boolean isPublic = true;
    private Set<Object> tags;
    @JsonProperty(value = "needed_line")
    private Integer neededLine;
    @JsonProperty(value = "choices_count")
    private Integer choicesCount;
    @JsonProperty(value = "sentences_count")
    private Integer sentencesCount;
    @JsonProperty(value = "organization_id")
    private String organizationId;
    private String level;
    @JsonProperty(value = "needed_time")
    private Integer neededTime;
    @Builder.Default
    private Integer used = 0;
    private Double telorance;
    private Object year;
    @JsonProperty(value = "subject_id")
    private ObjectId subjectId;

    public QuestionEntity() {
        setId(new ObjectId());
        isPublic = true;
        visibility = true;
        hasError = false;
        addedToQuiz = false;
        used = 0;
        tags = new HashSet<>();
    }

    public void incUsed() {
        used++;
    }

    @JsonIgnore
    private String subjectCode;
    @JsonIgnore
    private String authorCode;
    @JsonIgnore
    @Builder.Default
    private boolean hasError = false;
    @JsonIgnore
    private Double mark;
    @JsonIgnore
    @Builder.Default
    private boolean addedToQuiz = false;
}
