package irysc.gachesefid.Controllers.Badge;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class BadgeController {

    static final String FOLDER = "badges";

    // ##################### ADMIN SECTION ####################

    public static String add(
            MultipartFile locked,
            MultipartFile unlocked,
            JSONObject jsonObject
    ) {

        if (jsonObject.getNumber("award").doubleValue() < 0)
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

        if (jsonObject.getNumber("award").doubleValue() < 0)
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

    private static int getCountOfAction(ObjectId userId, Action action) {
        switch (action) {
            case COMMENT:
                return commentRepository.count(and(
                        eq("user_id", userId),
                        eq("status", "accept")
                ));
            case GET_TEACH_CLASS:
                return teachScheduleRepository.count(
                        eq("students._id", userId)
                );
            case BUY_CONTENT:
                return contentRepository.count(eq("users._id", userId));
            case COMPLETE_PROFILE:
                return userPointRepository.exist(and(
                        eq("user_id", userId),
                        eq("action", Action.COMPLETE_PROFILE.getName())
                )) ? 1 : 0;
            case BUY_EXAM:
                return iryscQuizRepository.count(eq("students._id", userId)) +
                        openQuizRepository.count(eq("students._id", userId)) +
                        escapeQuizRepository.count(eq("students_id", userId)) +
                        onlineStandQuizRepository.count(eq("students._id", userId));
            case SET_ADVISOR:
                return advisorRequestsRepository.count(and(
                        eq("user_id", userId),
                        eq("answer", "accept"),
                        exists("paid")
                ));
            case BUY_QUESTION:
                return customQuizRepository.find(and(
                                eq("user_id", userId),
                                or(
                                        eq("status", "paid"),
                                        eq("status", "finished")
                                )
                        ), new BasicDBObject("questions", 1))
                        .stream()
                        .mapToInt(document -> document.getList("questions", ObjectId.class).size())
                        .sum();
            case TEACHER_RATE:
                return stdRateRepository.count(and(
                        eq("student_id", userId),
                        gte("rate", 4)
                ));
            case RANK_IN_QUIZ:
                return (int)iryscQuizRepository.find(and(
                                exists("ranking_list"),
                                eq("students._id", userId)
                        ), new BasicDBObject("ranking_list", 1)
                ).stream()
                        .map(document -> document.getList("ranking_list", Document.class))
                        .flatMap(Collection::stream)
                        .filter(rankDoc -> rankDoc.getObjectId("_id").equals(userId))
                        .map(rankDoc ->
                                QuizAbstract.decodeFormatGeneral(
                                        rankDoc.get("stat", Binary.class).getData()
                                )
                        )
                        .mapToInt(objects -> objects.length > 1 ? (int)objects[1] : 100)
                        .filter(rank -> rank < 4)
                        .count();
        }

        return 0;
    }

    synchronized
    public static void checkForUpgrade(ObjectId userId, Action action) {
        List<Document> badges = badgeRepository.find(
                eq("actions.action", action.getName()), null
        );
        if (badges.size() == 0)
            return;

        Document userBadges = userBadgeRepository.findBySecKey(userId);
        List<ObjectId> userBadgesId = userBadges == null ? null :
                userBadges.getList("badges", Document.class)
                        .stream().map(document -> document.getObjectId("_id"))
                        .collect(Collectors.toList());
        if (userBadgesId != null) {
            badges = badges.stream()
                    .filter(document -> !userBadgesId.contains(document.getObjectId("_id")))
                    .collect(Collectors.toList());
        }
        long curr = System.currentTimeMillis();
        AtomicReference<Double> awards = new AtomicReference<>((double) 0);
        List<Document> newBadgesDoc = new ArrayList<>();
        HashMap<String, Integer> userActivityCount = new HashMap<>();

        List<Document> newBadges = badges.stream()
                .filter(badge -> {
                    for (Document iter : badge.getList("actions", Document.class)) {
                        int activityCount;
                        if(userActivityCount.containsKey(iter.getString("action")))
                            activityCount = userActivityCount.get(iter.getString("action"));
                        else {
                            activityCount = getCountOfAction(userId, action);
                            userActivityCount.put(iter.getString("action"), activityCount);
                        }

                        if (iter.getInteger("count") > activityCount)
                            return false;
                    }
                    return true;
                })
                .map(badge -> {
                    awards.updateAndGet(v -> v + badge.getDouble("award"));
                    newBadgesDoc.add(badge);
                    return new Document("_id", badge.getObjectId("_id"))
                            .append("created_at", curr);
                })
                .collect(Collectors.toList());

        if (newBadges.size() > 0) {
            new Thread(() -> {
                Document user = userRepository.findById(userId);
                double d = ((Number) user.get("coin")).doubleValue() + awards.get();
                user.put("coin", Math.round((d * 100.0)) / 100.0);

                newBadgesDoc.forEach(newBadge ->
                        createNotifAndSendSMS(user, newBadge.getString("name") + "__" + newBadge.getDouble("award"), "badge")
                );

                userRepository.updateOne(userId, new BasicDBObject("$set",
                                new BasicDBObject("events", user.get("events"))
                                        .append("coin", Math.round((d * 100.0)) / 100.0)
                        )
                );
            }).start();

            if (userBadges == null)
                userBadgeRepository.insertOne(new Document("user_id", userId)
                        .append("badges", newBadges)
                );
            else {
                userBadges.getList("badges", Document.class)
                        .addAll(newBadges);
                userBadgeRepository.replaceOneWithoutClearCache(
                        userBadges.getObjectId("_id"),
                        userBadges
                );
            }
        }
    }
}
