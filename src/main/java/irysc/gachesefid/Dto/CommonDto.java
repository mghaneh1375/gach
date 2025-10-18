package irysc.gachesefid.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.MongoNumberLongDeserializer;
import irysc.gachesefid.Dto.Deserializer.ObjectIdDeserializer;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@AllArgsConstructor
@NoArgsConstructor
public class CommonDto {
    @JsonProperty(value = "_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;
    @JsonProperty(value = "created_at")
    @JsonDeserialize(using = MongoNumberLongDeserializer.class)
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
}
