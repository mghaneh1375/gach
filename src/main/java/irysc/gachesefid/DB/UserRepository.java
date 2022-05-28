package irysc.gachesefid.DB;


import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.activationRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.printException;

public class UserRepository extends Common {

    public final static String FOLDER = "usersPic";

    public UserRepository() {
        init();
    }

    public static void deleteExpiredSMS() {
        activationRepository.deleteMany(lt("created_at", System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC));
    }

    public static PairValue existSMS(String phone) {

        Document doc = activationRepository.findOne(
                and(
                        eq("phone", phone),
                        gt("created_at", System.currentTimeMillis() - SMS_RESEND_MSEC)
                )
                , new BasicDBObject("token", 1).append("created_at", 1)
        );

        if (doc != null)
            return new PairValue(doc.getString("token"), SMS_RESEND_SEC - (System.currentTimeMillis() - doc.getLong("created_at")) / 1000);

        return null;
    }

    public static String sendNewSMSSignUp(String phone, String password, String introduced) {

        int code = Utility.randInt();
        String token = Utility.randomString(20);
        long now = System.currentTimeMillis();

        new Thread(() -> activationRepository.deleteMany(
                and(
                        eq("phone", phone),
                        lt("created_at", now)
                )
        )).start();

        activationRepository.insertOne(new Document("code", code)
                .append("created_at", now)
                .append("token", token)
                .append("phone", phone)
                .append("introduced", introduced)
                .append("password", password)
        );

        Utility.sendSMS(code, phone);
        return token;
    }

    public static String sendNewSMS(String phone, String via, String mail) {

        int code = Utility.randInt();
        String token = Utility.randomString(20);
        long now = System.currentTimeMillis();

        new Thread(() -> {

            activationRepository.deleteMany(
                    and(
                            eq("phone", phone),
                            lt("created_at", now)
                    )
            );

            activationRepository.insertOne(new Document("code", code)
                    .append("created_at", now)
                    .append("token", token)
                    .append("phone", phone)
                    .append("mail", mail)
            );

            if (via.equals("sms") || via.equals("both"))
                Utility.sendSMS(code, phone);
            if (via.equals("email") || via.equals("both"))
                Utility.sendMail(mail, code + "", "Forget password", "forget", null);

        }).start();

        return token;
    }

    public static JSONArray covertDocToJsonArr(ArrayList<Document> docs) {

        JSONArray users = new JSONArray();
        for (Document doc : docs) {
            JSONObject jsonObject = new JSONObject();
            for (String key : doc.keySet()) {

                if (
                        doc.get(key).getClass().getName().equals("java.util.ArrayList") ||
                                key.equals("password")
                )
                    continue;

                jsonObject.put(key, doc.get(key));
            }

            if (doc.containsKey("_id")) {
                jsonObject.put("id", doc.getObjectId("_id").toString());
                jsonObject.remove("_id");
            }

            if (doc.containsKey("pic"))
                jsonObject.put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + doc.getString("pic"));

            users.put(jsonObject);
        }

