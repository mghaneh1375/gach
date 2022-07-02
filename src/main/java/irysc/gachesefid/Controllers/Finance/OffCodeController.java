package irysc.gachesefid.Controllers.Finance;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.DateValidator;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.ObjectIdValidator;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.offcodeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Statics.Alerts.createOffCode;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class OffCodeController {

    public static String store(JSONObject jsonObject) {

        if (!DateValidator.isValid(jsonObject.getString("expireAt")))
            return JSON_NOT_VALID_PARAMS;

        String type = jsonObject.getString("type");
        if(!EnumValidatorImp.isValid(type, OffCodeTypes.class))
            return JSON_NOT_VALID_PARAMS;

        int amount = jsonObject.getInt("amount");
        if (type.equals("percent") && amount > 100)
            return JSON_NOT_VALID_PARAMS;

        String section = jsonObject.getString("section");

        if(!EnumValidatorImp.isValid(section, OffCodeSections.class))
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

        if(jsonObject.has("userId")) {

            if(!ObjectIdValidator.isValid(jsonObject.getString("userId")))
                return JSON_NOT_VALID_PARAMS;

            ObjectId userId = new ObjectId(jsonObject.getString("userId"));
            user = userRepository.findById(userId);
            if (user == null)
                return JSON_NOT_VALID_PARAMS;

            newDoc.append("user_id", userId);

        }

        offcodeRepository.insertOne(newDoc);

        if(user != null) {
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

    public static String store(JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if(!jsonObject.has("expireAt") ||
                    !jsonObject.has("type") ||
                    !jsonObject.has("amount") ||
                    !jsonObject.has("NID")
            ) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            if (!DateValidator.isValid(jsonObject.getString("expireAt"))) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            String type = jsonObject.getString("type");
            if(!EnumValidatorImp.isValid(type, OffCodeTypes.class)) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            int amount = jsonObject.getInt("amount");
            if (type.equals("percent") && amount > 100) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            String section = OffCodeSections.ALL.getName();

            if(jsonObject.has("section")) {
                section = jsonObject.getString("section");

                if (!EnumValidatorImp.isValid(section, OffCodeSections.class)) {
                    excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                    continue;
                }
            }

            int d = Utility.convertStringToDate(jsonObject.getString("expireAt"));
            if (Utility.getToday() > d) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            String NID = jsonObject.getString("NID");

            if(!Utility.validationNationalCode(NID)) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            Document user = userRepository.findBySecKey(NID);

            if (user == null) {
                excepts.put(jsonObject.has("NID") ? jsonObject.get("NID") : "ردیف " + (i + 1));
                continue;
            }

            Document newDoc = new Document("type", type)
                    .append("amount", amount)
                    .append("expire_at", d)
                    .append("section", section)
                    .append("user_id", user.getObjectId("_id"))
                    .append("used", false)
                    .append("created_at", System.currentTimeMillis());

            offcodeRepository.insertOne(newDoc);
            String finalSection = section;

            new Thread(() -> AlertController.store(
                    newDoc.getObjectId("user_id"),
                    createOffCode(amount, type, finalSection,
                            jsonObject.getString("expireAt")), false,
                    new PairValue("createOffCode", user.getString("mail")),
                    Utility.formatPrice(amount) + "__" + jsonObject.getString("expireAt"),
                    user.getString("name_fa") + " " + user.getString("last_name_fa"),
                    "کد تخفیف"
            )).start();
        }

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی کدهای تخفیف به درستی اضافه شدند"
            );

        return generateSuccessMsg(
                "excepts",
                "بجز موارد زیر سایرین به درستی اضافه گردیدند. " + excepts
        );
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

            if(!off.containsKey("user") || off.get("user") == null)
                continue;

            Document user = (Document) off.get("user");

            JSONObject jsonObject = new JSONObject()
                    .put("amount", off.getInteger("amount"))
                    .put("type", off.getString("type"))
                    .put("section", off.getString("section"))
                    .put("used", off.getBoolean("used"))
                    .put("user", user.getString("first_name") + " " + user.getString("last_name"))
                    .put("id", off.getObjectId("_id").toString())
                    .put("expireAt", Utility.convertStringToDate(off.getInteger("expire_at") + "", "/"))
                    .put("createdAt", Utility.getSolarDate(off.getLong("created_at")));

            if (off.getBoolean("used")) {
                jsonObject.put("description", off.getString("description"));
                jsonObject.put("usedSection", off.getString("used_section"));
                jsonObject.put("usedAt", Utility.getSolarDate(off.getLong("used_at")));
            }

            jsonArray.put(jsonObject);
        }

        return Utility.generateSuccessMsg("data", jsonArray);
    }

    public static void delete(ObjectId offCodeId) {
        offcodeRepository.deleteOne(and(
                eq("_id", offCodeId),
                ne("used", true)
        ));
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
