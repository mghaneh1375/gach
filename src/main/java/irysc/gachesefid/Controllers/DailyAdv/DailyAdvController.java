package irysc.gachesefid.Controllers.DailyAdv;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Utility.FileUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.uploadMultimediaFile;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class DailyAdvController {

    final static String FOLDER = "daily_adv";

    // ####################### ADMIN SECTION #####################

    public static String add(MultipartFile file, String title, long expireAt) {

        if(expireAt < System.currentTimeMillis())
            return generateErr("تاریخ انقضا باید از زمان جاری بزرگ تر باشد");

        if(file == null)
            return JSON_NOT_VALID_PARAMS;

        if (file.getSize() > MAX_DAILY_ADV_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز، " + MAX_DAILY_ADV_FILE_SIZE + " مگ است.");

        String fileType = uploadMultimediaFile(file);
        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        String filename = FileUtils.uploadFile(file, FOLDER);
        if (filename == null)
            return JSON_UNKNOWN_UPLOAD_FILE;

        Document newDoc = new Document("filename", filename)
                .append("expire_at", expireAt)
                .append("title", title);

        dailyAdvRepository.insertOneWithReturnId(newDoc);
        return generateSuccessMsg("data", Utility.convertToJSON(newDoc));
    }

    public static String delete(ObjectId id) {
        Document dailyAdv =
                dailyAdvRepository.findOneAndDelete(eq("_id", id));
        FileUtils.removeFile(dailyAdv.getString("filename"), FOLDER);
        return JSON_OK;
    }

    public static String getAll() {
        JSONArray jsonArray = new JSONArray();
        dailyAdvRepository.find(null, null)
                .forEach(document -> jsonArray.put(
                        Utility.convertToJSON(document)
                ));
        return generateSuccessMsg("data", jsonArray);
    }

    // ####################### PUBLIC SECTION #####################

    private static void checkAccessForAdv(ObjectId userId) {
        Document point = pointRepository.findBySecKey(Action.DAILY_ADV.getName());
        if (point == null || point.getInteger("point") == 0)
            throw new InvalidFieldsException("No point is available for this action");

        if(userPointRepository.exist(and(
                eq("user_id", userId),
                eq("action", Action.DAILY_ADV.getName()),
                eq("ref", getToday())
        )))
            throw new InvalidFieldsException("Point already has been given");
    }

    public static String canReqForAdv(ObjectId userId) {
        try {
            checkAccessForAdv(userId);
            return generateSuccessMsg("data",
                    dailyAdvRepository.count(null) > 0
            );
        }
        catch (Exception x) {
            return generateSuccessMsg("data", false);
        }
    }

    public static String getRandAdv(ObjectId userId) {
        checkAccessForAdv(userId);
        ArrayList<Document> advs = dailyAdvRepository.find(gt("expire_at", System.currentTimeMillis()), new BasicDBObject("filename", 1));
        Document adv = advs.get(Math.abs(new Random().nextInt()) % advs.size());
        return generateSuccessMsg("data", STATICS_SERVER + FOLDER + "/" + adv.getString("filename"));
    }

    public static String giveMyPointForAdv(ObjectId userId) {
        try {
            checkAccessForAdv(userId);
            PointController.addPointForAction(
                    userId, Action.DAILY_ADV,
                    getToday(), null
            );
            return JSON_OK;
        }
        catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }
}
