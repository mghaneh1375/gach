package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
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

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.giftRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

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
            return "شارژ حساب";

        if (useFor.equalsIgnoreCase(OffCodeSections.GACH_EXAM.getName()))
            return "آزمون های آیریسک";

        if (useFor.equalsIgnoreCase(OffCodeSections.BANK_EXAM.getName()))
            return "آزمون های شخصی ساز";

        if (useFor.equalsIgnoreCase(OffCodeSections.BOOK.getName()))
            return "خرید کتاب";

        if (useFor.equalsIgnoreCase(OffCodeSections.CLASSES.getName()))
            return "کلاس ها و همایش ها";

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

}
