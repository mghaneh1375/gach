package irysc.gachesefid.Dto.Dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.Collections;
import java.util.Comparator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDigestDto {
    private UserDigest sender;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long sendAt;
    private String title;
    private String description;

    public static TicketDigestDto convertDocToDto(Document doc) {
        Document student = doc.get("student", Document.class);
        return TicketDigestDto
                .builder()
                .sendAt(doc.getLong("send_date"))
                .description(
                        doc.getList("chats", Document.class)
                                .stream()
                                .filter(d -> d.getBoolean("is_for_user"))
                                .min(Collections.reverseOrder(Comparator.comparing(o -> o.getLong("created_at"))))
                                .get().getString("msg")
                )
                .title(doc.getString("title"))
                .sender(
                        UserDigest
                                .builder()
                                .id(student.getObjectId("_id"))
                                .firstname(student.getString("first_name"))
                                .lastname(student.getString("last_name"))
                                .build()
                )
                .build();
    }
}
