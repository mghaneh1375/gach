package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CertificateRepository extends Common {

    public CertificateRepository() {
        init();
    }
    public static final String FOLDER = "certificationsPDF";

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("certificate");
    }

}
