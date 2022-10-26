package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Utility.Authorization;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Quiz.Utility.checkFields;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasProtectedAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class OpenQuizController {

    private final static String[] mandatoryFields = {
            "price", "duration"
    };

    private final static String[] forbiddenFields = {
            "startRegistry", "start",
            "end", "isOnline", "showResultsAfterCorrection",
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "paperTheme", "database",
    };

    private final static String[] forbiddenTransferFields = {
            "start_registry", "start", "_id",
            "end", "end_registry", "show_results_after_correction", "launch_mode",
            "permute", "visibility", "back_en", "mysql_id", "created_by",
            "created_at", "top_students_count", "students", "registered",
            "ranking_list", "report_status", "general_stat", "question_stat"
    };

    public static String create(ObjectId userId, JSONObject jsonObject) {

        try {

            checkFields(mandatoryFields, forbiddenFields, jsonObject);
            jsonObject.put("mode", KindQuiz.OPEN.getName());
            Document newDoc = QuizController.store(userId, jsonObject);

            return iryscQuizRepository.insertOneWithReturn(newDoc);

        } catch (InvalidFieldsException e) {
            return irysc.gachesefid.Utility.Utility.generateErr(e.getMessage());
        }
    }

    public static String createFromIRYSCQuiz(ObjectId quizId) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if(quiz == null)
            return JSON_NOT_VALID_ID;

        Document newQuiz = new Document("created_at", System.currentTimeMillis())
                .append("students", new ArrayList<>())
                .append("last_build_at", null)
                .append("last_finished_at", null)
                .append("registered", 0);


        List<String> forbiddenKeys = Arrays.asList(forbiddenTransferFields);

        for (String key : quiz.keySet()) {

            if(forbiddenKeys.contains(key))
                continue;

            newQuiz.append(key, quiz.get(key));
        }

        openQuizRepository.insertOne(newQuiz);
        return JSON_OK;
    }

    public static String buy(ObjectId userId, JSONArray ids,
                             double money, String phone,
                             String mail, String offcode
    ) {

        Document off = null;
        long curr = System.currentTimeMillis();

        if (offcode != null) {

            off = validateOffCode(
                    offcode, userId, curr,
                    OffCodeSections.GACH_EXAM.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");
        }

        ArrayList<ObjectId> quizIds = new ArrayList<>();
        int totalPrice = 0;

        for (int i = 0; i < ids.length(); i++) {

            if (!ObjectId.isValid(ids.getString(i)))
                return JSON_NOT_VALID_PARAMS;

            ObjectId quizId = new ObjectId(ids.getString(i));

            Document quiz = openQuizRepository.findById(quizId);
            if(quiz == null)
                return JSON_NOT_VALID_ID;

            if(searchInDocumentsKeyValIdx(
                    quiz.getList("students", Document.class),
                    "_id", userId
            ) != -1)
                continue;

            quizIds.add(quizId);
            totalPrice += quiz.getInteger("price");

        }

        if(quizIds.size() == 0)
            return generateErr("شما در آزمون های مدنظر ثبت نام شده اید.");

        if (off == null)
            off = findAccountOff(
                    userId, curr, OffCodeSections.GACH_EXAM.getName()
            );

        double offAmount = 0;
        double shouldPayDouble = totalPrice;

        if (off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPayDouble * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPayDouble = totalPrice - offAmount;
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

                new OpenQuiz()
                        .registry(userId, phone, mail, quizIds, 0);

                if (finalOff != null) {

                    BasicDBObject update;

                    if (finalOff.containsKey("is_public") &&
                            finalOff.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                        students.add(userId);
                        update = new BasicDBObject("students", students);
                    } else
                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.GACH_EXAM.getName())
                                .append("used_for", quizIds);

                    offcodeRepository.updateOne(
                            finalOff.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }

                Document doc = new Document("user_id", userId)
                        .append("amount", 0)
                        .append("account_money", shouldPay)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.OPEN_EXAM.getName())
                        .append("products", quizIds);

                if (finalOff != null) {
                    doc.append("off_code", finalOff.getObjectId("_id"));
                    doc.append("off_amount", (int) finalOffAmount);
                }

                transactionRepository.insertOne(doc);

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
                        .append("amount", shouldPay - money)
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", quizIds)
                        .append("section", OffCodeSections.OPEN_EXAM.getName());

        if(off != null) {
            doc.append("off_code", off.getObjectId("_id"));
            doc.append("off_amount", (int)offAmount);
        }

        return goToPayment((int) (shouldPay - money), doc);

    }


    public static JSONObject getAll(boolean isAdmin, ObjectId userId) {

        JSONArray jsonArray = new JSONArray();
        long curr = System.currentTimeMillis();

        HashMap<String, ArrayList<String>> tags = new HashMap<>();
        JSONObject data = new JSONObject();

        ArrayList<Document> docs = openQuizRepository.find(
                isAdmin || userId == null ? null : nin("students", userId), null
        );


        OpenQuiz openQuiz = new OpenQuiz();
        for (Document doc : docs) {
            if (doc.containsKey("tags")) {
                List<String> t = doc.getList("tags", String.class);
                if (t.size() > 0) {
                    ArrayList<String> subTags;
                    if (tags.containsKey("المپیاد"))
                        subTags = tags.get("المپیاد");
                    else
                        subTags = new ArrayList<>();

                    for (String itr : t) {
                        if (!subTags.contains(itr))
                            subTags.add(itr);
                    }

                    tags.put("المپیاد", subTags);
                }
            }
            jsonArray.put(openQuiz.convertDocToJSON(
                    doc, true, false, false, true
            ).put("type", "quiz"));
        }
        data.put("tags", tags);

        if (!isAdmin && userId != null) {

            Document off = offcodeRepository.findOne(and(
                    exists("code", false),
                    eq("user_id", userId),
                    eq("used", false),
                    gt("expire_at", curr),
                    or(
                            eq("section", OffCodeSections.ALL.getName()),
                            eq("section", OffCodeSections.OPEN_EXAM.getName())
                    )
            ), null, Sorts.descending("amount"));

            if (off != null)
                data.put("off", new JSONObject()
                        .put("type", off.getString("type"))
                        .put("amount", off.getInteger("amount"))
                );
        }

        data.put("items", jsonArray);

        return data;
    }
}
