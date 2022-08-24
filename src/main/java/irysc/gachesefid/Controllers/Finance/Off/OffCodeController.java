package irysc.gachesefid.Controllers.Finance.Off;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.DB.OffcodeRepository;
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

        if(jsonObject.has("code"))
            off.put("code", jsonObject.getString("code"));

        off.put("amount", amount);
        off.put("type", type);
        off.put("expire_at", d);
        off.put("section", section);

        offcodeRepository.replaceOne(id, off);

        return generateSuccessMsg("data", convertDocToJSON(off));
    }


    public static String store(MultipartFile file,
                               String code, String type, int amount,
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

    public static String store(JSONObject jsonObject) {

        String type = jsonObject.getString("type");
        int amount = jsonObject.getInt("amount");
        long expireAt = jsonObject.getLong("expireAt");
        String section = jsonObject.has("section") ?
                jsonObject.getString("section") :
                OffCodeSections.ALL.getName();

        String code = jsonObject.has("code") ? jsonObject.getString("code") : null;

        boolean isPublic = jsonObject.has("isPublic") &&
                jsonObject.getBoolean("isPublic") &&
                !jsonObject.has("items") &&
                !jsonObject.has("counter");

        if(isPublic && code == null)
            return JSON_NOT_VALID_PARAMS;

        String err = preStoreCheck(type, amount, expireAt, section);
        if(err != null)
            return err;

        if(isPublic) {
            Document newDoc = new Document("type", type)
                    .append("amount", amount)
                    .append("expire_at", expireAt)
                    .append("section", section)
                    .append("code", code)
                    .append("students", new ArrayList<>())
                    .append("is_public", true)
                    .append("created_at", System.currentTimeMillis());

            offcodeRepository.insertOne(newDoc);
            return returnAddResponse(null, new JSONArray().put(convertDocToJSON(newDoc)));
        }

        if(!jsonObject.has("items"))
            return JSON_NOT_VALID_PARAMS;

        JSONArray jsonArray = jsonObject.getJSONArray("items");
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
                              String type,
                              Boolean isPublic,
                              String code,
                              String withCode) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if (userId != null)
            constraints.add(eq("user_id", userId));

        if (code != null)
            constraints.add(and(
                    exists("code"),
                    eq("code", code)
            ));

        if(withCode != null) {
            constraints.add(
                    exists("code", withCode.equalsIgnoreCase("withCode"))
            );
        }

        if (isPublic != null)
            constraints.add(isPublic ?
                    and(
                            exists("is_public"),
                            eq("is_public", true)
                    ) :
                    or(
                            exists("is_public", false),
                            eq("is_public", false)
                    )
        );

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

        for (Document off : offs)
            jsonArray.put(convertDocToJSON(off));

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

    public static String check(ObjectId userId, String code,
                               String section
    ) {

        Document off = offcodeRepository.findOne(and(
                exists("code"),
                eq("code", code),
                or(
                        and(
                                exists("user_id"),
                                eq("user_id", userId)
                        ),
                        and(
                                exists("is_public"),
                                eq("is_public", true)
                        )
                )
        ), null);

        if(off == null)
            return generateErr("کد تخفیف موردنظر اشتباه است.");

        if(off.containsKey("used") && off.getBoolean("used"))
            return generateErr("شما قبلا از این کد استفاده کرده اید.");

        if(off.getLong("expire_at") < System.currentTimeMillis())
            return generateErr("کد مدنظر منقضی شده است.");

        if(off.containsKey("students") &&
            off.getList("students", ObjectId.class).contains(userId)
        )
            return generateErr("شما قبلا از این کد استفاده کرده اید.");

        if(!off.getString("section").equalsIgnoreCase(
                OffCodeSections.ALL.getName())
        ) {
            if(!section.equalsIgnoreCase(off.getString("section")))
                return generateErr("این کد تنها در قسمت " + GiftController.translateUseFor(off.getString("section")) + " قابل استفاده است.");
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("type", off.getString("type"))
                .put("amount", off.get("amount"))
        );
    }
}
