package irysc.gachesefid.Controllers.Advisor;


import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.adviseTagRepository;
import static irysc.gachesefid.Main.GachesefidApplication.lifeStyleTagRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal;

public class AdvisorController {

    public static String toggleStdAcceptance(Document user) {

        user.put("accept_std", !(boolean)user.getOrDefault("accept_std", true));
        userRepository.replaceOne(user.getObjectId("_id"), user);

        return JSON_OK;

    }

    public static String getAllAdvisors() {

        List<Document> advisors = userRepository.find(
                eq("accesses", Access.ADVISOR.getName()),
                ADVISOR_PUBLIC_DIGEST, Sorts.descending("rate")
        );

    }

    public static String rate(ObjectId userId, ObjectId advisorId, int rate) {

        Document advisor = userRepository.findById(advisorId);
        if(advisor == null || !advisor.containsKey("students"))
            return JSON_NOT_UNKNOWN;

        List<Document> students = advisor.getList("students", Document.class);

        Document stdDoc = searchInDocumentsKeyVal(
                students, "_id", userId
        );

        if(stdDoc == null)
            return JSON_NOT_UNKNOWN;

        int oldRate = (int)stdDoc.getOrDefault("rate", 0);
        stdDoc.put("rate", rate);
        stdDoc.put("rate_at", System.currentTimeMillis());

        double oldTotalRate = (double)advisor.getOrDefault("rate", (double)0);
        int rateCount = (int)advisor.getOrDefault("rate_count", 0);

        oldTotalRate *= rateCount;

        if(oldRate == 0)
            rateCount++;

        oldTotalRate -= oldRate;
        oldTotalRate += rate;

        advisor.put("rate", Math.round(oldTotalRate / rateCount * 100.0) / 100.0);
        advisor.put("rate_count", rateCount);

        userRepository.replaceOne(advisorId, advisor);
        return JSON_OK;

    }

    public static String createTag(Common db, JSONObject jsonObject) {

        if (db.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", jsonObject.getString("label"))
                )
        ))
            return generateErr("این تگ در سیستم موجود است");

        return db.insertOneWithReturn(
                new Document("label", jsonObject.getString("label"))
        );
    }

    public static String removeTags(Common db, JSONArray jsonArray) {

        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for(int i = 0; i < jsonArray.length(); i++) {

            try {
                String id = jsonArray.getString(i);

                if(!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = db.findOneAndUpdate(
                        new ObjectId(id),
                        set("deleted_at", System.currentTimeMillis())
                );

                if(tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }

        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String getAllTags(Common db) {

        ArrayList<Document> tags = db.find(
                exists("deleted_at", false), null
        );
        JSONArray jsonArray = new JSONArray();

        for (Document tag : tags)
            jsonArray.put(new JSONObject()
                    .put("id", tag.getObjectId("_id").toString())
                    .put("label", tag.getString("label"))
            );

        return Utility.generateSuccessMsg("data", jsonArray);
    }

}
