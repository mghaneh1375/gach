package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.JustDateSerialization;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsAboutMeDigest {
    private UserDigest author;
    @JsonSerialize(using = JustDateSerialization.class)
    private Long createdAt;
    private String comment;
    private String section;

    public static CommentsAboutMeDigest buildFromDoc(Document doc) {
        Document author = doc.get("author", Document.class);
        return CommentsAboutMeDigest
                .builder()
                .author(
                        UserDigest
                                .builder()
                                .id(author.getObjectId("_id"))
                                .firstname(author.getString("first_name"))
                                .lastname(author.getString("last_name"))
                                .pic(author.getString("pic"))
                                .build()
                )
                .comment(doc.getString("comment"))
                .createdAt(doc.getLong("created_at"))
                .section(doc.getString("section"))
                .build();
    }

}
