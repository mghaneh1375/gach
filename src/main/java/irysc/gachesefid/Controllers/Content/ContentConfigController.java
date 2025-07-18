package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.mongodb.client.model.Filters.exists;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.FileUtils.uploadMultimediaFile;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class ContentConfigController {

    public static String getAdv() {

        Document doc = contentConfigRepository.findBySecKey("first");
        if(doc == null)
            return JSON_NOT_UNKNOWN;

        if(!doc.containsKey("advs"))
            return generateSuccessMsg("data", new JSONArray());

        List<Document> advs = doc.getList("advs", Document.class);
        JSONArray jsonArray = new JSONArray();

        for(Document adv : advs) {
            jsonArray.put(new JSONObject()
                    .put("id", adv.getObjectId("_id").toString())
                    .put("visibility", adv.getBoolean("visibility"))
                    .put("title", adv.getString("title"))
                    .put("file", STATICS_SERVER + ContentRepository.FOLDER + "/" + adv.getString("file"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String removeAdv(ObjectId id) {

        Document doc = contentConfigRepository.findBySecKey("first");
        if(doc == null)
            return JSON_NOT_UNKNOWN;

        if(!doc.containsKey("advs"))
            return JSON_NOT_VALID_ID;

        List<Document> advs = doc.getList("advs", Document.class);

        int idx = Utility.searchInDocumentsKeyValIdx(advs, "_id", id);

        if(idx == -1)
            return JSON_NOT_VALID_ID;

        FileUtils.removeFile(advs.get(idx).getString("file"), ContentRepository.FOLDER);
        advs.remove(idx);
        contentConfigRepository.replaceOne(doc.getObjectId("_id"), doc);

        return JSON_OK;
    }

    public static String storeAdv(MultipartFile file, JSONObject jsonObject) {

        if (file.getSize() > MAX_ADV_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز، " + MAX_ADV_FILE_SIZE + " مگ است.");

        String fileType = uploadMultimediaFile(file);

        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        Document doc = contentConfigRepository.findBySecKey("first");
        if(doc == null)
            return JSON_NOT_UNKNOWN;

        String filename = FileUtils.uploadFile(file, ContentRepository.FOLDER);

        if (filename == null)
            return JSON_NOT_VALID_FILE;

        List<Document> advs = doc.containsKey("advs") ?
                doc.getList("advs", Document.class) :
                new ArrayList<>()
        ;

        ObjectId id = new ObjectId();

        advs.add(new Document("file", filename)
                .append("_id", id)
                .append("visibility", jsonObject.getBoolean("visibility"))
                .append("title", jsonObject.getString("title"))
        );

        doc.put("advs", advs);
        contentConfigRepository.replaceOne(doc.getObjectId("_id"), doc);

        return generateSuccessMsg("data", new JSONObject()
                .put("filename", filename)
                .put("id", id.toString())
        );
    }

    public static String updateAdv(ObjectId id, JSONObject jsonObject) {

        Document doc = contentConfigRepository.findBySecKey("first");
        if(doc == null || !doc.containsKey("advs"))
            return JSON_NOT_UNKNOWN;

        List<Document> advs = doc.getList("advs", Document.class);
        Document ad = Utility.searchInDocumentsKeyVal(advs, "_id", id);
        if(ad == null)
            return JSON_NOT_VALID_ID;

        ad.put("visibility", jsonObject.getBoolean("visibility"));
        ad.put("title", jsonObject.getString("title"));

        contentConfigRepository.replaceOne(doc.getObjectId("_id"), doc);
        return JSON_OK;
    }

    private static Document getSeoDoc(ObjectId packageId) throws InvalidFieldsException {

        Document seo;

        if (packageId != null)
            seo = seoRepository.findBySecKey(packageId);
        else
            seo = seoRepository.findOne(exists("package_id", false), null);

        if (seo == null)
            throw new InvalidFieldsException("id is not valid");

        return seo;
    }

    public static String getSeo(ObjectId packageId) {

        try {
            Document seo = getSeoDoc(packageId);

            JSONArray jsonArray = new JSONArray();

            for (String key : seo.keySet()) {

                if(
                        key.equalsIgnoreCase("_id") ||
                            key.equalsIgnoreCase("package_id")
                )
                    continue;

                JSONObject jsonObject = new JSONObject()
                        .put("key", key)
                        .put("value", seo.get(key));

                jsonArray.put(jsonObject);
            }

            return generateSuccessMsg("data", jsonArray,
                    new PairValue("id", seo.getObjectId("_id").toString())
            );

        } catch (InvalidFieldsException e) {

            if (packageId == null ||
                    contentRepository.findById(packageId) != null
            ) {

                ObjectId id = packageId != null ?
                        seoRepository.insertOneWithReturnId(
                                new Document("package_id", packageId)
                        ) :
                        seoRepository.insertOneWithReturnId(
                                new Document()
                        );

                return generateSuccessMsg("data", new JSONArray(),
                        new PairValue("id", id.toString())
                );
            }

            return JSON_NOT_VALID_ID;
        }
    }

    public static String storeSeo(ObjectId packageId, JSONObject data) {
        try {
            Document seo = getSeoDoc(packageId);
            seo.put(data.getString("key"), data.get("value"));
            seoRepository.replaceOne(seo.getObjectId("_id"), seo);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return JSON_NOT_VALID_ID;
        }
    }

    public static String removeSeo(ObjectId id, String key) {
        Document seo = seoRepository.findById(id);
        seo.remove(key);
        seoRepository.replaceOne(seo.getObjectId("_id"), seo);
        return JSON_OK;
    }


    public static String getFAQ(boolean isAdmin, ObjectId contentId) {
        Document config = contentId == null
                ? contentConfigRepository.findBySecKey("first")
                : contentRepository.findById(contentId);
        List<Document> items = config.containsKey("faq")
                ? config.getList("faq", Document.class)
                : new ArrayList<>();

        List<JSONObject> jsonObjects = new ArrayList<>();
        for (Document item : items) {
            if (!isAdmin && !item.getBoolean("visibility"))
                continue;

            jsonObjects.add(irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(item, isAdmin));
        }

        jsonObjects.sort((o1, o2) -> {
            Integer age1 = o1.getInt("priority");
            Integer age2 = o2.getInt("priority");
            return age1.compareTo(age2);
        });
        return generateSuccessMsg("data", jsonObjects);
    }

    public static String store(ObjectId contentId, JSONObject data) {
        Document config = contentId == null
                ? contentConfigRepository.findBySecKey("first")
                : contentRepository.findById(contentId);

        List<Document> items = config.containsKey("faq")
                ? config.getList("faq", Document.class)
                : new ArrayList<>();
        Document doc = new Document("_id", new ObjectId())
                .append("question", data.getString("question"))
                .append("priority", data.getInt("priority"))
                .append("visibility", data.getBoolean("visibility"))
                .append("answer", data.getString("answer"));

        items.add(doc);
        if(!config.containsKey("faq"))
            config.put("faq", items);

        if(contentId == null)
            contentConfigRepository.replaceOneWithoutClearCache(config.getObjectId("_id"), config);
        else
            contentRepository.replaceOneWithoutClearCache(contentId, config);

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(doc, true));
    }

    public static String update(ObjectId id, JSONObject data, ObjectId contentId) {

        Document config = contentId == null
                ? contentConfigRepository.findBySecKey("first")
                : contentRepository.findById(contentId);
        if(!config.containsKey("faq"))
            return JSON_NOT_VALID_ID;

        List<Document> items = config.getList("faq", Document.class);
        Document faq = Utility.searchInDocumentsKeyVal(items, "_id", id);
        if (faq == null)
            return JSON_NOT_VALID_ID;

        faq.put("question", data.getString("question"));
        faq.put("priority", data.getInt("priority"));
        faq.append("visibility", data.getBoolean("visibility"));
        faq.append("answer", data.getString("answer"));

        items.sort(Comparator.comparing(o -> o.getInteger("priority")));

        if(contentId == null)
            contentConfigRepository.replaceOneWithoutClearCache(config.getObjectId("_id"), config);
        else
            contentRepository.replaceOneWithoutClearCache(contentId, config);

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convertFAQDigest(faq, true));
    }

    public static String remove(ObjectId id, ObjectId contentId) {
        Document config = contentId == null
                ? contentConfigRepository.findBySecKey("first")
                : contentRepository.findById(contentId);

        if(!config.containsKey("faq"))
            return JSON_NOT_ACCESS;

        List<Document> items = config.getList("faq", Document.class);
        int idx = Utility.searchInDocumentsKeyValIdx(items, "_id", id);
        if (idx == -1)
            return JSON_NOT_VALID_ID;

        items.remove(idx);
        if(contentId == null)
            contentConfigRepository.replaceOneWithoutClearCache(config.getObjectId("_id"), config);
        else
            contentRepository.replaceOneWithoutClearCache(contentId, config);

        return JSON_OK;
    }

}
