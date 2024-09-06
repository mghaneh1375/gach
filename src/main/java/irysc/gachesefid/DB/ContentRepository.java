package irysc.gachesefid.DB;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;


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

    public Integer getTeacherContentsBuyersSize(ObjectId teacherId) {
        List<Bson> filters = new ArrayList<>() {{
            add(match(eq("teacher_ids", teacherId)));
            add(new BasicDBObject("$group",
                            new BasicDBObject("_id", null)
                                    .append("total_sum", new BasicDBObject("$sum", new BasicDBObject("$size", "$users")))
                    )
            );
        }};

        AggregateIterable<Document> aggregate = documentMongoCollection.aggregate(filters);

        for (Document doc : aggregate)
            return doc.getInteger("total_sum");

        return 0;
    }

    @Override
    public void cleanRemove(Document doc) {

        if (doc.containsKey("img"))
            FileUtils.removeFile(doc.getString("img"), FOLDER);

        if (doc.containsKey("sessions")) {

            List<Document> sessions = doc.getList("sessions", Document.class);
            for (Document session : sessions)
                removeSession(session);

        }

        deleteOne(doc.getObjectId("_id"));
    }

    public void removeSession(Document session) {

        if (session.containsKey("attaches")) {
            List<String> attaches = session.getList("attaches", String.class);
            for (String attach : attaches)
                FileUtils.removeFile(attach, FOLDER);
        }

        if (session.containsKey("video") && !(Boolean) session.getOrDefault("external_link", false))
            FileUtils.removeFile(session.getString("video"), FOLDER);
    }
}
