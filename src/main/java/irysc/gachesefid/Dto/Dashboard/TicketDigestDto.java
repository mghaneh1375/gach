package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Comparator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDigestDto {
    @JsonSerialize(using = ObjectIdSerialization.class)
    private ObjectId id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDigest sender;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long sendAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = LongDateSerialization.class)
    private Long answerAt;
    private String title;
    private String description;

    public static TicketDigestDto convertDocToDto(Document doc) {
        Document student = doc.containsKey("student")
                ? doc.get("student", Document.class)
                : null;

        return TicketDigestDto
                .builder()
                .id(doc.getObjectId("_id"))
                .sendAt(doc.getLong("send_date"))
                .answerAt(doc.getLong("answer_date"))
                .description(
                        doc.getList("chats", Document.class)
                                .stream()
                                .filter(d -> d.getBoolean("is_for_user"))
                                .max(Comparator.comparing(o -> o.getLong("created_at")))
                                .get().getString("msg")
                )
                .title(doc.getString("title"))
                .sender(
                        student == null
                                ? null
                                : UserDigest
                                .builder()
                                .id(student.getObjectId("_id"))
                                .firstname(student.getString("first_name"))
                                .lastname(student.getString("last_name"))
                                .build()
                )
                .build();
    }
}
