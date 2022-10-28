package irysc.gachesefid.Controllers.Finance;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.parameters.P;

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

        if(userId != null)
            filters.add(eq("user_id", userId));

        if(section != null)
            filters.add(eq("section", section));

        if(useOffCode != null)
            filters.add(exists("off_code", useOffCode));

        if(start != null)
            filters.add(gte("created_at", start));

        if(end != null)
            filters.add(lte("created_at", end));

        AggregateIterable<Document> docs = transactionRepository.all(
                match(and(filters))
        );

        JSONArray data = new JSONArray();
        for(Document doc : docs) {

            if(!doc.containsKey("user") || doc.get("user") == null)
                continue;

            Document user = doc.get("user", Document.class);

            JSONObject jsonObject = new JSONObject()
                    .put("createdAt", Utility.getSolarDate(doc.getLong("created_at")))
                    .put("createdAtTs", doc.getLong("created_at"))
                    .put("refId", doc.get("ref_id"))
                    .put("useOff", doc.containsKey("off_code"))
                    .put("section", GiftController.translateUseFor(doc.getString("section")))
                    .put("amount", doc.get("amount"))
                    .put("user", user.getString("first_name") + " " + user.getString("last_name"))
                    .put("userNID", user.getString("NID"))
                    .put("userPhone", user.getString("phone"));

            data.put(jsonObject);
        }

        return generateSuccessMsg("data", data);
    }

    public static void fetchQuizInvoice(StringBuilder section,
                                          Document transaction) {
        boolean checkAllItems = true;

        if(transaction.containsKey("package_id")) {
            Document wantedPackage = packageRepository.findById(transaction.getObjectId("package_id"));
            if(wantedPackage != null) {
                section.append(" - ").append("بسته آزمونی ").append(wantedPackage.getString("title"));
                checkAllItems = false;
            }
        }

        if(checkAllItems) {
            Object products = transaction.get("products");
            if (products instanceof ObjectId) {
                Document quiz = iryscQuizRepository.findById((ObjectId) products);
                if (quiz != null)
                    section.append(" - ").append(quiz.getString("title"));
                else {
                    quiz = openQuizRepository.findById((ObjectId) products);
                    if(quiz != null)
                        section.append(" - ").append(quiz.getString("title"));
                }
            } else if (products instanceof List) {
                for (ObjectId quizId : (List<ObjectId>) products) {

                    Document quiz = iryscQuizRepository.findById(quizId);
                    if (quiz != null)
                        section.append(" - ").append(quiz.getString("title"));
                    else {
                        quiz = openQuizRepository.findById(quizId);
                        if (quiz != null)
                            section.append(" - ").append(quiz.getString("title"));
                    }
                }
            }
        }
    }

    static String getTransactionTitle(Document transaction) {

        StringBuilder section = new StringBuilder(GiftController.translateUseFor(
                transaction.getString("section")
        ));

        if(transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.GACH_EXAM.getName()
        ))
            fetchQuizInvoice(section, transaction);

        else if(transaction.getString("section").equalsIgnoreCase(
                OffCodeSections.BANK_EXAM.getName()
        )) {
            Document quiz = customQuizRepository.findById(transaction.getObjectId("products"));

            if(quiz != null)
                section.append(" - ").append("خرید ").append(
                        quiz.getList("questions", ObjectId.class).size()
                ).append(" سوال ");
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

        if(transaction == null)
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
