package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.GiftType;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class GiftController {

    private static String translateType(String type) {

        if (type.equalsIgnoreCase(GiftType.COIN.getName()))
            return "سکه";

        if (type.equalsIgnoreCase(GiftType.MONEY.getName()))
            return "پول";

        return "کد تخفیف";
    }

    public static String translateUseFor(String useFor) {

        if (useFor.equalsIgnoreCase("charge"))
            return "افزایش شارژ حساب";

        if (useFor.equalsIgnoreCase(OffCodeSections.GACH_EXAM.getName()))
            return "آزمون های آیریسک";

        if (useFor.equalsIgnoreCase(OffCodeSections.BANK_EXAM.getName()))
            return "آزمون های شخصی ساز";

        if (useFor.equalsIgnoreCase(OffCodeSections.BOOK.getName()))
            return "خرید کتاب";

        if (useFor.equalsIgnoreCase(OffCodeSections.CLASSES.getName()))
            return "کلاس ها و همایش ها";

        if (useFor.equalsIgnoreCase(OffCodeSections.CONTENT.getName()))
            return "بسته های آموزشی";

        if (useFor.equalsIgnoreCase(OffCodeSections.RAVAN_EXAM.getName()))
            return "آزمون های روان شناسی";

        if (useFor.equalsIgnoreCase(OffCodeSections.COUNSELING.getName()))
            return "مشاوره";

        return "همه";
    }

    private static String translateOffCodeType(String offCodeType) {

        if (offCodeType.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()))
            return "درصد";

        return "مقدار";
    }

    public static String removeAll(JSONArray items) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        long curr = System.currentTimeMillis();

        for (int i = 0; i < items.length(); i++) {

            String id = items.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            Document doc = giftRepository.findOneAndUpdate(
                    new ObjectId(id), set("deleted_at", curr)
            );

            if (doc == null) {
                excepts.put(i + 1);
                continue;
            }

            doneIds.put(id);
        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String getAll(String useFor) {

        ArrayList<Bson> filter = new ArrayList<>();
        filter.add(exists("deleted_at", false));

        if (useFor != null)
            filter.add(eq("use_for", useFor));

        ArrayList<Document> docs = giftRepository.find(
                and(filter), null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs)
            jsonArray.put(convertDocToJSON(doc));

        return generateSuccessMsg("data", jsonArray);

    }

    private static JSONObject convertDocToJSON(Document doc) {

        JSONObject jsonObject = new JSONObject().
                put("id", doc.getObjectId("_id").toString())
                .put("type", doc.getString("type"))
                .put("count", doc.getInteger("count"))
                .put("prob", doc.get("prob"))
                .put("amount", doc.get("amount"))
                .put("priority", doc.getInteger("priority"))
                .put("isForSite", doc.getBoolean("is_for_site"))
                .put("isForSiteFa", doc.getBoolean("is_for_site") ? "سایت" : "اپ")
                .put("used", doc.containsKey("users") ? doc.getList("users", Document.class).size() : 0);

        if (doc.getString("type").equalsIgnoreCase(GiftType.OFFCODE.getName()))
            jsonObject
                    .put("typeFa",
                            translateType(doc.getString("type")) + " - " +
                                    "برای " + translateUseFor(doc.getString("use_for")) + " - " +
                                    " به صورت " + translateOffCodeType(doc.getString("off_code_type")) + " -" +
                                    " تاریخ انقضا " + Utility.getSolarDate(doc.getLong("expire_at"))
                    )
                    .put("offCodeType", doc.getString("off_code_type"))
                    .put("expireAt", doc.getLong("expire_at"))
                    .put("useFor", doc.getString("use_for"));
        else
            jsonObject
                    .put("typeFa", translateType(doc.getString("type")));

        return jsonObject;
    }

    public static String store(ObjectId id, JSONObject data) {

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("useFor")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("offCodeType")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.getString("type").equalsIgnoreCase(
                GiftType.OFFCODE.getName()
        ) !=
                data.has("expireAt")
        )
            return JSON_NOT_VALID_PARAMS;

        if (data.has("expireAt") && data.getLong("expireAt") < System.currentTimeMillis())
            return JSON_NOT_VALID_PARAMS;

        Document newDoc = id == null ?
                new Document() : giftRepository.findById(id);

        if(newDoc == null)
            return JSON_NOT_VALID_ID;

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        if(id == null) {
            newDoc.put("reminder", data.getInt("count"));
            giftRepository.insertOne(newDoc);
            return generateSuccessMsg("data", convertDocToJSON(newDoc));
        }

        giftRepository.replaceOne(id, newDoc);
        return JSON_OK;
    }

    public static String getConfig() {

        Document config = Utility.getConfig();
        JSONObject jsonObject = new JSONObject();

        for (String key : config.keySet()) {

            if (!key.equalsIgnoreCase("max_web_gift_slot") &&
                    !key.equalsIgnoreCase("max_app_gift_slot") &&
                    !key.equalsIgnoreCase("app_gift_days") &&
                    !key.equalsIgnoreCase("web_gift_days")
            )
                continue;

            boolean hasLittleChar = false;
            for (int i = 0; i < key.length(); i++) {
                if (!Character.isUpperCase(key.charAt(i))) {
                    hasLittleChar = true;
                    break;
                }
            }

            if (hasLittleChar)
                jsonObject.put(Utility.camel(key, false), config.get(key));
            else
                jsonObject.put(key, config.get(key));

        }

        return Utility.generateSuccessMsg("data", jsonObject);
    }

    public static String buildSpinner(String mode, ObjectId userId) {

        if(!mode.equalsIgnoreCase("site") && !mode.equalsIgnoreCase("app"))
            return JSON_NOT_VALID_PARAMS;

        long curr = System.currentTimeMillis();
        Document config = Utility.getConfig();
        boolean isForSite = mode.equalsIgnoreCase("site");

        if(
                (isForSite && !config.containsKey("web_gift_days")) ||
                (!isForSite && !config.containsKey("app_gift_days"))
        )
            return JSON_NOT_ACCESS;

        List<Long> dates = isForSite ? config.getList("web_gift_days", Long.class) :
                config.getList("app_gift_days", Long.class);

        boolean findAppropriateDate = false;

        for(Long date : dates) {

            if(curr > date)
                continue;

            if(date - curr > ONE_DAY_MIL_SEC)
                continue;

            findAppropriateDate = true;
            break;
        }

        if(!findAppropriateDate)
            return JSON_NOT_ACCESS;

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(exists("deleted_at", false));
        filters.add(gt("reminder", 0));
        filters.add(eq("is_for_site", isForSite));

        ArrayList<Document> gifts = giftRepository.find(and(filters), null, Sorts.ascending("priority"));

        JSONArray jsonArray = new JSONArray();
        int limit = Math.min(gifts.size(), (Integer) config.getOrDefault(isForSite ? "max_web_gift_slot" : "max_app_gift_slot", 8));
        int totalW = 0;
        ArrayList<Integer> upperBounds = new ArrayList<>();

        for(int i = 0; i < limit; i++) {

            Document gift = gifts.get(i);

            totalW += gift.getInteger("prob");
            upperBounds.add(totalW);

            jsonArray.put(new JSONObject()
                    .put("id", gift.getObjectId("_id").toString())
                    .put("created_at", curr - getRandIntForGift(100000))
                    .put("label", getGiftString(gift))
            );
        }

        int r = getRandIntForGift(totalW);

        int selectedGiftIdx = -1;

        for(int i = 0; i < upperBounds.size(); i++) {
            if(upperBounds.get(i) > r) {
                selectedGiftIdx = i;
                break;
            }
        }

        if(selectedGiftIdx != -1) {
            System.out.println(gifts.get(selectedGiftIdx).getString("type"));
            jsonArray.getJSONObject(selectedGiftIdx).put("created_at", curr - 400000);
        }

        userGiftRepository.insertOneWithReturnId(new Document("created_at", curr)
                .append("status", "init")
                .append("user_id", userId)
                .append("mode", mode)
                .append("gift", gifts.get(selectedGiftIdx).getObjectId("_id"))
        );

        return generateSuccessMsg("data", jsonArray);
    }

    private static String getGiftString(Document gift) {

        if(gift.getString("type").equalsIgnoreCase("offcode")) {
            if(gift.getString("off_code_type").equalsIgnoreCase("percent"))
                return "کد تخفیف " + gift.getInteger("amount") + "% " + translateUseFor(gift.getString("use_for"));

            return "تخفیف " + gift.getInteger("amount") + " تومان " + translateUseFor(gift.getString("use_for"));
        }

        if(gift.getString("type").equalsIgnoreCase("coin"))
            return  gift.get("amount") + " ایکس پول";

        if(gift.getString("type").equalsIgnoreCase("money"))
            return  gift.get("amount") + " تومان اعتبار";

        return "";
    }

    public static String giveMyGift(ObjectId userId) {

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Document doc = userGiftRepository.findOne(and(
                eq("user_id", userId),
                eq("status", "init"),
                gt("created_at", System.currentTimeMillis() - ONE_DAY_MIL_SEC)
        ), null);

        if(doc == null)
            return JSON_NOT_ACCESS;

        doc.put("status", "finish");
        userGiftRepository.replaceOne(doc.getObjectId("_id"), doc);
        return JSON_OK;

    }

    private static JSONObject offGift(Document gift) {
        return new JSONObject()
                .put("amount", gift.getInteger("amount"))
                .put("type", gift.getString("off_code_type"))
                .put("section", gift.getString("use_for"))
                .put("sectionFa", translateUseFor(gift.getString("use_for")))
                .put("expireAtTs", gift.getLong("expire_at"))
                .put("expireAt", gift.getLong("expire_at"));
    }

    public static String giveMyGifts(ObjectId userId) {

        ArrayList<Document> docs = userGiftRepository.find(and(
                eq("user_id", userId),
                eq("status", "finish")
        ), null);

        JSONArray jsonArray = new JSONArray();

        for(Document doc : docs) {

            Document gift = giftRepository.findById(doc.getObjectId("gift"));
            if(gift == null)
                continue;

            if(gift.getString("type").equalsIgnoreCase("offcode"))
                jsonArray.put(new JSONObject()
                        .put("type", "off")
                        .put("createdAt", getSolarDate(gift.getLong("created_at")))
                        .put("obj", offGift(gift))
                );
            else if(gift.getString("type").equalsIgnoreCase("coin"))
                jsonArray.put(new JSONObject()
                        .put("type", "coin")
                        .put("createdAt", getSolarDate(gift.getLong("created_at")))
                        .put("label", getGiftString(gift))
                );
            else if(gift.getString("type").equalsIgnoreCase("money"))
                jsonArray.put(new JSONObject()
                        .put("type", "money")
                        .put("createdAt", getSolarDate(gift.getLong("created_at")))
                        .put("label", getGiftString(gift))
                );
        }

        return generateSuccessMsg("data", jsonArray);
    }

}
