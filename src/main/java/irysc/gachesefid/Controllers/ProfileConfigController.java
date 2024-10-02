package irysc.gachesefid.Controllers;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Level.LevelController;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class ProfileConfigController {

    public static String setMyConfig(ObjectId userId, JSONObject jsonObject) {
        Document config = profileConfigRepository.findBySecKey(userId);
        boolean isNew = false;

        if (config == null) {
            config = new Document("user_id", userId);
            isNew = true;
        }

        config.put("show_content_packages", jsonObject.getBoolean("showContentPackages"));
        config.put("show_quizzes", jsonObject.getBoolean("showQuizzes"));
        config.put("show_my_advisor", jsonObject.getBoolean("showMyAdvisor"));
        config.put("show_teachers", jsonObject.getBoolean("showTeachers"));
        config.put("show_my_comments", jsonObject.getBoolean("showMyComments"));
        config.put("show_my_rate", jsonObject.getBoolean("showMyRate"));
        config.put("show_grade", jsonObject.getBoolean("showGrade"));
        config.put("show_branch", jsonObject.getBoolean("showBranch"));
        config.put("show_school", jsonObject.getBoolean("showSchool"));
        config.put("show_city", jsonObject.getBoolean("showCity"));

        if (isNew)
            profileConfigRepository.insertOne(config);
        else
            profileConfigRepository.replaceOneWithoutClearCache(
                    config.getObjectId("_id"),
                    config
            );

        return JSON_OK;
    }

    public static String getMyConfig(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        JSONObject jsonObject = new JSONObject()
                .put("showContentPackages", config != null && config.getBoolean("show_content_packages"))
                .put("showQuizzes", config != null && config.getBoolean("show_quizzes"))
                .put("showMyAdvisor", config != null && config.getBoolean("show_my_advisor"))
                .put("showTeachers", config != null && config.getBoolean("show_teachers"))
                .put("showMyComments", config != null && config.getBoolean("show_my_comments"))
                .put("showMyRate", config != null && config.getBoolean("show_my_rate"))
                .put("showGrade", config != null && config.getBoolean("show_grade"))
                .put("showBranch", config != null && config.getBoolean("show_branch"))
                .put("showSchool", config != null && config.getBoolean("show_school"))
                .put("showCity", config != null && config.getBoolean("show_city"));
        return generateSuccessMsg("data", jsonObject);
    }

    public static String getUserProfile(ObjectId userId) {
        Document user = userRepository.findById(userId);
        if (user == null || !user.getString("status").equals("active"))
            return JSON_NOT_ACCESS;

        JSONObject userLevel = new JSONObject(LevelController.getMyCurrLevel(userId))
                .getJSONObject("data");

        Document config = profileConfigRepository.findBySecKey(userId);

        JSONObject jsonObject = new JSONObject()
                .put("showComments", config != null &&
                        config.getBoolean("show_my_comments") &&
                        commentRepository.count(and(
                                eq("user_id", userId),
                                eq("status", "accept")
                        )) > 0
                )
                .put("showTeachers", config != null &&
                        config.getBoolean("show_teachers") &&
                        teachScheduleRepository.count(and(
                                eq("students._id", userId),
                                lt("start_at", System.currentTimeMillis())
                        )) > 0
                )
                .put("showAdvisors", config != null &&
                        config.getBoolean("show_my_advisor") &&
                        user.containsKey("my_advisors") &&
                        user.getList("my_advisors", Object.class).size() > 0
                )
                .put("showContentPackages", config != null &&
                        config.getBoolean("show_content_packages") &&
                        contentRepository.count(and(
                                eq("users._id", userId),
                                eq("visibility", true)
                        )) > 0
                )
                .put("showQuizzes", config != null &&
                                config.getBoolean("show_quizzes")
//                        && (
//                                iryscQuizRepository.count()
//                        )
                )
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("id", user.getObjectId("_id").toString())
                .put("level", userLevel.getString("name"))
                .put("point", userLevel.get("point"));

        if (config != null && config.getBoolean("show_grade") &&
                user.containsKey("grade")
        ) {
            Document grade = (Document) user.get("grade");
            if (grade != null && grade.containsKey("name"))
                jsonObject.put("grade", grade.getString("name"));
        }

        if (config != null && config.getBoolean("show_school") &&
                user.containsKey("school")
        ) {
            Document school = (Document) user.get("school");
            if (school != null && school.containsKey("name"))
                jsonObject.put("school", school.getString("name"));
        }

        if (config != null && config.getBoolean("show_city") &&
                user.containsKey("city")
        ) {
            Document city = (Document) user.get("city");
            if (city != null && city.containsKey("name"))
                jsonObject.put("city", city.getString("name"));
        }

        if (config != null && config.getBoolean("show_branch") &&
                user.containsKey("branches")
        ) {
            List<Document> branches = user.getList("branches", Document.class);
            if (branches.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Document branch : branches) {
                    sb.append(branch.getString("name")).append(" - ");
                }
                jsonObject.put("branches", sb.substring(0, sb.toString().length() - 3));
            }
        }

        return generateSuccessMsg("data", jsonObject);
    }

    public static String getUserComments(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        if (config == null || !config.getBoolean("show_my_comments"))
            return JSON_NOT_ACCESS;

        ArrayList<Document> comments = commentRepository.findLimited(
                and(
                        eq("user_id", userId),
                        eq("status", "accept")
                ), null,
                Sorts.descending("created_at"),
                0, 10
        );

        return generateSuccessMsg(
                "data",
                CommentController.returnCommentsJSONArr(comments)
        );
    }

    public static String getUserAdvisors(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        if (config == null || !config.getBoolean("show_my_advisor"))
            return JSON_NOT_ACCESS;

        Document user = userRepository.findById(userId);
        if (user == null)
            return JSON_NOT_UNKNOWN;

        List<Document> advisors = userRepository.findByIds(
                user.getList("my_advisors", Object.class),
                false, ADVISOR_PUBLIC_INFO
        );

        JSONArray advisorsJSONArr = new JSONArray();
        if (advisors != null) {
            advisors.forEach(advisor -> advisorsJSONArr.put(new JSONObject()
                    .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                    .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic"))
                    .put("id", advisor.getObjectId("_id").toString())
                    .put("rate", advisor.getOrDefault("rate", 0))
            ));
        }

        return generateSuccessMsg("data", advisorsJSONArr);
    }

    public static String getUserTeachers(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        if (config == null || !config.getBoolean("show_teachers"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();
        List<Object> teacherIds =
                teachScheduleRepository.find(
                                and(
                                        eq("students._id", userId),
                                        lt("start_at", System.currentTimeMillis())
                                ), JUST_USER_ID
                        ).stream().map(document -> document.getObjectId("user_id"))
                        .collect(Collectors.toList());

        ArrayList<Document> users = userRepository.findByIds(
                teacherIds, false, TEACHER_PUBLIC_INFO
        );
        if (users == null)
            return JSON_NOT_UNKNOWN;

        users.forEach(user -> data.put(new JSONObject()
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("id", user.getObjectId("_id").toString())
                .put("rate", user.getOrDefault("teach_rate", 0))
        ));

        return generateSuccessMsg("data", data);
    }

    public static String getUserContents(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        if (config == null || !config.getBoolean("show_content_packages"))
            return JSON_NOT_ACCESS;

        JSONArray data = new JSONArray();
        contentRepository.find(
                and(
                        eq("users._id", userId),
                        eq("visibility", true)
                ),
                CONTENT_DIGEST,
                Sorts.ascending("priority")
        ).forEach(doc ->
                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false))
        );

        return generateSuccessMsg("data", data);
    }

    public static String getUserQuizzes(ObjectId userId) {
        Document config = profileConfigRepository.findBySecKey(userId);
        if (config == null || !config.getBoolean("show_quizzes"))
            return JSON_NOT_ACCESS;

        JSONArray jsonArray = new JSONArray();
        iryscQuizRepository.findLimited(
                eq("students._id", userId),
                QUIZ_DIGEST, Sorts.descending("created_at"), 0, 10
        ).forEach(document -> jsonArray.put(
                regularQuizController.convertDocToJSON(
                        document, true, false, false, false
                )
        ));
        if(jsonArray.length() < 10) {
            openQuizRepository.findLimited(
                    eq("students._id", userId),
                    QUIZ_DIGEST, Sorts.descending("created_at"), 0, 10 - jsonArray.length()
            ).forEach(document -> jsonArray.put(
                    openQuiz.convertDocToJSON(
                            document, true, false, false, false
                    )
            ));
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
