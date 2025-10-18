package irysc.gachesefid.Service.content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.content.MissedDto;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.exists;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.Utility.hasMissed;

@Service
public class ContentService {

    @Value(value = "${static.server.url}")
    private String STATIC_SERVER_URL;

    private final static String[] faNumbers = new String[] {
            "اول", "دوم", "سوم", "چهارم", "پنجم",
            "ششم", "هفتم", "هشتم", "نهم", "دهم"
    };

    public ResponseEntity<ResponseDto<List<MissedDto>>> findMissed() {
        List<MissedDto> missedList = new ArrayList<>();
        ArrayList<Document> contents = contentRepository.find(exists("sessions.0"), null);
        for (Document content : contents) {
            if(!content.containsKey("sessions") ||
                    content.getList("sessions", Document.class).isEmpty()
            )
                continue;

            for(Document session : content.getList("sessions", Document.class)) {
                List<String> missedAttaches = new ArrayList<>();
                if(session.containsKey("attaches") &&
                        !session.getList("attaches", String.class).isEmpty()) {
                    int counter = 0;
                    for(String attach : session.getList("attaches", String.class)) {
                        if(hasMissed(String.format("%s%s/%s", STATIC_SERVER_URL, ContentRepository.FOLDER, attach)))
                            missedAttaches.add(String.format("پیوست %s", faNumbers[counter]));
                        counter++;
                    }
                }

                boolean isChunked = session.containsKey("chunk_at");
                if(!missedAttaches.isEmpty() || !isChunked) {
                    missedList.add(
                            MissedDto
                                    .builder()
                                    .contentTitle(content.getString("title"))
                                    .sessionTitle(session.get("title").toString())
                                    .missedAttaches(missedAttaches)
                                    .isChunked(isChunked)
                                    .build()
                    );
                }
            }
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builderList(MissedDto.class)
                        .status("ok")
                        .data(missedList)
                        .build(),
                HttpStatus.OK
        );
    }
}
