package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class UserQuestionRepository extends Common {

    @Override
    void init() {
        table = "question";
        secKey = "organization_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}
