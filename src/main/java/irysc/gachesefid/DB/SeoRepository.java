package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class SeoRepository extends Common {

    public SeoRepository() {
        init();
    }

    @Override
    void init() {
        table = "seo";
        secKey = "package_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
