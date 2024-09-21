package irysc.gachesefid.Controllers;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class CommentController {

    private static final Integer MAX_COMMENT_PER_USER = 5;
    private static final Integer PAGE_SIZE = 10;
    private static final Integer ADMIN_PAGE_SIZE = 20;

    public static String writeComment(
            ObjectId userId, ObjectId refId,
            String section, String comment
    ) {

        if (comment.length() > 1000)
            return generateErr("متن نظر باید حداکثر 1000 کاراکتر باشد");

        String sectionFa = "";
        if (section.equals(CommentSection.TEACH.getName())) {
            if (!teachScheduleRepository.exist(and(
                    eq("user_id", refId),
                    eq("students._id", userId),
                    gt("start_at", System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30)
            )))
                return generateErr("شما اجازه نوشتن نظر برای این دبیر را ندارید (برای نوشتن نظر باید در یک ماه گذشته با این استاد کلاس داشته باشید)");

            sectionFa = "استاد";
        } else if (section.equals(CommentSection.CONTENT.getName())) {
            Document content = contentRepository.findById(refId);
            if (content == null ||
                    searchInDocumentsKeyValIdx(
                            content.getList("users", Document.class), "_id", userId
                    ) == -1
            )
                return JSON_NOT_ACCESS;
            sectionFa = "دوره آموزشی";
        }

        if (commentRepository.count(and(
                eq("section", section),
                eq("ref_id", refId),
                eq("user_id", userId),
                or(
                        eq("status", "pending"),
                        eq("status", "accept")
                )
        )) > MAX_COMMENT_PER_USER)
            return generateErr("شما برای این " + sectionFa + " تنها می توانید " + MAX_COMMENT_PER_USER + " نظر ثبت کنید");

        commentRepository.insertOne(
                new Document("user_id", userId)
                        .append("status", "pending")
                        .append("created_at", System.currentTimeMillis())
                        .append("ref_id", refId)
                        .append("comment", comment)
                        .append("section", section)
        );

        return JSON_OK;
    }

    public static String removeComment(ObjectId userId, ObjectId commentId) {
        if (userId != null) {
            commentRepository.deleteOne(and(
                    eq("user_id", userId),
                    eq("_id", commentId)
            ));
        } else {
            commentRepository.deleteOne(
                    eq("_id", commentId)
            );
        }

        return JSON_OK;
    }

    public static String setCommentStatus(
            ObjectId userId, ObjectId commentId,
            boolean status
    ) {
        Document comment = commentRepository.findById(commentId);
        if (comment == null)
            return JSON_NOT_VALID_ID;

        comment.put("status", status ? "accept" : "reject");
        comment.put("consider_by", userId);
        comment.put("consider_at", System.currentTimeMillis());
        commentRepository.replaceOneWithoutClearCache(commentId, comment);

        if (status) {
            new Thread(() -> {
                // todo: check badge
                PointController.addPointForAction(
                        comment.getObjectId("user_id"), Action.COMMENT, comment.getObjectId("_id"), null
                );
            }).start();
        }

        return JSON_OK;
    }

    public static String toggleTopStatus(ObjectId commentId) {

        Document comment = commentRepository.findById(commentId);
        if (comment == null)
            return JSON_NOT_VALID_ID;

        if (!comment.getString("status").equals("accept"))
            return generateErr("لطفا ابتدا این نظر را تایید نمایید");

        if (!comment.containsKey("is_top")) {
            comment.put("is_top", true);
            commentRepository.updateOne(commentId, set("is_top", true));
        } else {
            comment.remove("is_top");
            commentRepository.updateOne(commentId, unset("is_top"));
        }

        return JSON_OK;
    }

    public static String getComments(
            ObjectId refId, String section,
            Integer pageIndex, String status,
            boolean isForAdmin, Long from, Long to,
            Boolean justTop
    ) {
        List<Bson> filters = new ArrayList<>();
        if (refId != null)
            filters.add(eq("ref_id", refId));

        if (section != null) {
            if (!EnumValidatorImp.isValid(section, CommentSection.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("section", section));
        }

        if (from != null)
            filters.add(gt("created_at", from));

        if (to != null)
            filters.add(lt("created_at", to));

        if (justTop != null && justTop)
            filters.add(exists("is_top"));

        if (isForAdmin && status != null) {
            if (
                    !status.equalsIgnoreCase("pending") &&
                            !status.equalsIgnoreCase("accept") &&
                            !status.equalsIgnoreCase("reject")
            )
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("status", status));
        } else if (!isForAdmin)
            filters.add(eq("status", "accept"));

        AggregateIterable<Document> docs =
                commentRepository.findWithJoinUser(
                        "user_id", "student",
                        filters.size() > 0 ? match(filters.size() == 1 ? filters.get(0) : and(filters)) : null, null,
                        Sorts.descending("created_at"),
                        (pageIndex - 1) * (isForAdmin ? ADMIN_PAGE_SIZE : PAGE_SIZE),
                        isForAdmin ? ADMIN_PAGE_SIZE : PAGE_SIZE, project(USER_DIGEST)
                );

        JSONArray jsonArray = new JSONArray();
        List<Document> comments = new ArrayList<>();
        for (Document doc : docs)
            comments.add(doc);

        List<Document> users = null;
        List<Document> contents = null;
        if (isForAdmin) {
            PairValue p = findRefs(comments);
            users = (List<Document>) p.getKey();
            contents = (List<Document>) p.getValue();
        }

        for (Document doc : comments) {
            try {
                Document student = doc.get("student", Document.class);
                JSONObject jsonObject = new JSONObject()
                        .put("comment", doc.getString("comment"))
                        .put("createdAt", getSolarDate(doc.getLong("created_at")));
                fillJSONWithUserPublicInfo(jsonObject, student);
                if (isForAdmin) {
                    jsonObject.put("id", doc.getObjectId("_id").toString())
                            .put("status", doc.getString("status"))
                            .put("section", doc.getString("section"))
                            .put("refId", doc.getObjectId("ref_id"))
                            .put("isTop", doc.getOrDefault("is_top", false))
                            .put("considerAt", doc.containsKey("consider_at") ? getSolarDate(doc.getLong("consider_at")) : "");

                    if ((doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                            doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())) && users != null
                    ) {
                        users.stream().filter(user -> user.getObjectId("_id")
                                        .equals(doc.getObjectId("ref_id")))
                                .findFirst().ifPresent(user ->
                                        jsonObject.put("ref", user.getString("first_name") + " " + user.getString("last_name")));
                    } else if (doc.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()) && contents != null)
                        contents.stream().filter(content -> content.getObjectId("_id")
                                .equals(doc.getObjectId("ref_id"))
                        ).findFirst().ifPresent(content ->
                                jsonObject.put("ref", content.getString("title"))
                        );
                }

                jsonArray.put(jsonObject);
            } catch (Exception ignore) {
            }
        }

        JSONObject jsonObject = new JSONObject()
                .put("comments", jsonArray);

        if (!isForAdmin)
            jsonObject.put("hasNextPage", commentRepository.count(filters.size() == 0 ? null : and(filters)) > pageIndex * PAGE_SIZE);

        return generateSuccessMsg("data", jsonObject);
    }

    public static String getTopComments(String section) {

        List<Bson> filters = new ArrayList<>();

        if (!EnumValidatorImp.isValid(section, CommentSection.class))
            return JSON_NOT_VALID_PARAMS;

        filters.add(eq("section", section));
        filters.add(exists("is_top"));
        filters.add(eq("status", "accept"));

        AggregateIterable<Document> docs =
                commentRepository.findWithJoinUser(
                        "user_id", "student",
                        match(and(filters)), null,
                        Sorts.descending("created_at"),
                        null, null, project(USER_DIGEST)
                );

        JSONArray jsonArray = new JSONArray();
        List<Document> comments = new ArrayList<>();
        for (Document doc : docs)
            comments.add(doc);

        PairValue p = findRefs(comments);
        List<Document> users = (List<Document>) p.getKey();
        List<Document> contents = (List<Document>) p.getValue();

        for (Document doc : comments) {
            try {
                Document student = doc.get("student", Document.class);
                JSONObject jsonObject = new JSONObject()
                        .put("comment", doc.getString("comment"))
                        .put("createdAt", getSolarDate(doc.getLong("created_at")));

                fillJSONWithUserPublicInfo(jsonObject, student);

                if ((doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                        doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())) && users != null
                ) {
                    users.stream().filter(user -> user.getObjectId("_id")
                                    .equals(doc.getObjectId("ref_id")))
                            .findFirst().ifPresent(user ->
                                    jsonObject.put("ref", user.getString("first_name") + " " + user.getString("last_name")));
                } else if (doc.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()) && contents != null)
                    contents.stream().filter(content -> content.getObjectId("_id")
                            .equals(doc.getObjectId("ref_id"))
                    ).findFirst().ifPresent(content ->
                            jsonObject.put("ref", content.getString("title"))
                    );

                jsonArray.put(jsonObject);
            } catch (Exception ignore) {
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getCommentsCount(
            ObjectId refId, String section,
            String status, boolean isForAdmin,
            Long from, Long to, Boolean justTop
    ) {
        List<Bson> filters = new ArrayList<>();
        if (refId != null)
            filters.add(eq("ref_id", refId));

        if (section != null) {
            if (!EnumValidatorImp.isValid(section, CommentSection.class))
                return JSON_NOT_VALID_PARAMS;
            filters.add(eq("section", section));
        }

        if (from != null)
            filters.add(gt("created_at", from));

        if (to != null)
            filters.add(lt("created_at", to));

        if (justTop != null && justTop)
            filters.add(exists("is_top"));

        if (isForAdmin && status != null) {
            if (
                    !status.equalsIgnoreCase("pending") &&
                            !status.equalsIgnoreCase("accept") &&
                            !status.equalsIgnoreCase("reject")
            )
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("status", status));
        }

        JSONObject jsonObject = new JSONObject()
                .put("count", commentRepository.count(filters.size() == 0 ? null : and(filters)))
                .put("perPage", ADMIN_PAGE_SIZE);

        return generateSuccessMsg("data", jsonObject);
    }

    private static PairValue findRefs(List<Document> comments) {

        Set<ObjectId> userIds = new HashSet<>();
        Set<ObjectId> contentIds = new HashSet<>();

        for (Document doc : comments) {
            if (doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                    doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())
            )
                userIds.add(doc.getObjectId("ref_id"));
            else if (doc.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()))
                contentIds.add(doc.getObjectId("ref_id"));
        }

        List<Document> users = null;
        List<Document> contents = null;

        if (userIds.size() > 0)
            users = userRepository.findByIds(new ArrayList<>(userIds), false, JUST_NAME);

        if (contentIds.size() > 0)
            contents = contentRepository.findByIds(new ArrayList<>(contentIds), false, JUST_TITLE);

        return new PairValue(users, contents);
    }

    private static List<Document> findJustContentsRef(List<Document> comments) {

        List<Object> contentIds = comments.stream().filter(document ->
                        document.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName())
                )
                .map(document -> document.getObjectId("ref_id"))
                .distinct()
                .collect(Collectors.toList());

        List<Document> contents = null;
        if (contentIds.size() > 0)
            contents = contentRepository.findByIds(contentIds, false, JUST_TITLE);

        return contents;
    }

    public static String getMyComments(ObjectId userId,
                                       String commentSection,
                                       Long from, Long to,
                                       String status
    ) {

        List<Bson> filters = new ArrayList<>() {{
            add(eq("user_id", userId));
        }};
        if (from != null)
            filters.add(gt("created_at", from));

        if (to != null)
            filters.add(lt("created_at", to));

        if (status != null) {
            if (!status.equalsIgnoreCase("pending") &&
                    !status.equalsIgnoreCase("accept") &&
                    !status.equalsIgnoreCase("reject")
            )
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("status", status));
        }

        if (commentSection != null) {
            if (!EnumValidatorImp.isValid(commentSection, CommentSection.class))
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("section", commentSection));
        }

        List<Document> comments = commentRepository.find(
                and(filters), null,
                Sorts.descending("created_at")
        );

        JSONArray jsonArray = new JSONArray();
        PairValue p = findRefs(comments);

        List<Document> users = (List<Document>) p.getKey();
        List<Document> contents = (List<Document>) p.getValue();

        for (Document doc : comments) {
            try {
                JSONObject jsonObject = new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("comment", doc.getString("comment"))
                        .put("createdAt", getSolarDate(doc.getLong("created_at")))
                        .put("considerAt", doc.containsKey("consider_at") ? getSolarDate(doc.getLong("consider_at")) : "")
                        .put("status", doc.getString("status"))
                        .put("section", doc.getString("section"))
                        .put("refId", doc.getObjectId("ref_id"));

                if ((doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                        doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())) && users != null
                ) {
                    users.stream().filter(user -> user.getObjectId("_id")
                                    .equals(doc.getObjectId("ref_id")))
                            .findFirst().ifPresent(user ->
                                    jsonObject.put("ref", user.getString("first_name") + " " + user.getString("last_name")));
                } else if (doc.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()) && contents != null)
                    contents.stream().filter(content -> content.getObjectId("_id")
                            .equals(doc.getObjectId("ref_id"))
                    ).findFirst().ifPresent(content ->
                            jsonObject.put("ref", content.getString("title"))
                    );

                jsonArray.put(jsonObject);
            } catch (Exception ignore) {
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getCommentsAboutMe(
            ObjectId userId,
            String commentSection,
            Long from, Long to
    ) {
        List<ObjectId> refIds = new ArrayList<>() {{
            add(userId);
        }};
        if (commentSection == null ||
                commentSection.equalsIgnoreCase(CommentSection.CONTENT.getName())
        ) {
            refIds.addAll(
                    contentRepository.find(
                                    eq("teacher_ids", userId), JUST_ID
                            ).stream()
                            .map(document -> document.getObjectId("_id"))
                            .collect(Collectors.toList())
            );
        }
        List<Bson> filters = new ArrayList<>() {{
            add(in("ref_id", refIds));
            add(eq("status", "accept"));
        }};

        if (from != null)
            filters.add(gt("created_at", from));
        if (to != null)
            filters.add(lt("created_at", to));
        if (commentSection != null)
            filters.add(eq("section", commentSection));

        List<Document> comments = commentRepository.find(and(filters), null);
        List<Document> contents = findJustContentsRef(comments);

        JSONArray jsonArray = new JSONArray();
        comments.forEach(comment -> {
            JSONObject jsonObject =
                    new JSONObject()
                            .put("id", comment.getObjectId("_id").toString())
                            .put("comment", comment.getString("comment"))
                            .put("marked", comment.getOrDefault("marked", false))
                            .put("createdAt", getSolarDate(comment.getLong("created_at")))
                            .put("section", comment.getString("section"));

            if (comment.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()) && contents != null)
                contents.stream().filter(content -> content.getObjectId("_id")
                        .equals(comment.getObjectId("ref_id"))
                ).findFirst().ifPresent(content ->
                        jsonObject.put("ref", content.getString("title"))
                );

            jsonArray.put(jsonObject);
        });

        return generateSuccessMsg("data", jsonArray);
    }

    public static String toggleCommentMarkedStatus(
            ObjectId userId, ObjectId commentId
    ) {
        Document comment = commentRepository.findById(commentId);
        if (comment == null)
            return JSON_NOT_VALID_ID;

        if ((comment.getString("section").equalsIgnoreCase(
                CommentSection.ADVISOR.getName()
        ) || comment.getString("section").equalsIgnoreCase(
                CommentSection.TEACH.getName()
        )) && !comment.getObjectId("ref_id").equals(userId))
            return JSON_NOT_ACCESS;

        if (comment.getString("section").equalsIgnoreCase(
                CommentSection.CONTENT.getName()
        )) {
            if (!contentRepository.exist(
                    and(
                            eq("_id", comment.getObjectId("ref_id")),
                            eq("teacher_ids", userId)
                    )
            ))
                return JSON_NOT_ACCESS;
        }

        comment.put("marked", !(boolean) comment.getOrDefault("marked", false));
        commentRepository.updateOne(eq("_id", commentId), set("marked", comment.getBoolean("marked")));

        return JSON_OK;
    }

    public static String getTeacherMarkedComments(ObjectId userId) {
        List<ObjectId> refIds = new ArrayList<>() {{
            add(userId);
        }};
        refIds.addAll(
                contentRepository.find(
                                eq("teacher_ids", userId), JUST_ID
                        )
                        .stream()
                        .map(document -> document.getObjectId("_id"))
                        .collect(Collectors.toList())
        );


        AggregateIterable<Document> docs =
                commentRepository.findWithJoinUser(
                        "user_id", "student",
                        match(and(
                                in("ref_id", refIds),
                                eq("status", "accept"),
                                exists("marked"),
                                eq("marked", true)
                        )), null,
                        Sorts.descending("created_at"),
                        null, null, project(USER_DIGEST)
                );

        List<Document> comments = new ArrayList<>();
        for(Document doc : docs)
            comments.add(doc);

        List<Document> contents = findJustContentsRef(comments);
        JSONArray jsonArray = new JSONArray();
        comments.forEach(comment -> {
            Document student = comment.get("student", Document.class);
            JSONObject jsonObject =
                    new JSONObject()
                            .put("comment", comment.getString("comment"))
                            .put("createdAt", getSolarDate(comment.getLong("created_at")))
                            .put("section", comment.getString("section"));
            fillJSONWithUserPublicInfo(jsonObject, student);
            if (comment.getString("section").equalsIgnoreCase(CommentSection.CONTENT.getName()) && contents != null)
                contents.stream().filter(content -> content.getObjectId("_id")
                        .equals(comment.getObjectId("ref_id"))
                ).findFirst().ifPresent(content ->
                        jsonObject.put("ref", content.getString("title"))
                );

            jsonArray.put(jsonObject);
        });

        return generateSuccessMsg("data", jsonArray);
    }
}
