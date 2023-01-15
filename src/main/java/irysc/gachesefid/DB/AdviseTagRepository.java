package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdviseTagRepository extends Common {

    public AdviseTagRepository() {
        init();
    }

    @Override
    void init() {
        table = "advise_tag";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
