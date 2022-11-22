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

        if(session.containsKey("img"))
            FileUtils.removeFile(session.getString("img"), FOLDER);

        if(session.containsKey("attaches")) {
            List<String> attaches = session.getList("attaches", String.class);
            for(String attach : attaches)
                FileUtils.removeFile(attach, FOLDER);
        }

        if(session.containsKey("videos")) {
            List<String> videos= session.getList("videos", String.class);
            for(String video : videos)
                FileUtils.removeFile(video, FOLDER);
        }
    }
}
