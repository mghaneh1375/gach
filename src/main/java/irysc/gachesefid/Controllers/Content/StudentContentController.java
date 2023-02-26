package irysc.gachesefid.Controllers.Content;


import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Quiz.ContentQuizController;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.Controllers.Quiz.StudentQuizController;
import irysc.gachesefid.DB.ContentQuizRepository;
import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Quiz.Utility.saveStudentAnswers;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class StudentContentController {

    public static String distinctTags() {
        return generateSuccessMsg("data", contentRepository.distinctTags("tags"));
    }

    public static String chapters(ObjectId contentId) {

        Document doc = contentRepository.findById(contentId);
        if(doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> chapters = (List<Document>) doc.getOrDefault("chapters", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for(Document chapter : chapters) {
            jsonArray.put(new JSONObject()
                    .put("title", chapter.getString("title"))
                    .put("desc", chapter.getString("desc"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String distinctTeachers() {
        return generateSuccessMsg("data", contentRepository.distinctTags("teacher"));
    }

    public static String getTeacherBio(String teacher) {

        Document doc = contentRepository.findOne(eq("teacher", teacher), new BasicDBObject("teacher_bio", 1));
        if(doc == null)
            return generateSuccessMsg("data", "");

        return generateSuccessMsg("data", doc.getString("teacher_bio"));
    }

    public static String getAll(ObjectId userId,
                                boolean isAdmin,
                                String tag,
                                String title,
                                String teacher,
                                Boolean visibility,
                                Boolean hasCert,
                                Integer minPrice,
                                Integer maxPrice,
                                Integer minDurationFilter,
                                Integer maxDurationFilter) {

        ArrayList<Bson> filters = new ArrayList<>();

        if(!isAdmin)
            filters.add(eq("visibility", true));

        if(!isAdmin && userId != null)
            filters.add(nin("users._id", userId));

        if(title != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(title), Pattern.CASE_INSENSITIVE)));

        if(tag != null)
            filters.add(in("tags", tag));

        if(hasCert != null)
            filters.add(exists("cert_id", hasCert));

        if(minPrice != null)
            filters.add(gte("price", minPrice));

        if(maxPrice != null)
            filters.add(lte("price", maxPrice));

        if(minDurationFilter != null)
            filters.add(gte("duration", minDurationFilter));

        if(maxDurationFilter != null)
            filters.add(lte("duration", maxDurationFilter));

        if(isAdmin) {

            if(visibility != null)
                filters.add(eq("visibility", visibility));

            if(teacher != null)
                filters.add(eq("teacher", teacher));

        }

        JSONArray data = new JSONArray();

        ArrayList<Document> docs = contentRepository.find(
                filters.size() == 0 ? null : and(filters),
                isAdmin ? CONTENT_DIGEST_FOR_ADMIN : CONTENT_DIGEST,
                Sorts.ascending("priority")
        );

        if(!isAdmin && filters.size() == 2) {

            int min = 1000000000;
            int max = -1;

            int maxDuration = -1;
            int minDuration = 10000000;

            for(Document doc : docs) {

                int price = doc.getInteger("price");
                int duration = doc.getInteger("duration");

                if(price < min)
                    min = price;

                if(price > max)
                    max = price;

                if(duration < minDuration)
                    minDuration = duration;

                if(duration > maxDuration)
                    maxDuration = duration;

                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));
            }

            return generateSuccessMsg("data", data,
                    new PairValue("min", min),
                    new PairValue("max", max),
                    new PairValue("minDuration", minDuration),
                    new PairValue("maxDuration", maxDuration),
                    new PairValue("tags", contentRepository.distinctTags("tags"))
            );
        }

        else {
            for (Document doc : docs)
                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));
        }

        return generateSuccessMsg("data", data);
    }


    public static String getMy(ObjectId userId) {
        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                in("users._id", userId),
                CONTENT_DIGEST
        );

        for(Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false));

        return generateSuccessMsg("data", data);
    }


    public static String get(boolean isAdmin, ObjectId userId, String slug, String NID) {

        Document content = contentRepository.findBySecKey(slug);
        if(content == null)
            return JSON_NOT_VALID_ID;

        Document stdDoc = null;

        if(!isAdmin && userId != null)
            stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    content.getList("users", Document.class), "_id", userId
            );

        try {
            return generateSuccessMsg("data", Utility.convert(
                    content, isAdmin,isAdmin || stdDoc != null, true, stdDoc, true
                    )
            );
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getSessions(
            boolean isAdmin, ObjectId userId,
            String slug, ObjectId sessionId
    ) {

        Document content = contentRepository.findBySecKey(slug);
        if(content == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = content.getList("sessions", Document.class);

        Document wantedSession = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                sessions, "_id", sessionId
        );

        if(wantedSession == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();
        boolean afterBuy = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                content.getList("users", Document.class), "_id", userId
        ) != -1;

        for(Document session : sessions) {

            if(!wantedSession.getString("chapter").equalsIgnoreCase(session.getString("chapter")))
                continue;

            JSONObject jsonObject = Utility.sessionDigest(
                    session, isAdmin, afterBuy
            );

            jsonObject.put("selected", session.getObjectId("_id").equals(sessionId));
            jsonArray.put(jsonObject);
        }

        JSONObject data = new JSONObject().put("sessions", jsonArray);

        Document config = contentConfigRepository.findBySecKey("first");
        if(config != null) {
            List<Document> advs = config.getList("advs", Document.class);

            List<Document> visible_advs = new ArrayList<>();
            for(Document itr : advs) {
                if(itr.getBoolean("visibility"))
                    visible_advs.add(itr);
            }

            if(visible_advs.size() > 0) {
                int idx = irysc.gachesefid.Utility.Utility.getRandIntForGift(visible_advs.size());
                data.put("adv", STATICS_SERVER + ContentRepository.FOLDER + "/" + visible_advs.get(idx).getString("file"));
            }
        }


        return generateSuccessMsg("data", data);
    }

    public static Document registry(ObjectId contentId, ObjectId userId,
                                  double paid, String phone, String mail) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return null;

        List<Document> users = content.getList("users", Document.class);
        Document tmp = new Document("_id", userId)
                .append("paid", paid).append("register_at", System.currentTimeMillis());

        users.add(tmp);

        contentRepository.replaceOne(contentId, content);

        if(mail != null) {
//            new Thread(() -> sendMail(
//                    mail,
//                    SERVER + "recp/" + transaction.getObjectId("_id").toString(),
//                    "successQuiz",
//                    user.getString("first_name") + " " + user.getString("last_name")
//            )).start();
        }

        return tmp;
    }

    public static String buy(ObjectId contentId, JSONObject data, ObjectId userId,
                             double money, String phone, String mail
    ) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return JSON_NOT_VALID_ID;

        if(!content.getBoolean("visibility"))
            return JSON_NOT_ACCESS;

        if(irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                content.getList("users", Document.class), "_id", userId) != -1
        )
            return generateErr("شما قبلا این دوره را خریداری را کرده اید.");

        Document off = null;
        long curr = System.currentTimeMillis();

        if (data != null && data.has("off")) {

            off = validateOffCode(
                    data.getString("off"), userId, curr,
                    OffCodeSections.CONTENT.getName()
            );

            if (off == null)
                return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");

        }

        if (data == null || !data.has("off")) {
            off = findAccountOff(
                    userId, curr, OffCodeSections.CONTENT.getName()
            );
        }

        double shouldPayDouble = content.getInteger("price") * 1.0;
        double offAmount = 0;

        if(off != null) {
            offAmount +=
                    off.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                            shouldPayDouble * off.getInteger("amount") / 100.0 :
                            off.getInteger("amount")
            ;
            shouldPayDouble -= offAmount;
        }

        int shouldPay = (int) shouldPayDouble;

        if (shouldPay - money <= 100) {

            double newUserMoney = money;

            if (shouldPay > 100) {
                newUserMoney -= Math.min(shouldPay, money);
                Document user = userRepository.findById(userId);
                user.put("money", newUserMoney);
                userRepository.replaceOne(userId, user);
            }

            Document finalOff = off;
            double finalOffAmount = offAmount;

            new Thread(() -> {

                Document doc = new Document("user_id", userId)
                        .append("amount", 0)
                        .append("account_money", shouldPay)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.CONTENT.getName())
                        .append("products", contentId);

                if (finalOff != null) {
                    doc.append("off_code", finalOff.getObjectId("_id"));
                    doc.append("off_amount", (int) finalOffAmount);
                }

                transactionRepository.insertOne(doc);
                registry(contentId, userId, shouldPay, phone, mail);

                if (finalOff != null) {

                    BasicDBObject update;

                    if (finalOff.containsKey("is_public") &&
                            finalOff.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = finalOff.getList("students", ObjectId.class);
                        students.add(userId);
                        update = new BasicDBObject("students", students);
                    } else {

                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.GACH_EXAM.getName())
                                .append("used_for", userId);
                    }

                    offcodeRepository.updateOne(
                            finalOff.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }


            }).start();

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "action", "success",
                    new PairValue("refId", newUserMoney)
            );
        }


        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document doc =
                new Document("user_id", userId)
                        .append("account_money", money)
                        .append("amount", (int) (shouldPay - money))
                        .append("created_at", curr)
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("products", contentId)
                        .append("section", OffCodeSections.CONTENT.getName());

        if (off != null) {
            doc.append("off_code", off.getObjectId("_id"));
            doc.append("off_amount", (int) offAmount);
        }

        return goToPayment((int) (shouldPay - money), doc);

    }

    public static String startFinalQuiz(ObjectId contentId, ObjectId userId) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return JSON_NOT_VALID_ID;

        if(!content.containsKey("final_exam_id"))
            return JSON_NOT_ACCESS;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if(std == null)
            return JSON_NOT_ACCESS;

        if(std.containsKey("start_at"))
            return generateErr("شما یکبار در آزمون پایان دوره شرکت کرده اید.");

        Document quiz = contentQuizRepository.findById(
                content.getObjectId("final_exam_id")
        );

        if(quiz == null)
            return JSON_NOT_UNKNOWN;

        int neededTime = ContentQuizController.calcLenStatic(quiz);
        long curr = System.currentTimeMillis();

        if(!std.containsKey("start_at")) {

            Document stdDoc = new Document("start_at", curr)
                    .append("finish_at", null)
                    .append("_id", userId)
                    .append("expire_at", curr + neededTime * 1000L)
                    .append("answers", new byte[0]);

            if ((boolean) quiz.getOrDefault("permute", false))
                stdDoc.append("question_indices", new ArrayList<>());

            quiz.getList("students", Document.class).add(stdDoc);
            quiz.put("registered", quiz.getInteger("registered") + 1);
            contentQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);

            std.put("start_at", curr);
            contentRepository.replaceOne(contentId, content);
        }

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("mode", quiz.getString("mode"))
                .put("refresh", 2)
                .put("duration", neededTime)
                .put("reminder", neededTime)
                .put("isNewPerson", true);

        List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (String attach : attaches)
            jsonArray.put(STATICS_SERVER + ContentQuizRepository.FOLDER + "/" + attach);

        quizJSON.put("attaches", jsonArray);

        return StudentQuizController.returnQuiz(quiz, std, false, quizJSON);
    }


    public static String storeAnswers(ObjectId quizId, ObjectId studentId, JSONArray answers) {

        long allowedDelay = 1800000; // 0.5hour

        try {
            Document quiz = contentQuizRepository.findById(quizId);
            if(quiz == null)
                return JSON_NOT_VALID_ID;

            Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class), "_id", studentId
            );

            if(stdDoc == null)
                return JSON_NOT_ACCESS;

            long curr = System.currentTimeMillis();
            if(curr > stdDoc.getLong("expire_at") + allowedDelay)
                return generateErr("فرصت شرکت در آزمون به اتمام رسیده است.");

            int neededTime = QuizAbstract.calcLenStatic(quiz);
            long startAt = stdDoc.getLong("start_at");

            int reminder = neededTime - (int) ((curr - startAt) / 1000);

            stdDoc.put("finish_at", curr);
            quiz.put("last_finished_at", curr);

            String result = saveStudentAnswers(quiz, answers, stdDoc, contentQuizRepository);
            if (result.contains("nok"))
                return result;

            return generateSuccessMsg("reminder", reminder,
                    new PairValue("refresh", Math.abs(new Random().nextInt(3)) + 3)
            );

        } catch (Exception x) {
            return null;
        }
    }


    public static String reviewFinalQuiz(ObjectId contentId, ObjectId userId) {

        Document content = contentRepository.findById(contentId);

        if(content == null)
            return JSON_NOT_VALID_ID;

        if(!content.containsKey("final_exam_id"))
            return JSON_NOT_ACCESS;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if(std == null)
            return JSON_NOT_ACCESS;

        if(!std.containsKey("start_at"))
            return generateErr("شما در این آزمون شرکت نکرده اید.");

        Document quiz = contentQuizRepository.findById(
                content.getObjectId("final_exam_id")
        );

        if(quiz == null)
            return JSON_NOT_UNKNOWN;

        int neededTime = ContentQuizController.calcLenStatic(quiz);

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                .put("mode", quiz.getString("mode"))
                .put("duration", neededTime);

        List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (String attach : attaches)
            jsonArray.put(STATICS_SERVER + ContentQuizRepository.FOLDER + "/" + attach);

        quizJSON.put("attaches", jsonArray);
        Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", userId
        );

        return StudentQuizController.returnQuiz(quiz, stdDoc, true, quizJSON);
    }

}
