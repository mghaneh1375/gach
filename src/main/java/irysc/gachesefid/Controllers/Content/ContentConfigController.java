package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.contentConfigRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class ContentConfigController {

    public static String getFAQ(boolean isAdmin) {

        Document config = contentConfigRepository.findBySecKey("first");
        List<Document> items = config.getList("faq", Document.class);

        JSONArray jsonArray = new JSONArray();

        for(Document item : items) {
            if(!isAdmin && !item.getBoolean("visibility"))
                continue;

            jsonArray.put(irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(item, isAdmin));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String store(JSONObject data) {

        Document config = contentConfigRepository.findBySecKey("first");
        List<Document> items = config.getList("faq", Document.class);
        Document doc = new Document("_id", new ObjectId())
                .append("question", data.getString("question"))
                .append("priority", data.getInt("priority"))
                .append("visibility", data.getBoolean("visibility"))
                .append("answer", data.getString("answer"));

        items.add(doc);
        contentConfigRepository.replaceOne(config.getObjectId("_id"), config);

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(doc, true));
    }

    public static String update(ObjectId id, JSONObject data) {

        Document config = contentConfigRepository.findBySecKey("first");
        List<Document> items = config.getList("faq", Document.class);

        Document faq = Utility.searchInDocumentsKeyVal(items, "_id", id);
        if(faq == null)
            return JSON_NOT_VALID_ID;

        faq.put("question", data.getString("question"));
        faq.put("priority", data.getInt("priority"));
        faq.append("visibility", data.getBoolean("visibility"));
        faq.append("answer", data.getString("answer"));

        items.sort(Comparator.comparing(o -> o.getInteger("priority")));

        contentConfigRepository.replaceOne(config.getObjectId("_id"), config);

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(faq, true));
    }

    public static String remove(ObjectId id) {

        Document config = contentConfigRepository.findBySecKey("first");
        List<Document> items = config.getList("faq", Document.class);

        int idx = Utility.searchInDocumentsKeyValIdx(items, "_id", id);
        if(idx == -1)
            return JSON_NOT_VALID_ID;

        items.remove(idx);
        contentConfigRepository.replaceOne(config.getObjectId("_id"), config);

        return JSON_OK;
    }

}
