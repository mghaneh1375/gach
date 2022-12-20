package irysc.gachesefid.Controllers.Content;


import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Quiz.OpenQuiz;
import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.packageRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentContentController {

    public static String distinctTags() {
        return generateSuccessMsg("data", contentRepository.distinctTags("tags"));
    }

    public static String distinctTeachers() {
        return generateSuccessMsg("data", contentRepository.distinctTags("teacher"));
    }

    public static String getAll(ObjectId userId,
                                boolean isAdmin,
                                String tag,
                                String title,
                                String teacher,
                                Boolean visibility,
                                Boolean hasCert,
                                Integer minPrice,
                                Integer maxPrice) {

        ArrayList<Bson> filters = new ArrayList<>();

        if(!isAdmin)
            filters.add(eq("visibility", true));

        if(!isAdmin && userId != null)
            filters.add(nin("users._id", userId));

        if(title != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(title), Pattern.CASE_INSENSITIVE)));

        if(tag != null)
            filters.add(in("tags", tag));

        if(hasCert != null)
            filters.add(exists("cert_id", hasCert));

        if(minPrice != null)
            filters.add(lte("price", minPrice));

        if(maxPrice != null)
            filters.add(gte("price", maxPrice));

        if(isAdmin) {

            if(visibility != null)
                filters.add(eq("visibility", visibility));

            if(teacher != null)
                filters.add(eq("teacher", teacher));

        }

        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                filters.size() == 0 ? null : and(filters),
                isAdmin ? CONTENT_DIGEST_FOR_ADMIN : CONTENT_DIGEST
        );

        if(!isAdmin && filters.size() == 2) {

            int min = 1000000000;
            int max = -1;

            int maxDuration = -1;
            int minDuration = 10000000;

            for(Document doc : docs) {

                int price = doc.getInteger("price");
                int duration = doc.getInteger("duration");

                if(price < min)
                    min = price;

                if(price > max)
                    max = price;

                if(duration < minDuration)
                    minDuration = duration;

                if(duration > maxDuration)
                    maxDuration = duration;

                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));
            }

            return generateSuccessMsg("data", data,
                    new PairValue("min", min),
                    new PairValue("max", max),
                    new PairValue("minDuration", minDuration),
                    new PairValue("maxDuration", maxDuration),
                    new PairValue("tags", contentRepository.distinctTags("tags"))
            );
        }

        else {
            for (Document doc : docs)
                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));
        }

        return generateSuccessMsg("data", data);
    }


    public static String getMy(ObjectId userId) {
        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                in("users._id", userId),
                CONTENT_DIGEST
        );

        for(Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false));

        return generateSuccessMsg("data", data);
    }


    public static String get(boolean isAdmin, ObjectId userId, String slug) {

        Document content = contentRepository.findBySecKey(slug);
        if(content == null)
            return JSON_NOT_VALID_ID;

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convert(
                content, isAdmin,
                isAdmin || userId != null && irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        content.getList("users", Document.class), "_id", userId
                ) != -1, true)
        );
    }

    public static String registry(ObjectId contentId, ObjectId userId,
                                  double paid, String phone, String mail) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = content.getList("users", Document.class);
        users.add(new Document("_id", userId)
                .append("paid", paid));

        contentRepository.replaceOne(contentId, content);

        if(mail != null) {
//            new Thread(() -> sendMail(
//                    mail,
//                    SERVER + "recp/" + transaction.getObjectId("_id").toString(),
//                    "successQuiz",
//                    user.getString("first_name") + " " + user.getString("last_name")
//            )).start();
        }

        return JSON_OK;
    }

    public static String buy(ObjectId contentId, JSONObject data, ObjectId userId,
                             double money, String phone, String mail
    ) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return JSON_NOT_VALID_ID;

        if(!content.getBoolean("visibility"))
            return JSON_NOT_ACCESS;

        if(irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                content.getList("users", Document.class), "_id", userId) != -1
        )
            return generateErr("شما قبلا این دوره را خریداری را کرده اید.");

        Document off = null;
        long curr = System.currentTimeMillis();

        if (data != null && data.has("off")) {

            off = validateOffCode(
                    data.getString("off"), userId, curr,
                    OffCodeSections.CONTENT.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");

        }

        if (data == null || !data.has("off")) {
            off = findAccountOff(
                    userId, curr, OffCodeSections.CONTENT.getName()
            );
        }

        double shouldPayDouble = content.getInteger("price") * 1.0;
        double offAmount = 0;

        if(off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPayDouble * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPayDouble -= offAmount;
        }

        int shouldPay = (int) shouldPayDouble;

        if (shouldPay - money <= 100) {

            double newUserMoney = money;

            if (shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, money);
                Document user = userRepository.findById(userId);
                user.put("money", newUserMoney);
                userRepository.replaceOne(userId, user);
            }

            Document finalOff = off;
            double finalOffAmount = offAmount;

            new Thread(() -> {

                Document doc = new Document("user_id", userId)
                        .append("amount", 0)
                        .append("account_money", shouldPay)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.CONTENT.getName())
                        .append("products", contentId);

                if (finalOff != null) {
                    doc.append("off_code", finalOff.getObjectId("_id"));
                    doc.append("off_amount", (int) finalOffAmount);
                }

                transactionRepository.insertOne(doc);
                registry(contentId, userId, shouldPay, phone, mail);

                if (finalOff != null) {

                    BasicDBObject update;

                    if (finalOff.containsKey("is_public") &&
                            finalOff.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                        students.add(userId);
                        update = new BasicDBObject("students", students);
                    } else {

                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.GACH_EXAM.getName())
                                .append("used_for", userId);
                    }

                    offcodeRepository.updateOne(
                            finalOff.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }


            }).start();

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", newUserMoney)
            );
        }


        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document doc =
                new Document("user_id", userId)
                        .append("account_money", money)
                        .append("amount", (int) (shouldPay - money))
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", contentId)
                        .append("section", OffCodeSections.CONTENT.getName());

        if (off != null) {
            doc.append("off_code", off.getObjectId("_id"));
            doc.append("off_amount", (int) offAmount);
        }

        return goToPayment((int) (shouldPay - money), doc);

    }
}