        return users;
    }

    public static JSONObject convertUser(Document user) {

        boolean hasEducationalHistory = false;
        boolean hasFinishedEducationalRequest = false;

        if (user.containsKey("passed") && user.getList("passed", Document.class).size() > 0)
            hasEducationalHistory = true;

        ObjectId userId = user.getObjectId("_id");

        return new JSONObject()
                .put("id", userId.toString())
                .put("pic", (user.containsKey("pic")) ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("nameFa", (user.containsKey("name_fa")) ? user.getString("name_fa") : "")
                .put("nameEn", (user.containsKey("name_en")) ? user.getString("name_en") : "")
                .put("lastNameFa", (user.containsKey("last_name_fa")) ? user.getString("last_name_fa") : "")
                .put("lastNameEn", (user.containsKey("last_name_en")) ? user.getString("last_name_en") : "")
                .put("mail", user.getString("mail"))
                .put("sex", user.getString("sex"))
                .put("hasEducationalHistory", hasEducationalHistory)
                .put("hasFinishedEducationalRequest", hasFinishedEducationalRequest)
                .put("username", user.getString("username"))
                .put("country", (user.containsKey("country")) ? user.getString("country") : "");

    }

    public static JSONObject convertUserDigestDocumentToJSON(Document user) {
        return new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("pic", user.containsKey("pic") ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("nameFa", user.getString("name_fa"))
                .put("nameEn", user.getString("name_en"))
                .put("lastNameFa", user.getString("last_name_fa"))
                .put("lastNameEn", user.getString("last_name_en"))
                .put("NIDOrPassport", (user.containsKey("NID")) ?
                        user.getString("NID") : (user.containsKey("passport_no")) ?
                        user.getString("passport_no") : "")
                .put("username", user.getString("username"));
    }

    public synchronized Document findByUnique(String unique, boolean searchInAll) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if (Utility.isValidMail(unique))
            constraints.add(and(
                    exists("NID"),
                    eq("mail", unique)
            ));

        if (Utility.isValidNum(unique)) {

            constraints.add(eq("username", unique));

            constraints.add(and(
                    exists("NID"),
                    eq("NID", unique)
            ));
        }

        if (searchInAll) {

            try {
                if (ObjectIdValidator.isValid(unique))
                    constraints.add(eq("_id", new ObjectId(unique)));

            } catch (Exception x) {
                printException(x);
            }
        }

        if (constraints.size() == 0)
            return null;

        FindIterable<Document> cursor = documentMongoCollection.find(or(constraints));

        if (cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public synchronized Document findByUsername(String username) {

        Document user = isInCache(table, username);
        if (user != null)
            return user;

        FindIterable<Document> cursor = documentMongoCollection.find(eq("username", username));
        if (cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();
            addToCache(table, doc, username, USER_LIMIT_CACHE_SIZE, USER_EXPIRATION_SEC);
            return doc;
        }

        return null;
    }

    @Override
    void init() {
        table = "user";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public int findAny(String phone, String mail) {

        FindIterable<Document> cursor = documentMongoCollection.find(or(
                eq("username", phone),
                eq("mail", mail)
        )).projection(new BasicDBObject("username", true));

        if (!cursor.iterator().hasNext())
            return 0;

        Document doc = cursor.iterator().next();
        if (doc.getString("username").equals(phone))
            return -4;

        return -2;
    }

    public int isExistByNID(String NID, ObjectId userId) {

        FindIterable<Document> cursor = documentMongoCollection.find(or(
                eq("NID", NID),
                eq("passport_no", NID)
        )).projection(new BasicDBObject("_id", true).append("NID", true).append("passport_no", true));

        for (Document doc : cursor) {

            if (doc.getObjectId("_id").equals(userId))
                continue;

            if (doc.containsKey("NID") && doc.getString("NID").equals(NID))
                return -4;

            return -2;
        }

        return 0;
    }

    public Document findByNID(String NID) {

        FindIterable<Document> cursor = documentMongoCollection.find(or(
                eq("NID", NID),
                eq("passport_no", NID)
        ));

        if (cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public ArrayList<Document> findByIds(ArrayList<ObjectId> objectIds) {

        List<Document> cursor = new ArrayList<>();

        for (ObjectId objectId : objectIds)
            cursor.add(userRepository.findById(objectId));

        ArrayList<Document> infos = new ArrayList<>(cursor);
        if (infos.size() != objectIds.size())
            return null;

        return infos;
    }

    public String searchByPhone(String phone) {

        JSONObject result = new JSONObject();
        FindIterable<Document> cursor = documentMongoCollection.find(eq("username", phone)).projection(USER_MANAGEMENT_INFO_DIGEST);

        ArrayList<Document> docs = new ArrayList<>();
        for (Document doc : cursor)
            docs.add(doc);

        if (docs.size() == 0)
            return result.put("status", "nok").toString();

        return result.put("status", "ok")
                .put("user", UserRepository.covertDocToJsonArr(docs).getJSONObject(0))
                .toString();
    }

    public void checkCache(Document newDoc) {
        removeFromCache(table, newDoc.getObjectId("_id"));
    }
}
