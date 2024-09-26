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
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
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

        if(jsonObject.getNumber("award").doubleValue() < 0)
            return generateErr("مقدار جایزه باید بزرگ تر از 0 باشد");

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
                .append("award", jsonObject.getNumber("award").doubleValue())
                .append("created_at", System.currentTimeMillis());

        badgeRepository.insertOne(newDoc);
        return generateSuccessMsg("data", Utility.convertToJSON(newDoc, true, null));
    }

    public static String update(
            ObjectId badgeId,
            MultipartFile locked,
            MultipartFile unlocked,
            JSONObject jsonObject
    ) {

        if(jsonObject.getNumber("award").doubleValue() < 0)
            return generateErr("مقدار جایزه باید بزرگ تر از 0 باشد");

        if (locked != null || unlocked != null) {
            if ((locked != null && locked.getSize() > StaticValues.ONE_MB) ||
                    (unlocked != null && unlocked.getSize() > StaticValues.ONE_MB)
            )
                return generateErr("حجم تصاویر باید کمتر از 1MB باشد");

            if (locked != null) {
                if (FileUtils.uploadImageFile(locked) == null)
                    return generateErr("فرمت فایل تصویر قفل معتبر نمی باشد");
            }

            if (unlocked != null) {
                if (FileUtils.uploadImageFile(unlocked) == null)
                    return generateErr("فرمت فایل تصویر مدال معتبر نمی باشد");
            }
        }

        Document badge = badgeRepository.findById(badgeId);
        if (badge == null)
            return JSON_NOT_VALID_ID;

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

        String lockedFilename = null;
        if (locked != null) {
            lockedFilename = FileUtils.uploadFile(locked, FOLDER);
            if (lockedFilename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;
        }

        String unlockedFilename = null;
        if (unlocked != null) {
            unlockedFilename = FileUtils.uploadFile(unlocked, FOLDER);
            if (unlockedFilename == null) {
                FileUtils.removeFile(lockedFilename, FOLDER);
                return JSON_UNKNOWN_UPLOAD_FILE;
            }
        }

        if (lockedFilename != null) {
            FileUtils.removeFile(badge.getString("locked_img"), FOLDER);
            badge.put("locked_img", lockedFilename);
        }
        if (unlockedFilename != null) {
            FileUtils.removeFile(badge.getString("unlocked_img"), FOLDER);
            badge.put("unlocked_img", unlockedFilename);
        }

        badge.put("award", jsonObject.getNumber("award").doubleValue());
        badge.put("name", jsonObject.getString("name"));
        badge.put("actions", actionsPoints);
        badge.put("priority", jsonObject.getInt("priority"));

        badgeRepository.replaceOneWithoutClearCache(badgeId, badge);
        return generateSuccessMsg("data", Utility.convertToJSON(badge, true, null));
    }

    public static String remove(ObjectId badgeId) {

        if (userBadgeRepository.exist(eq("badges._id", badgeId)))
            return generateErr("دانش آموز/دانش آموزانی این مدال را کسب کرده اند و امکان حذف آن وجود ندارد");

        Document badge = badgeRepository.findOneAndDelete(eq("_id", badgeId));
        if (badge == null)
            return JSON_NOT_VALID_ID;

        FileUtils.removeFile(badge.getString("locked_img"), FOLDER);
        FileUtils.removeFile(badge.getString("unlocked_img"), FOLDER);
        return JSON_OK;
    }

    // ###################### PUBLIC SECTION ##################

    public static String getAll(ObjectId userId) {
        List<Document> badges = badgeRepository.find(null, null, Sorts.ascending("priority"));
        Document userBadges = userId == null ? null : userBadgeRepository.findBySecKey(userId);
        List<ObjectId> userBadgesId = userBadges == null ? null :
            userBadges.getList("badges", Document.class)
                    .stream().map(document -> document.getObjectId("_id"))
                    .collect(Collectors.toList());

        JSONArray jsonArray = new JSONArray();
        badges.forEach(badge -> {
            boolean hasIt = userBadgesId != null && userBadgesId.contains(badge.getObjectId("_id"));
            jsonArray.put(Utility.convertToJSON(badge, userId == null, hasIt)
                    .put("hasIt", hasIt));
        });
        return generateSuccessMsg("data", jsonArray);
    }
}
