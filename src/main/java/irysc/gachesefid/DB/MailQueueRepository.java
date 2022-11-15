package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class MailQueueRepository extends Common {

    public MailQueueRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("mail_queue");
    }
}
