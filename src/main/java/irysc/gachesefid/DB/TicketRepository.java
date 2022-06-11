package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.NewAlert;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.newThingsCache;

public class TicketRepository extends Common {

    public final static String FOLDER = "tickets";

    public TicketRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("ticket");
    }

    @Override
    public void cleanRemove(Document doc) {

        cleanReject(doc);

        List<Document> chats = doc.getList("chats", Document.class);
        for (Document chat : chats) {
            List<String> files = chat.getList("files", String.class);

            for (String file : files)
                FileUtils.removeFile(file, TicketRepository.FOLDER);
        }

    }

    @Override
    public void cleanReject(Document doc) {

        if (doc.getString("status").equals("pending")) {

            List<Document> chats = doc.getList("chats", Document.class);

            if (chats.size() > 0)
                newThingsCache.put(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName(),
                        newThingsCache.get(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName()) - 1);
            else
                newThingsCache.put(NewAlert.NEW_TICKETS.getName(),
                        newThingsCache.get(NewAlert.NEW_TICKETS.getName()) - 1);
        }
    }
}
