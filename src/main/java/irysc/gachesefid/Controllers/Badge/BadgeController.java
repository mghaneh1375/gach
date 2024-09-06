package irysc.gachesefid.Controllers.Badge;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.badgeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userBadgeRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class BadgeController {

    static final String FOLDER = "badges";

    // ##################### ADMIN SECTION ####################

    public static String add(
            MultipartFile locked,
            MultipartFile unlocked,
            JSONObject jsonObject
    ) {
        if (locked.getSize() > StaticValues.ONE_MB ||
                unlocked.getSize() > StaticValues.ONE_MB
        )
            return generateErr("حجم تصاویر باید کمتر از 1MB باشد");

        String fileType = FileUtils.uploadImageFile(locked);
        if (fileType == null)
            return generateErr("فرمت فایل تصویر قفل معتبر نمی باشد");

        fileType = FileUtils.uploadImageFile(unlocked);
        if (fileType == null)
            return generateErr("فرمت فایل تصویر مدال معتبر نمی باشد");

        List<Document> actionsPoints = new ArrayList<>();

        try {
            JSONArray actions = jsonObject.getJSONArray("actions");
            if (actions.length() == 0)
                return generateErr("لطفا متریک های کسب مدال را مشخص نمایید");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject jsonObject1 = actions.getJSONObject(i);
                String action = jsonObject1.getString("action");
                if (jsonObject1.getInt("count") < 1)
                    return JSON_NOT_VALID_PARAMS;

                actionsPoints.add(
                        new Document("action", Action.valueOf(action.toUpperCase()).getName())
                                .append("count", jsonObject1.getInt("count"))
                );
            }
        } catch (Exception x) {
            return JSON_NOT_VALID_PARAMS;
        }

        String lockedFilename = FileUtils.uploadFile(locked, FOLDER);
        if (lockedFilename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        String unlockedFilename = FileUtils.uploadFile(unlocked, FOLDER);
        if (unlockedFilename == null) {
            FileUtils.removeFile(lockedFilename, FOLDER);
            return JSON_UNKNOWN_UPLOAD_FILE;
        }

        Document newDoc = new Document("name", jsonObject.getString("name"))
                .append("actions", actionsPoints)
                .append("priority", jsonObject.getInt("priority"))
                .append("locked_img", lockedFilename)
                .append("unlocked_img", unlockedFilename)
                .append("created_at", System.currentTimeMillis());

        badgeRepository.insertOne(newDoc);
        return generateSuccessMsg("data", Utility.convertToJSON(newDoc, true));
    }

    public static String remove(ObjectId badgeId) {

        if (userBadgeRepository.exist(eq("badge_id", badgeId)))
            return generateErr("دانش آموز/دانش آموزانی این مدال را کسب کرده اند و امکان حذف آن وجود ندارد");

        Document badge = badgeRepository.findOneAndDelete(eq("_id", badgeId));
        if (badge == null)
            return JSON_NOT_VALID_ID;

        FileUtils.removeFile(badge.getString("locked_img"), FOLDER);
        FileUtils.removeFile(badge.getString("unlocked_img"), FOLDER);
        return JSON_OK;
    }

    public static String getAll(boolean isForAdmin) {
        List<Document> badges = badgeRepository.find(null, null, Sorts.ascending("priority"));
        JSONArray jsonArray = new JSONArray();
        badges.forEach(badge ->
                jsonArray.put(Utility.convertToJSON(badge, isForAdmin))
        );
        return generateSuccessMsg("data", jsonArray);
    }

    // ###################### PUBLIC SECTION ##################

}
