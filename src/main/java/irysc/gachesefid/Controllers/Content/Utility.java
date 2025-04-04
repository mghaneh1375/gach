package irysc.gachesefid.Controllers.Content;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.OffCodeTypes;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static irysc.gachesefid.Controllers.Certification.AdminCertification.*;
import static irysc.gachesefid.Controllers.Quiz.AdminReportController.buildContentQuizTaraz;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.StaticValues.VIDEO_STATICS_SERVER;

public class Utility {

    static Document returnIfNoRegistry(ObjectId id) throws InvalidFieldsException {

        Document doc = contentRepository.findById(id);
        if (doc == null)
            throw new InvalidFieldsException("id is not valid");

        if (doc.getList("users", Document.class).size() > 0)
            throw new InvalidFieldsException("بسته موردنظر خریده شده است و این امکان وجود ندارد.");

        return doc;

    }

    static JSONObject convertFAQDigest(Document item, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("question", item.get("question"))
                .put("answer", item.get("answer"));

        if (isAdmin) {
            jsonObject.put("id", item.getObjectId("_id").toString())
                    .put("priority", item.get("priority"))
                    .put("visibility", item.get("visibility"));
        }

        return jsonObject;
    }

    public static JSONObject convertDigest(Document doc, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("priority", doc.get("priority"))
                .put("rate", doc.getOrDefault("rate", 5))
                .put("tags", doc.get("tags"))
                .put("id", doc.getObjectId("_id").toString())
                .put("level", doc.containsKey("level") ? doc.get("level", Document.class).getString("title") : "")
                .put("teacher", doc.getString("teacher").split("__"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("certDuration", doc.getOrDefault("cert_duration", ""))
                .put("duration", doc.getInteger("duration"))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if (!isAdmin && doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if (doc.containsKey("off")) {

            long curr = System.currentTimeMillis();

            if (doc.getLong("off_start") <= curr && doc.getLong("off_expiration") >= curr) {

                int val = doc.getInteger("off");
                String type = doc.getString("off_type");

                int offAmount = type.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()) ?
                        (doc.getInteger("price") * val) / 100 : val;

                jsonObject.put("off", val)
                        .put("offType", type)
                        .put("afterOff", doc.getInteger("price") - offAmount);

            }

        }

        if (isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("createdAt", irysc.gachesefid.Utility.Utility.getSolarDate(doc.getLong("created_at")))
                    .put("buyers", doc.getList("users", Document.class).size());
        }

        return jsonObject;
    }

    static JSONObject convert(Document doc, boolean isAdmin, boolean afterBuy,
                              boolean includeFAQ, Document stdDoc, boolean isSessionsNeeded,
                              Document user
    ) throws InvalidFieldsException {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("description", doc.getString("description"))
                .put("teacherBio", doc.getOrDefault("teacher_bio", ""))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("rate", doc.getOrDefault("rate", 5))
                .put("tags", doc.getOrDefault("tags", new ArrayList<>()))
                .put("id", doc.getObjectId("_id").toString())
                .put("level", doc.containsKey("level") ? doc.get("level", Document.class).getString("title") : "")
                .put("icon", doc.containsKey("level") ? doc.get("level", Document.class).getString("icon") : "")
                .put("teacher", doc.getString("teacher").split("__"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("certDuration", doc.getOrDefault("cert_duration", ""))
                .put("duration", doc.getInteger("duration"))
                .put("preReq", doc.get("pre_req"))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        List<Document> students = doc.getList("users", Document.class);

        if (students.size() > 10)
            jsonObject.put("buyers", students.size());

        JSONArray lastBuyers = new JSONArray();
        ArrayList<Document> buyers = userRepository.findByIds(
                students
                        .stream()
                        .sorted((o1, o2) -> o2.getLong("register_at").compareTo(o1.getLong("register_at")))
                        .limit(4)
                        .map(document -> document.getObjectId("_id"))
                        .collect(Collectors.toList()),
                false,
                new BasicDBObject("first_name", 1).append("last_name", 1).append("pic", 1)
        );

        if(buyers != null) {
            buyers.forEach(buyer ->
                    lastBuyers.put(new JSONObject()
                            .put("id", buyer.getObjectId("_id").toString())
                            .put("name", buyer.getString("first_name") + " " + buyer.getString("last_name"))
                            .put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + buyer.getString("pic"))
                    ));
        }

        jsonObject.put("lastBuyers", lastBuyers);

        if (includeFAQ) {
            Document config = contentConfigRepository.findBySecKey("first");
            if (config != null) {

                List<Document> faqs = config.getList("faq", Document.class);
                JSONArray faqJSON = new JSONArray();

                for (Document faq : faqs) {

                    if (!faq.getBoolean("visibility") && !isAdmin)
                        continue;

                    faqJSON.put(convertFAQDigest(faq, isAdmin));
                }

                jsonObject.put("faqs", faqJSON);
            }
        }

        if (doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if (!afterBuy && doc.containsKey("off")) {

            long curr = System.currentTimeMillis();

            if (doc.getLong("off_start") <= curr && doc.getLong("off_expiration") >= curr) {

                int val = doc.getInteger("off");
                String type = doc.getString("off_type");

                int offAmount = type.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()) ?
                        (doc.getInteger("price") * val) / 100 : val;

                jsonObject.put("off", val)
                        .put("offType", type)
                        .put("afterOff", doc.getInteger("price") - offAmount);

            }

        }

        if (afterBuy && stdDoc != null && stdDoc.containsKey("rate"))
            jsonObject.put("stdRate", stdDoc.get("rate"));

        if (afterBuy && doc.containsKey("final_exam_id") && stdDoc != null) {

            ObjectId quizId = doc.getObjectId("final_exam_id");
            long curr = System.currentTimeMillis();

            if (stdDoc.containsKey("start_at") &&
                    !stdDoc.containsKey("check_cert") &&
                    doc.containsKey("cert_id")
            ) {

                boolean needCheck = true;
                int diffDay = 0;

                if (doc.containsKey("cert_duration")) {

                    long registerAt = stdDoc.getLong("register_at");
                    diffDay = (int) Math.floor((curr - registerAt) * 1.0 / ONE_DAY_MIL_SEC);

                    if (diffDay > doc.getInteger("cert_duration")) {
                        stdDoc.put("check_cert", false);
                        contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                        needCheck = false;
                    }

                }

                if (needCheck) {

                    double percent = -1;

                    if (doc.containsKey("final_exam_min_mark")) {

                        Document quiz = contentQuizRepository.findById(quizId);
                        if (quiz == null)
                            throw new InvalidFieldsException("unknown2");

                        ObjectId userId = stdDoc.getObjectId("_id");

                        Document stdDocInQuiz = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                                quiz.getList("students", Document.class), "_id", userId
                        );

                        if (stdDocInQuiz == null)
                            throw new InvalidFieldsException("unknown3");

                        if (!stdDocInQuiz.containsKey("last_build")) {

                            stdDocInQuiz.put("last_build", curr);
                            buildContentQuizTaraz(quiz, stdDocInQuiz);

                            quiz = contentQuizRepository.findById(quizId);
                            stdDocInQuiz = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                                    quiz.getList("students", Document.class), "_id", userId
                            );

                            int totalCorrect = 0;
                            int totalInCorrect = 0;
                            int totalWhites = 0;

                            for (Document lesson : stdDocInQuiz.getList("lessons", Document.class)) {
                                Object[] stats = QuizAbstract.decodeCustomQuiz(lesson.get("stat", Binary.class).getData());
                                totalWhites += (int) stats[0];
                                totalCorrect += (int) stats[1];
                                totalInCorrect += (int) stats[2];
                            }

                            if ((boolean) quiz.getOrDefault("minus_mark", true))
                                percent = ((totalCorrect * 3.0 - totalInCorrect) * 100.0) / (3.0 * (totalInCorrect + totalWhites + totalCorrect));
                            else
                                percent = totalCorrect * 100.0 / (totalInCorrect + totalWhites + totalCorrect);

                            stdDoc.put("check_cert", percent >= doc.getInteger("final_exam_min_mark"));
                            contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                        }
                    } else {
                        stdDoc.put("check_cert", true);
                        contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                    }

                    if ((boolean) stdDoc.getOrDefault("check_cert", false)) {

                        JSONObject params = new JSONObject()
                                .put("std_name", user.getString("first_name") + " " + user.getString("last_name"))
                                .put("NID", user.getString("NID"))
                                .put("cert_title", doc.getString("title"))
                                .put("issue_at", irysc.gachesefid.Utility.Utility.getToday("/"))
                                .put("diff_day", diffDay + "");

                        if (percent != -1)
                            params.put("mark", String.format("%.2f", percent / 5));

                        addUserToContentCert(doc.getObjectId("cert_id"), params);

                    }

                }


            }

            jsonObject
                    .put("quizStatus", stdDoc.containsKey("start_at") ? "result" : "start")
                    .put("finalExamId", quizId.toString())
            ;

            if ((boolean) stdDoc.getOrDefault("check_cert", false))
                jsonObject.put("certStatus", "ready")
                        .put("certId", doc.getObjectId("cert_id").toString());

        }

        if (isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("priority", doc.getOrDefault("priority", 1))
                    .put("finalExamMinMark", doc.getOrDefault("final_exam_min_mark", -1))
                    .put("buyers", doc.getList("users", Document.class).size());

            if (doc.containsKey("off")) {
                jsonObject.put("off", doc.get("off"));
                jsonObject.put("offType", doc.get("off_type"));
                jsonObject.put("offExpiration", doc.get("off_expiration"));
                jsonObject.put("offStart", doc.get("off_start"));
            }

            if (doc.containsKey("cert_id"))
                jsonObject.put("certId", doc.getObjectId("cert_id").toString());

            if (doc.containsKey("final_exam_id"))
                jsonObject.put("finalExamId", doc.getObjectId("final_exam_id").toString());

            if (doc.containsKey("final_exam_min_mark"))
                jsonObject.put("finalExamMinMark", doc.get("final_exam_min_mark"));

        } else
            jsonObject.put("afterBuy", afterBuy);

        if (isSessionsNeeded) {

            List<Document> sessions = doc.getList("sessions", Document.class);
            JSONArray sessionsJSON = new JSONArray();

            for (Document session : sessions) {
                if (!isAdmin && !afterBuy && !session.getBoolean("visibility")) continue;
                sessionsJSON.put(
                        sessionDigest(session, false, afterBuy, doc.getInteger("price") == 0, true, null)
                );
            }

            jsonObject.put("sessions", sessionsJSON);
        }

        List<Document> chapters = (List<Document>) doc.getOrDefault("chapters", new ArrayList<>());
        JSONArray chaptersJSON = new JSONArray();

        for (Document chapter : chapters)
            chaptersJSON.put(chapterDigest(chapter));

        jsonObject.put("chapters", chaptersJSON);

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
                jsonObject.put("adv", STATICS_SERVER + ContentRepository.FOLDER + "/" + visible_advs.get(idx).getString("file"));
            }
        }

