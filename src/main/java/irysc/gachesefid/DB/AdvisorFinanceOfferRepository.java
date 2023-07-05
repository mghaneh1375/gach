package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdvisorFinanceOfferRepository extends Common {

    public AdvisorFinanceOfferRepository() {
        init();
    }

    @Override
    void init() {
        table = "advisor_finance_offer";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
