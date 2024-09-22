package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Controllers.Quiz.StudentQuizController;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Action;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Security.JwtTokenFilter.blackListTokens;
import static irysc.gachesefid.Security.JwtTokenFilter.validateTokens;
import static irysc.gachesefid.Utility.SkyRoomUtils.deleteMeeting;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class Jobs implements Runnable {

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new TokenHandler(), ONE_DAY_MIL_SEC, ONE_DAY_MIL_SEC); // 1 day
        timer.schedule(new QuizReminder(), ONE_MIN_MSEC * 12, ONE_HOUR_MIL_SEC); // 1 hour
        timer.schedule(new SiteStatsHandler(), ONE_DAY_MIL_SEC, ONE_DAY_MIL_SEC); // 1 day
        timer.schedule(new RemoveRedundantCustomQuizzes(), ONE_MIN_MSEC * 15, ONE_DAY_MIL_SEC);

        timer.schedule(new RemoveExpiredNotifs(), 300000, ONE_DAY_MIL_SEC * 7); // delay: 5 min
        timer.schedule(new RemoveExpiredCaches(), ONE_DAY_MIL_SEC + ONE_HOUR_MIL_SEC, ONE_DAY_MIL_SEC);
        timer.schedule(new RemoveExpiredMeetings(), 600000, ONE_DAY_MIL_SEC * 7); // delay: 10 min
        timer.schedule(new RejectExpiredTeachRequests(), 60000, ONE_HOUR_MIL_SEC); // delay: 1 min

        timer.schedule(new CheckContentBuys(), 1200000, ONE_HOUR_MIL_SEC); // delay: 20 min

        timer.schedule(new InactiveExpiredAdvice(), ONE_MIN_MSEC * 7, ONE_DAY_MIL_SEC);
        timer.schedule(new BirthDayPoint(), ONE_MIN_MSEC * 3, ONE_HOUR_MIL_SEC * 12);
        timer.schedule(new DailyPoint(), ONE_MIN_MSEC * 4, ONE_MIN_MSEC * 30);
        timer.schedule(new SendMails(), 0, ONE_MIN_MSEC * 5);
        timer.schedule(new SendSMS(), 0, ONE_MIN_MSEC * 5);
        timer.schedule(new CalcSubjectQuestions(), 1800000, ONE_DAY_MIL_SEC); // delay: 30 min
    }

    //todo remove redundant transactions
    //todo remove redundant school questions

    private static class InactiveExpiredAdvice extends TimerTask {
        @Override
        public void run() {

            userRepository.updateMany(exists("check_advice_status_again"), inc("check_advice_status_again", -1));

            List<Document> users = userRepository.find(and(
                    exists("my_advisors.0"),
                    or(
                            exists("check_advice_status_again", false),
                            lte("check_advice_status_again", 2)
                    )
            ), new BasicDBObject("my_advisors", 1));

            long curr = System.currentTimeMillis();
            List<WriteModel<Document>> writes = new ArrayList<>();
            HashMap<ObjectId, List<ObjectId>> expiredStudents = null;
            Set<ObjectId> shouldClearFromCache = new HashSet<>();

            for (Document user : users) {
                int minReminder = 32;
                List<ObjectId> expiredAdvisors = null;

                for (ObjectId advisorId : user.getList("my_advisors", ObjectId.class)) {
                    Document adviceRequest = advisorRequestsRepository.findOne(and(
                            eq("user_id", user.getObjectId("_id")),
                            eq("advisor_id", advisorId),
                            eq("answer", "accept"),
                            exists("paid_at")
                    ), new BasicDBObject("paid_at", 1), Sorts.descending("paid_at"));

                    if (adviceRequest == null)
                        continue;

                    int r = (int) ((curr - adviceRequest.getLong("paid_at")) / ONE_DAY_MIL_SEC);
                    if (r > 31) {
                        if (expiredAdvisors == null) expiredAdvisors = new ArrayList<>();
                        expiredAdvisors.add(advisorId);

                        if (expiredStudents == null)
                            expiredStudents = new HashMap<>();

                        if (!expiredStudents.containsKey(advisorId))
                            expiredStudents.put(advisorId, new ArrayList<>() {{
                                add(user.getObjectId("_id"));
                            }});
                        else
                            expiredStudents.get(advisorId).add(user.getObjectId("_id"));
                    } else
                        minReminder = Math.min(minReminder, r);
                }

                Document updateQuery = new Document();
                if (minReminder < 32)
                    updateQuery.append("check_advice_status_again", minReminder);

                if (expiredAdvisors != null) {
                    List<ObjectId> finalExpiredAdvisors = expiredAdvisors;
                    updateQuery.append("my_advisors", user.getList("my_advisors", ObjectId.class)
                            .stream().filter(objectId -> !finalExpiredAdvisors.contains(objectId)).collect(Collectors.toList())
                    );
                }

                shouldClearFromCache.add(user.getObjectId("_id"));
                writes.add(new UpdateOneModel<>(
                        eq("_id", user.getObjectId("_id")),
                        new BasicDBObject("$set", updateQuery)
                ));
            }

            if (expiredStudents != null) {
                List<Document> advisors = userRepository.find(
                        in("_id", expiredStudents.keySet().toArray()),
                        new BasicDBObject("students", 1)
                );
                for (ObjectId advisorId : expiredStudents.keySet()) {
                    final List<ObjectId> excludeList = expiredStudents.get(advisorId);
                    advisors.stream().filter(advisor -> advisor.getObjectId("_id").equals(advisorId))
                            .findFirst().ifPresent(advisor -> {
                                shouldClearFromCache.add(advisorId);
                                writes.add(new UpdateOneModel<>(
                                        eq("_id", advisorId),
                                        new BasicDBObject("$set",
                                                new BasicDBObject("students",
                                                        advisor.getList("students", Document.class)
                                                                .stream().filter(std -> !excludeList.contains(std.getObjectId("_id")))
                                                                .collect(Collectors.toList())
                                                )
                                        )
                                ));
                            });
                }
            }

            if (writes.size() > 0) {
                userRepository.bulkWrite(writes);
                userRepository.clearBatchFromCache(new ArrayList<>(shouldClearFromCache));
            }
        }
    }

    private static class BirthDayPoint extends TimerTask {
        @Override
        public void run() {

            Document point = pointRepository.findBySecKey(Action.BIRTH_DAY.getName());
            if(point == null || point.getInteger("point") == 0)
                return;

            long curr = System.currentTimeMillis();
            List<Document> users = userRepository.find(
                    and(
                            exists("birth_day"),
                            or(
                                    exists("last_birth_day_point", false),
                                    lt("last_birth_day_point", curr - ONE_DAY_MIL_SEC * 360)
                            )
                    ), new BasicDBObject("birth_day", 1)
            );

            Date currDate = new Date();
            int currDay = currDate.getDate();
            int currMonth = currDate.getMonth();
            List<ObjectId> wantedUsers = new ArrayList<>();

            users.forEach(user -> {
                Date d = new Date(user.getLong("birth_day"));
                if (d.getDate() == currDay && d.getMonth() == currMonth)
                    wantedUsers.add(user.getObjectId("_id"));
            });

            if (wantedUsers.size() > 0) {
                String msg = "<p>" + "سلام " + "<br/>";
                msg += "امیدواریم سال\u200Cها چرخت به خوبی و بدون لنگ زدن بچرخه و هر سال برات پر از خاطرات خوب باشه. برای شیرین\u200Cتر شدن شروع امسالت، "
                        + point.getInteger("point") + " امتیاز از آیریسک هدیه گرفتی!" + "<br/><br/>حالش رو ببر";

                ObjectId notifId = new ObjectId();
                Document notif = new Document("_id", notifId)
                        .append("users_count", wantedUsers.size())
                        .append("title", "تولدت مبارک")
                        .append("text", msg)
                        .append("send_via", "site")
                        .append("created_at", curr)
                        .append("users", new ArrayList<ObjectId>() {{
                            addAll(wantedUsers);
                        }});

                userRepository.updateManyByIds(wantedUsers,
                        new BasicDBObject("$push", new BasicDBObject("events",
                                new Document("created_at", curr)
                                        .append("notif_id", notifId)
                                        .append("seen", false)
                        )).append("$set", new BasicDBObject("last_birth_day_point", curr))
                );

                notifRepository.insertOne(notif);
                new Thread(() -> wantedUsers.forEach(oId -> PointController.addPointForAction(oId, Action.BIRTH_DAY, currDate.getYear(), null))).start();
            }
        }
    }

    private static class DailyPoint extends TimerTask {
        @Override
        public void run() {

            Document point = pointRepository.findBySecKey(Action.DAILY_POINT.getName());
            if(point == null || point.getInteger("point") == 0)
                return;

            long curr = System.currentTimeMillis();
            List<ObjectId> users = userRepository.findLimited(
                    or(
                            exists("last_daily_point", false),
                            lt("last_daily_point", curr - ONE_HOUR_MIL_SEC * 24)
                    ), JUST_ID, Sorts.ascending("created_at"), 0, 100
            ).stream().map(document -> document.getObjectId("_id")).collect(Collectors.toList());

            if (users.size() > 0) {
                int today = getToday();
                userRepository.updateMany(in("_id", users), set("last_daily_point", curr));
                new Thread(() ->
                        users.forEach(objectId -> PointController.addPointForAction(objectId, Action.DAILY_POINT, today, null))
                ).start();
            }
        }
    }

    private static class RemoveExpiredNotifs extends TimerTask {

        public void run() {

            long lastMonth = System.currentTimeMillis() - ONE_DAY_MIL_SEC * 30;
            List<Document> users = userRepository.find(and(
                    exists("events.0"),
                    lt("events.0.created_at", lastMonth)
            ), null);

            List<WriteModel<Document>> writes = new ArrayList<>();

            for (Document user : users) {

                List<Document> notifs = user.getList("events", Document.class);
                List<Document> newList = new ArrayList<>();

                for (Document notif : notifs) {

                    if (notif.getLong("created_at") < lastMonth)
                        continue;

                    newList.add(notif);
                }

                if (newList.size() < notifs.size()) {

                    user.put("events", newList);
                    writes.add(new UpdateOneModel<>(
                            eq("_id", user.getObjectId("_id")),
                            new BasicDBObject("$set",
                                    new BasicDBObject("events", newList)
                            )
                    ));

                }
            }

            if (writes.size() > 0)
                userRepository.bulkWrite(writes);
        }
    }

    private static class RemoveExpiredCaches extends TimerTask {
        public void run() {
            Repository.removeExpiredItems();
        }
    }

    private static class RejectExpiredTeachRequests extends TimerTask {
        @Override
        public void run() {
            long curr = System.currentTimeMillis();
            List<Document> docs = teachScheduleRepository.find(
                    elemMatch("requests", and(
                            lt("expire_at", curr),
                            or(
                                    eq("status", "pending"),
                                    eq("status", "accept")
                            )
                    )), new BasicDBObject("requests", 1).append("start_at", 1)
                            .append("teach_mode", 1).append("max_cap", 1)
                            .append("can_request", 1)
            );

            List<WriteModel<Document>> writes = new ArrayList<>();
            for (Document doc : docs) {
                doc.getList("requests", Document.class).removeIf(request ->
                        (
                                request.getString("status").equalsIgnoreCase("pending") ||
                                        request.getString("status").equalsIgnoreCase("accept")
                        ) && request.getLong("expire_at") < curr
                );

                BasicDBObject update = new BasicDBObject("requests", doc.getList("requests", Document.class));
                if (!doc.getBoolean("can_request")) {
                    int maxCap = (Integer) doc.getOrDefault("max_cap", 1);
                    if (doc.getList("requests", Document.class).size() < maxCap)
                        update.append("can_request", true);
                }

                writes.add(new UpdateOneModel<>(
                        eq("_id", doc.getObjectId("_id")),
                        new BasicDBObject("$set", update)
                ));
            }

            if (writes.size() > 0) {
                teachScheduleRepository.bulkWrite(writes);
                docs.forEach(doc -> teachScheduleRepository.clearFromCache(doc.getObjectId("_id")));
            }
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

            for (Document doc : docs)
                deleteMeeting(doc.getInteger("room_id"));

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

            for (Document quiz : quizzes) {

                ObjectId quizId = quiz.getObjectId("_id");

                for (Document student : quiz.getList("students", Document.class)) {

                    ObjectId userId = student.getObjectId("_id");

                    Document user = userRepository.findById(userId);

                    if (user == null || !user.containsKey("mail"))
                        continue;

                    if (mailQueueRepository.exist(and(
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

            for (ObjectId sId : subjectFilterHashMap.keySet()) {

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

            for (int i = 0; i < tags.length(); i++) {

                String tag = tags.getString(i);

                Document questionTag = questionTagRepository.findOne(
                        eq("tag", tag), null
                );

                if (questionTag == null)
                    continue;

                ArrayList<Document> docs = questionRepository.find(
                        in("tags", tag),
                        new BasicDBObject("level", 1)
                );

                int easy = 0, mid = 0, hard = 0;

                for (Document doc : docs) {
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

            for (int i = 0; i < authors.length(); i++) {

                String author = authors.getString(i);

                Document authorDoc = authorRepository.findOne(
                        eq("name", author), null
                );

                if (authorDoc == null)
                    continue;

                ArrayList<Document> docs = questionRepository.find(
                        eq("author", author),
                        new BasicDBObject("level", 1)
                );

                int easy = 0, mid = 0, hard = 0;

                for (Document doc : docs) {
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
            if (allFiles == null || allFiles.length == 0)
                return;

            ArrayList<Document> quizzes = iryscQuizRepository.find(
                    or(
                            exists("desc"),
                            exists("desc_after")
                    ), new BasicDBObject("desc", 1).append("desc_after", 1)
            );

            for (File f : allFiles) {

                if (f.isDirectory() || f.getName().startsWith("."))
                    continue;

                boolean find = false;

                for (Document quiz : quizzes) {
                    if (
                            (quiz.containsKey("desc") && quiz.getString("desc").contains(f.getName())) ||
                                    (quiz.containsKey("desc_after") && quiz.getString("desc_after").contains(f.getName()))
                    ) {
                        find = true;
                        break;
                    }
                }

                if (!find)
                    f.delete();
            }

        }

    }

    private static class SendMails extends TimerTask {

        @Override
        public void run() {

            ArrayList<Document> mails =
                    mailQueueRepository.find(eq("status", "pending"), null, Sorts.descending("created_at"));

            if (mails.size() == 0)
                return;

            long yesterday = System.currentTimeMillis() - 86400000;

            int limit = Math.min(mails.size(), 30);
            ArrayList<ObjectId> ids = new ArrayList<>();

            for (int i = 0; i < limit; i++) {

                try {
                    Document mail = mails.get(i);

                    if (!mail.containsKey("mail") ||
                            mail.getString("mail") == null) {
                        ids.add(mail.getObjectId("_id"));
                        continue;
                    }

                    if (Utility.sendMailWithAttach(
                            mail.containsKey("title") ?
                                    mail.getString("title") + "___" + mail.getString("mail") :
                                    mail.getString("mail"),
                            (String) mail.getOrDefault("msg", ""),
                            (String) mail.getOrDefault("name", ""),
                            (String) mail.getOrDefault("attach", "")
                    )) {
                        if (
                                !mail.getString("mode").equalsIgnoreCase("quizReminder") ||
                                        mail.getLong("created_at") < yesterday
                        )
                            ids.add(mail.getObjectId("_id"));
                        else if (mail.getString("mode").equalsIgnoreCase("quizReminder")) {
                            mail.put("status", "success");
                            mailQueueRepository.replaceOne(mail.getObjectId("_id"), mail);
                        }

                    } else {
                        mail.put("status", "failed");
                        mailQueueRepository.replaceOne(mail.getObjectId("_id"), mail);
                    }

                    Thread.sleep(1000);
                } catch (Exception ignore) {
                }
            }

            if (ids.size() > 0)
                mailQueueRepository.deleteMany(in("_id", ids));
        }
    }

    private static class SendSMS extends TimerTask {

        @Override
        public void run() {

            ArrayList<Document> allSms =
                    smsQueueRepository.find(eq("status", "pending"), null, Sorts.descending("created_at"));

            if (allSms.size() == 0)
                return;

            HashMap<ObjectId, ArrayList<String>> receivers = new HashMap<>();
            HashMap<ObjectId, ArrayList<ObjectId>> ids = new HashMap<>();
            HashMap<ObjectId, String> messages = new HashMap<>();

            List<ObjectId> shouldRemove = new ArrayList<>();

            int sent = 0;

            for (Document sms : allSms) {

                if (!PhoneValidator.isValid(sms.getString("phone"))) {
                    shouldRemove.add(sms.getObjectId("_id"));
                    continue;
                }

                ObjectId notifId = sms.getObjectId("notif_id");

                if (sms.getString("msg").contains("newNotif")) {

                    String name = sms.getString("msg").split("__")[1];
                    sendSMSWithTemplate(sms.getString("phone"), 815, new PairValue("name", name));

                    sent++;

                    if (sent >= 30) {
                        sent = 0;
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    shouldRemove.add(sms.getObjectId("_id"));
                } else {

                    if (!messages.containsKey(notifId)) {
                        messages.put(notifId, sms.getString("msg"));
                        receivers.put(notifId, new ArrayList<>() {{
                            add(sms.getString("phone"));
                        }});
                    } else {
                        receivers.get(notifId).add(sms.getString("phone"));
                    }

                    if (!ids.containsKey(notifId)) {
                        ids.put(notifId, new ArrayList<>() {{
                            add(sms.getObjectId("_id"));
                        }});
                    } else {
                        ids.get(notifId).add(sms.getObjectId("_id"));
                    }

                }

            }

            if (sent > 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (ObjectId key : messages.keySet()) {

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

            if (shouldRemove.size() > 0) {
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

            for (Document transaction : transactions) {

                ObjectId contentId = transaction.getObjectId("products");
                ObjectId userId = transaction.getObjectId("user_id");

                Document content = contentRepository.findById(contentId);
                if (content == null)
                    continue;

                List<Document> students = content.getList("users", Document.class);
                if (Utility.searchInDocumentsKeyValIdx(students, "_id", userId) == -1) {

                    Document user = userRepository.findById(userId);

                    if (user != null)
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
