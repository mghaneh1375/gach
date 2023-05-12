package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class HWRepository extends Common {

    public final static String FOLDER = "hw";

    public HWRepository() {
        init();
    }

    @Override
    void init() {
        table = "hw";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
