package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;

public class AvatarRepository extends Common {

    public AvatarRepository() {
        init();
    }

    @Override
    void init() {
        table = "avatar";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    @Override
    public void cleanRemove(Document doc) {
        FileUtils.removeFile(doc.getString("file"), UserRepository.FOLDER);
    }
}
