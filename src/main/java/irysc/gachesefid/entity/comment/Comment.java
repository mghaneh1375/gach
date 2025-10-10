package irysc.gachesefid.entity.comment;

import com.fasterxml.jackson.annotation.JsonProperty;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.entity.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Comment extends MongoEntity {
    @JsonProperty(value = "user_id")
    private ObjectId userId;
    @JsonProperty(value = "user_id")
    private String status; // pending, reject, accept
    @JsonProperty(value = "ref_id")
    private ObjectId refId;
    private String comment;
    private CommentSection section;
}
