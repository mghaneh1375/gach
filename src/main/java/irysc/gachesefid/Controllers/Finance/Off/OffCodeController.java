package irysc.gachesefid.Controllers.Finance.Off;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
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
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Finance.Off.Utility.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class OffCodeController {

    private final static String token = "LYqPozxUPrpDVAxqBs7vXp1knTnEgEYUrnbpqKtggn1DHDQofCn=?aAdh6Mo/5I8qTcJSkAQppUCB/pzfD2G-0PNkl-R3Ub42BAv4QHFUlWgAfu6SwyRH!O3jUF426jw33EMCGJmYWSApEbCg?t4dRGowUX-ixRMUjqoCF/6s8IcyTQHtOYK9OD/xBGKjq/yzA/AEfz6xP3A7k-CBElUbTRU=3WyZaOjZnANV6EUE3PiK9g-5XKvos-qPP?Pcau=o8li6fDopR/L1ccV9SHlGEGxIMM7V5ZyKCKgxbc4J7lx0nyd0DCI?ddiWHXqAYbe2!oIZT-o4pEdJF2IVYj-jpyJI?VOwMrVR4P-4hrj2fyvW5fFhlgrpBVe6CpwGYdmcx7QxMyjwb5vAh!1tDM=jcVP/AL2a-JzbdtGhYCgkbxexHoZYFjV5goTGjTMXB?yQJrn!ABGC?BYV0B59NNi!0n/u/tbIqTant-FixPJ5otO4h6278ZHpiL9llIg9t0Rk3Sym=fnhEkE4GkfaM6jVTMaSC2iVc9jMg2ug2pXoK1NMaNA/LW77kiJTfrmnobzbPSK7fX84Klk7QtE53i-CdD=a=gB2NnWXKwqX7dic=S289OeaNlP705GZ9-6wvF!HvFTBtlyckzaAVXR2zjip7JLTvnwqrIWKd=P3Hs/!r3Rdmp7OTJgYw85Wq4dIX=Aa/lAc77n0wRyQ5AtaocYAYhstBoF2U96QTcUofNWOn1j6hJAUhboqd99L2dWykVtreRi7E7PYWNef1qrV/yUQbPKaNH4EKvppN03D3Rd1iCq1Kq8N=5ayyx=hOvrvqZ3EoT-x3RA-CY7m0i452xbKSP4nJLzT-/t!uB1/VlxDUoK8XdoGMZo0DMjUWbV8t5j9Kk/w7ta5vcPELszuqA-JkAnDlly?Tg=XBnbhJEyUTndjXrW3Y09BLn/hS7dcF2pU7Az6OleSnuMWGcBO2qPtwrUf2EgcwgPrqG4EwWzdx5IfeoKSbhC?BKi-mFcBV/bv!mnSZi7?BiLG?e1srXRJx?uY??lFXf3B2Lh-?R?d2BB7PV0x!UPgqwnRLCc8noaw0dqbrl6ab7U?Sl7CGlS2R4oeDIM=?jWBUL659cYQ/SdKJ-0xw9jWGWo?fx?qUzwbnrDgPvls2PdWot9ybfuuBJU7Kh2EgW?DbYBaU8MSfqnMLxTD1GKWVIGBFhCL-6n=oczCtLObrwz3j1g15ua2Igiuhf4s?LaRGtB1nFgD3q!5DRsye6IFK15PCS-TaQGFcNSXvh8woN4cOfSWhslDTKrlY5oiZN05qWu25?FI-2gWekEFKuZw3dMPIohxA2wlnx4ILk9Q6Y0iacefvPUm=zMHFot8jP5mFgZoLQRha-Q=6Xvl2ooUlqSdwMi83uoSfJgKAdMK2fa=NP?BT1vOdraDGZoXlQFmL4=2?FnN3Pa8iXVp/ZpNJ?DXnhGzsa624PkH5!HhsBS=YD?9qsX6y=XILOhuGrwub?sokmEVK0IKzd0AvHVpD1m96nlwXqO1KXmPjUodjBiZb/RQHH0?JzjydqV-AQmAdBWM-nel/In8wpRqkqsOHq!l5B6eNlCbqVjVB-?y8PHPorDL0?C1a9z0p7befPM?RaRD9Y127EWyqFgsrSzMJ-!chcC0CtKebR5uiopTQM=eI1jsiiXI5KNB7gIj95tGP=TcWy=HSr6mmfEhw1mUky-7!5m9ZUxYl9ghU6AvCQ!Yl?piMJZyYVVsrz07dELveO6pppGu0hEZ8qL-yfL2fGeR6MmCWdEsAkGg4cfTzQRp3?wfSAtziJ-49=U3XA7E?QS4hA?ZInmx!vTAFN?VvMDgg1LC9xzj8l2Dkzz?!=3TYHRLfAJVvgBBhS!WJkJSscQia-QEBAeKwe0347X6uwefT?0bo!9=iP0/vWgSF-DQ0=WgT/yARdniodePw!T-CAZlF1bfU/Z1O!RFJ2hunLYkdFWNyoepO?bFMCAV6h3hEhjSZkxtQy?fiV-BtX!Sh?-?jZBOa4Cecjj0eqaXehVB!G8WZEu";

    public static String getShopCopunReport() {

        try {
            HttpResponse<JsonNode> res = Unirest.post("https://shop.irysc.com/getReport.php")
                    .header("accept", "application/json")
                    .field("token", "LYqPozxUPrpDVAxqBs7vXp1knTnEgEYUrnbpqKtggn1DHDQofCn=?")
                    .asJson();

            if (res.getStatus() == 200) {

                JSONArray output = new JSONArray();
                JSONArray data = res.getBody().getObject().getJSONArray("data");

                for (int i = 0; i < data.length(); i++) {

                    JSONObject jsonObject = data.getJSONObject(i);

                    if (!jsonObject.has("name") || !jsonObject.has("discount") ||
                            !jsonObject.has("code")
                    )
                        continue;

                    String name = jsonObject.getString("name");
                    if (!name.contains("_"))
                        continue;

                    String[] splited = name.split("_");
                    if (splited.length != 2)
                        continue;

                    if (!ObjectId.isValid(splited[1]))
                        continue;

                    long createdAt;
                    try {
                        createdAt = Long.parseLong(splited[0]);
                    } catch (Exception xx) {
                        continue;
                    }

                    Document user = userRepository.findById(new ObjectId(splited[1]));
                    if (user == null)
                        continue;

                    JSONObject jsonObject1 = new JSONObject();
                    fillJSONWithUser(jsonObject1, user);

                    output.put(
                            jsonObject1.put("createdAt", getSolarDate(createdAt))
                                    .put("discount", jsonObject.get("discount"))
                                    .put("code", jsonObject.get("code"))
                    );

                }

                return generateSuccessMsg("data", output);
            }

        } catch (UnirestException e) {
            return generateErr(e.getMessage());
        }

        return generateErr("خطا در برقراری ارتیاط");
    }

    public static String getShopCopunRevReport() {

        try {
            HttpResponse<JsonNode> res = Unirest.post("https://shop.irysc.com/getReportRev.php")
                    .header("accept", "application/json")
                    .field("token", "LYqPozxUPrpDVAxqBs7vXp1knTnEgEYUrnbpqKtggn1DHDQofCn=?")
                    .asJson();

            if (res.getStatus() == 200) {

                JSONArray output = new JSONArray();
                JSONArray data = res.getBody().getObject().getJSONArray("data");

                for (int i = 0; i < data.length(); i++) {

                    JSONObject jsonObject = data.getJSONObject(i);

                    if (!jsonObject.has("firstname") || !jsonObject.has("lastname") ||
                            !jsonObject.has("telephone") || !jsonObject.has("email") ||
                            !jsonObject.has("value") || !jsonObject.has("result_api") ||
                            !jsonObject.has("created_at")
                    )
                        continue;

                    output.put(jsonObject);
                }

                return generateSuccessMsg("data", output);
            }

        } catch (UnirestException e) {
            return generateErr(e.getMessage());
        }

        return generateErr("خطا در برقراری ارتیاط");
    }

    public static String storeFromShop(JSONObject jsonObject, String ip) {

        if (!jsonObject.getString("token").equals(token))
            return JSON_NOT_ACCESS;

        if (!ip.equals("31.41.35.5"))
            return JSON_NOT_ACCESS;

        Document config = getConfig();
        if (!(boolean) config.getOrDefault("create_shop_off_visibility", false))
            return "not active";

        double total = Double.parseDouble(jsonObject.get("total").toString());
        if (total < (int) config.getOrDefault("min_buy_amount_for_shop", 50000))
            return "not enough total";

        double credit = (total * (int) config.getOrDefault("percent_of_shop_buy", 50)) / 100;

        String phone = jsonObject.getString("phone");
        String phoneWithZero, phoneWithOutZero;

        if (phone.startsWith("0")) {
            phoneWithOutZero = phone.substring(1);
            phoneWithZero = phone;
        } else {
            phoneWithOutZero = phone;
            phoneWithZero = "0" + phone;
        }

        Document user = userRepository.findOne(or(
                eq("phone", phoneWithZero),
                eq("phone", phoneWithOutZero),
                eq("mail", jsonObject.getString("email"))
        ), null);

        String name = jsonObject.getString("firstName") + " " + jsonObject.getString("lastName");

        if (user == null) {

            Document tmp = creditRepository.findOne(eq("phone", phoneWithZero), null);

            if (tmp == null)
                creditRepository.insertOne(
                        new Document("credit", credit)
                                .append("phone", phoneWithZero)
                                .append("mail", jsonObject.getString("email"))
                                .append("created_at", System.currentTimeMillis())
                );
            else {
                tmp.put("credit", ((Number) tmp.get("credit")).doubleValue() + credit);
                creditRepository.updateOne(tmp.getObjectId("_id"), set("credit", tmp.get("credit")));
            }

            sendSMSWithTemplate(phoneWithZero, 949,
                    new PairValue("name", name),
                    new PairValue("price", (int) credit + "")
            );

            return "not exist";
        }


        user = userRepository.findById(user.getObjectId("_id"));

        double mainMoney = ((Number) (user.get("money"))).doubleValue();
        user.put("money", mainMoney + credit);

        sendSMSWithTemplate(phoneWithZero, 815, new PairValue("name", name));
        String msg = "<p>" + "سلام " + name + "<br/>";
        msg += "اعتبار شما به دلیل خرید از فروشگاه آیریسک " + credit + " تومان افزایش یافت.";
        msg += "</p>";

        ObjectId notifId = new ObjectId();
        Document finalUser = user;
        Document notif = new Document("_id", notifId)
                .append("users_count", 1)
                .append("title", "افزایش اعتبار")
                .append("text", msg)
                .append("send_via", "site")
                .append("created_at", System.currentTimeMillis())
                .append("users", new ArrayList<ObjectId>() {{
                    add(finalUser.getObjectId("_id"));
                }});

        List<Document> events = (List<Document>) user.getOrDefault("events", new ArrayList<Document>());
        events.add(
                new Document("created_at", System.currentTimeMillis())
                        .append("notif_id", notifId)
                        .append("seen", false)
        );

        user.put("events", events);
        notifRepository.insertOne(notif);

        userRepository.replaceOne(user.getObjectId("_id"), user);
        return "ok";
    }

    public static String update(ObjectId id, JSONObject jsonObject) {

        Document off = offcodeRepository.findById(id);
        if (off == null)
            return JSON_NOT_VALID_ID;

        String type, section;
        int amount;
        long d;

        if (jsonObject.has("type")) {
            type = jsonObject.getString("type");
            if (!EnumValidatorImp.isValid(type, OffCodeTypes.class))
                return JSON_NOT_VALID_PARAMS;
        } else
            type = off.getString("type");

        if (jsonObject.has("amount")) {
            amount = jsonObject.getInt("amount");
            if (type.equals("percent") && amount > 100)
                return JSON_NOT_VALID_PARAMS;
        } else
            amount = off.getInteger("amount");

        if (jsonObject.has("section")) {
            section = jsonObject.getString("section");

            if (!EnumValidatorImp.isValid(section, OffCodeSections.class))
                return JSON_NOT_VALID_PARAMS;
        } else
            section = off.getString("section");

        if (jsonObject.has("expireAt")) {
            d = jsonObject.getLong("expireAt");
            if (System.currentTimeMillis() > d)
                return JSON_NOT_VALID_PARAMS;
        } else
            d = off.getLong("expireAt");

        if (jsonObject.has("code"))
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
        if (err != null)
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

        return addAll(jsonArray, type, amount, expireAt, section, excepts, null);
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

        if (isPublic && code == null)
            return JSON_NOT_VALID_PARAMS;

        String err = preStoreCheck(type, amount, expireAt, section);
        if (err != null)
            return err;

        if (isPublic) {
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

        if (!jsonObject.has("items"))
            return JSON_NOT_VALID_PARAMS;

        JSONArray jsonArray = jsonObject.getJSONArray("items");
        return addAll(jsonArray, type, amount, expireAt, section, new JSONArray(), code);
    }

    public static String offs(ObjectId userId,
                              String section,
                              Boolean used,
                              Boolean expired,
                              Long createdAt,
                              Long createdAtEndLimit,
                              Long expiredAt,
                              Long expiredAtEndLimit,
                              Long usedAt,
                              Long usedAtEndLimit,
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

        if (withCode != null) {
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

        if (usedAt != null)
            constraints.add(and(
                    exists("used_at"),
                    gte("used_at", usedAt)
            ));

        if (usedAtEndLimit != null)
            constraints.add(and(
                    exists("used_at"),
                    lte("used_at", usedAtEndLimit)
            ));

        if (expiredAt != null)
            constraints.add(and(
                    exists("expire_at"),
                    gte("expire_at", expiredAt)
            ));

        if (expiredAtEndLimit != null)
            constraints.add(and(
                    exists("expire_at"),
                    lte("expire_at", expiredAtEndLimit)
            ));

        if (createdAt != null)
            constraints.add(and(
                    exists("created_at"),
                    gte("created_at", createdAt)
            ));

        if (createdAtEndLimit != null)
            constraints.add(and(
                    exists("created_at"),
                    lte("created_at", createdAtEndLimit)
            ));

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

        List<Document> offs = offcodeRepository.find(and(
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

        if (offs.size() == 0)
            return generateErr("کد تخفیف موردنظر اشتباه است.");

        Document off = null;

        for (Document itr : offs) {

            if (itr.containsKey("used") && itr.getBoolean("used"))
                continue;

            off = itr;
            break;
        }

        if (off == null)
            return generateErr("شما قبلا از این کد استفاده کرده اید.");

        if (off.getLong("expire_at") < System.currentTimeMillis())
            return generateErr("کد مدنظر منقضی شده است.");

        if (off.containsKey("students") &&
                off.getList("students", ObjectId.class).contains(userId)
        )
            return generateErr("شما قبلا از این کد استفاده کرده اید.");

        if (!off.getString("section").equalsIgnoreCase(
                OffCodeSections.ALL.getName())
        ) {
            if (!section.equalsIgnoreCase(off.getString("section")))
                return generateErr("این کد تنها در قسمت " + GiftController.translateUseFor(off.getString("section")) + " قابل استفاده است.");
        }

        return generateSuccessMsg("data", new JSONObject()
                .put("type", off.getString("type"))
                .put("amount", off.get("amount"))
        );
    }
}
