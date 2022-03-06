package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CertificateRepository extends Common {

    public CertificateRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("certificate");
    }

}
