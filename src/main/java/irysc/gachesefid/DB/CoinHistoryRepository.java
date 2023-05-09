package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CoinHistoryRepository extends Common {

    public CoinHistoryRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("coin_history");
    }

}
