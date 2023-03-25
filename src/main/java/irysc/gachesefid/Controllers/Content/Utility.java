package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.DB.ContentRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.OffCodeTypes;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Controllers.Certification.AdminCertification.addUserToCert;
import static irysc.gachesefid.Controllers.Quiz.AdminReportController.buildContentQuizTaraz;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.STATICS_SERVER;
import static irysc.gachesefid.Utility.StaticValues.VIDEO_STATICS_SERVER;
import static irysc.gachesefid.Utility.Utility.generateErr;

public class Utility {

    static Document returnIfNoRegistry(ObjectId id) throws InvalidFieldsException {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            throw new InvalidFieldsException("id is not valid");

        if(doc.getList("users", Document.class).size() > 0)
            throw new InvalidFieldsException("بسته موردنظر خریده شده است و این امکان وجود ندارد.");

        return doc;

    }

    static JSONObject convertFAQDigest(Document item, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("question", item.get("question"))
                .put("answer", item.get("answer"));

        if(isAdmin) {
            jsonObject.put("id", item.getObjectId("_id").toString())
                    .put("priority", item.get("priority"))
                    .put("visibility", item.get("visibility"));
        }

        return jsonObject;
    }

    static JSONObject convertDigest(Document doc, boolean isAdmin) {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("priority", doc.get("priority"))
                .put("rate", doc.getOrDefault("rate", 5))
                .put("tags", doc.get("tags"))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("certDuration", doc.getOrDefault("cert_duration", ""))
                .put("duration", isAdmin ? doc.getInteger("duration") : doc.getInteger("duration") * 60)
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if(!isAdmin && doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if(doc.containsKey("off")) {

            long curr = System.currentTimeMillis();

            if(doc.getLong("off_start") <= curr && doc.getLong("off_expiration") >= curr) {

                int val = doc.getInteger("off");
                String type = doc.getString("off_type");

                int offAmount = type.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()) ?
                        (doc.getInteger("price") * val) / 100 : val;

                jsonObject.put("off", val)
                        .put("offType", type)
                        .put("afterOff", doc.getInteger("price") - offAmount);

            }

        }

        if(isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("createdAt", irysc.gachesefid.Utility.Utility.getSolarDate(doc.getLong("created_at")))
                    .put("buyers", doc.getList("users", Document.class).size());
        }

        return jsonObject;
    }

    static JSONObject convert(Document doc, boolean isAdmin, boolean afterBuy,
                              boolean includeFAQ, Document stdDoc, boolean isSessionsNeeded) throws InvalidFieldsException {

        JSONObject jsonObject = new JSONObject()
                .put("price", doc.get("price"))
                .put("description", doc.getString("description"))
                .put("teacherBio", doc.getOrDefault("teacher_bio", ""))
                .put("title", doc.get("title"))
                .put("slug", doc.get("slug"))
                .put("rate", doc.getOrDefault("rate", 5))
                .put("tags", doc.getOrDefault("tags", new ArrayList<>()))
                .put("id", doc.getObjectId("_id").toString())
                .put("teacher", doc.getString("teacher"))
                .put("hasFinalExam", doc.containsKey("final_exam_id"))
                .put("hasCert", doc.containsKey("cert_id"))
                .put("certDuration", doc.getOrDefault("duration", ""))
                .put("duration", isAdmin ? doc.getInteger("duration") : doc.getInteger("duration") * 60)
                .put("preReq", doc.get("pre_req"))
                .put("sessionsCount", doc.getInteger("sessions_count"));

        if(includeFAQ) {
            Document config = contentConfigRepository.findBySecKey("first");
            if(config != null) {

                List<Document> faqs = config.getList("faq", Document.class);
                JSONArray faqJSON = new JSONArray();

                for(Document faq : faqs) {

                    if(!faq.getBoolean("visibility") && !isAdmin)
                        continue;

                    faqJSON.put(convertFAQDigest(faq, isAdmin));
                }

                jsonObject.put("faqs", faqJSON);
            }
        }

        if(doc.containsKey("img"))
            jsonObject.put("img", STATICS_SERVER + ContentRepository.FOLDER + "/" + doc.get("img"));

        if(!afterBuy && doc.containsKey("off")) {

            long curr = System.currentTimeMillis();

            if(doc.getLong("off_start") <= curr && doc.getLong("off_expiration") >= curr) {

                int val = doc.getInteger("off");
                String type = doc.getString("off_type");

                int offAmount = type.equalsIgnoreCase(OffCodeTypes.PERCENT.getName()) ?
                        (doc.getInteger("price") * val) / 100 : val;

                jsonObject.put("off", val)
                        .put("offType", type)
                        .put("afterOff", doc.getInteger("price") - offAmount);

            }

        }

        if(afterBuy && stdDoc != null && stdDoc.containsKey("rate"))
            jsonObject.put("stdRate", stdDoc.get("rate"));

        if(afterBuy && doc.containsKey("final_exam_id") && stdDoc != null) {

            ObjectId quizId = doc.getObjectId("final_exam_id");
            long curr = System.currentTimeMillis();
            boolean oldCert = (boolean)stdDoc.getOrDefault("check_cert", false);

            if(stdDoc.containsKey("start_at") &&
                    !stdDoc.containsKey("check_cert") &&
                    doc.containsKey("cert_id")
            ) {

                boolean needCheck = true;

                if(doc.containsKey("cert_duration")) {

                    long registerAt = stdDoc.getLong("register_at");
                    int diffDay = (int) Math.floor((curr - registerAt) * 1.0 / ONE_DAY_MIL_SEC);

                    if(diffDay > doc.getInteger("cert_duration")) {
                        stdDoc.put("check_cert", false);
                        contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                        needCheck = false;
                    }

                }

                if(needCheck) {

                    if(doc.containsKey("final_exam_min_mark")) {

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

                            int totalCorrect = 0;
                            int totalInCorrect = 0;
                            int totalWhites = 0;

                            for (Document lesson : stdDocInQuiz.getList("lessons", Document.class)) {
                                Object[] stats = QuizAbstract.decodeCustomQuiz(lesson.get("stat", Binary.class).getData());
                                totalWhites += (int) stats[0];
                                totalCorrect += (int) stats[1];
                                totalInCorrect += (int) stats[2];
                            }

                            double percent;
                            if ((boolean) quiz.getOrDefault("minus_mark", true))
                                percent = ((totalCorrect * 3.0 - totalInCorrect) * 100.0) / (totalInCorrect + totalWhites + totalCorrect);
                            else
                                percent = totalCorrect * 100.0 / (totalInCorrect + totalWhites + totalCorrect);

                            stdDoc.put("check_cert", percent >= doc.getInteger("final_exam_min_mark"));
                            contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                        }
                    }
                    else {
                        stdDoc.put("check_cert", true);
                        contentRepository.replaceOne(doc.getObjectId("_id"), doc);
                    }
                }
            }

            jsonObject
                    .put("quizStatus", stdDoc.containsKey("start_at") ? "result" : "start")
                    .put("finalExamId", quizId.toString())
            ;

            if((boolean)stdDoc.getOrDefault("check_cert", false)) {
                if(!oldCert) {

//                    JSONArray params = new JSONArray();
//                    params.put(user.getString("first_name") + " " + user.getString("last_name"));
//                    params.put(doc.getString("title"));
//                    params.put(stdDoc.getLong(""));
//                    params.put(rank + "");

//                    addUserToCert(null, doc.getObjectId("cert_id"),
//                            NID, params);
                }

                jsonObject.put("certStatus", "ready");
            }

        }

