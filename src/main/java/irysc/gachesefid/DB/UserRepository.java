package irysc.gachesefid.DB;


import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Main.GachesefidApplication;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Models.Quiz;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.ObjectIdValidator;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.convertPersianDigits;
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

        if(authVia.equals(AuthVia.SMS.getName()))
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

            if(savePhoneOrMail)
                newDoc.append("phone_or_mail", phoneOrMail);

            activationRepository.insertOne(newDoc);

            if (via.equals(AuthVia.SMS.getName()))
                Utility.sendSMS(phoneOrMail, code + "" , "", "", "activationCode");
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


    @Override
    void init() {
        table = "user";
        secKey = "NID";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
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

    public void checkCache(Document newDoc) {
        removeFromCache(table, newDoc.getObjectId("_id"));
    }

    public void checkCache(ObjectId oId) {
        removeFromCache(table, oId);
    }

    // todo : after all transfer from mysql to mongo it should be delete
    public static ArrayList<Quiz> findAllMysql() {

        ArrayList<Quiz> users = new ArrayList<>();

        try {
            String sql = "select u.*, r.cityId, r.email, s.id as schooId from users u, redundantinfo1 r, schoolstudent ss, school s where s.uId = ss.sId and ss.uId = u.id and r.uId = u.id and u.level = 1 and u.NID is not null and u.phoneNum is not null";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                Quiz user = new Quiz();

                boolean firstId = true;

                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String key = rsmd.getColumnName(i);
                    if(key.equalsIgnoreCase("id")) {
                        if(firstId)
                            firstId = false;
                        else
                          key = "sId";
                    }

                    if (rs.getObject(i) instanceof String)
                        user.cols.put(key, convertPersianDigits(rs.getObject(i).toString()));
                    else
                        user.cols.put(key, rs.getObject(i));
                }

                user.cols.put("NID", user.cols.get("NID").toString().replace("-", "").replace(" ", ""));
                if(!Utility.validationNationalCode(user.cols.get("NID").toString()))
                    continue;

                if(!PhoneValidator.isValid(user.cols.get("phoneNum").toString()))
                    continue;

                if(user.cols.get("firstName").toString().isEmpty() ||
                    user.cols.get("lastName").toString().isEmpty()
                )
                    continue;

                users.add(user);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }

        return users;
    }

    public static ArrayList<Document> fetchAuthorsFromMySQL() {


        ArrayList<Document> output = new ArrayList<>();

        try {
            String sql = "select u.firstName, u.lastName, u.username, (select count(*) from question where author = u.id) as q_no, u.id from users u where u.level = 10";
            PreparedStatement ps = GachesefidApplication.con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Document doc = new Document("first_name", rs.getString(1))
                        .append("last_name", rs.getString(2))
                        .append("username", rs.getString(3))
                        .append("code", rs.getInt(5))
                        .append("q_no", rs.getInt(4));

                if(doc.getString("last_name").isEmpty()) {
                    String[] splited = doc.getString("first_name").split(" ");
                    if(splited.length >= 2) {

                        String tmp = "";
                        for(int i = 0; i < splited.length - 1; i++)
                            tmp += splited[i] + " ";

                        doc.put("first_name", tmp);
                        doc.put("last_name", splited[splited.length - 1]);
                    }
                }

                output.add(doc);
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return output;
    }
}
