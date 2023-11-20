package irysc.gachesefid.Controllers.Finance;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class TransactionController {

    public static String get(ObjectId userId,
                             Long start, Long end,
                             Boolean useOffCode, String section) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("status", "success"));

        if (userId != null)
            filters.add(eq("user_id", userId));

        if (section != null)
            filters.add(eq("section", section));

        if (useOffCode != null)
            filters.add(exists("off_code", useOffCode));

        if (start != null)
            filters.add(gte("created_at", start));

        if (end != null)
            filters.add(lte("created_at", end));

        AggregateIterable<Document> docs = transactionRepository.all(
                match(and(filters))
        );

        JSONArray data = new JSONArray();
        double sum = 0;
        double accountMoneySum = 0;

        for (Document doc : docs) {

            if (!doc.containsKey("user") || doc.get("user") == null)
                continue;

            Document user = doc.get("user", Document.class);

            JSONObject jsonObject = new JSONObject()
                    .put("createdAt", Utility.getSolarDate(doc.getLong("created_at")))
                    .put("createdAtTs", doc.getLong("created_at"))
                    .put("refId", doc.get("ref_id"))
                    .put("useOff", doc.containsKey("off_code"))
                    .put("section", GiftController.translateUseFor(doc.getString("section")))
                    .put("amount", doc.get("amount"))
                    .put("accountMoney", doc.getOrDefault("account_money", 0))
                    .put("user", user.getString("first_name") + " " + user.getString("last_name"))
                    .put("userNID", user.getString("NID"))
                    .put("userPhone", user.getString("phone"));

            sum += jsonObject.getNumber("amount").doubleValue();
            accountMoneySum += jsonObject.getNumber("accountMoney").doubleValue();

            data.put(jsonObject);
        }

        return generateSuccessMsg("data", data,
                new PairValue("sum", sum),
                new PairValue("accountMoneySum", accountMoneySum)
        );
    }

    public static void fetchSchoolQuizInvoice(StringBuilder section,
                                              Document transaction) {

        Object products = transaction.get("products");

        Document quiz = schoolQuizRepository.findById((ObjectId) products);
        if (quiz != null) {
            section.append(" - ").append(quiz.getString("title")).append(" ( تعداد دانش آموزان: ");
            section.append(quiz.getList("students", Document.class).size()).append(" - تعداد سوالات: ");
            section.append(
                    quiz.getBoolean("pdf_quiz", false) ?
                            quiz.getInteger("q_no") :
                            quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size()
            );

            if ((boolean) quiz.getOrDefault("database", true))
                section.append(" - استفاده شده از مجموعه سوالات آیریسک ");
            else
                section.append(" - استفاده شده از مجموعه سوالات مدرسه ");

            section.append(" )");
        }

    }

    public static void fetchSchoolHWInvoice(StringBuilder section,
                                            Document transaction) {

        Object products = transaction.get("products");

        Document quiz = hwRepository.findById((ObjectId) products);
        if (quiz != null) {
            section.append(" - ").append(quiz.getString("title")).append(" ( تعداد دانش آموزان: ");
            section.append(quiz.getList("students", Document.class).size());
            section.append(" )");
        }

    }

    private static void fetchAbstractQuizName(ObjectId quizId, StringBuilder section) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz != null)
            section.append(" - ").append(quiz.getString("title"));
        else {
            quiz = openQuizRepository.findById(quizId);
            if (quiz != null)
                section.append(" - ").append(quiz.getString("title"));
            else {
                quiz = schoolQuizRepository.findById(quizId);
                if (quiz != null)
                    section.append(" - ").append(quiz.getString("title"));
                else {
                    quiz = onlineStandQuizRepository.findById(quizId);
                    if (quiz != null)
                        section.append(" - ").append(quiz.getString("title"));
                    else {
                        quiz = escapeQuizRepository.findById(quizId);
                        if (quiz != null)
                            section.append(" - ").append(quiz.getString("title"));
                    }
                }
            }
        }
    }

    public static void fetchQuizInvoice(StringBuilder section,
                                        Document transaction) {
        boolean checkAllItems = true;

        if (transaction.containsKey("package_id")) {
            Document wantedPackage = packageRepository.findById(transaction.getObjectId("package_id"));
            if (wantedPackage != null) {
                section.append(" - ").append("بسته آزمونی ").append(wantedPackage.getString("title"));
                checkAllItems = false;
            }
        }

        if (checkAllItems) {
            Object products = transaction.get("products");
            if (products instanceof ObjectId) {
                fetchAbstractQuizName((ObjectId) products, section);
            } else if (products instanceof List) {
                for (ObjectId quizId : (List<ObjectId>) products) {
                    fetchAbstractQuizName(quizId, section);
                }
            }
        }
    }

    static String getTransactionTitle(Document transaction) {

        StringBuilder section = new StringBuilder(GiftController.translateUseFor(
                transaction.getString("section")
        ));

        if (transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.GACH_EXAM.getName()
        ))
            fetchQuizInvoice(section, transaction);

        else if (transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.SCHOOL_QUIZ.getName()
        ) || transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.COUNSELING_QUIZ.getName()
        ))
            fetchSchoolQuizInvoice(section, transaction);

        else if (transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.SCHOOL_HW.getName()
        ))
            fetchSchoolHWInvoice(section, transaction);

        else if (transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.BANK_EXAM.getName()
        )) {
            Document quiz = customQuizRepository.findById(transaction.getObjectId("products"));

            if (quiz != null)
                section.append(" - ").append("خرید ").append(
                        quiz.getList("questions", ObjectId.class).size()
                ).append(" سوال ");
        } else if (transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.CONTENT.getName()
        )) {
            Document content = contentRepository.findById(transaction.getObjectId("products"));

            if (content != null)
                section.append(" - ").append(content.getString("title"));
        }

        return section.toString();
    }

    public static String fetchInvoice(ObjectId userId, ObjectId refId) {

        Document transaction = transactionRepository.findOne(
                and(
                        eq("_id", refId),
                        eq("user_id", userId),
                        eq("status", "success")
                ), null
        );

        if (transaction == null)
            return JSON_NOT_VALID_ID;

        return generateSuccessMsg("data",
                new JSONObject()
                        .put("for", getTransactionTitle(transaction))
                        .put("account", transaction.getOrDefault("account_money", 0))
                        .put("offAmount", transaction.getOrDefault("off_amount", 0))
                        .put("paid", transaction.getInteger("amount"))
                        .put("refId", transaction.getOrDefault("ref_id", ""))
                        .put("createdAt", getSolarDate(transaction.getLong("created_at")))
        );
    }

}
