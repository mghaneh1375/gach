package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.List;


public class ContentRepository extends Common {

    public static final String FOLDER = "content";
    public ContentRepository() {
        init();
    }

    @Override
    void init() {
        table = "content";
        secKey = "slug";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    @Override
    public void cleanRemove(Document doc) {

        if(doc.containsKey("img"))
            FileUtils.removeFile(doc.getString("img"), FOLDER);

        if(doc.containsKey("sessions")) {

            List<Document> sessions = doc.getList("sessions", Document.class);
            for(Document session : sessions)
                removeSession(session);

        }

        deleteOne(doc.getObjectId("_id"));
    }

    public void removeSession(Document session) {

        if(session.containsKey("attaches")) {
            List<String> attaches = session.getList("attaches", String.class);
            for(String attach : attaches)
                FileUtils.removeFile(attach, FOLDER);
        }

        if(session.containsKey("video") && !(Boolean)session.getOrDefault("external_link", false))
            FileUtils.removeFile(session.getString("video"), FOLDER);
    }
}
