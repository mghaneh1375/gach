package irysc.gachesefid.Controllers.Notif;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.NotifVia;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class NotifController {

    public static String getAll(String sendVia, Long from, Long to) {

        ArrayList<Bson> filter = new ArrayList<>();
        filter.add(eq("send_via", sendVia));
        ArrayList<Document> docs = notifRepository.find(and(filter), null, Sorts.descending("created_at"));

        JSONArray jsonArray = new JSONArray();
        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("usersCount", doc.getOrDefault("users_count", 0))
                    .put("title", doc.getString("title"))
                    .put("createdAt", getSolarDate(doc.getLong("created_at")))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String myNotifs(Document user) {

        if(!user.containsKey("events"))
            return generateSuccessMsg("data", new JSONArray());

        List<Document> notifs = user.getList("events", Document.class);
        JSONArray jsonArray = new JSONArray();

        for(int i = notifs.size() - 1; i >= 0; i--) {

            Document notif = notifs.get(i);

            Document n = notifRepository.findById(notif.getObjectId("notif_id"));
            if(n == null)
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("id", n.getObjectId("_id").toString())
                    .put("seen", notif.getBoolean("seen"))
                    .put("title", n.getString("title"))
                    .put("desc", n.getString("text"))
                    .put("createdAt", getSolarDate(n.getLong("created_at")));

            if(n.containsKey("attach") && !n.getString("attach").isEmpty())
                jsonObject.put("attach", STATICS_SERVER + "notifs/" + n.getString("attach"));

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);

    }

    private static Bson buildOrQuery(JSONArray jsonArray, String key) {

        ArrayList<Bson> orFilter = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String id = jsonArray.getString(i);
                if (!ObjectId.isValid(id))
                    continue;

                orFilter.add(eq(key, new ObjectId(id)));
            }
            catch (Exception ignore) {}
        }

        if(orFilter.size() > 0)
            return or(orFilter);

        return null;
    }

    private static void processFilter(ArrayList<Bson> filters, JSONObject filtersJSON,
                                      String jsonKey, String dbKey
    ) {
        try {
            Bson res = buildOrQuery(filtersJSON.getJSONArray(jsonKey), dbKey);
            if (res != null)
                filters.add(res);

        }
        catch (Exception ignore) {};
    }

    private static Bson fetchFilters(JSONObject filtersJSON) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("status", "active"));
        filters.add(exists("block_notif", false));

        if(filtersJSON.getString("via").equalsIgnoreCase(NotifVia.MAIL.toString()))
            filters.add(exists("mail", true));

        if(filtersJSON.getString("via").equalsIgnoreCase(NotifVia.SMS.toString()))
            filters.add(exists("phone", true));

        if(filtersJSON.has("cities"))
            processFilter(filters, filtersJSON, "cities", "city._id");

        if(filtersJSON.has("states"))
            processFilter(filters, filtersJSON, "states", "state._id");

        if(filtersJSON.has("grades"))
            processFilter(filters, filtersJSON, "grades", "grade._id");

        if(filtersJSON.has("branches"))
            processFilter(filters, filtersJSON, "branches", "branches._id");

        if(filtersJSON.has("schools"))
            processFilter(filters, filtersJSON, "schools", "school._id");

        if(filtersJSON.has("minCoin"))
            filters.add(gte("coin", filtersJSON.getNumber("minCoin").doubleValue()));

        if(filtersJSON.has("maxCoin"))
            filters.add(lte("coin", filtersJSON.getNumber("maxCoin").doubleValue()));

        if(filtersJSON.has("minMoney"))
            filters.add(gte("money", filtersJSON.getInt("minMoney")));

        if(filtersJSON.has("maxMoney"))
            filters.add(lte("money", filtersJSON.getInt("maxMoney")));

        if(filtersJSON.has("sex"))
            filters.add(eq("sex", filtersJSON.getString("sex")));

        if(filtersJSON.has("nids")) {

            JSONArray jsonArray = filtersJSON.getJSONArray("nids");
            ArrayList<String> validatedNids = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++) {

                String nid = Utility.convertPersianDigits(jsonArray.getString(i));

                if(!Utility.validationNationalCode(nid))
                    continue;

                validatedNids.add(nid);
            }

            filters.add(in("NID", validatedNids));
        }

        if(filtersJSON.has("phones")) {

            JSONArray jsonArray = filtersJSON.getJSONArray("phones");
            ArrayList<String> validatedPhones = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++) {

                String phone = Utility.convertPersianDigits(jsonArray.getString(i));

                if(!PhoneValidator.isValid(phone))
                    continue;

                validatedPhones.add(phone);
            }

            filters.add(in("phone", validatedPhones));
        }

        if(filtersJSON.has("accesses")) {

            JSONArray jsonArray = filtersJSON.getJSONArray("accesses");
            ArrayList<Bson> orFilter = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++) {

                if(!EnumValidatorImp.isValid(jsonArray.getString(i), Access.class))
                    continue;

                orFilter.add(in("accesses", jsonArray.getString(i)));
            }

            if(orFilter.size() > 0)
                filters.add(or(orFilter));
        }

        return and(filters);
    }

    private static ArrayList fetchIds(JSONArray jsonArray) {

        ArrayList<ObjectId> ids = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String id = jsonArray.getString(i);
                if (!ObjectId.isValid(id))
                    continue;

                ids.add(new ObjectId(id));
            }
            catch (Exception ignore) {}
        }

        return ids;
    }

    private static ArrayList<Document> postFilter(JSONObject filtersJSON, ArrayList<Document> users) {

        try {
            if (filtersJSON.has("quizzes")) {

                ArrayList<Document> quizzes = iryscQuizRepository.find(in("_id",
                                fetchIds(filtersJSON.getJSONArray("quizzes"))
                        ),
                        new BasicDBObject("_id", 1).append("students", 1)
                );

                ArrayList<Document> newFounded = new ArrayList<>();
                for (Document user : users) {

                    ObjectId userId = user.getObjectId("_id");

                    for (Document quiz : quizzes) {
                        if (Utility.searchInDocumentsKeyValIdx(
                                quiz.getList("students", Document.class),
                                "_id", userId
                        ) != -1) {
                            newFounded.add(user);
                            break;
                        }
                    }

                }

                users = newFounded;
            }
        }
        catch (Exception ignore) {}

        try {
            if (filtersJSON.has("packages")) {

                ArrayList<Document> contents = contentRepository.find(in("_id",
                                fetchIds(filtersJSON.getJSONArray("packages"))
                        ),
                        new BasicDBObject("_id", 1).append("users", 1)
                );

                ArrayList<Document> newFounded = new ArrayList<>();
                for (Document user : users) {

                    ObjectId userId = user.getObjectId("_id");

                    for (Document content : contents) {
                        if (Utility.searchInDocumentsKeyValIdx(
                                content.getList("users", Document.class),
                                "_id", userId
                        ) != -1) {
                            newFounded.add(user);
                            break;
                        }
                    }

                }

                users = newFounded;
            }
        }
        catch (Exception ignore) {}

        try {

            if (filtersJSON.has("minRank") || filtersJSON.has("maxRank") && !(
                    filtersJSON.has("minRank") && filtersJSON.has("maxRank")
            )) {

                ArrayList<ObjectId> userIds = new ArrayList<>();
                for(Document user : users)
                    userIds.add(user.getObjectId("_id"));

                int min = filtersJSON.has("minRank") ? filtersJSON.getInt("minRank") : -1;
                int max = filtersJSON.has("maxRank") ? filtersJSON.getInt("maxRank") : 1000000;

                ArrayList<Document> ranks = tarazRepository.find(in("user_id", userIds),
                        new BasicDBObject("_id", 1).append("user_id", 1)
                            .append("rank", 1)
                );

                ArrayList<Document> newFounded = new ArrayList<>();
                for (Document user : users) {

                    ObjectId userId = user.getObjectId("_id");
                    Document rank = Utility.searchInDocumentsKeyVal(
                            ranks, "user_id", userId
                    );

                    if(rank == null) {
                        if(min == -1)
                            newFounded.add(user);
                        continue;
                    }

                    boolean satisfyRank;

                    if(min != -1)
                        satisfyRank = rank.getInteger("rank") <= min;
                    else
                        satisfyRank = rank.getInteger("rank") >= max;

                    if(satisfyRank)
                        newFounded.add(user);
                }

                users = newFounded;
            }
        }
        catch (Exception ignore) {}

        return users;
    }

    public static String store(JSONObject filtersJSON, MultipartFile f) {

        BasicDBObject neededFields = new BasicDBObject("_id", 1);
        String sendVia = filtersJSON.getString("via");

        if(sendVia.equalsIgnoreCase(NotifVia.SITE.toString())) {
            neededFields.append("events", 1);

            if(filtersJSON.getBoolean("sendMail")) {
                neededFields.append("mail", 1);
                neededFields.append("first_name", 1);
                neededFields.append("last_name", 1);
            }

            if(filtersJSON.getBoolean("sendSMS"))
                neededFields.append("phone", 1);
        }

        else if(sendVia.equalsIgnoreCase(NotifVia.MAIL.toString())) {
            neededFields.append("mail", 1);
            neededFields.append("first_name", 1);
            neededFields.append("last_name", 1);
        }

        else if(sendVia.equalsIgnoreCase(NotifVia.SMS.toString()))
            neededFields.append("phone", 1);


        ArrayList<Document> users = userRepository.find(and(fetchFilters(filtersJSON)),
                neededFields
        );

        if(users.size() == 0)
            return generateErr("کاربری با فیلترهای اعمال شده پیدا نشد");

        users = postFilter(filtersJSON, users);

        if(users.size() == 0)
            return generateErr("کاربری با فیلترهای اعمال شده پیدا نشد");

        ObjectId notifId = new ObjectId();
        long curr = System.currentTimeMillis();

        Document notif = new Document("_id", notifId)
                .append("users_count", users.size())
                .append("title", filtersJSON.getString("title"))
                .append("text", filtersJSON.getString("text"))
                .append("send_via", sendVia)
                .append("created_at", curr);

        ArrayList<ObjectId> userIds = new ArrayList<>();

        List<WriteModel<Document>> writes = new ArrayList<>();
        List<WriteModel<Document>> mailWrites = new ArrayList<>();
        List<WriteModel<Document>> smsWrites = new ArrayList<>();

        String attach = null;

        if(users.size() > 0 && f != null)
            attach = FileUtils.uploadFile(f, "notifs");

        for (Document user : users) {
            userIds.add(user.getObjectId("_id"));
            if(sendVia.equalsIgnoreCase(NotifVia.SITE.toString())) {
                List<Document> events = user.getList("events", Document.class);
                if(attach != null)
                    events.add(
                            new Document("created_at", curr)
                                    .append("notif_id", notifId)
                                    .append("attach", attach)
                                    .append("seen", false)
                    );
                else
                    events.add(
                            new Document("created_at", curr)
                                    .append("notif_id", notifId)
                                    .append("seen", false)
                    );
                writes.add(new UpdateOneModel<>(
                        eq("_id", user.getObjectId("_id")),
                        new BasicDBObject("$set",
                                new BasicDBObject("events", events)
                        )
                ));
            }

            if((sendVia.equalsIgnoreCase(NotifVia.MAIL.toString()) ||
                    filtersJSON.getBoolean("sendMail")) && user.containsKey("mail")
            ) {

                Document d = new Document("mode", "notif")
                        .append("status", "pending")
                        .append("notif_id", notifId.toString())
                        .append("mail", user.getString("mail"))
                        .append("title", filtersJSON.getString("title"))
                        .append("msg", sendVia.equalsIgnoreCase(NotifVia.MAIL.toString()) ?
                                filtersJSON.getString("text") : "پیام جدیدی در آیریسک داری" + "<br/>https://e.irysc.com")
                        .append("created_at", curr)
                        .append("name", user.getString("first_name") + " " + user.getString("last_name"));

                if(attach != null)
                    d.append("attach", attach);

                mailWrites.add(new InsertOneModel<>(d));
            }

            if((sendVia.equalsIgnoreCase(NotifVia.SMS.toString()) ||
                    filtersJSON.getBoolean("sendSMS")) && user.containsKey("phone")
            ) {
                smsWrites.add(new InsertOneModel<>(
                        new Document("mode", "notif")
                                .append("status", "pending")
                                .append("notif_id", notifId)
                                .append("phone", user.getString("phone"))
                                .append("msg", sendVia.equalsIgnoreCase(NotifVia.SMS.toString()) ?
                                        Jsoup.parse(filtersJSON.getString("text")).text() : "پیام جدیدی در آیریسک داری" + "\n" + "https://e.irysc.com")
                                .append("created_at", curr)
                ));
            }
        }

        notif.append("users", userIds);
        if(attach != null)
            notif.append("attach", attach);

        notifRepository.insertOne(notif);

        if(mailWrites.size() > 0)
            mailQueueRepository.bulkWrite(mailWrites);

        if(smsWrites.size() > 0)
            smsQueueRepository.bulkWrite(smsWrites);

        if(writes.size() > 0) {
            new Thread(() -> {
                userRepository.bulkWrite(writes);
                for (ObjectId userId : userIds)
                    userRepository.clearFromCache(userId);
            }).start();

        }

        return generateSuccessMsg("id", notifId.toString(),
                new PairValue("usersCount", userIds.size()),
                new PairValue("createdAt",
                        Utility.getSolarDate(System.currentTimeMillis())
                )
        );
    }

    public static String remove(JSONArray jsonArray) {

        JSONArray done = new JSONArray();
        JSONArray excepts = new JSONArray();

        ArrayList<ObjectId> removed = new ArrayList<>();
        ArrayList<Object> userIds = new ArrayList<>();

        for(int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if(!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(id);

            Document notif = notifRepository.findOneAndDelete(eq("_id", oId));
            if(notif == null) {
                excepts.put(i + 1);
                continue;
            }

            userIds.addAll(notif.getList("users", ObjectId.class));
            removed.add(oId);
            done.put(oId.toString());
        }

        if(removed.size() > 0) {
            for(ObjectId objectId : removed) {
                Bson update = Updates.pull("events", eq("notif_id", objectId));
                userRepository.updateMany(exists("events.0"), update);
            }

            mailQueueRepository.deleteMany(and(
                    eq("mode", "notif"),
                    exists("notif_id"),
                    in("notif_id", removed)
            ));

            smsQueueRepository.deleteMany(and(
                    eq("mode", "notif"),
                    exists("notif_id"),
                    in("notif_id", removed)
            ));

            userRepository.clearBatchFromCache(userIds);
        }

        return returnRemoveResponse(excepts, done);
    }

    public static String getNotif(ObjectId id, Document user) {

        Document notif = notifRepository.findById(id);
        if(notif == null)
            return JSON_NOT_VALID_ID;

        boolean oldSeen = false;

        if(user != null) {
            ObjectId userId = user.getObjectId("_id");

            if (!notif.getList("users", ObjectId.class).contains(userId))
                return JSON_NOT_ACCESS;

            List<Document> events = user.getList("events", Document.class);

            Document n = Utility.searchInDocumentsKeyVal(
                    events, "notif_id", id
            );

            if(n != null)
                oldSeen = n.getBoolean("seen");

            if (n != null && !n.getBoolean("seen")) {
                n.put("seen", true);
                userRepository.replaceOne(userId, user);
            }
        }

        JSONObject jsonObject = new JSONObject()
                .put("title", notif.getString("title"))
                .put("desc", notif.getString("text"))
                .put("oldSeen", oldSeen)
                .put("createdAt", Utility.getSolarDate(notif.getLong("created_at")));

        if(notif.containsKey("attach") && !notif.getString("attach").isEmpty())
            jsonObject.put("attach", STATICS_SERVER + "notifs/" + notif.getString("attach"));

        return generateSuccessMsg("data", jsonObject);
    }

    public static String setSeen(ObjectId id, Document user) {

        List<Document> events = user.getList("events", Document.class);

        Document n = Utility.searchInDocumentsKeyVal(
                events, "notif_id", id
        );

        if(n != null && !n.getBoolean("seen")) {
            n.put("seen", true);
            userRepository.replaceOne(user.getObjectId("_id"), user);
        }

        return JSON_OK;
    }

    public static String getStudents(ObjectId id) {

        Document doc = notifRepository.findById(id);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> users = userRepository.findByIds(
                doc.getList("users", ObjectId.class), false
        );

        if(users == null)
            return JSON_NOT_UNKNOWN;

        for(Document user : users) {
            JSONObject jsonObject = new JSONObject();
            Utility.fillJSONWithUser(jsonObject, user);
            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
