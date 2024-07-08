package irysc.gachesefid.Controllers;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Models.CommentSection;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class CommentController {

    private static final Integer MAX_COMMENT_PER_USER = 5;
    private static final Integer PAGE_SIZE = 10;

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
        }

        if (commentRepository.count(and(
                eq("section", section),
                eq("ref_id", refId),
                eq("user_id", userId)
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
            ObjectId commentId, ObjectId userId,
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

    public static String getComments(
            ObjectId refId, String section,
            Integer pageIndex, String status,
            boolean isForAdmin
    ) {
        List<Bson> filters = new ArrayList<>();
        filters.add(eq("ref_id", refId));
        filters.add(eq("section", section));
        if (isForAdmin && status != null) {
            if (
                    !status.equalsIgnoreCase("pending") &&
                            !status.equalsIgnoreCase("accept") &&
                            !status.equalsIgnoreCase("reject")
            )
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("status", status));
        }

        AggregateIterable<Document> docs =
                commentRepository.findWithJoinUser(
                        "user_id", "student",
                        match(and(filters)), null,
                        Sorts.descending("created_at"),
                        (pageIndex - 1) * PAGE_SIZE, PAGE_SIZE
                );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {
            try {
                Document student = doc.get("student", Document.class);
                JSONObject jsonObject = new JSONObject()
                        .put("comment", doc.getString("comment"))
                        .put("createdAt", getSolarDate(doc.getLong("created_at")))
                        .put("user", student.getString("first_name") + " " + student.getString("last_name"))
                        .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + student.getString("pic"));

                if (isForAdmin) {
                    jsonObject.put("status", doc.getString("status"));
                    jsonObject.put("section", doc.getString("section"));
                    jsonObject.put("refId", doc.getObjectId("ref_id"));

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

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getCommentsCount(
            ObjectId refId, String section,
            String status, boolean isForAdmin
    ) {
        List<Bson> filters = new ArrayList<>();
        filters.add(eq("ref_id", refId));
        filters.add(eq("section", section));
        if (isForAdmin && status != null) {
            if (
                    !status.equalsIgnoreCase("pending") &&
                            !status.equalsIgnoreCase("accept") &&
                            !status.equalsIgnoreCase("reject")
            )
                return JSON_NOT_VALID_PARAMS;

            filters.add(eq("status", status));
        }

        return generateSuccessMsg("count", commentRepository.count(and(filters)));
    }

    public static String getMyComments(ObjectId userId, Integer pageIndex) {

        List<Document> comments = commentRepository.findLimited(
                eq("user_id", userId), null, PAGE_SIZE * (pageIndex - 1), PAGE_SIZE
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
            } catch (Exception ignore) {}
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getMyCommentsCount(ObjectId userId) {
        return generateSuccessMsg("count", commentRepository.count(eq("user_id", userId)));
    }

}
