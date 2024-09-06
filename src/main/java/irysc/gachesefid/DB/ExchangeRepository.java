package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ExchangeRepository extends Common {

    public ExchangeRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("exchange");
    }

}
