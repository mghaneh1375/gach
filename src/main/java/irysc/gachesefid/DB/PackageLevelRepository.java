package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class PackageLevelRepository extends Common {

    public static final String PACKAGE_LEVEL_FOLDER = "package_level";
    public PackageLevelRepository() {
        init();
    }

    @Override
    void init() {
        table = "package_level";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
