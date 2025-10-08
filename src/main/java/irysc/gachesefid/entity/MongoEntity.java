package irysc.gachesefid.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.ObjectArrayDeserializer;
import irysc.gachesefid.Dto.Deserializer.ObjectIdDeserializer;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerialization;
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
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    @JsonSerialize(using = ObjectIdSerialization.class)
    private ObjectId id;
    private Long created;
}
