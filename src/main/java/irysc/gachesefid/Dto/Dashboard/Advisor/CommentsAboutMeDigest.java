package irysc.gachesefid.Dto.Dashboard.Advisor;

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

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsAboutMeDigest {
    private UserDigest author;
    @JsonSerialize(using = ObjectIdSerialization.class)
    private ObjectId id;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    private String title;
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
                .id(doc.getObjectId("_id"))
                .comment(doc.getString("comment"))
                .createdAt(doc.getLong("created_at"))
                .section(doc.getString("section"))
                .title(doc.getString("title"))
                .build();
    }

}
