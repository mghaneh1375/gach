package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdvisorMeetingRepository extends Common {

    public AdvisorMeetingRepository() {
        init();
    }

    @Override
    void init() {
        table = "advisor_meeting";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
