package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class MailRepository extends Common {

    public MailRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("mail");
    }
}
