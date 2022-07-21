package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class PackageRepository extends Common {

    public PackageRepository() {
        init();
    }

    @Override
    void init() {
        table = "quiz_package";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
