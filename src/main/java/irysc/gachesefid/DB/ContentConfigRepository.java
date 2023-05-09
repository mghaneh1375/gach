package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

import java.util.List;


public class ContentConfigRepository extends Common {

    public ContentConfigRepository() {
        init();
    }

    @Override
    void init() {
        table = "content_config";
        secKey = "row";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
