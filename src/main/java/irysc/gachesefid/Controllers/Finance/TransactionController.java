package irysc.gachesefid.Controllers.Finance;

import com.mongodb.client.AggregateIterable;
import irysc.gachesefid.Controllers.Config.GiftController;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.transactionRepository;
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

    public static String fetchInvoice(ObjectId userId, String refId) {

        Document doc = transactionRepository.findOne(
                and(
                        eq("ref_id", refId),
                        eq("user_id", userId),
                        eq("status", "success")
                ), null
        );

        if(doc == null)
            return JSON_NOT_VALID_ID;

        JSONObject jsonObject = new JSONObject()
                .put("paid", doc.get("amount"))
                .put("createdAt", getSolarDate(doc.getLong("created_at")))
                .put("for", GiftController.translateUseFor(doc.getString("section")));

        if(doc.containsKey("ref_id"))
            jsonObject.put("refId", doc.get("sale_ref_id"));

        return generateSuccessMsg("data", jsonObject);
    }

    public static String getMyRecp(ObjectId userId,
                                   String section) {
        return "";
    }

}
