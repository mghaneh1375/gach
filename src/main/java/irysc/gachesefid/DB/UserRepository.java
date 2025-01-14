package irysc.gachesefid.DB;


import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Variable;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.activationRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.printException;

public class UserRepository extends Common {

    public final static String FOLDER = "usersPic";

    public UserRepository() {
        init();
    }

    public static PairValue existSMS(String username) {

        Document doc = activationRepository.findOne(
                and(
                        eq("username", username),
                        gt("created_at", System.currentTimeMillis() - SMS_RESEND_MSEC)
                )
                , new BasicDBObject("token", 1).append("created_at", 1)
        );

        if (doc != null)
            return new PairValue(doc.getString("token"), SMS_RESEND_SEC - (System.currentTimeMillis() - doc.getLong("created_at")) / 1000);

        return null;
    }

    public static String sendNewSMSSignUp(String username, String password,
                                          String firstName, String lastName,
                                          String NID, String authVia) {

        int code = Utility.randInt();
        String token = Utility.randomString(20);
        long now = System.currentTimeMillis();

        new Thread(() -> activationRepository.deleteMany(
                and(
                        eq("username", username),
                        lt("created_at", now)
                )
        )).start();

        activationRepository.insertOne(new Document("code", code)
                .append("created_at", now)
                .append("token", token)
                .append("username", username)
                .append("first_name", firstName)
                .append("last_name", lastName)
                .append("NID", NID)
                .append("password", password)
                .append("auth_via", authVia)
        );

        if (authVia.equals(AuthVia.SMS.getName()))
            Utility.sendSMS(username, code + "", "", "", "activationCode");
        else
            Utility.sendMail(
                    username, code + "", "signUp",
                    firstName + " " + lastName
            );

        return token;
    }

    public static String sendNewSMS(
            String NID, String phoneOrMail,
            String via, boolean savePhoneOrMail
    ) {

        int code = Utility.randInt();
        String token = Utility.randomString(20);
        long now = System.currentTimeMillis();

        new Thread(() -> {

            new Thread(() -> activationRepository.deleteMany(
                    and(
                            eq("username", NID),
                            lt("created_at", now)
                    )
            )).start();

            Document newDoc = new Document("code", code)
                    .append("created_at", now)
                    .append("token", token)
                    .append("auth_via", via)
                    .append("username", NID);

            if (savePhoneOrMail)
                newDoc.append("phone_or_mail", phoneOrMail);

            activationRepository.insertOne(newDoc);

            if (via.equals(AuthVia.SMS.getName()))
                Utility.sendSMS(phoneOrMail, code + "", "", "", "activationCode");
            else
                Utility.sendMail(phoneOrMail, code + "", "forget", null);

        }).start();

        return token;
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
                    exists("mail"),
                    eq("mail", unique)
            ));

        if (Utility.isValidNum(unique)) {

            constraints.add(eq("phone", unique));

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

        FindIterable<Document> cursor = documentMongoCollection.find(
                or(
                        eq("phone", username),
                        eq("mail", username)

                )
        );
        if (cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();
            return findById(doc.getObjectId("_id"));
        }

        return null;
    }

    public List<Document> findUsersWithSettlementStatus(
            Bson match, Bson sortFilter,
            Integer skip, Integer limit,
            Boolean justSettled
    ) {
        List<Bson> filters = new ArrayList<>() {{
            add(match(match));
            add(lookup("teach_schedule",
                    Collections.singletonList(new Variable<>("myId", "$_id")), Arrays.asList(
                            match(
                                    and(
                                            expr(
                                                    new Document("$eq", Arrays.asList("$user_id", "$$myId"))
                                            ),
                                            exists("settled_at", false),
                                            exists("students.0")
                                    )),
                            project(JUST_ID)
                    ), "teachSettlements"));
            add(lookup("advisor_requests",
                    Collections.singletonList(new Variable<>("myId", "$_id")), Arrays.asList(
                            match(and(
                                    expr(
                                            new Document("$eq", Arrays.asList("$advisor_id", "$$myId"))
                                    ),
                                    exists("settled_at", false),
                                    exists("paid_at")
                            )),
                            project(JUST_ID)
                    ), "adviceSettlements"));
            add(
                    project(new BasicDBObject(USER_MANAGEMENT_INFO_DIGEST)
                            .append("teachSettlementsCount", new BasicDBObject("$size", "$teachSettlements"))
                            .append("adviceSettlementsCount", new BasicDBObject("$size", "$adviceSettlements"))
                    )
            );
            add(addFields(new Field<>(
                    "settlementsCount", new BasicDBObject("$add", Arrays.asList("$teachSettlementsCount", "$adviceSettlementsCount")))
            ));
        }};
        if(justSettled != null && justSettled)
            filters.add(match(eq("settlementsCount", 0)));
        else if(justSettled != null)
            filters.add(match(gt("settlementsCount", 0)));

        filters.addAll(List.of(
                Aggregates.sort(sortFilter),
                skip(skip),
                limit(limit)
        ));

        List<Document> users = new ArrayList<>();
        for (Document doc : documentMongoCollection.aggregate(filters))
            users.add(doc);

        return users;
    }

    public Integer countUsersWithSettlementStatus(
            Bson match, Boolean justSettled
    ) {
        List<Bson> filters = new ArrayList<>() {{
            add(match(match));
            add(lookup("teach_schedule",
                    Collections.singletonList(new Variable<>("myId", "$_id")), Arrays.asList(
                            match(
                                    and(
                                            expr(
                                                    new Document("$eq", Arrays.asList("$user_id", "$$myId"))
                                            ),
                                            exists("settled_at", false),
                                            exists("students.0")
                                    )),
                            project(JUST_ID)
                    ), "teachSettlements"));
            add(lookup("advisor_requests",
                    Collections.singletonList(new Variable<>("myId", "$_id")), Arrays.asList(
                            match(and(
                                    expr(
                                            new Document("$eq", Arrays.asList("$advisor_id", "$$myId"))
                                    ),
                                    exists("settled_at", false),
                                    exists("paid_at")
                            )),
                            project(JUST_ID)
                    ), "adviceSettlements"));
            add(
                    project(new BasicDBObject()
                            .append("teachSettlementsCount", new BasicDBObject("$size", "$teachSettlements"))
                            .append("adviceSettlementsCount", new BasicDBObject("$size", "$adviceSettlements"))
                    )
            );
            add(addFields(new Field<>(
                    "settlementsCount", new BasicDBObject("$add", Arrays.asList("$teachSettlementsCount", "$adviceSettlementsCount")))
            ));
        }};
        if(justSettled != null && justSettled)
            filters.add(match(eq("settlementsCount", 0)));
        else if(justSettled != null)
            filters.add(match(gt("settlementsCount", 0)));

        int counter = 0;
        for (Document ignored : documentMongoCollection.aggregate(filters))
            counter++;

        return counter;
    }

    @Override
    void init() {
        table = "user";
        secKey = "NID";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public void checkCache(Document newDoc) {
        removeFromCache(table, newDoc.getObjectId("_id"));
        if (secKey != null)
            removeFromCache(table, newDoc.get(secKey));
    }

    public void checkCache(ObjectId oId) {
        removeFromCache(table, oId);
    }
}
