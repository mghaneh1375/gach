package irysc.gachesefid.Controllers.Finance.Off;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.DateValidator;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.ObjectIdValidator;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Finance.Off.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Statics.Alerts.createOffCode;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class OffCodeController {

    public static String update(ObjectId id, JSONObject jsonObject) {

        Document off = offcodeRepository.findById(id);
        if(off == null)
            return JSON_NOT_VALID_ID;

        String type, section;
        int amount;
        long d;

        if(jsonObject.has("type")) {
            type = jsonObject.getString("type");
            if (!EnumValidatorImp.isValid(type, OffCodeTypes.class))
                return JSON_NOT_VALID_PARAMS;
        }
        else
            type = off.getString("type");

        if(jsonObject.has("amount")) {
            amount = jsonObject.getInt("amount");
            if (type.equals("percent") && amount > 100)
                return JSON_NOT_VALID_PARAMS;
        }
        else
            amount = off.getInteger("amount");

        if(jsonObject.has("section")) {
            section = jsonObject.getString("section");

            if (!EnumValidatorImp.isValid(section, OffCodeSections.class))
                return JSON_NOT_VALID_PARAMS;
        }
        else
            section = off.getString("section");

        if(jsonObject.has("expireAt")) {
            d = jsonObject.getLong("expireAt");
            if (System.currentTimeMillis() > d)
                return JSON_NOT_VALID_PARAMS;
        }
        else
            d = off.getLong("expireAt");

        off.put("amount", amount);
        off.put("type", type);
        off.put("expire_at", d);
        off.put("section", section);

        offcodeRepository.replaceOne(id, off);
        return JSON_OK;
    }

    public static String store(JSONObject jsonObject) {

        if (!DateValidator.isValid(jsonObject.getString("expireAt")))
            return JSON_NOT_VALID_PARAMS;

        String type = jsonObject.getString("type");
        if (!EnumValidatorImp.isValid(type, OffCodeTypes.class))
            return JSON_NOT_VALID_PARAMS;

        int amount = jsonObject.getInt("amount");
        if (type.equals("percent") && amount > 100)
            return JSON_NOT_VALID_PARAMS;

        String section = jsonObject.getString("section");

        if (!EnumValidatorImp.isValid(section, OffCodeSections.class))
            return JSON_NOT_VALID_PARAMS;

        int d = Utility.convertStringToDate(jsonObject.getString("expireAt"));
        if (Utility.getToday() > d)
            return JSON_NOT_VALID_PARAMS;

        Document newDoc = new Document("type", type)
                .append("amount", amount)
                .append("expire_at", d)
                .append("section", section)
                .append("used", false)
                .append("created_at", System.currentTimeMillis());

        Document user = null;

        if (jsonObject.has("userId")) {

            if (!ObjectIdValidator.isValid(jsonObject.getString("userId")))
                return JSON_NOT_VALID_PARAMS;

            ObjectId userId = new ObjectId(jsonObject.getString("userId"));
            user = userRepository.findById(userId);
            if (user == null)
                return JSON_NOT_VALID_PARAMS;

            newDoc.append("user_id", userId);

        }

        offcodeRepository.insertOne(newDoc);

        if (user != null) {
            AlertController.store(
                    newDoc.getObjectId("user_id"),
                    createOffCode(amount, type, section,
                            jsonObject.getString("expireAt")), false,
                    new PairValue("createOffCode", user.getString("mail")),
                    Utility.formatPrice(amount) + "__" + jsonObject.getString("expireAt"),
                    user.getString("name_fa") + " " + user.getString("last_name_fa"),
                    "کد تخفیف"
            );
        }

        return JSON_OK;
    }


    public static String store(MultipartFile file,
                               String type, int amount,
                               long expireAt, String section) {

        String err = preStoreCheck(type, amount, expireAt, section);
        if(err != null)
            return err;

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            return generateErr("File is not valid");

        rows.remove(0);
        int rowIdx = 0;

        JSONArray excepts = new JSONArray();
        JSONArray jsonArray = new JSONArray();

        for (Row row : rows) {

            rowIdx++;

            try {

                if (row.getLastCellNum() < 1) {
                    excepts.put(rowIdx);
                    continue;
                }

                jsonArray.put(row.getCell(1).getStringCellValue());

            } catch (Exception x) {
                printException(x);
                excepts.put(rowIdx);
            }
        }

        return addAll(jsonArray, type, amount, expireAt, section, excepts);
    }

    public static String store(JSONArray jsonArray,
                               String type, int amount,
                               long expireAt, String section) {

        String err = preStoreCheck(type, amount, expireAt, section);
        if(err != null)
            return err;

        return addAll(jsonArray, type, amount, expireAt, section, new JSONArray());
    }

    public static String offs(ObjectId userId,
                              String section,
                              Boolean used,
                              Boolean expired,
                              String date,
                              String dateEndLimit,
                              Integer minValue,
                              Integer maxValue,
                              String type) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if (userId != null)
            constraints.add(eq("user_id", userId));

        if (section != null)
            constraints.add(eq("section", section));

        if (type != null) {

            if (!type.equals("value") && !type.equals("percent"))
                return JSON_NOT_VALID_PARAMS;

            constraints.add(eq("type", type));
        }

        if (minValue != null)
            constraints.add(gte("amount", minValue));

        if (maxValue != null)
            constraints.add(lte("amount", maxValue));

        if (used != null) {
            constraints.add(exists("used", true));
            constraints.add(eq("used", used));
        }

        if (date != null) {
            long ts = Utility.getTimestamp(date);
            if (ts != -1)
                constraints.add(gte("used_at", ts));
        }

        if (dateEndLimit != null) {
            long ts = Utility.getTimestamp(dateEndLimit);
            if (ts != -1)
                constraints.add(lte("used_at", ts));
        }

        if (expired != null) {
            int today = Utility.getToday();

            if (expired)
                constraints.add(lt("expire_at", today));
            else
                constraints.add(gte("expire_at", today));
        }

        AggregateIterable<Document> offs;

        if (constraints.size() == 0)
            offs = offcodeRepository.all(null);
        else
            offs = offcodeRepository.all(match(and(constraints)));

        JSONArray jsonArray = new JSONArray();

        for (Document off : offs) {

            if (!off.containsKey("user") || off.get("user") == null)
                continue;

            jsonArray.put(convertDocToJSON(off));
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    public static void deleteByUserId(ObjectId userId) {

        if (DEV_MODE)
            offcodeRepository.deleteMany(and(
                    eq("user_id", userId)
            ));
        else
            offcodeRepository.deleteMany(and(
                    eq("user_id", userId),
                    ne("used", true)
            ));
    }
}
