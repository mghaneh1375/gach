package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ConfigDashboardRepository extends Common {

    public ConfigDashboardRepository() {
        init();
    }

    @Override
    void init() {
        table = "config_dashboard";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