        if(isAdmin) {
            jsonObject.put("visibility", doc.getBoolean("visibility"))
                    .put("priority", doc.getOrDefault("priority", 1))
                    .put("finalExamMinMark", doc.getOrDefault("final_exam_min_mark", -1))
                    .put("buyers", doc.getList("users", Document.class).size());

            if(doc.containsKey("off")) {
                jsonObject.put("off", doc.get("off"));
                jsonObject.put("offType", doc.get("off_type"));
                jsonObject.put("offExpiration", doc.get("off_expiration"));
                jsonObject.put("offStart", doc.get("off_start"));
            }

            if(doc.containsKey("cert_id"))
                jsonObject.put("certId", doc.getObjectId("cert_id").toString());

            if(doc.containsKey("final_exam_id"))
                jsonObject.put("finalExamId", doc.getObjectId("final_exam_id").toString());

            if(doc.containsKey("final_exam_min_mark"))
                jsonObject.put("finalExamMinMark", doc.get("final_exam_min_mark"));

        }
        else
            jsonObject.put("afterBuy", afterBuy);

        if(isSessionsNeeded) {

            List<Document> sessions = doc.getList("sessions", Document.class);
            JSONArray sessionsJSON = new JSONArray();

            for (Document session : sessions) {
                if(!isAdmin && !afterBuy && !session.getBoolean("visibility")) continue;
                sessionsJSON.put(sessionDigest(session, false, afterBuy, doc.getInteger("price") == 0, true));
            }

            jsonObject.put("sessions", sessionsJSON);
        }

        List<Document> chapters = (List<Document>) doc.getOrDefault("chapters", new ArrayList<>());
        JSONArray chaptersJSON = new JSONArray();

        for (Document chapter : chapters)
            chaptersJSON.put(chapterDigest(chapter));

        jsonObject.put("chapters", chaptersJSON);

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
                jsonObject.put("adv", STATICS_SERVER + ContentRepository.FOLDER + "/" + visible_advs.get(idx).getString("file"));
            }
        }

        return jsonObject;
    }

    static JSONObject sessionDigest(Document doc, boolean isAdmin,
                                    boolean afterBuy, boolean isFree, boolean returnFree) {

        if(doc == null)
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

        if(doc.containsKey("exam_id")) {
            jsonObject
                    .put("examId", doc.getObjectId("exam_id").toString())
                    .put("hasExam", true)
                    .put("minMark", doc.getOrDefault("min_mark", -1))
            ;
        }
        else
            jsonObject.put("hasExam", false);

        if(isAdmin)
            jsonObject.put("visibility", doc.get("visibility"))
                    .put("priority", doc.get("priority"))
                    .put("hasVideo", doc.containsKey("video") && doc.get("video") != null);

        if(isAdmin || afterBuy || doc.getInteger("price") == 0 || isFree) {

            if(!returnFree && !afterBuy && isFree && doc.getInteger("price") > 0) {
                return jsonObject;
            }

            JSONArray attachesJSONArr = new JSONArray();
            for(String itr : attaches)
                attachesJSONArr.put(itr);

            jsonObject.put("attaches", attachesJSONArr);
            String video = null;

            if(isAdmin)
                video = doc.containsKey("video") ? doc.getString("video") : null;
            else if(doc.containsKey("chunk_at") && doc.containsKey("video")) {
                String folderName = doc.getString("video").split("\\.mp4")[0];
                video = VIDEO_STATICS_SERVER + "videos/" + folderName + "/playlist.m3u8";
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
