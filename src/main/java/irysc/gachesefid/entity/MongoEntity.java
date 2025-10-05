package irysc.gachesefid.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MongoEntity {
    @JsonProperty("_id")
    private ObjectId id;
    private Long created;
}
