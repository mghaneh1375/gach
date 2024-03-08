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

    public static String changeTeacherName(JSONObject jsonObject) {

        String oldName = jsonObject.getString("oldName");
        String newName = jsonObject.getString("newName");

        List<Document> docs = contentRepository.find(
                regex("teacher", Pattern.compile(Pattern.quote(oldName))),
                new BasicDBObject("teacher", 1)
        );

        for (Document doc : docs) {

            List<String> newList = new ArrayList<>();
            String[] splited = doc.getString("teacher").split("__");

            for (String itr : splited) {

                if (itr.equals(oldName))
                    newList.add(newName);
                else
                    newList.add(itr);

            }

            Document d = contentRepository.findById(doc.getObjectId("_id"));
            d.put("teacher", String.join("__", newList));
            contentRepository.replaceOneWithoutClearCache(doc.getObjectId("_id"), d);
        }

        return JSON_OK;
    }

    public static String distinctTags() {
        return generateSuccessMsg("data", contentRepository.distinctTags("tags"));
    }

    public static String chapters(ObjectId contentId) {

        Document doc = contentRepository.findById(contentId);
        if (doc == null)
            return JSON_NOT_VALID_ID;

        List<Document> chapters = (List<Document>) doc.getOrDefault("chapters", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (Document chapter : chapters) {
            jsonArray.put(new JSONObject()
                    .put("title", chapter.getString("title"))
                    .put("desc", chapter.getString("desc"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String distinctTeachers() {
        return generateSuccessMsg("data", findDistinctTeachers());
    }

    public static String teacherPackages(String teacher) {

        List<Document> docs = contentRepository.find(
                regex("teacher", Pattern.compile(Pattern.quote(teacher))),
                CONTENT_DIGEST,
                Sorts.ascending("priority")
        );

        JSONArray data = new JSONArray();

        for (Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false));

        Document doc = contentRepository.findOne(and(
                regex("teacher", Pattern.compile("^" + teacher + "(__|$)")),
                eq("visibility", true),
                exists("teacher_bio")
        ), new BasicDBObject("teacher_bio", 1));

        return generateSuccessMsg("data", new JSONObject()
                .put("packages", data).put("bio", doc == null ? "" : doc.getString("teacher_bio"))
        );
    }

    public static String getTeacherBio(String teacher) {

        Document doc = contentRepository.findOne(and(
                regex("teacher", Pattern.compile("^" + teacher + "(__|$)")),
                eq("visibility", true),
                exists("teacher_bio")
        ), new BasicDBObject("teacher_bio", 1));

        if (doc == null)
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

        if (!isAdmin)
            filters.add(eq("visibility", true));

        if (!isAdmin && userId != null)
            filters.add(nin("users._id", userId));

        if (title != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(title), Pattern.CASE_INSENSITIVE)));

        if (tag != null)
            filters.add(in("tags", tag));

        if (hasCert != null)
            filters.add(exists("cert_id", hasCert));

        if (minPrice != null)
            filters.add(gte("price", minPrice));

        if (maxPrice != null)
            filters.add(lte("price", maxPrice));

        if (minDurationFilter != null)
            filters.add(gte("duration", minDurationFilter));

        if (maxDurationFilter != null)
            filters.add(lte("duration", maxDurationFilter));

        if (teacher != null)
            filters.add(regex("teacher", Pattern.compile(Pattern.quote(teacher))));

        if (isAdmin) {
            if (visibility != null)
                filters.add(eq("visibility", visibility));
        }

        JSONArray data = new JSONArray();

        ArrayList<Document> docs = contentRepository.find(
                filters.size() == 0 ? null : and(filters),
                isAdmin ? CONTENT_DIGEST_FOR_ADMIN : CONTENT_DIGEST,
                Sorts.ascending("priority")
        );

        if (!isAdmin && filters.size() <= 2) {

            int min = 1000000000;
            int max = -1;

            int maxDuration = -1;
            int minDuration = 10000000;

            for (Document doc : docs) {

                int price = doc.getInteger("price");
                int duration = doc.getInteger("duration");

                if (price < min)
                    min = price;

                if (price > max)
                    max = price;

                if (duration < minDuration)
                    minDuration = duration;

                if (duration > maxDuration)
                    maxDuration = duration;

                data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false));
            }

            return generateSuccessMsg("data", data,
                    new PairValue("min", min),
                    new PairValue("max", max),
                    new PairValue("minDuration", minDuration),
                    new PairValue("maxDuration", maxDuration),
                    new PairValue("tags", contentRepository.distinctTags("tags")),
                    new PairValue("teachers", findDistinctTeachers())
            );
        }

        for (Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));

        return generateSuccessMsg("data", data);
    }

    private static List<String> findDistinctTeachers() {

        JSONArray jsonArray = contentRepository.distinctTags("teacher");
        List<String> distincts = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            String[] splited = jsonArray.getString(i).split("__");
            for (String itr : splited) {
                if (!distincts.contains(itr))
                    distincts.add(itr);
            }

        }

        return distincts;
    }


    public static String getMy(ObjectId userId) {
        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                in("users._id", userId),
                CONTENT_DIGEST
        );

        for (Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, false));

        return generateSuccessMsg("data", data);
    }


    public static String rate(ObjectId contentId, ObjectId userId, int rate) {

        if (rate <= 0 || rate > 5)
            return JSON_NOT_VALID_PARAMS;

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        Document stdDoc = searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if (stdDoc == null)
            return JSON_NOT_ACCESS;

        int oldRate = (int) stdDoc.getOrDefault("rate", 0);
        stdDoc.put("rate", rate);
        stdDoc.put("rate_at", System.currentTimeMillis());

        double oldTotalRate = (double) content.getOrDefault("rate", (double) 0);
        int rateCount = (int) content.getOrDefault("rate_count", 0);

        oldTotalRate *= rateCount;

        if (oldRate == 0)
            rateCount++;

        oldTotalRate -= oldRate;
        oldTotalRate += rate;

        content.put("rate", Math.round(oldTotalRate / rateCount * 100.0) / 100.0);
        content.put("rate_count", rateCount);

        contentRepository.replaceOneWithoutClearCache(contentId, content);

        return generateSuccessMsg("data", content.get("rate"));
    }


    public static String get(boolean isAdmin, Document user, String slug) {

        Document content = contentRepository.findBySecKey(slug);
        if (content == null)
            return JSON_NOT_VALID_ID;

        Document stdDoc = null;

        ObjectId userId = user == null ? null : user.getObjectId("_id");

        if (!isAdmin && userId != null)
            stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    content.getList("users", Document.class), "_id", userId
            );

        try {

            return generateSuccessMsg("data", Utility.convert(
                    content, isAdmin, isAdmin || stdDoc != null, true, stdDoc, true, user
            ));

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getSessions(
            boolean isAdmin, ObjectId userId,
            String slug, ObjectId sessionId
    ) {

        Document content = contentRepository.findBySecKey(slug);
        if (content == null)
            return JSON_NOT_VALID_ID;

        List<Document> sessions = content.getList("sessions", Document.class);

        Document wantedSession = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                sessions, "_id", sessionId
        );

        if (wantedSession == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();
        List<Document> users = content.getList("users", Document.class);

        boolean afterBuy = userId != null && irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                users, "_id", userId
        ) != -1;

        boolean isFree = content.getInteger("price") == 0;
        if (!afterBuy && !isFree && wantedSession.getInteger("price") > 0)
            return JSON_NOT_ACCESS;

        for (Document session : sessions) {

            if (!wantedSession.getString("chapter").equalsIgnoreCase(session.getString("chapter")))
                continue;

            JSONObject jsonObject = Utility.sessionDigest(
                    session, isAdmin, afterBuy, isFree, false, userId
            );

            jsonObject.put("selected", session.getObjectId("_id").equals(sessionId));
            jsonArray.put(jsonObject);
        }

        if (!afterBuy && userId != null && isFree) {

            users.add(new Document("_id", userId)
                    .append("paid", 0).append("register_at", System.currentTimeMillis()));

            contentRepository.replaceOneWithoutClearCache(content.getObjectId("_id"), content);
        }

        JSONObject data = new JSONObject().put("sessions", jsonArray);

        Document config = contentConfigRepository.findBySecKey("first");
        if (config != null) {
            List<Document> advs = config.getList("advs", Document.class);

            List<Document> visible_advs = new ArrayList<>();
            for (Document itr : advs) {
                if (itr.getBoolean("visibility"))
                    visible_advs.add(itr);
            }

            if (visible_advs.size() > 0) {
                int idx = irysc.gachesefid.Utility.Utility.getRandIntForGift(visible_advs.size());
                data.put("adv", STATICS_SERVER + ContentRepository.FOLDER + "/" + visible_advs.get(idx).getString("file"));
            }
        }

        data.put("contentId", content.getObjectId("_id").toString());

        return generateSuccessMsg("data", data);
    }

    public static Document registry(ObjectId contentId, ObjectId userId,
                                    double paid, String phone, String mail) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return null;

        List<Document> users;
        if (content.size() == 0)
            users = new ArrayList<>();
        else
            users = content.getList("users", Document.class);

        Document tmp = new Document("_id", userId)
                .append("paid", paid)
                .append("register_at", System.currentTimeMillis());

        users.add(tmp);
        content.put("users", users);

        contentRepository.replaceOneWithoutClearCache(contentId, content);

