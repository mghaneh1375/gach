package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ActivationRepository extends Common {

    public ActivationRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("activation");
    }

}
