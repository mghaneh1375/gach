package irysc.gachesefid.Controllers.Finance;

import irysc.gachesefid.Models.SettledStatus;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class AdminSettlementController {

    public static String changeSettlementRequestStatus(
            ObjectId id, JSONObject data
    ) {

        Document doc = settlementRequestRepository.findById(id);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        if (doc.getString("status").equals("paid"))
            return generateErr("وضعیت این درخواست پرداخت شده است و امکان تغییر این وضعیت وجود ندارد");

        long curr = System.currentTimeMillis();

        doc.put("status", data.getString("status"));
        doc.put("answer_at", curr);

        if (data.has("desc"))
            doc.put("desc", data.getString("desc"));

        if (data.getString("status").equalsIgnoreCase("paid")) {
            doc.put("paid_at", curr);
            if (doc.getString("section").equals("class")) {
                teachScheduleRepository.updateMany(
                        and(
                                eq("user_id", doc.getObjectId("user_id")),
                                lte("start_at", doc.getLong("created_at")),
                                exists("settled_at", false)
                        ),
                        set("settled_at", curr)
                );
            }
        }

        settlementRequestRepository.replaceOne(
                id, doc
        );

        return JSON_OK;
    }

    public static String getSettledRequests(
            String status,
            Long createdFrom, Long createdTo,
            Long answerFrom, Long answerTo
    ) {
        List<Bson> filters = new ArrayList<>();
        if (status != null) {
            if (!EnumValidatorImp.isValid(status, SettledStatus.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("status", status));
        }

        if (createdFrom != null)
            filters.add(gte("created_at", createdFrom));

        if (createdTo != null)
            filters.add(lte("created_at", createdTo));

        if (answerFrom != null || answerTo != null)
            filters.add(exists("answer_at"));

        if (answerFrom != null)
            filters.add(gte("answer_at", answerFrom));

        if (answerTo != null)
            filters.add(lte("answer_at", answerTo));

        List<Document> requests = settlementRequestRepository.find(
                filters.size() == 0 ? null : and(filters),
                null
        );

        Set<ObjectId> userIds = new HashSet<>();
        for (Document request : requests)
            userIds.add(request.getObjectId("user_id"));

        List<Document> users = userRepository.findByIds(new ArrayList<>(userIds), false, JUST_NAME);
        if (users == null)
            return JSON_NOT_UNKNOWN;

        JSONArray jsonArray = new JSONArray();
        for (Document request : requests) {
            String username = users
                    .stream()
                    .filter(document -> document.getObjectId("_id").equals(request.getObjectId("user_id")))
                    .findFirst()
                    .map(u -> u.getString("first_name") + " " + u.getString("last_name"))
                    .get();

            jsonArray.put(new JSONObject()
                    .put("id", request.getObjectId("_id").toString())
                    .put("username", username)
                    .put("status", request.getString("status"))
                    .put("amount", request.get("amount"))
                    .put("section", request.getString("section"))
                    .put("desc", request.getOrDefault("desc", ""))
                    .put("createdAt", getSolarDate(request.getLong("created_at")))
                    .put("answerAt", request.containsKey("answer_at") ? getSolarDate(request.getLong("answer_at")) : "")
                    .put("paidAt", request.containsKey("paid_at") ? getSolarDate(request.getLong("paid_at")) : ""));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String createSettlementRequest(
            String section, ObjectId userId,
            Integer amount, ObjectId refId,
            String desc
    ) {

        if(!section.equalsIgnoreCase("class") &&
                !section.equalsIgnoreCase("advice")
        )
            return JSON_NOT_VALID_PARAMS;

        long curr = System.currentTimeMillis();
        Document newDoc = new Document("section", section)
                .append("user_id", userId)
                .append("ref_id", refId)
                .append("created_at", curr)
                .append("status", "paid")
                .append("amount", amount)
                .append("paid_at", curr)
                .append("answer_at", curr);

        if (desc != null)
            newDoc.put("desc", desc);

        settlementRequestRepository.insertOne(newDoc);

        if (section.equals("class")) {
            teachScheduleRepository.updateOne(
                    refId, set("settled_at", curr)
            );
        }
        else if(section.equals("advice")) {
            advisorRequestsRepository.updateOne(
                    refId, set("settled_at", curr)
            );
        }

        return JSON_OK;
    }
}
