package irysc.gachesefid.Controllers.Finance.Off;

import irysc.gachesefid.Controllers.AlertController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.offcodeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Statics.Alerts.createOffCode;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class Utility {

    static String preStoreCheck(String type, int amount,
                                long expireAt, String section) {

        if (!EnumValidatorImp.isValid(type, OffCodeTypes.class))
            return JSON_NOT_VALID_PARAMS;

        if (type.equals("percent") && amount > 100)
            return generateErr("مقدار تخفیف باید کمتر از ۱۰۰ درصد باشد.");

        if (!EnumValidatorImp.isValid(section, OffCodeSections.class))
            return JSON_NOT_VALID_PARAMS;

        if (System.currentTimeMillis() > expireAt)
            return generateErr("تاریخ انقضا باید از امروز بزرگ تر باشد.");

        return null;
    }

    static String addAll(JSONArray jsonArray,
                         String type, int amount,
                         long expireAt, String section,
                         JSONArray excepts) {

        JSONArray added = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String NID = jsonArray.getString(i);

            if (!irysc.gachesefid.Utility.Utility.validationNationalCode(NID)) {
                excepts.put(NID);
                continue;
            }

            Document user = userRepository.findBySecKey(NID);

            if (user == null) {
                excepts.put(NID);
                continue;
            }

            Document newDoc = new Document("type", type)
                    .append("amount", amount)
                    .append("expire_at", expireAt)
                    .append("section", section)
                    .append("user_id", user.getObjectId("_id"))
                    .append("used", false)
                    .append("created_at", System.currentTimeMillis());

            offcodeRepository.insertOne(newDoc);
            added.put(convertDocToJSON(Document.parse(newDoc.toJson()).append("user", user)));

            new Thread(() -> AlertController.store(
                    newDoc.getObjectId("user_id"),
                    createOffCode(amount, type, section, getSolarDate(expireAt)), false,
                    new PairValue("createOffCode", user.getString("mail")),
                    irysc.gachesefid.Utility.Utility.formatPrice(amount) + "__" + expireAt,
                    user.getString("name_fa") + " " + user.getString("last_name_fa"),
                    "کد تخفیف"
            )).start();
        }

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی کدهای تخفیف به درستی اضافه شدند",
                    new PairValue("addedItems", added)
            );

        return generateSuccessMsg(
                "excepts",
                "بجز موارد زیر سایرین به درستی اضافه گردیدند. " + excepts,
                new PairValue("addedItems", added)
        );
    }

    static JSONObject convertDocToJSON(Document off) {

        Document user = (Document) off.get("user");

        JSONObject jsonObject = new JSONObject()
                .put("amount", off.getInteger("amount"))
                .put("type", off.getString("type"))
                .put("section", off.getString("section"))
                .put("used", off.getBoolean("used"))
                .put("user", user.getString("first_name") + " " + user.getString("last_name"))
                .put("id", off.getObjectId("_id").toString())
                .put("expireAt", getSolarDate(off.getLong("expire_at")))
                .put("createdAt", getSolarDate(off.getLong("created_at")));

        if (off.getBoolean("used")) {
            jsonObject.put("description", off.getString("description"));
            jsonObject.put("usedSection", off.getString("used_section"));
            jsonObject.put("usedAt", irysc.gachesefid.Utility.Utility.getSolarDate(off.getLong("used_at")));
        }

        return jsonObject;
    }
}
