package irysc.gachesefid.Controllers.Config;

import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.DB.PackageLevelRepository.PACKAGE_LEVEL_FOLDER;
import static irysc.gachesefid.Main.GachesefidApplication.packageLevelRepository;
import static irysc.gachesefid.Main.GachesefidApplication.packageRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class PackageLevelController {

    public static String store(JSONObject jsonObject, MultipartFile file) {
        if(packageLevelRepository.exist(eq("title", jsonObject.getString("title"))))
            return generateErr("سطحی با این نام در سیستم موجود است");

        if (file.getSize() > StaticValues.ONE_MB)
            return generateErr("حجم تصویر باید کمتر از 1MB باشد");

        String fileType = FileUtils.uploadImageFile(file);
        if (fileType == null)
            return generateErr("فرمت فایل تصویر معتبر نمی باشد");

        String filename = FileUtils.uploadFile(file, PACKAGE_LEVEL_FOLDER);
        if (filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        Document doc = new Document("title", jsonObject.getString("title"))
                .append("icon", filename);

        packageLevelRepository.insertOne(doc);
        return generateSuccessMsg(
                "data",
                new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("title", doc.getString("title"))
                        .put("icon", STATICS_SERVER + PACKAGE_LEVEL_FOLDER + "/" + doc.getString("icon"))
        );
    }

    public static String remove(JSONArray items) {
        JSONArray excepts = new JSONArray();
        JSONArray removedIds = new JSONArray();
        for (int i = 0; i < items.length(); i++) {
            String item = items.getString(i);
            if(!ObjectId.isValid(item.toString()))
                excepts.put(i + 1);

            ObjectId oId = new ObjectId(item.toString());
            if(packageRepository.count(
                    and(
                            exists("level", true),
                            eq("level._id", oId)
                    )) > 0
            )
                excepts.put(i + 1);

            Document packageLevel = packageLevelRepository.findOneAndDelete(eq("_id", oId));
            FileUtils.removeFile(packageLevel.getString("icon"), PACKAGE_LEVEL_FOLDER);
            removedIds.put(oId.toString());
        }

        return Utility.returnRemoveResponse(excepts, removedIds);
    }

    public static String update(ObjectId id, JSONObject jsonObject, MultipartFile file) {
        Document level = packageLevelRepository.findById(id);
        if(level == null)
            return JSON_NOT_VALID_ID;

        if(!level.getString("title").equals(jsonObject.getString("title")) &&
                packageLevelRepository.exist(eq("title", jsonObject.getString("title")))
        )
            return generateErr("سطحی با این نام در سیستم موجود است");

        if(file != null) {
            if (file.getSize() > StaticValues.ONE_MB)
                return generateErr("حجم تصویر باید کمتر از 1MB باشد");

            String fileType = FileUtils.uploadImageFile(file);
            if (fileType == null)
                return generateErr("فرمت فایل تصویر معتبر نمی باشد");

            String filename = FileUtils.uploadFile(file, PACKAGE_LEVEL_FOLDER);
            if (filename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;

            FileUtils.removeFile(level.getString("icon"), PACKAGE_LEVEL_FOLDER);
            level.put("icon", filename);
        }
        level.put("title", jsonObject.getString("title"));
        packageLevelRepository.replaceOne(id, level);

        return generateSuccessMsg(
                "data",
                new JSONObject()
                        .put("title", level.getString("title"))
                        .put("icon", STATICS_SERVER + PACKAGE_LEVEL_FOLDER + "/" + level.getString("icon"))
        );
    }

    public static String list() {
        ArrayList<Document> levels = packageLevelRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();
        levels.forEach(document -> {
            jsonArray.put(
                    new JSONObject()
                            .put("id", document.getObjectId("_id").toString())
                            .put("title", document.getString("title"))
                            .put("icon", STATICS_SERVER + PACKAGE_LEVEL_FOLDER + "/" + document.getString("icon"))
            );
        });

        return generateSuccessMsg("data", jsonArray);
    }
}
