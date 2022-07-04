package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class CommonController {

    public static String removeAll(Common db, JSONArray jsonArray,
                                   Bson additionalFilter) {

        JSONArray excepts = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            Document doc = additionalFilter == null ?
                    db.findOneAndDelete(
                            eq("_id", new ObjectId(id))
                    ) : db.findOneAndDelete(
                    and(
                            eq("_id", new ObjectId(id)),
                            additionalFilter
                    )
            );

            if (doc == null) {
                excepts.put(i + 1);
                continue;
            }

            db.cleanRemove(doc);
        }

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی موارد به درستی حذف گردیدند"
            );

        return generateSuccessMsg(
                "excepts",
                "بجز موارد زیر سایرین به درستی حذف گردیدند. " + excepts
        );
    }

    public static String removeAllFormDocList(Common db, JSONArray jsonArray,
                                              ObjectId docId, String listKey,
                                              Bson additionalFilter) {

        Document doc = additionalFilter == null ?
                db.findById(docId) : db.findOne(
                        and(
                                eq("_id", docId),
                                additionalFilter
                        ), new BasicDBObject(listKey, 1));

        if (doc == null) {

            if(additionalFilter == null)
                return JSON_NOT_VALID_ID;

            return JSON_NOT_ACCESS;
        }

        if(additionalFilter != null) {
            //todo : better
            doc = db.findById(docId);
        }

        List<Document> list = doc.getList(listKey, Document.class);
        int initListCount = list.size();

        JSONArray excepts = new JSONArray();
        JSONArray removedIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(id);

            int idx = Utility.searchInDocumentsKeyValIdx(
                    list, "_id", oId
            );

            if(idx == -1) {
                excepts.put(i + 1);
                continue;
            }

            list.remove(idx);
            removedIds.put(oId);
        }

        if(list.size() < initListCount)
            db.updateOne(docId, set(listKey, list));

        return Utility.returnRemoveResponse(excepts, removedIds);
    }

}
