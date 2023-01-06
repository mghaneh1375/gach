package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class SMSQueueRepository extends Common {

    public SMSQueueRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("sms_queue");
    }
}
