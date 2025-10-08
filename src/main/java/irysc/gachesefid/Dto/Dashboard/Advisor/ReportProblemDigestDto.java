package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    private String sendForm; // student, teacher
    private UserDigest reporter;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest reportAbout;
    private String section; // teach, advice
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String desc;
    private Boolean seen;
    private List<String> tags;
}
