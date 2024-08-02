package irysc.gachesefid.Controllers;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class CommentController {

    private static final Integer MAX_COMMENT_PER_USER = 5;
    private static final Integer PAGE_SIZE = 20;
    private static final Integer ADMIN_PAGE_SIZE = 2;

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

        return JSON_OK;
    }

    public static String toggleTopStatus(ObjectId commentId) {

        Document comment = commentRepository.findById(commentId);
        if (comment == null)
            return JSON_NOT_VALID_ID;

        if (!comment.getString("status").equals("accept"))
            return generateErr("لطفا ابتدا این نظر را تایید نمایید");

        if (comment.containsKey("is_top")) {
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
                        isForAdmin ? ADMIN_PAGE_SIZE : PAGE_SIZE
                );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {
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

                    if (doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                            doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())
                    ) {
                        Document ref = userRepository.findById(doc.getObjectId("ref_id"));
                        if (ref != null)
                            jsonObject.put("ref", ref.getString("first_name") + " " + ref.getString("last_name"));
                    }
                }

                jsonArray.put(jsonObject);
            } catch (Exception ignore) {
            }
        }

        JSONObject jsonObject = new JSONObject()
                .put("comments", jsonArray);

        if(!isForAdmin)
            jsonObject.put("hasNextPage", commentRepository.count(filters.size() == 0 ? null : and(filters)) > pageIndex * PAGE_SIZE);

        return generateSuccessMsg("data", jsonObject);
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

    public static String getMyComments(ObjectId userId, Integer pageIndex) {

        List<Document> comments = commentRepository.findLimited(
                eq("user_id", userId), null,
                Sorts.descending("created_at"),
                PAGE_SIZE * (pageIndex - 1), PAGE_SIZE
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : comments) {
            try {
                JSONObject jsonObject = new JSONObject()
                        .put("comment", doc.getString("comment"))
                        .put("createdAt", getSolarDate(doc.getLong("created_at")))
                        .put("status", doc.getString("status"))
                        .put("section", doc.getString("section"))
                        .put("refId", doc.getObjectId("ref_id"));

                if (doc.getString("section").equalsIgnoreCase(CommentSection.TEACH.getName()) ||
                        doc.getString("section").equalsIgnoreCase(CommentSection.ADVISOR.getName())
                ) {
                    Document ref = userRepository.findById(doc.getObjectId("ref_id"));
                    if (ref != null)
                        jsonObject.put("ref", ref.getString("first_name") + " " + ref.getString("last_name"));
                }

                jsonArray.put(jsonObject);
            } catch (Exception ignore) {
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getMyCommentsCount(ObjectId userId) {
        return generateSuccessMsg("count", commentRepository.count(eq("user_id", userId)));
    }

}
