package irysc.gachesefid.schedule;

import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.lte;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Main.GachesefidApplication.missedChunkRepository;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;
import static irysc.gachesefid.Utility.Utility.hasMissed;

@Component
public class MissedChunksScheduler {

    @Value(value = "${video.server.url}")
    private String VIDEO_SERVER_URL;

    private final boolean shouldRunFundMissedChunks;
    private final int maxLengthForMissed;
    private List<Session> sessions = null;
    private int currIndex;

    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    private static class Session {
        private String contentTitle;
        private String sessionTitle;
        private String video;
    }

    public MissedChunksScheduler(
            @Value("${should.find.missed.chunks}") boolean shouldRunFundMissedChunks,
            @Value("${max.length.for.missed}") int maxLengthForMissed

    ) {
        this.shouldRunFundMissedChunks = shouldRunFundMissedChunks;
        this.maxLengthForMissed = maxLengthForMissed;
    }


    @Scheduled(cron = "${missed.chunks.cron}")
    public void findMissedChunks() {
        if (!shouldRunFundMissedChunks || sessions == null || sessions.isEmpty())
            return;

        int reminder = sessions.size() - currIndex;
        List<WriteModel<Document>> writes = new ArrayList<>();
        final long curr = System.currentTimeMillis();

        sessions
                .stream()
                .skip(currIndex)
                .limit(Math.min(maxLengthForMissed, reminder))
                .filter(session -> hasMissed(session.video))
                .forEach(session -> writes.add(new InsertOneModel<>(
                        new Document("content", session.contentTitle)
                                .append("session", session.sessionTitle)
                                .append("video", session.video)
                                .append("created_at", curr)
                )));

        if(!writes.isEmpty())
            missedChunkRepository.bulkWrite(writes);

        currIndex += Math.min(maxLengthForMissed, reminder);
    }

    @Scheduled(cron = "${find.videos.cron}")
    public void findVideos() {
        if (!shouldRunFundMissedChunks)
            return;

        sessions = new ArrayList<>();
        currIndex = 0;

        ArrayList<Document> contents = contentRepository.find(exists("sessions.0"), null);
        for (Document content : contents) {
            if (!content.containsKey("sessions") ||
                    content.getList("sessions", Document.class).isEmpty()
            )
                continue;

            for (Document session : content.getList("sessions", Document.class)) {
                if (session.containsKey("video")) {
                    String video;
                    if (!(Boolean) session.getOrDefault("external_link", false)) {
                        String folderName = session.getString("video").split("\\.mp4")[0];
                        video = VIDEO_SERVER_URL + "videos/" + folderName + "/playlist.m3u8";
                    } else
                        video = session.getString("video");

                    sessions.add(
                            Session
                                    .builder()
                                    .contentTitle(content.getString("title"))
                                    .sessionTitle(session.getString("title"))
                                    .video(video)
                                    .build()
                    );
                }
            }
        }

        missedChunkRepository.deleteMany(
                lte("created_at", System.currentTimeMillis() - ONE_DAY_MIL_SEC)
        );
    }
}
