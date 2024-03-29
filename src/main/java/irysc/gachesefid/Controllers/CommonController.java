package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class CommonController {

    private static Document remove(String id, Bson additionalFilter, Common db) throws Exception {

        if (!ObjectId.isValid(id))
            throw new Exception();

        ObjectId oId = new ObjectId(id);

        Document doc = additionalFilter == null ?
                db.findOneAndDelete(
                        eq("_id", oId)
                ) : db.findOneAndDelete(
                and(
                        eq("_id", oId),
                        additionalFilter
                )
        );

        if (doc == null)
            throw new Exception();

        db.cleanRemove(doc);
        return doc;
    }

    public static String removeAll(Common db, JSONArray jsonArray,
                                   Bson additionalFilter) {

        JSONArray excepts = new JSONArray();
        JSONArray removedIds = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                Document doc = remove(jsonArray.getString(i), additionalFilter, db);
                removedIds.put(doc.getObjectId("_id").toString());
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }
        }

        return Utility.returnRemoveResponse(excepts, removedIds);
    }

    public static PairValue removeAllReturnDocs(Common db, JSONArray jsonArray,
                                                Bson additionalFilter) {

        JSONArray excepts = new JSONArray();
        JSONArray removedIds = new JSONArray();
        ArrayList<Document> deleted = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            try {
                Document doc = remove(jsonArray.getString(i), additionalFilter, db);
                removedIds.put(doc.getObjectId("_id").toString());
                deleted.add(doc);
            }
            catch (Exception x) {
                excepts.put(i + 1);
            }
        }

        return new PairValue(
                Utility.returnRemoveResponse(excepts, removedIds),
                deleted
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

            if (additionalFilter == null)
                return JSON_NOT_VALID_ID;

            return JSON_NOT_ACCESS;
        }

        if (additionalFilter != null) {
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

            if (idx == -1) {
                excepts.put(i + 1);
                continue;
            }

            list.remove(idx);
            removedIds.put(oId);
        }

        if (list.size() < initListCount)
            db.updateOne(docId, set(listKey, list));

        return Utility.returnRemoveResponse(excepts, removedIds);
    }

}
