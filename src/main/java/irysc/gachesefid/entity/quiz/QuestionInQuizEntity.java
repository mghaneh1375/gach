package irysc.gachesefid.entity.quiz;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ObjectArraySerializer;
import irysc.gachesefid.Dto.Deserializer.MongoByteArrayDeserializer;
import irysc.gachesefid.Dto.Serializer.MongoByteArraySerializer;
import irysc.gachesefid.Dto.Serializer.ObjectIdListSerializer;
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
    @JsonSerialize(using = ObjectIdListSerializer.class)
    private List<ObjectId> ids;
    @JsonDeserialize(using = MongoByteArrayDeserializer.class)
    @JsonSerialize(using = MongoByteArraySerializer.class)
    private byte[] answers;
}
