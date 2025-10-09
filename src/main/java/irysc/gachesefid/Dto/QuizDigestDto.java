package irysc.gachesefid.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@Builder
public class QuizDigestDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String title;
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long startRegistry;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = LongDateSerialization.class)
    private Long endRegistry;
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long start;
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long end;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mode;

    public static QuizDigestDto buildFromDoc(Document doc) {
        return QuizDigestDto
                .builder()
                .id(doc.getObjectId("_id"))
                .title(doc.getString("title"))
                .mode(
                        doc.containsKey("mode")
                                ? doc.getString("mode")
                                : null
                )
                .tags(
                        doc.containsKey("tags")
                                ? doc.getList("tags", String.class)
                                : null
                )
                .startRegistry(
                        doc.containsKey("start_registry")
                                ? doc.getLong("start_registry")
                                : null
                )
                .endRegistry(
                        doc.containsKey("end_registry")
                                ? doc.getLong("end_registry")
                                : null
                )
                .start(
                        doc.containsKey("start")
                                ? doc.getLong("start")
                                : null
                )
                .end(
                        doc.containsKey("end")
                                ? doc.getLong("end")
                                : null
                )
                .build();
    }

}
