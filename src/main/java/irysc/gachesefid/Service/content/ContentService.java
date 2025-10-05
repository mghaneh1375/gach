package irysc.gachesefid.Service.content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.content.MissedDto;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.exists;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;

@Service
public class ContentService {

    @Value(value = "${static.server.url}")
    private String STATIC_SERVER_URL;
    @Value(value = "${video.server.url}")
    private String VIDEO_SERVER_URL;

    private final static String[] faNumbers = new String[] {
            "اول", "دوم", "سوم", "چهارم", "پنجم",
            "ششم", "هفتم", "هشتم", "نهم", "دهم"
    };

    public ResponseEntity<ResponseDto<List>> findMissed() {
        List<MissedDto> missedList = new ArrayList<>();
//        ArrayList<Document> contents = contentRepository.findLimited(exists("_id"), null, Sorts.descending("created_at"), 0, 10);
        ArrayList<Document> contents = contentRepository.find(exists("sessions.0"), null);
        for (Document content : contents) {
            if(!content.containsKey("sessions") ||
                    content.getList("sessions", Document.class).size() == 0
            )
                continue;

            for(Document session : content.getList("sessions", Document.class)) {
                List<String> missedAttaches = new ArrayList<>();
                if(session.containsKey("attaches") &&
                        session.getList("attaches", String.class).size() > 0) {
                    int counter = 0;
                    for(String attach : session.getList("attaches", String.class)) {
                        if(hasMissed(String.format("%s%s/%s", STATIC_SERVER_URL, ContentRepository.FOLDER, attach)))
                            missedAttaches.add(String.format("پیوست %s", faNumbers[counter]));
                        counter++;
                    }
                }
                boolean isChunked = false;
                boolean isVideoMissed = false;

                if(session.containsKey("chunk_at")) {
                    isChunked = true;
                    String video = null;

//                    if(session.containsKey("video")) {
//                        if (!(Boolean) session.getOrDefault("external_link", false)) {
//                            String folderName = session.getString("video").split("\\.mp4")[0];
//                            video = VIDEO_SERVER_URL + "videos/" + folderName + "/playlist.m3u8";
//                        } else
//                            video = session.getString("video");
//                    }

//                    if(video == null || hasMissed(video))
//                        isVideoMissed = true;
                }

                if(missedAttaches.size() > 0 || !isChunked || isVideoMissed) {
                    missedList.add(
                            MissedDto
                                    .builder()
                                    .contentTitle(content.getString("title"))
                                    .sessionTitle(session.get("title").toString())
                                    .missedAttaches(missedAttaches)
                                    .isChunked(isChunked)
                                    .isVideoMissed(isVideoMissed)
                                    .build()
                    );
                }
            }
        }

        return new ResponseEntity<>(
                ResponseDto.builder(List.class)
                        .status("ok")
                        .data(missedList)
                        .build(),
                HttpStatus.OK
        );
    }

    private static long checkCount = 0l;

    private boolean hasMissed(String address) {
        try {
//            if(checkCount >= 50 && checkCount % 50 == 0)
//                Thread.sleep(10000);

            checkCount++;
            URL url = new URL(address);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            int responseCode = huc.getResponseCode();
            return responseCode == 404;
        }
        catch (Exception ignore) {
            return true;
        }
    }
}
