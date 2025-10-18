package irysc.gachesefid.Dto.dashboard.Advisor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.MongoNumberLongDeserializer;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportProblemDigestDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sendFrom; // student, teacher
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest reporter;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest reportAbout;
    private String section; // teach, advice, question
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonDeserialize(using = MongoNumberLongDeserializer.class)
    private Long createdAt;
    private String desc;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean seen;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
}
