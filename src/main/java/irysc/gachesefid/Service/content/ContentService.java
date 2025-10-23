package irysc.gachesefid.Service.content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.content.MissedChunkDto;
import irysc.gachesefid.Dto.content.MissedAttachDto;
import irysc.gachesefid.Dto.content.MissedDto;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Main.GachesefidApplication.missedChunkRepository;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;
import static irysc.gachesefid.Utility.Utility.hasMissed;

@Service
public class ContentService {

    @Value(value = "${static.server.url}")
    private String STATIC_SERVER_URL;

    private final static String[] faNumbers = new String[] {
            "اول", "دوم", "سوم", "چهارم", "پنجم",
            "ششم", "هفتم", "هشتم", "نهم", "دهم"
    };

    public ResponseEntity<ResponseDto<MissedDto>> findMissed() {
        List<MissedAttachDto> missedAttaches = new ArrayList<>();
        ArrayList<Document> contents = contentRepository.find(exists("sessions.0"), null);
        for (Document content : contents) {
            if(!content.containsKey("sessions") ||
                    content.getList("sessions", Document.class).isEmpty()
            )
                continue;

            for(Document session : content.getList("sessions", Document.class)) {
                List<String> missedAttachFiles = new ArrayList<>();
                if(session.containsKey("attaches") &&
                        !session.getList("attaches", Document.class).isEmpty()
                ) {
                    int counter = 0;
                    for(Document attach : session.getList("attaches", Document.class)) {
                        if(hasMissed(String.format("%s%s/%s", STATIC_SERVER_URL, ContentRepository.FOLDER, attach.getString("filename"))))
                            missedAttachFiles.add(attach.getString("title"));
                        counter++;
                    }
                }

                boolean isChunked = session.containsKey("chunk_at");
                if(!missedAttachFiles.isEmpty() || !isChunked) {
                    missedAttaches.add(
                            MissedAttachDto
                                    .builder()
                                    .contentTitle(content.getString("title"))
                                    .sessionTitle(session.get("title").toString())
                                    .missedAttaches(missedAttachFiles)
                                    .isChunked(isChunked)
                                    .build()
                    );
                }
            }
        }

        List<Document> list = missedChunkRepository.find(
                gte("created_at", System.currentTimeMillis() - ONE_DAY_MIL_SEC),
                null
        );
        List<MissedChunkDto> missedChunkList = new ArrayList<>();
        for (Document itr : list) {
            missedChunkList.add(MissedChunkDto.buildFromDoc(itr));
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(MissedDto.class)
                        .status("ok")
                        .data(
                                MissedDto
                                        .builder()
                                        .missedChunks(missedChunkList)
                                        .missedAttaches(missedAttaches)
                                        .build()
                        )
                        .build(),
                HttpStatus.OK
        );
    }
}