//        if(mail != null) {
////            new Thread(() -> sendMail(
////                    mail,
////                    SERVER + "recp/" + transaction.getObjectId("_id").toString(),
////                    "successQuiz",
////                    user.getString("first_name") + " " + user.getString("last_name")
////            )).start();
//        }

        return tmp;
    }

    public static String buy(ObjectId contentId, JSONObject data, ObjectId userId,
                             double money, String phone, String mail
    ) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        if (!content.getBoolean("visibility"))
            return JSON_NOT_ACCESS;

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
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

        if (content.containsKey("off")) {

            if (content.getLong("off_start") <= curr && content.getLong("off_expiration") >= curr) {

                int val = content.getInteger("off");
                String type = content.getString("off_type");

                int offAmount = type.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()) ?
                        (content.getInteger("price") * val) / 100 : val;

                shouldPayDouble -= offAmount;
            }

        }

        double offAmount = 0;

        if (off != null) {
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
                userRepository.replaceOneWithoutClearCache(userId, user);
            }

            Document doc = new Document("user_id", userId)
                    .append("amount", 0)
                    .append("account_money", shouldPay)
                    .append("created_at", curr)
                    .append("status", "success")
                    .append("section", OffCodeSections.CONTENT.getName())
                    .append("products", contentId);

            if (off != null) {
                doc.append("off_code", off.getObjectId("_id"));
                doc.append("off_amount", (int) offAmount);
            }

            transactionRepository.insertOne(doc);

            registry(contentId, userId, shouldPay, phone, mail);

            if (off != null) {

                BasicDBObject update;

                if (off.containsKey("is_public") &&
                        off.getBoolean("is_public")
                ) {
                    List<ObjectId> students = off.getList("students", ObjectId.class);
                    students.add(userId);
                    update = new BasicDBObject("students", students);
                } else {
                    update = new BasicDBObject("used", true)
                            .append("used_at", curr)
                            .append("used_section", OffCodeSections.CONTENT.getName())
                            .append("used_for", userId);
                }

                offcodeRepository.updateOne(
                        off.getObjectId("_id"),
                        new BasicDBObject("$set", update)
                );
            }


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

    public static String startSessionQuiz(ObjectId contentId, ObjectId sessionId, ObjectId userId) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        Document session = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("sessions", Document.class), "_id", sessionId
        );

        if (session == null || !session.containsKey("exam_id"))
            return JSON_NOT_VALID_ID;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if (std == null)
            return JSON_NOT_ACCESS;

        Document quiz = contentQuizRepository.findById(
                session.getObjectId("exam_id")
        );

        if (quiz == null)
            return JSON_NOT_UNKNOWN;

        List<Document> studentsList = quiz.getList("students", Document.class);

        Document stdInQuiz = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                studentsList, "_id", userId
        );

        if (stdInQuiz != null)
            return generateErr("شما یکبار در آزمون این جلسه شرکت کرده اید.");

        int neededTime = ContentQuizController.calcLenStatic(quiz);
        long curr = System.currentTimeMillis();

        stdInQuiz = new Document("start_at", curr)
                .append("finish_at", null)
                .append("_id", userId)
                .append("expire_at", curr + neededTime * 1000L)
                .append("answers", new byte[0]);

        studentsList.add(stdInQuiz);

        quiz.put("registered", quiz.getInteger("registered") + 1);
        contentQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
                .put("refresh", 2)
                .put("duration", neededTime)
                .put("reminder", neededTime)
                .put("isNewPerson", true);

        List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (String attach : attaches)
            jsonArray.put(STATICS_SERVER + ContentQuizRepository.FOLDER + "/" + attach);

        quizJSON.put("attaches", jsonArray);

        return StudentQuizController.returnQuiz(quiz, null, false, quizJSON);
    }

    public static String startFinalQuiz(ObjectId contentId, ObjectId userId) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        if (!content.containsKey("final_exam_id"))
            return JSON_NOT_ACCESS;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if (std == null)
            return JSON_NOT_ACCESS;

        if (std.containsKey("start_at"))
            return generateErr("شما یکبار در آزمون پایان دوره شرکت کرده اید.");

        Document quiz = contentQuizRepository.findById(
                content.getObjectId("final_exam_id")
        );

        if (quiz == null)
            return JSON_NOT_UNKNOWN;

        int neededTime = ContentQuizController.calcLenStatic(quiz);
        long curr = System.currentTimeMillis();

        if (!std.containsKey("start_at")) {

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
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
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
            if (quiz == null)
                return JSON_NOT_VALID_ID;

            Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    quiz.getList("students", Document.class), "_id", studentId
            );

            if (stdDoc == null)
                return JSON_NOT_ACCESS;

            long curr = System.currentTimeMillis();
            if (curr > stdDoc.getLong("expire_at") + allowedDelay)
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

        if (content == null)
            return JSON_NOT_VALID_ID;

        if (!content.containsKey("final_exam_id"))
            return JSON_NOT_ACCESS;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if (std == null)
            return JSON_NOT_ACCESS;

        if (!std.containsKey("start_at"))
            return generateErr("شما در این آزمون شرکت نکرده اید.");

        Document quiz = contentQuizRepository.findById(
                content.getObjectId("final_exam_id")
        );

        if (quiz == null)
            return JSON_NOT_UNKNOWN;

        int neededTime = ContentQuizController.calcLenStatic(quiz);

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
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

    public static String reviewSessionQuiz(ObjectId contentId, ObjectId sessionId, ObjectId userId) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        Document session = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("sessions", Document.class), "_id", sessionId
        );

        if (session == null || !session.containsKey("exam_id"))
            return JSON_NOT_VALID_ID;

        Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                content.getList("users", Document.class),
                "_id", userId
        );

        if (std == null)
            return JSON_NOT_ACCESS;

        Document quiz = contentQuizRepository.findById(
                session.getObjectId("exam_id")
        );

        if (quiz == null)
            return JSON_NOT_UNKNOWN;

        Document stdInQuiz = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                quiz.getList("students", Document.class), "_id", userId
        );

        if (stdInQuiz == null)
            return generateErr("شما در این آزمون شرکت نکرده اید.");

        int neededTime = ContentQuizController.calcLenStatic(quiz);

        JSONObject quizJSON = new JSONObject()
                .put("title", quiz.getString("title"))
                .put("id", quiz.getObjectId("_id").toString())
                .put("generalMode", "content")
                .put("questionsNo", quiz.get("questions", Document.class).getList("_ids", ObjectId.class).size())
                .put("description", quiz.getOrDefault("description", ""))
                .put("descriptionAfter", quiz.getOrDefault("desc_after", ""))
                .put("mode", quiz.getOrDefault("mode", "regular").toString())
                .put("duration", neededTime);

        List<String> attaches = (List<String>) quiz.getOrDefault("attaches", new ArrayList<>());
        JSONArray jsonArray = new JSONArray();

        for (String attach : attaches)
            jsonArray.put(STATICS_SERVER + ContentQuizRepository.FOLDER + "/" + attach);

        quizJSON.put("attaches", jsonArray);

        return StudentQuizController.returnQuiz(quiz, stdInQuiz, true, quizJSON);
    }

    public static String rates(ObjectId contentId) {

        Document content = contentRepository.findById(contentId);

        if (content == null)
            return JSON_NOT_VALID_ID;

        List<Document> users = content.getList("users", Document.class);
        JSONArray data = new JSONArray();
        long curr = System.currentTimeMillis();

        for (Document user : users) {

            if (!user.containsKey("rate"))
                continue;

            Document std = userRepository.findById(user.getObjectId("_id"));
            if (std == null)
                continue;

            JSONObject jsonObject = new JSONObject()
                    .put("rate", user.get("rate"))
                    .put("rateAt", getSolarDate((Long) user.getOrDefault("rate_at", curr)));

            irysc.gachesefid.Utility.Utility.fillJSONWithUser(jsonObject, user);
            data.put(jsonObject);
        }

        return generateSuccessMsg("data", data);
    }
}
