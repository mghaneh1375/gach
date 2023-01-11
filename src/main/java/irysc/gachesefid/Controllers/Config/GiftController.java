package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Finance.Off.OffCodeController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.GiftTarget;
import irysc.gachesefid.Models.GiftType;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class GiftController {

    private static String translateType(String type) {

        if (type.equalsIgnoreCase(GiftType.COIN.getName()))
            return "ایکس پول    ";

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
                .put("reminder", doc.getOrDefault("reminder", 0))
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
        long curr = System.currentTimeMillis();

        for (String key : config.keySet()) {

            if (!key.equalsIgnoreCase("max_web_gift_slot") &&
                    !key.equalsIgnoreCase("max_app_gift_slot") &&
                    !key.equalsIgnoreCase("app_gift_days") &&
                    !key.equalsIgnoreCase("web_gift_days") &&
                    !key.equalsIgnoreCase("coin_for_second_time") &&
                    !key.equalsIgnoreCase("coin_for_third_time") &&
                    !key.equalsIgnoreCase("coin_for_forth_time") &&
                    !key.equalsIgnoreCase("coin_for_fifth_time")
            )
                continue;

            boolean hasLittleChar = false;
            for (int i = 0; i < key.length(); i++) {
                if (!Character.isUpperCase(key.charAt(i))) {
                    hasLittleChar = true;
                    break;
                }
            }

            Object val = config.get(key);

            if(
                    key.equalsIgnoreCase("web_gift_days") ||
                            key.equalsIgnoreCase("app_gift_days")
            ) {
                List<Document> days = config.getList(key, Document.class);
                val = days.stream().filter(itr -> (itr.getLong("date") > curr) || (curr - itr.getLong("date") < TWO_DAY_MIL_SEC)).collect(Collectors.toList());

                JSONArray finalList = new JSONArray();

                for(Document itr : (List<Document>)val) {

                    JSONObject jsonObject1 = new JSONObject();

                    if(itr.containsKey("ref_id")) {

                        ObjectId id = itr.getObjectId("ref_id");
                        String title = null;

                        if(itr.getString("target").equalsIgnoreCase(
                                GiftTarget.QUIZ.getName()
                        )) {
                            Document ref = iryscQuizRepository.findById(id);
                            if(ref == null)
                                continue;

                            title = ref.getString("title");
                        }
                        else if(itr.getString("target").equalsIgnoreCase(
                                GiftTarget.PACKAGE.getName()
                        )) {
                            Document ref = contentRepository.findById(id);
                            if(ref == null)
                                continue;

                            title = ref.getString("title");
                        }

                        if(title == null)
                            continue;

                        jsonObject1.put("additional", new JSONObject()
                                .put("id", id.toString())
                                .put("name", title)
                        );
                    }

                    jsonObject1.put("date", itr.getLong("date"))
                            .put("target", itr.getString("target"))
                            .put("id", itr.getObjectId("_id").toString())
                    ;

                    finalList.put(jsonObject1);
                }

                if (hasLittleChar)
                    jsonObject.put(Utility.camel(key, false), finalList);
                else
                    jsonObject.put(key, finalList);
            }
            else {

                if (hasLittleChar)
                    jsonObject.put(Utility.camel(key, false), val);
                else
                    jsonObject.put(key, val);
            }

        }

        if(!jsonObject.has("maxWebGiftSlot"))
            jsonObject.put("maxWebGiftSlot", 8);

        if(!jsonObject.has("maxAppGiftSlot"))
            jsonObject.put("maxAppGiftSlot", 8);

        if(!jsonObject.has("webGiftDays"))
            jsonObject.put("webGiftDays", new JSONArray());

        if(!jsonObject.has("appGiftDays"))
            jsonObject.put("appGiftDays", new JSONArray());

        return Utility.generateSuccessMsg("data", jsonObject);
    }

    public static String buildSpinner(String mode, ObjectId userId, double coin, ObjectId id) {

        if(!mode.equalsIgnoreCase("site") && !mode.equalsIgnoreCase("app"))
            return JSON_NOT_VALID_PARAMS;

        Document userGift = userGiftRepository.findById(id);

        if(userGift == null ||
                !userGift.getObjectId("user_id").equals(userId) ||
                !userGift.getString("mode").equals(mode)
        )
            return JSON_NOT_VALID_ID;

        long curr = System.currentTimeMillis();
        if(userGift.getString("status").equals("finish") ||
                userGift.getLong("expire_at") < curr
        )
            return JSON_NOT_ACCESS;

        JSONObject data = new JSONObject();
        Document config = Utility.getConfig();

        if(config.containsKey("coin_for_second_time") && coin > config.getDouble("coin_for_second_time")) {

            data.put("coinForSecondTime", config.get("coin_for_second_time"));

            if(config.containsKey("coin_for_third_time") && coin > config.getDouble("coin_for_third_time")) {
                data.put("coinForThirdTime", config.get("coin_for_third_time"));

                if(config.containsKey("coin_for_forth_time") && coin > config.getDouble("coin_for_forth_time")) {
                    data.put("coinForForthTime", config.get("coin_for_forth_time"));

                    if(config.containsKey("coin_for_fifth_time") && coin > config.getDouble("coin_for_fifth_time"))
                        data.put("coinForFifthTime", config.get("coin_for_fifth_time"));

                }

            }

        }

        boolean isForSite = mode.equalsIgnoreCase("site");
        List<Document> docs;

        if(!userGift.containsKey("gift"))
            docs = calcSpins(config, isForSite, userGift, null);
        else
            docs = userGift.getList("gifts", Document.class);

        JSONArray jsonArray = new JSONArray();
        for(Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("label", doc.getString("label"));

            if(doc.getBoolean("selected"))
                jsonObject.put("created_at", curr - 400000);
            else
                jsonObject.put("created_at", curr - getRandIntForGift(100000));


            jsonArray.put(jsonObject);
        }

        data.put("spins", jsonArray);
        return generateSuccessMsg("data", data);
    }

    private static List<Document> calcSpins(Document config, boolean isForSite,
                                            Document userGift,
                                            String repeat) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(exists("deleted_at", false));
        filters.add(gt("reminder", 0));
        filters.add(eq("is_for_site", isForSite));

        ArrayList<Document> gifts = giftRepository.find(and(filters), null, Sorts.ascending("priority"));
        int limit = Math.min(gifts.size(), (Integer) config.getOrDefault(isForSite ? "max_web_gift_slot" : "max_app_gift_slot", 8));
        int totalW = 0;
        ArrayList<Integer> upperBounds = new ArrayList<>();
        ArrayList<Document> docs = new ArrayList<>();

        for (int i = 0; i < limit; i++) {

            Document gift = gifts.get(i);

            totalW += gift.getInteger("prob");
            upperBounds.add(totalW);

            docs.add(new Document("_id", gift.getObjectId("_id"))
                    .append("selected", false)
                    .append("label", getGiftString(gift)));
        }

        int r = getRandIntForGift(totalW);
        int selectedGiftIdx = -1;

        for(int i = 0; i < upperBounds.size(); i++) {
            if(upperBounds.get(i) > r) {
                selectedGiftIdx = i;
                break;
            }
        }

        docs.get(selectedGiftIdx).put("selected", true);
        if(repeat == null) {
            userGift.put("gifts", docs);
            userGift.put("gift", gifts.get(selectedGiftIdx).getObjectId("_id"));
        }
        else {
            userGift.put("gifts_" + repeat, docs);
            userGift.put("gift_" + repeat, gifts.get(selectedGiftIdx).getObjectId("_id"));
        }

        userGiftRepository.replaceOne(userGift.getObjectId("_id"), userGift);
        return docs;
    }

    public static String buildSpinnerAgain(String mode, ObjectId userId,
                                           double coin, ObjectId id,
                                           String repeat) {

        if(!mode.equalsIgnoreCase("site") && !mode.equalsIgnoreCase("app"))
            return JSON_NOT_VALID_PARAMS;

        Document userGift = userGiftRepository.findById(id);

        if(userGift == null ||
                !userGift.getObjectId("user_id").equals(userId) ||
                !userGift.getString("mode").equals(mode)
        )
            return JSON_NOT_VALID_ID;

        long curr = System.currentTimeMillis();
        if(!userGift.getString("status").equals("finish") ||
                userGift.getLong("expire_at") < curr
        )
            return JSON_NOT_ACCESS;

        if(userGift.containsKey("finished_" + repeat))
            return JSON_NOT_ACCESS;

        JSONObject data = new JSONObject();
        Document config = Utility.getConfig();
        double neededCoin = -1;

        if(repeat.equalsIgnoreCase("second")) {

            if(
                !config.containsKey("coin_for_second_time") ||
                        coin < config.getDouble("coin_for_second_time")
            )
                return JSON_NOT_ACCESS;

            neededCoin = config.getDouble("coin_for_second_time");
        }

        else if(repeat.equalsIgnoreCase("third")) {

            if(
                    !config.containsKey("coin_for_third_time") ||
                            coin < config.getDouble("coin_for_third_time")
            )
                return JSON_NOT_ACCESS;

            neededCoin = config.getDouble("coin_for_third_time");
        }

        else if(repeat.equalsIgnoreCase("forth")) {

            if(
                    !config.containsKey("coin_for_forth_time") ||
                            coin < config.getDouble("coin_for_forth_time")
            )
                return JSON_NOT_ACCESS;

            neededCoin = config.getDouble("coin_for_forth_time");
        }

        else if(repeat.equalsIgnoreCase("fifth")) {

            if(
                    !config.containsKey("coin_for_fifth_time") ||
                            coin < config.getDouble("coin_for_ffifth_time")
            )
                return JSON_NOT_ACCESS;

            neededCoin = config.getDouble("coin_for_fifth_time");
        }

        if(neededCoin < 0)
            return JSON_NOT_ACCESS;

        userGift.put("needed_for_" + repeat, neededCoin);
        boolean isForSite = mode.equalsIgnoreCase("site");

        List<Document> docs;

        if(!userGift.containsKey("gift_" + repeat))
            docs = calcSpins(config, isForSite, userGift, repeat);
        else
            docs = userGift.getList("gifts", Document.class);

        if(docs == null)
            return JSON_NOT_UNKNOWN;

        JSONArray jsonArray = new JSONArray();
        for(Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("label", doc.getString("label"));

            if(doc.getBoolean("selected"))
                jsonObject.put("created_at", curr - 400000);
            else
                jsonObject.put("created_at", curr - getRandIntForGift(100000));


            jsonArray.put(jsonObject);
        }

        data.put("spins", jsonArray);
        return generateSuccessMsg("data", data);
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

    public static String giveMyGift(ObjectId id, String repeat, Document user) {

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Document doc = userGiftRepository.findById(id);
        ObjectId userId = user.getObjectId("_id");
        long curr = System.currentTimeMillis();

        if(doc == null ||
                !doc.getObjectId("user_id").equals(userId) ||
                doc.getLong("expire_at") < curr
        )
            return JSON_NOT_ACCESS;

        if(repeat == null && doc.getString("status").equalsIgnoreCase("finish"))
            return JSON_NOT_ACCESS;

        Document myGift;
        double neededCoin = 0;

        if(repeat != null) {

            if(
                    !repeat.equalsIgnoreCase("second") &&
                    !repeat.equalsIgnoreCase("third") &&
                    !repeat.equalsIgnoreCase("forth") &&
                    !repeat.equalsIgnoreCase("fifth")
            )
                return JSON_NOT_ACCESS;

            if(
                    !doc.containsKey("needed_for_" + repeat) ||
                    !doc.containsKey("gift_" + repeat) ||
                    doc.containsKey("status_" + repeat)
            )
                return JSON_NOT_ACCESS;

            neededCoin = doc.getDouble("needed_for_" + repeat);
            if(neededCoin > user.getDouble("coin"))
                return JSON_NOT_ACCESS;

            myGift = giftRepository.findById(doc.getObjectId("gift_" + repeat));
        }
        else
            myGift = giftRepository.findById(doc.getObjectId("gift"));

        if(myGift == null)
            return JSON_NOT_UNKNOWN;

        boolean changeUser = false;

        if(myGift.getString("type").equalsIgnoreCase("coin")) {
            user.put("coin",
                    Math.round((user.getDouble("coin") + ((Number)myGift.get("amount")).doubleValue() - neededCoin) * 100.0) / 100.0
            );
            changeUser = true;
        }
        else if(myGift.getString("type").equalsIgnoreCase("money")) {

            if(neededCoin > 0)
                user.put("coin", Math.round((user.getDouble("coin") - neededCoin) * 100.0) / 100.0);

            user.put("money", Math.round((((Number)user.get("money")).doubleValue() + myGift.getInteger("amount")) * 100.0 / 100.0));
            changeUser = true;
        }
        else if(myGift.getString("type").equalsIgnoreCase("offcode")) {

            JSONObject res = new JSONObject(OffCodeController.store(new JSONObject()
                    .put("expireAt", myGift.getLong("expire_at"))
                    .put("type", myGift.getString("off_code_type"))
                    .put("amount", myGift.getInteger("amount"))
                    .put("section", myGift.getString("use_for"))
                    .put("items", new JSONArray().put(user.getString("NID")))
            ));

            if(!res.getString("status").equalsIgnoreCase("ok"))
                return JSON_NOT_UNKNOWN;

            if(neededCoin > 0) {
                user.put("coin", Math.round((user.getDouble("coin") - neededCoin) * 100.0) / 100.0);
                changeUser = true;
            }
        }

        if(repeat == null)
            doc.put("status", "finish");
        else
            doc.put("status_" + repeat, "finish");

        userGiftRepository.replaceOne(doc.getObjectId("_id"), doc);

        if(changeUser)
            userRepository.replaceOne(userId, user);

        myGift.put("reminder", myGift.getInteger("reminder") - 1);
        giftRepository.replaceOne(myGift.getObjectId("_id"), myGift);

        if(repeat == null)
            return JSON_OK;

        double coin = user.getDouble("coin");
        Document config = Utility.getConfig();
        JSONObject data = new JSONObject();

        int counter = repeat.equalsIgnoreCase("second") ? 2 :
                repeat.equalsIgnoreCase("third") ? 3 :
                        repeat.equalsIgnoreCase("forth") ? 4 : 5;


        if(counter < 3 && config.containsKey("coin_for_third_time") && coin > config.getDouble("coin_for_third_time"))
            data.put("coinForThirdTime", config.get("coin_for_third_time"));

        if(counter < 4 && config.containsKey("coin_for_forth_time") && coin > config.getDouble("coin_for_forth_time"))
            data.put("coinForForthTime", config.get("coin_for_forth_time"));

        if(counter < 5 && config.containsKey("coin_for_fifth_time") && coin > config.getDouble("coin_for_fifth_time"))
            data.put("coinForFifthTime", config.get("coin_for_fifth_time"));

        return generateSuccessMsg("data", data);
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

    public static String updateConfig(JSONObject jsonObject) {

        Document config = Utility.getConfig();

        if(jsonObject.has("maxWebGiftSlot")) {

            config.put("max_web_gift_slot", jsonObject.getInt("maxWebGiftSlot"));

            if(jsonObject.has("webGiftDays")) {

                JSONArray webGiftDays = jsonObject.getJSONArray("webGiftDays");
                ArrayList<Document> validated = new ArrayList<>();

                for(int i = 0; i < webGiftDays.length(); i++) {

                    JSONObject info = webGiftDays.getJSONObject(i);

                    if(!info.has("target") || !info.has("date"))
                        continue;

                    String target = info.getString("target");
                    try {

                        if (!EnumValidatorImp.isValid(target, GiftTarget.class))
                            continue;

                        if (target.equalsIgnoreCase(GiftTarget.PACKAGE.getName()) ||
                                target.equalsIgnoreCase(GiftTarget.QUIZ.getName())
                        ) {

                            if (!info.has("additional"))
                                continue;

                            if(!info.getJSONObject("additional").has("id"))
                                continue;

                            String id = info.getJSONObject("additional").getString("id");
                            if(!ObjectId.isValid(id))
                                continue;

                            if(target.equalsIgnoreCase(GiftTarget.PACKAGE.getName()) &&
                                contentRepository.findById(new ObjectId(id)) == null
                            )
                                continue;

                            if(target.equalsIgnoreCase(GiftTarget.QUIZ.getName()) &&
                                    iryscQuizRepository.findById(new ObjectId(id)) == null
                            )
                                continue;
                        }

                        Document newDoc = new Document("date", info.getLong("date"))
                                .append("target", target).append("_id", new ObjectId());

                        if (target.equalsIgnoreCase(GiftTarget.PACKAGE.getName()) ||
                                target.equalsIgnoreCase(GiftTarget.QUIZ.getName())
                        )
                            newDoc.put("ref_id", new ObjectId(info.getJSONObject("additional").getString("id")));

                        validated.add(newDoc);
                    }
                    catch (Exception ignore) {}

                }

                config.put("web_gift_days", validated);
            }
            else
                config.put("web_gift_days", new ArrayList<>());

        }

        if(jsonObject.has("coinForSecondTime"))
            config.put("coin_for_second_time", jsonObject.getNumber("coinForSecondTime").doubleValue());

        if(jsonObject.has("coinForThirdTime"))
            config.put("coin_for_third_time", jsonObject.getNumber("coinForThirdTime").doubleValue());

        if(jsonObject.has("coinForForthTime"))
            config.put("coin_for_forth_time", jsonObject.getNumber("coinForForthTime").doubleValue());

        if(jsonObject.has("coinForFifthTime"))
            config.put("coin_for_fifth_time", jsonObject.getNumber("coinForFifthTime").doubleValue());


        configRepository.replaceOne(config.getObjectId("_id"), config);
        configRepository.clearFromCache(config.getObjectId("_id"));
        return JSON_OK;
    }

    public static String report(Long from, Long to,
                                ObjectId giftId, String repeat,
                                ObjectId userId) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("status", "finish"));

        if(from != null)
            filters.add(gte("expire_at", from + ONE_DAY_MIL_SEC));

        if(to != null)
            filters.add(lte("expire_at", to + ONE_DAY_MIL_SEC));

        if(userId != null)
            filters.add(eq("user_id", userId));

        if(giftId != null)
            filters.add(or(
                    eq("gift", giftId),
                    eq("gift_second", giftId),
                    eq("gift_third", giftId),
                    eq("gift_forth", giftId),
                    eq("gift_fifth", giftId)
            ));

        if(repeat != null) {
            if(repeat.equalsIgnoreCase("second") ||
                    repeat.equalsIgnoreCase("third") ||
                    repeat.equalsIgnoreCase("forth") ||
                    repeat.equalsIgnoreCase("fifth")
            )
                filters.add(exists("status_" + repeat));
        }

        ArrayList<Document> docs = userGiftRepository.find(and(filters), null);
        JSONArray jsonArray = new JSONArray();
        ArrayList<ObjectId> userIds = new ArrayList<>();

        for(Document doc : docs)
            userIds.add(doc.getObjectId("user_id"));

        ArrayList<Document> users = userRepository.findByIds(userIds, true);
        if(users == null)
            return JSON_NOT_UNKNOWN;

        int i = 0;
        String[] repeats = new String[] {"second", "third", "forth", "fifth"};
        String[] repeatsFa = new String[] {"دوم", "سوم", "چهارم", "پنجم"};

        HashMap<ObjectId, String> gifts = new HashMap<>();
        String g;

        for(Document doc : docs) {

            JSONObject student = new JSONObject();
            Utility.fillJSONWithUser(student, users.get(i));
            String createdAt = Utility.getSolarDate(doc.getLong("expire_at") - ONE_DAY_MIL_SEC);

            if(
                    (giftId == null || giftId.equals(doc.getObjectId("gift"))) &&
                    (repeat == null || repeat.equalsIgnoreCase("first"))
            ) {

                g = null;
                if (gifts.containsKey(doc.getObjectId("gift")))
                    g = gifts.get(doc.getObjectId("gift"));
                else {
                    Document gift = giftRepository.findById(doc.getObjectId("gift"));
                    if (gift != null)
                        g = getGiftString(gift);

                    gifts.put(doc.getObjectId("gift"), g);
                }

                if (g != null) {
                    jsonArray.put(new JSONObject()
                            .put("student", student.getJSONObject("student"))
                            .put("gift", g)
                            .put("createdAt", createdAt)
                            .put("repeat", "اول")
                    );
                }
            }

            int j = 0;
            for(String r : repeats) {
                if(doc.containsKey("status_" + r)) {

                    if(
                            (giftId == null || giftId.equals(doc.getObjectId("gift_" + r))) &&
                                    (repeat == null || repeat.equalsIgnoreCase(repeats[j]))
                    ) {

                        g = null;

                        if(gifts.containsKey(doc.getObjectId("gift_" + r)))
                            g = gifts.get(doc.getObjectId("gift_" + r));
                        else {
                            Document gift = giftRepository.findById(doc.getObjectId("gift_" + r));
                            if(gift != null)
                                g = getGiftString(gift);

                            gifts.put(doc.getObjectId("gift_" + r), g);
                        }

                        if(g != null)
                            jsonArray.put(new JSONObject()
                                    .put("student", student.getJSONObject("student"))
                                    .put("gift", g)
                                    .put("createdAt", createdAt)
                                    .put("repeat", repeatsFa[j])
                            );
                    }

                }
                else break;
                j++;
            }

            i++;
        }

        if(filters.size() == 1) {

            JSONArray allGifts = new JSONArray();
            for(ObjectId id : gifts.keySet())
                allGifts.put(new JSONObject()
                        .put("id", id.toString())
                        .put("item", gifts.get(id))
                );

            return generateSuccessMsg("data", jsonArray,
                    new PairValue("gifts", allGifts)
            );
        }


        return generateSuccessMsg("data", jsonArray,
                new PairValue("gifts", new JSONArray())
        );
    }
}
