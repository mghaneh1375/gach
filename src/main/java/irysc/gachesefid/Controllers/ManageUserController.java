package irysc.gachesefid.Controllers;

import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Models.Access;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class ManageUserController {

    public static String fetchUser(Document user, String unique, String access) {

        if (user == null) {
            user = userRepository.findByUnique(unique, true);
            if (user == null)
                return Utility.generateSuccessMsg("user", "");
        }

        boolean isAdmin = Authorization.isAdmin(access);
        if (!isAdmin && !Authorization.isPureStudent(user.getString("access")))
            return JSON_NOT_ACCESS;

        JSONObject userJson = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("pic", user.containsKey("pic") ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic") : "")
                .put("nameFa", user.getString("name_fa"))
                .put("nameEn", user.getString("name_en"))
                .put("lastNameFa", user.getString("last_name_fa"))
                .put("lastNameEn", user.getString("last_name_en"))
                .put("address", user.containsKey("address") && isAdmin ? user.getString("address") : "")
                .put("preTel", user.containsKey("pre_tel") && isAdmin ? user.getString("pre_tel") : "")
                .put("tel", user.containsKey("tel") && isAdmin ? user.getString("tel") : "")
                .put("passportFile", user.containsKey("passport_file") ? STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("passport_file") : "")
                .put("access", user.getString("access"))
                .put("city", user.containsKey("city") && isAdmin ? user.getString("city") : "")
                .put("NIDOrPassport", (user.containsKey("NID")) ?
                        user.getString("NID") : (user.containsKey("passport_no")) ?
                        user.getString("passport_no") : "")
                .put("NIDOrPassportType", (user.containsKey("NID")) ?
                        "NID" : "passport")
                .put("birthDay", user.containsKey("birth_day") ?
                        user.getString("birth_day") : "")
                .put("sex", user.getString("sex"))
                .put("postalCode", user.containsKey("postal_code") && isAdmin ?
                        user.getString("postal_code") : "")
                .put("mail", user.getString("mail"))
                .put("username", user.getString("username"))
                .put("birthCountry", user.containsKey("birth_country") && isAdmin ?
                        user.getString("birth_country") : "")
                .put("country", user.containsKey("country") && isAdmin ?
                        user.getString("country") : "")
                .put("title", user.containsKey("title") ?
                        user.getString("title") : "")
                .put("birthCity", user.containsKey("birth_city") && isAdmin ?
                        user.getString("birth_city") : "");

        return Utility.generateSuccessMsg("user", userJson);
    }

    public static String setCoins(ObjectId userId, int newCoins) {

        Document user = userRepository.findOneAndUpdate(
                eq("_id", userId),
                set("coin", newCoins)
        );

        if (user == null)
            return JSON_NOT_VALID_ID;

        user.put("coin", newCoins);
        return JSON_OK;
    }

    public static String addAccess(ObjectId userId, String newAccess) {

        Document user = userRepository.findById(userId);

        if (user == null)
            return JSON_NOT_VALID_ID;

        boolean change = false;

        if (!newAccess.equals(Access.STUDENT.getName()) && !user.getBoolean("level")) {
            user.put("level", true);
            change = true;
        } else if (newAccess.equals(Access.STUDENT.getName()) && user.getBoolean("level")) {
            user.put("level", false);
            user.remove("access");
            change = true;
        }

        if (!newAccess.equals(Access.STUDENT.getName())) {

            List<String> accesses;
            if (user.containsKey("access"))
                accesses = user.getList("accesses", String.class);
            else
                accesses = new ArrayList<>();

            if (!accesses.contains(newAccess)) {
                accesses.add(newAccess);
                user.put("accesses", accesses);
                change = true;
            }
        }

        if (change)
            userRepository.replaceOne(userId, user);

        return JSON_OK;
    }

    public static String removeAccess(ObjectId userId, String role) {

        Document user = userRepository.findById(userId);

        if (user == null || !user.getBoolean("level"))
            return JSON_NOT_VALID_ID;

        List<String> accesses = user.getList("accesses", String.class);

        if (!accesses.contains(role))
            return JSON_NOT_VALID_PARAMS;

        accesses.remove(role);
        if (accesses.size() == 0) {
            user.remove("accesses");
            user.put("level", false);
        } else
            user.put("accesses", accesses);

        userRepository.replaceOne(userId, user);
        return JSON_OK;
    }

    public static String fetchUserLike(String nameEn, String lastNameEn,
                                       String nameFa, String lastNameFa) {

        ArrayList<Bson> constraints = new ArrayList<>();

        if (nameEn != null)
            constraints.add(regex("name_en", Pattern.compile(Pattern.quote(nameEn), Pattern.CASE_INSENSITIVE)));

        if (nameFa != null)
            constraints.add(regex("name_fa", Pattern.compile(Pattern.quote(nameFa), Pattern.CASE_INSENSITIVE)));

        if (lastNameFa != null)
            constraints.add(regex("last_name_fa", Pattern.compile(Pattern.quote(lastNameFa), Pattern.CASE_INSENSITIVE)));

        if (lastNameEn != null)
            constraints.add(regex("last_name_en", Pattern.compile(Pattern.quote(lastNameEn), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> users = userRepository.find(and(constraints), USER_DIGEST);
        JSONArray jsonArray = new JSONArray();

        for (Document user : users)
            jsonArray.put(UserRepository.convertUserDigestDocumentToJSON(user));

        return Utility.generateSuccessMsg("users", jsonArray);
    }

    public static String setAdvisorPercent(ObjectId advisorId, int percent) {

        Document advisor = userRepository.findById(advisorId);

        if (advisor == null || !advisor.containsKey("accesses") ||
                !advisor.getList("accesses", String.class).contains(Access.ADVISOR.getName()))
            return JSON_NOT_VALID_ID;

        advisor.put("percent", percent);
        userRepository.updateOne(advisorId, set("percent", percent));

        return JSON_OK;
    }
}