        return jsonObject;
    }

    static JSONObject sessionDigest(
            Document doc, boolean isAdmin,
            boolean afterBuy, boolean isFree, boolean returnFree,
            ObjectId userId
    ) {

        if (doc == null)
            return new JSONObject();

        List<String> attaches = doc.containsKey("attaches") ? doc.getList("attaches", String.class)
                : new ArrayList<>();

        JSONObject jsonObject = new JSONObject()
                .put("id", doc.getObjectId("_id").toString())
                .put("title", doc.get("title"))
                .put("duration", doc.get("duration"))
                .put("chapter", doc.get("chapter"))
                .put("price", doc.getOrDefault("price", -1))
                .put("description", doc.get("description"))
                .put("attachesCount", attaches.size());

        if (doc.containsKey("exam_id") && (afterBuy || isAdmin)) {
            jsonObject
                    .put("examId", doc.getObjectId("exam_id").toString())
                    .put("hasExam", true)
                    .put("minMark", doc.getOrDefault("min_mark", -1))
            ;

            Document exam = contentQuizRepository.findById(doc.getObjectId("exam_id"));
            boolean canDoQuiz = false;

            if (exam != null)
                canDoQuiz = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        exam.getList("students", Document.class), "_id", userId
                ) == -1;

            jsonObject.put("canDoQuiz", canDoQuiz);
        } else
            jsonObject.put("hasExam", false);

        if (isAdmin)
            jsonObject.put("visibility", doc.get("visibility"))
                    .put("priority", doc.get("priority"))
                    .put("hasVideo", doc.containsKey("video") && doc.get("video") != null);

        if (isAdmin || afterBuy || doc.getInteger("price") == 0 || isFree) {

            if (!returnFree && !afterBuy && isFree && doc.getInteger("price") > 0) {
                return jsonObject;
            }

            JSONArray attachesJSONArr = new JSONArray();
            for (String itr : attaches)
                attachesJSONArr.put(STATICS_SERVER + ContentRepository.FOLDER + "/" + itr);

            jsonObject.put("attaches", attachesJSONArr);
            String video = null;

            if (isAdmin)
                video = doc.containsKey("video") ? doc.getString("video") : null;
            else if (doc.containsKey("chunk_at") && doc.containsKey("video")) {
                if (!(Boolean) doc.getOrDefault("external_link", false)) {
                    String folderName = doc.getString("video").split("\\.mp4")[0];
                    video = VIDEO_STATICS_SERVER + "videos/" + folderName + "/playlist.m3u8";
                } else
                    video = doc.getString("video");
            }

            jsonObject.put("video", video);
        }

        return jsonObject;

    }

    static JSONObject chapterDigest(Document doc) {
        return new JSONObject()
                .put("title", doc.get("title"))
                .put("duration", doc.get("duration"))
                .put("sessions", doc.get("sessions"))
                .put("price", doc.getOrDefault("price", -1))
                .put("description", doc.get("desc"));
    }

}
