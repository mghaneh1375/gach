package irysc.gachesefid.Controllers;

import irysc.gachesefid.DB.Common;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
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

}
