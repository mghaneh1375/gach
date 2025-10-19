package irysc.gachesefid.Dto.content;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MissedChunkDto {
    private String contentTitle;
    private String sessionTitle;
    private String video;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;

    public static MissedChunkDto buildFromDoc(Document doc) {
        return MissedChunkDto
                .builder()
                .contentTitle(doc.getString("content"))
                .sessionTitle(doc.getString("session"))
                .video(doc.getString("video"))
                .createdAt(doc.getLong("created_at"))
                .build();
    }
}
