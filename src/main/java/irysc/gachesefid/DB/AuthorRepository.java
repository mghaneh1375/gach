package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

public class AuthorRepository extends Common {

    public AuthorRepository() {
        init();
    }

    @Override
    void init() {
        table = "author";
        secKey = "code";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
