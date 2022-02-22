package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AvatarRepository extends Common {

    public AvatarRepository() {
        init();
    }

    @Override
    void init() {
        table = "avatar";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
