package irysc.gachesefid.Controllers.Config;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.regex;
import static irysc.gachesefid.Main.GachesefidApplication.authorRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class AuthorController {

    public static String addAuthor(JSONObject data) {

        Document newDoc = new Document();

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        return authorRepository.insertOneWithReturn(newDoc);
    }

    public static String editAuthor(ObjectId authorId, JSONObject data) {

        Document document = authorRepository.findById(authorId);
        if(document == null)
            return JSON_NOT_VALID_ID;

        for (String key : data.keySet()) {
            document.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        authorRepository.replaceOne(authorId, document);

        return JSON_OK;
    }

    public static String addAuthorTransaction(ObjectId authorId,
                                              JSONObject data) {

        Document author = authorRepository.findById(authorId);
        if (author == null)
            return JSON_NOT_VALID_ID;

        List<Document> transactions = author.containsKey("transactions") ?
                author.getList("transactions", Document.class) : new ArrayList<>();

        Document newDoc = new Document(
                "_id", new ObjectId()
        );

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        transactions.add(newDoc);
        author.put("transactions", transactions);

        authorRepository.replaceOne(authorId, author);
        return returnLastAuthorTransaction(transactions, new PairValue("id", newDoc.getObjectId("_id").toString()));
    }

    public static String returnLastAuthorTransaction(List<Document> transactions, PairValue additionalField) {

        long lastTransaction = 0;
        int sumPayment = 0;

        for (Document itr : transactions) {
            sumPayment += itr.getInteger("pay");
            if (lastTransaction < itr.getLong("pay_at"))
                lastTransaction = itr.getLong("pay_at");
        }

        if(additionalField == null)
            return generateSuccessMsg("sumPayment", sumPayment,
                    new PairValue("lastTransaction", lastTransaction == 0 ? "" :
                            Utility.getSolarDate(lastTransaction)
                    )
            );

        return generateSuccessMsg("sumPayment", sumPayment,
                new PairValue("lastTransaction", lastTransaction == 0 ? "" :
                        Utility.getSolarDate(lastTransaction)
                ), additionalField
        );
    }

    public static String getAuthorTransactions(ObjectId authorId) {

        Document author = authorRepository.findById(authorId);
        if (author == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();

        if (author.containsKey("transactions")) {
            List<Document> transactions = author.getList("transactions", Document.class);
            if (transactions.size() > 0) {
                for (Document itr : transactions) {
                    jsonArray.put(
                            new JSONObject()
                                    .put("id", itr.getObjectId("_id").toString())
                                    .put("pay", itr.getInteger("pay"))
                                    .put("payAt", Utility.getSolarDate(itr.getLong("pay_at")))
                                    .put("description", itr.getOrDefault("description", ""))
                    );
                }
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getAuthorsKeyVals() {

        ArrayList<Document> docs = authorRepository.find(null, new BasicDBObject("name", 1).append("_id", 1));
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("name", doc.getString("name")));
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getAuthors(String tag) {

        ArrayList<Bson> filter = new ArrayList<>();

        if (tag != null)
            filter.add(regex("tag", Pattern.compile(Pattern.quote(tag), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> docs = authorRepository.find(
                filter.size() == 0 ? null : and(filter),
                null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            long lastTransaction = 0;
            int sumPayment = 0;

            if (doc.containsKey("transactions")) {
                List<Document> transactions = doc.getList("transactions", Document.class);
                if (transactions.size() > 0) {
                    for (Document itr : transactions) {
                        sumPayment += itr.getInteger("pay");
                        if (lastTransaction < itr.getLong("pay_at"))
                            lastTransaction = itr.getLong("pay_at");
                    }
                }
            }

            JSONObject jsonObject = new JSONObject().
                    put("id", doc.getObjectId("_id").toString())
                    .put("name", doc.getString("name"))
                    .put("tag", doc.getString("tag"))
                    .put("sumPayment", sumPayment)
                    .put("lastTransaction", lastTransaction == 0 ? "" : Utility.getSolarDate(lastTransaction))
                    .put("questionCount", doc.getOrDefault("q_no", 0));

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
