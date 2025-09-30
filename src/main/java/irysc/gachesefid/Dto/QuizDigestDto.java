package irysc.gachesefid.Dto;

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
    private Long startRegistry;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long endRegistry;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long start;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long end;
    private List<String> tags;
    private String mode;

    public static QuizDigestDto buildFromDoc(Document doc) {
        return QuizDigestDto
                .builder()
                .id(doc.getObjectId("_id"))
                .title(doc.getString("title"))
                .mode(doc.getString("mode"))
                .tags(doc.containsKey("tags")
                        ? doc.getList("tags", String.class)
                        : null
                )
                .startRegistry(doc.containsKey("start_registry")
                        ? doc.getLong("start_registry")
                        : null
                )
                .endRegistry(doc.containsKey("end_registry")
                        ? doc.getLong("end_registry")
                        : null
                )
                .start(doc.getLong("start"))
                .end(doc.containsKey("end") ? doc.getLong("end") : null)
                .build();
    }

}
