package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Controllers.Quiz.StudentQuizController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Security.JwtTokenFilter.blackListTokens;
import static irysc.gachesefid.Security.JwtTokenFilter.validateTokens;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.sendSMSWithTemplate;

public class Jobs implements Runnable {

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new TokenHandler(), 0, ONE_DAY_MIL_SEC); // 1 day
        timer.schedule(new QuizReminder(), 0, 3600000); // 1 hour
        timer.schedule(new SiteStatsHandler(), ONE_DAY_MIL_SEC, ONE_DAY_MIL_SEC); // 1 day
        timer.schedule(new RemoveRedundantCustomQuizzes(), 0, 86400000);

        timer.schedule(new RemoveExpiredNotifs(), 0, ONE_DAY_MIL_SEC * 7);
        timer.schedule(new RemoveExpiredMeetings(), 0, ONE_DAY_MIL_SEC * 7);

        timer.schedule(new CheckContentBuys(), 0, ONE_HOUR_MIL_SEC);

        timer.schedule(new SendMails(), 0, 300000);
        timer.schedule(new SendSMS(), 0, 300000);
        timer.schedule(new CalcSubjectQuestions(), 0, 86400000);
    }

    //todo remove redundant transactions
    //todo remove redundant school questions

    class RemoveExpiredNotifs extends TimerTask {

        public void run() {

            long lastMonth = System.currentTimeMillis() - ONE_DAY_MIL_SEC * 30;
            List<Document> users = userRepository.find(and(
                    exists("events.0"),
                    lt("events.0.created_at", lastMonth)
            ), null);

            List<WriteModel<Document>> writes = new ArrayList<>();

            for(Document user : users) {

                List<Document> notifs = user.getList("events", Document.class);
                List<Document> newList = new ArrayList<>();

                for(Document notif : notifs) {

                    if(notif.getLong("created_at") < lastMonth)
                        continue;

                    newList.add(notif);
                }

                if(newList.size() < notifs.size()) {

                    user.put("events", newList);
                    writes.add(new UpdateOneModel<>(
                            eq("_id", user.getObjectId("_id")),
                            new BasicDBObject("$set",
                                    new BasicDBObject("events", newList)
                            )
                    ));

                }
            }

            if(writes.size() > 0)
                userRepository.bulkWrite(writes);
        }
    }

    private static class RemoveExpiredMeetings extends TimerTask {

        @Override
        public void run() {

            long timeLimit = System.currentTimeMillis() - ONE_HOUR_MIL_SEC * 5;

            List<Document> docs = advisorMeetingRepository.find(
                    lt("created_at", timeLimit),
                    new BasicDBObject("room_id", 1)
            );

            for(Document doc : docs)
                irysc.gachesefid.Controllers.Advisor.Utility.deleteMeeting(doc.getInteger("room_id"));

        }
    }

    private static class QuizReminder extends TimerTask {

        @Override
        public void run() {

            long curr = System.currentTimeMillis();
            long tomorrow = curr + 86400000;
            long next8H = curr + 28800000;

            ArrayList<Document> quizzes = iryscQuizRepository.find(
                    and(
                            gt("start", curr),
                            lt("start", tomorrow),
                            gt("start", next8H),
                            exists("students.0")
                    ), new BasicDBObject("students", 1).append("title", 1)
            );

            for(Document quiz : quizzes) {

                ObjectId quizId = quiz.getObjectId("_id");

                for(Document student : quiz.getList("students", Document.class)) {

                    ObjectId userId = student.getObjectId("_id");

                    Document user = userRepository.findById(userId);

                    if (user == null || !user.containsKey("mail"))
                        continue;

                    if(mailQueueRepository.exist(and(
                            eq("mode", "quizReminder"),
                            eq("quiz_id", quizId),
                            eq("user_id", userId)
                    )))
                        continue;

                    mailQueueRepository.insertOne(
                            new Document("created_at", curr)
                                    .append("status", "pending")
                                    .append("mail", user.getString("mail"))
                                    .append("name", user.getString("first_name") + " " + user.getString("last_name"))
                                    .append("mode", "quizReminder")
                                    .append("quiz_id", quizId)
                                    .append("user_id", userId)
                                    .append("msg", quiz.getString("title"))
                    );

                }

            }

        }

    }

    private static class TokenHandler extends TimerTask {

        public void run() {

            synchronized (validateTokens) {
                validateTokens.removeIf(itr -> !itr.isValidateYet());
            }

            synchronized (blackListTokens) {
                blackListTokens.removeIf(itr -> (long) itr.getValue() < System.currentTimeMillis());
            }

            activationRepository.deleteMany(lt("created_at", System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC));
        }
    }

    private static class SiteStatsHandler extends TimerTask {

        public void run() {
//            SCHOOLS = schoolRepository.count(exists("user_id"));
            SCHOOLS = schoolRepository.count(null);
            QUESTIONS = questionRepository.count(null);
            STUDENTS = userRepository.count(eq("level", false));
        }
    }

    private static class RemoveRedundantCustomQuizzes extends TimerTask {

        @Override
        public void run() {

            customQuizRepository.deleteMany(
                    and(
                            eq("status", "wait"),
                            lt("created_at", System.currentTimeMillis() - 1200000) // 20min
                    )
            );

        }
    }

    public static class CalcSubjectQuestions extends TimerTask {

        @Override
        public void run() {

            HashMap<ObjectId, StudentQuizController.SubjectFilter> subjectFilterHashMap = new HashMap<>();
            ArrayList<Document> questions = questionRepository.find(null,
                    new BasicDBObject("level", 1).append("subject_id", 1)
            );

            ArrayList<ObjectId> subjectIds = new ArrayList<>();

            for (Document question : questions) {

                ObjectId sId = question.getObjectId("subject_id");
                String l = question.getString("level");

                if (subjectFilterHashMap.containsKey(sId))
                    subjectFilterHashMap.get(sId).add(1, l);
                else {
                    subjectFilterHashMap.put(sId, new StudentQuizController.SubjectFilter(sId, 1, l));
                    subjectIds.add(sId);
                }
            }

            ArrayList<Document> subjects = subjectRepository.findByIds(
                    subjectIds, true
            );

            int idx = 0;

            List<WriteModel<Document>> writes = new ArrayList<>();

            for(ObjectId sId : subjectFilterHashMap.keySet()) {

                subjects.get(idx).put("q_no", subjectFilterHashMap.get(sId).total());
                subjects.get(idx).put("q_no_easy", subjectFilterHashMap.get(sId).easy());
                subjects.get(idx).put("q_no_mid", subjectFilterHashMap.get(sId).mid());
                subjects.get(idx).put("q_no_hard", subjectFilterHashMap.get(sId).hard());

                writes.add(new UpdateOneModel<>(
                        eq("_id", sId),
                        new BasicDBObject("$set",
                                new BasicDBObject("q_no", subjectFilterHashMap.get(sId).total())
                                        .append("q_no_easy", subjectFilterHashMap.get(sId).easy())
                                        .append("q_no_mid", subjectFilterHashMap.get(sId).mid())
                                        .append("q_no_hard", subjectFilterHashMap.get(sId).hard())
                        )
                ));

                idx++;
            }

            subjectRepository.bulkWrite(writes);
            try {
                Thread.sleep(60000);
                calcTagQNo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        void calcTagQNo() {

            JSONArray tags = questionRepository.distinctTags("tags");

            List<WriteModel<Document>> writes = new ArrayList<>();

            for(int i = 0; i < tags.length(); i++) {

                String tag = tags.getString(i);

                Document questionTag = questionTagRepository.findOne(
                        eq("tag", tag), null
                );

                if(questionTag == null)
                    continue;

                ArrayList<Document> docs = questionRepository.find(
                        in("tags", tag),
                        new BasicDBObject("level", 1)
                );

                int easy = 0, mid = 0, hard = 0;

                for(Document doc : docs) {
                    switch (doc.getString("level")) {
                        case "easy":
                            easy++;
                            break;
                        case "mid":
                            mid++;
                            break;
                        case "hard":
                            hard++;
                            break;
                    }
                }

                writes.add(
                        new UpdateOneModel<>(
                                eq("_id", questionTag.getObjectId("_id")),
                                new BasicDBObject("$set",
                                        new BasicDBObject("q_no_easy", easy)
                                                .append("q_no_mid", mid)
                                                .append("q_no_hard", hard)
                                                .append("q_no", docs.size())
                                )
                        )
                );
            }

            questionTagRepository.bulkWrite(writes);

            try {
                Thread.sleep(60000);
                calcAuthorsQNo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        void calcAuthorsQNo() {

            JSONArray authors = questionRepository.distinctTags("author");

            List<WriteModel<Document>> writes = new ArrayList<>();

            for(int i = 0; i < authors.length(); i++) {

                String author = authors.getString(i);

                Document authorDoc = authorRepository.findOne(
                        eq("name", author), null
                );

                if(authorDoc == null)
                    continue;

                ArrayList<Document> docs = questionRepository.find(
                        eq("author", author),
                        new BasicDBObject("level", 1)
                );

                int easy = 0, mid = 0, hard = 0;

                for(Document doc : docs) {
                    switch (doc.getString("level")) {
                        case "easy":
                            easy++;
                            break;
                        case "mid":
                            mid++;
                            break;
                        case "hard":
                            hard++;
                            break;
                    }
                }

                writes.add(
                        new UpdateOneModel<>(
                                eq("_id", authorDoc.getObjectId("_id")),
                                new BasicDBObject("$set",
                                        new BasicDBObject("q_no_easy", easy)
                                                .append("q_no_mid", mid)
                                                .append("q_no_hard", hard)
                                                .append("q_no", docs.size())
                                )
                        )
                );
            }

            authorRepository.bulkWrite(writes);
        }
    }

    private static class RemoveRedundantAttaches extends TimerTask {

        @Override
        public void run() {

            File ckFolder = new File(
                    DEV_MODE ? FileUtils.uploadDir_dev + "ck" :
                    FileUtils.uploadDir + "ck"
            );

            if (!ckFolder.exists() || !ckFolder.isDirectory())
                return;

            File[] allFiles = ckFolder.listFiles();
            if(allFiles == null || allFiles.length == 0)
                return;

            ArrayList<Document> quizzes = iryscQuizRepository.find(
                    or(
                            exists("desc"),
                            exists("desc_after")
                    ), new BasicDBObject("desc", 1).append("desc_after", 1)
            );

            for(File f : allFiles) {

                if(f.isDirectory() || f.getName().startsWith("."))
                    continue;

                boolean find = false;

                for(Document quiz : quizzes) {
                    if(
                            (quiz.containsKey("desc") && quiz.getString("desc").contains(f.getName())) ||
                            (quiz.containsKey("desc_after") && quiz.getString("desc_after").contains(f.getName()))
                    ) {
                        find = true;
                        break;
                    }
                }

                if(!find)
                    f.delete();
            }

        }

    }

    private static class SendMails extends TimerTask {

        @Override
        public void run() {

            ArrayList<Document> mails =
                    mailQueueRepository.find(eq("status", "pending"), null, Sorts.descending("created_at"));

            if(mails.size() == 0)
                return;

            long yesterday = System.currentTimeMillis() - 86400000;

            int limit = mails.size() > 30 ? 30 : mails.size();
            ArrayList<ObjectId> ids = new ArrayList<>();

            for(int i = 0; i < limit; i++) {

                try {
                    Document mail = mails.get(i);

                    if(Utility.sendMailWithAttach(
                            mail.containsKey("title") ?
                                    mail.getString("title") + "___" + mail.getString("mail") :
                                    mail.getString("mail"),
                            (String) mail.getOrDefault("msg", ""),
                            (String) mail.getOrDefault("name", ""),
                            (String) mail.getOrDefault("attach", "")
                    )) {
                        if(
                                !mail.getString("mode").equalsIgnoreCase("quizReminder") ||
                                        mail.getLong("created_at") < yesterday
                        )
                            ids.add(mail.getObjectId("_id"));
                        else if(mail.getString("mode").equalsIgnoreCase("quizReminder")) {
                            mail.put("status", "success");
                            mailQueueRepository.replaceOne(mail.getObjectId("_id"), mail);
                        }

                    }
                    else {
                        mail.put("status", "failed");
                        mailQueueRepository.replaceOne(mail.getObjectId("_id"), mail);
                    }

                    Thread.sleep(1000);
                }
                catch (Exception ignore) {}
            }

            if(ids.size() > 0)
                mailQueueRepository.deleteMany(in("_id", ids));
        }
    }

    private static class SendSMS extends TimerTask {

        @Override
        public void run() {

            ArrayList<Document> allSms =
                    smsQueueRepository.find(eq("status", "pending"), null, Sorts.descending("created_at"));

            if(allSms.size() == 0)
                return;

            HashMap<ObjectId, ArrayList<String>> receivers = new HashMap<>();
            HashMap<ObjectId, ArrayList<ObjectId>> ids = new HashMap<>();
            HashMap<ObjectId, String> messages = new HashMap<>();

            List<ObjectId> shouldRemove = new ArrayList<>();

            int sent = 0;

            for(Document sms : allSms) {

                if(!PhoneValidator.isValid(sms.getString("phone"))) {
                    shouldRemove.add(sms.getObjectId("_id"));
                    continue;
                }

                ObjectId notifId = sms.getObjectId("notif_id");

                if(sms.getString("msg").contains("newNotif")) {

                    String name = sms.getString("msg").split("__")[1];
                    sendSMSWithTemplate(sms.getString("phone"), 815, new PairValue("name", name));

                    sent++;

                    if(sent >= 30) {
                        sent = 0;
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    shouldRemove.add(sms.getObjectId("_id"));
                }

                else {

                    if (!messages.containsKey(notifId)) {
                        messages.put(notifId, sms.getString("msg"));
                        receivers.put(notifId, new ArrayList<>() {{
                            add(sms.getString("phone"));
                        }});
                    } else {
                        receivers.get(notifId).add(sms.getString("phone"));
                    }

                    if(!ids.containsKey(notifId)) {
                        ids.put(notifId, new ArrayList<>() {{
                            add(sms.getObjectId("_id"));
                        }});
                    }
                    else {
                        ids.get(notifId).add(sms.getObjectId("_id"));
                    }

                }

            }

            if(sent > 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for(ObjectId key : messages.keySet()) {

                int reminder = ids.get(key).size();
                int limit;
                int curr = 0;

                ArrayList<String> recv = receivers.get(key);

                while (curr < ids.get(key).size()) {

                    StringBuilder sb = new StringBuilder();
                    limit = Math.min(reminder, 30);

                    for (int i = curr; i < curr + limit; i++) {
                        if (i == 0)
                            sb.append(recv.get(i));
                        else
                            sb.append("-").append(recv.get(i));
                    }

                    Utility.sendSMSWithoutTemplate(
                            sb.toString(), messages.get(key)
                    );

                    smsQueueRepository.deleteMany(
                            in("_id", ids.get(key).subList(curr, curr + limit))
                    );

                    reminder -= limit;
                    curr += limit;

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            if(shouldRemove.size() > 0) {
                smsQueueRepository.deleteMany(
                        in("_id", shouldRemove)
                );
            }
        }
    }

    private static class CheckContentBuys extends TimerTask {

        @Override
        public void run() {

            List<Document> transactions = transactionRepository.find(and(
                    eq("status", "success"),
                    eq("section", OffCodeSections.CONTENT.getName())
            ), new BasicDBObject("products", 1).append("user_id", 1).append("amount", 1));

            for(Document transaction : transactions) {

                ObjectId contentId = transaction.getObjectId("products");
                ObjectId userId = transaction.getObjectId("user_id");

                Document content = contentRepository.findById(contentId);
                if(content == null)
                    continue;

                List<Document> students = content.getList("users", Document.class);
                if(Utility.searchInDocumentsKeyValIdx(students, "_id", userId) == -1) {

                    Document user = userRepository.findById(userId);
                    System.out.println(user.getString("first_name") + " " + user.getString("last_name"));
                    StudentContentController.registry(
                            contentId, userId,
                            transaction.getInteger("amount"),
                            null, null
                    );

                }

            }

        }
    }

}
