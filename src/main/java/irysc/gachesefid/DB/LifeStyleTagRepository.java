package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class LifeStyleTagRepository extends Common {

    public LifeStyleTagRepository() {
        init();
    }

    @Override
    void init() {
        table = "life_style_tag";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
