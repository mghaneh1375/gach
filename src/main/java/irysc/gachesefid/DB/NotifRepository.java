package irysc.gachesefid.DB;

import irysc.gachesefid.Dto.dashboard.NotifDigestDto;
import irysc.gachesefid.Main.GachesefidApplication;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Utility.StaticValues.NOTIF_DIGEST;

public class NotifRepository extends Common {

    public NotifRepository() {
        init();
    }

    @Override
    void init() {
        table = "notif";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public List<NotifDigestDto> notifs(List<ObjectId> ids) {
        ArrayList<Document> documents = find(in("_id", ids), NOTIF_DIGEST);
        return documents
                .stream()
                .map(document ->
                        NotifDigestDto
                                .builder()
                                .id(document.getObjectId("_id"))
                                .createdAt(document.getLong("created_at"))
                                .title(document.getString("title"))
                                .build()
                )
                .sorted(Collections.reverseOrder(Comparator.comparing(NotifDigestDto::getCreatedAt)))
                .collect(Collectors.toList());
    }
}
