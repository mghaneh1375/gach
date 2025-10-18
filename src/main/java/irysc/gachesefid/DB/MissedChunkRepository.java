package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class MissedChunkRepository extends Common {
    public MissedChunkRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("missed_chunks");
    }
}
