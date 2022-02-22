package irysc.gachesefid.Controllers.Config;

import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.avatarRepository;
import static irysc.gachesefid.Main.GachesefidApplication.configRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class AvatarController {

    public static String store(MultipartFile file) {

        String filename = FileUtils.uploadFile(file, UserRepository.FOLDER);
        if(filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        avatarRepository.insertOne(new Document("file", filename)
                .append("used", 0)
        );

        return JSON_OK;
    }

    public static String delete(ObjectId avatarId) {

        Document avatar = avatarRepository.findOneAndDelete(
                and(
                        eq("_id", avatarId),
                        eq("used", 0)
                )
        );

        if(avatar == null)
            return Utility.generateErr("نفراتی از این آواتار استفاده می کنند و شما مجاز به حذف آن نیستید.");

        return JSON_OK;
    }

    public static String get() {

        ArrayList<Document> avatars = avatarRepository.find(null, null);
        Document config = Utility.getConfig();
        JSONArray data = new JSONArray();

        ObjectId defaultAvatar = config.getObjectId("default_avatar");

        for(Document avatar : avatars) {
            data.put(new JSONObject()
                    .put("file", STATICS_SERVER + UserRepository.FOLDER + "/" + avatar.getString("file"))
                    .put("id", avatar.getObjectId("_id").toString())
                    .put("isDefault", avatar.getObjectId("_id").equals(defaultAvatar))
            );
        }

        return Utility.generateSuccessMsg(
                "data", data
        );
    }

    public static String edit(ObjectId avatarId, MultipartFile file) {

        Document avatar = avatarRepository.findById(avatarId);
        if(avatar == null)
            return JSON_NOT_VALID_ID;

        String filename = FileUtils.uploadFile(file, UserRepository.FOLDER);
        if(filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        FileUtils.removeFile(avatar.getString("file"), UserRepository.FOLDER);
        avatar.put("file", filename);

        avatarRepository.updateOne(avatarId,
                set("file", filename)
        );

        return JSON_OK;
    }

    public static String setDefault(ObjectId avatarId) {

        Document avatar = avatarRepository.findById(avatarId);
        if(avatar == null)
            return JSON_NOT_VALID_ID;

        Document config = Utility.getConfig();
        config.put("default_avatar", avatarId);

        configRepository.updateOne(config.getObjectId("_id"),
                set("default_avatar", avatarId)
        );

        return JSON_OK;
    }
}
