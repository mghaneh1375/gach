package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class QueueJobRepository extends Common {

    public QueueJobRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("queue_job");
    }
}
