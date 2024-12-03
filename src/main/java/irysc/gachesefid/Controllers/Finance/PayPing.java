package irysc.gachesefid.Controllers.Finance;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Advisor.AdvisorController;
import irysc.gachesefid.Controllers.Badge.BadgeController;
import irysc.gachesefid.Controllers.Content.StudentContentController;
import irysc.gachesefid.Controllers.Point.PointController;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Finance.TransactionController.getTransactionTitle;
import static irysc.gachesefid.Controllers.Teaching.Utility.completePrePayForSemiPrivateSchedule;
import static irysc.gachesefid.Controllers.Teaching.Utility.register;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class PayPing {

    public static String exchange(ObjectId userId,
                                  double money,
                                  double coin,
                                  double amount,
                                  String mode) {

        if (amount <= 0)
            return JSON_NOT_VALID_PARAMS;

        String numberD = String.valueOf(amount);
        numberD = numberD.substring(numberD.indexOf(".") + 1);

        if (numberD.length() > 1)
            return generateErr("تنها تا یک رقم اعشار می توانید عدد خود را وارد نمایید.");

        Document config = getConfig();
//        double exchangeCoef =
//                mode.equalsIgnoreCase(ExchangeMode.COIN_TO_MONEY.getName()) ?
//                        ((Number)config.get("coin_rate_coef")).doubleValue() :
//                        ((Number)config.get("money_rate_coef")).doubleValue();

        double exchangeCoef =
                mode.equalsIgnoreCase(ExchangeMode.COIN_TO_MONEY.getName()) ?
                        ((Number) config.get("coin_rate_coef")).doubleValue() :
                        10000.0 / ((Number) config.get("coin_rate_coef")).doubleValue();

        if (
                mode.equalsIgnoreCase(ExchangeMode.COIN_TO_MONEY.getName()) &&
                        coin < amount
        )
            return generateErr("مقدار انتخاب شده بیش از حد مجاز است.");

        if (
                mode.equalsIgnoreCase(ExchangeMode.MONEY_TO_COIN.getName()) &&
                        money < amount
        )
            return generateErr("مقدار انتخاب شده بیش از حد مجاز است.");

        if (
                mode.equalsIgnoreCase(ExchangeMode.MONEY_TO_COIN.getName())
        )
            amount /= 10000.0;

        double finalVal = amount * exchangeCoef;
        BasicDBObject update = new BasicDBObject();

        if (
                mode.equalsIgnoreCase(ExchangeMode.MONEY_TO_COIN.getName())
        )
            update.append("money", Math.round((money - amount * 10000) * 100.0) / 100.0)
                    .append("coin", Math.round((coin + finalVal) * 100.0) / 100.0);
        else {
            update.append("money", Math.round((money + finalVal) * 100.0) / 100.0)
                    .append("coin", Math.round((coin - amount) * 100.0) / 100.0);
        }

        userRepository.updateOne(userId, new BasicDBObject("$set", update));
        userRepository.checkCache(userId);

        return JSON_OK;
    }

    private static String execPHP(String scriptName, String param) {

        StringBuilder output = new StringBuilder();

        try {
            String line;
            Process p = Runtime.getRuntime().exec("php /var/www/scripts/" + scriptName + " " + param);
            BufferedReader input =
                    new BufferedReader
                            (new InputStreamReader(p.getInputStream()));

            while ((line = input.readLine()) != null) {
                output.append(line);
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }

        return output.toString();
    }

    private static void completePay(Document transaction) {

        ObjectId studentId = transaction.getObjectId("user_id");
        Document user = userRepository.findById(studentId);

        if (user != null) {
            if (transaction.getString("section").equalsIgnoreCase("charge")) {

                if (user.containsKey("mail")) {
                    new Thread(() -> sendMail(
                            user.getString("mail"),
                            SERVER + "recp/" + transaction.getObjectId("_id").toString(),
                            "successTransaction",
                            user.getString("first_name") + " " + user.getString("last_name")
                    )).start();
                }

                user.put("money", ((Number) user.get("money")).doubleValue() + transaction.getInteger("amount"));
            } else {
                user.put("money", (double) 0);
            }

            userRepository.replaceOneWithoutClearCache(
                    user.getObjectId("_id"), user
            );

            if (transaction.containsKey("products")) {

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.COUNSELING.getName())
                ) {
                    new Thread(() -> {
                        BadgeController.checkForUpgrade(studentId, Action.SET_ADVISOR);
                        PointController.addPointForAction(studentId, Action.SET_ADVISOR, transaction.getObjectId("products"), null);
                    }).start();
                    Document request = advisorRequestsRepository.findById(transaction.getObjectId("products"));
                    if (request != null && studentId.equals(request.getObjectId("user_id"))) {
                        long curr = System.currentTimeMillis();
                        request.put("paid", transaction.getInteger("amount"));
                        request.put("paid_at", curr);

                        Document olderAdvisorRequest = advisorRequestsRepository.findOne(and(
                                eq("user_id", user.getObjectId("_id")),
                                eq("advisor_id", request.getObjectId("advisor_id")),
                                eq("answer", "accept"),
                                exists("active_at"),
                                lte("active_at", curr - 24 * ONE_DAY_MIL_SEC),
                                gt("active_at", curr - 31 * ONE_DAY_MIL_SEC)
                        ), new BasicDBObject("active_at", 1), Sorts.descending("created_at"));
                        if (olderAdvisorRequest != null)
                            request.put("active_at", 31 * ONE_DAY_MIL_SEC + olderAdvisorRequest.getLong("active_at"));
                        else
                            request.put("active_at", curr);

                        advisorRequestsRepository.replaceOne(request.getObjectId("_id"), request);
                        AdvisorController.setAdvisor(user, userRepository.findById(request.getObjectId("advisor_id")));
                    }
                }

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.SCHOOL_QUIZ.getName())
                ) {

                    Document quiz = schoolQuizRepository.findById(transaction.getObjectId("products"));

                    if (quiz != null) {
                        quiz.put("status", "finish");
                        schoolQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);
                    }
                }

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.CLASSES.getName())
                ) {
                    new Thread(() -> {
                        BadgeController.checkForUpgrade(studentId, Action.GET_TEACH_CLASS);
                        PointController.addPointForAction(studentId, Action.GET_TEACH_CLASS, transaction.getObjectId("products"), null);
                    }).start();

                    Document schedule = teachScheduleRepository.findById(transaction.getObjectId("products"));
                    if (schedule != null && schedule.containsKey("students")) {
                        Document studentRequest = searchInDocumentsKeyVal(
                                schedule.getList("requests", Document.class),
                                "_id", user.getObjectId("_id")
                        );

                        new Thread(() -> {
                            Document advisor = userRepository.findById(schedule.getObjectId("user_id"));
                            createNotifAndSendSMS(
                                    advisor,
                                    user.getString("first_name") + " " + user.getString("last_name"),
                                    "finalizeTeach"
                            );
                        }).start();

                        teachScheduleRepository.updateOne(
                                transaction.getObjectId("products"),
                                new BasicDBObject(
                                        "$set",
                                        register(
                                                schedule, user.getObjectId("_id"),
                                                System.currentTimeMillis(), studentRequest,
                                                schedule.getString("teach_mode").equalsIgnoreCase(TeachMode.PRIVATE.getName())
                                        )
                                )
                        );
                    }
                }

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals("prePay")
                ) {
                    try {
                        Number n = (Number) transaction.get("account_money");
                        completePrePayForSemiPrivateSchedule(
                                transaction.getObjectId("products"), null, user,
                                (int) (n.doubleValue() + transaction.getInteger("amount"))
                        );
                    } catch (InvalidFieldsException ignore) {
                    }
                }

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(AllKindQuiz.ONLINESTANDING.getName())
                ) {
                    onlineStandingController.registry(studentId,
                            user.getString("phone") + "__" + user.getString("mail"),
                            transaction.get("products").toString() + "__" + transaction.getString("team_name"),
                            transaction.getList("members", ObjectId.class),
                            transaction.getInteger("amount"),
                            transaction.getObjectId("_id"),
                            user.getString("first_name") + " " + user.getString("last_name")
                    );
                    new Thread(() -> {
                        transaction.getList("members", ObjectId.class).forEach(memberId -> {
                            BadgeController.checkForUpgrade(memberId, Action.BUY_EXAM);
                            PointController.addPointForAction(memberId, Action.BUY_EXAM, transaction.getObjectId("products"), null);
                        });
                        if(!transaction.getList("members", ObjectId.class).contains(studentId)) {
                            BadgeController.checkForUpgrade(studentId, Action.BUY_EXAM);
                            PointController.addPointForAction(studentId, Action.BUY_EXAM, transaction.getObjectId("products"), null);
                        }
                    }).start();
                }


                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.COUNSELING_QUIZ.getName())
                ) {

                    Document quiz = schoolQuizRepository.findById(transaction.getObjectId("products"));

                    if (quiz != null) {

                        Document studentDoc = searchInDocumentsKeyVal(
                                quiz.getList("students", Document.class),
                                "_id", studentId
                        );

                        if (studentDoc != null) {
                            studentDoc.put("pay_at", System.currentTimeMillis());
                            studentDoc.put("paid", transaction.getInteger("amount"));
                            schoolQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);
                        }

                    }
                }


                if (transaction.containsKey("package_id")) {
                    Document thePackage = packageRepository.findById(transaction.getObjectId("package_id"));
                    if (thePackage != null) {
                        thePackage.put("buyers", (int) thePackage.getOrDefault("buyers", 0) + 1);
                        packageRepository.updateOne(thePackage.getObjectId("_id"), set("buyers", thePackage.get("buyers")));
                        packageRepository.clearFromCache(thePackage.getObjectId("_id"));
                    }
                }

                if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.BANK_EXAM.getName())
                ) {
                    new Thread(() -> {
                        BadgeController.checkForUpgrade(studentId, Action.BUY_QUESTION);
                        PointController.addPointForAction(studentId, Action.BUY_QUESTION, transaction.getObjectId("products"), null);
                    }).start();
                    Document quiz = customQuizRepository.findById(transaction.getObjectId("products"));
                    if (quiz != null) {
                        quiz.put("status", "paid");

                        PairValue p = irysc.gachesefid.Controllers.Quiz.Utility.getAnswersByteArrWithNeededTime(
                                quiz.getList("questions", ObjectId.class)
                        );

                        quiz.put("answers", p.getValue());
                        quiz.put("duration", p.getKey());
                        quiz.put("start_at", null);

                        customQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);

                        if (user.containsKey("mail")) {
                            new Thread(() -> sendMail(
                                    user.getString("mail"),
                                    SERVER + "recp/" + transaction.getObjectId("_id").toString(),
                                    "successQuiz",
                                    user.getString("first_name") + " " + user.getString("last_name")
                            )).start();
                        }

                    }
                } else if (transaction.get("products") instanceof ObjectId &&
                        transaction.getString("section").equals(OffCodeSections.CONTENT.getName())
                ) {
                    new Thread(() -> {
                        BadgeController.checkForUpgrade(studentId, Action.BUY_CONTENT);
                        PointController.addPointForAction(studentId, Action.BUY_CONTENT, transaction.getObjectId("products"), null);
                    }).start();
                    Document content = contentRepository.findById(transaction.getObjectId("products"));
                    if (content != null) {
                        StudentContentController.registry(
                                content.getObjectId("_id"),
                                studentId, ((Number) transaction.get("amount")).intValue(),
                                user.getOrDefault("phone", "").toString(),
                                user.getOrDefault("mail", "").toString()
                        );
                    }

                } else if (transaction.getString("section").equals(OffCodeSections.GACH_EXAM.getName())) {
                    List<ObjectId> products = transaction.getList("products", ObjectId.class);
                    if (!transaction.containsKey("student_ids")) {
                        List<ObjectId> iryscQuizIds = new ArrayList<>();
                        List<ObjectId> openQuizIds = new ArrayList<>();
                        List<ObjectId> escapeQuizIds = new ArrayList<>();

                        for (ObjectId id : products) {
                            if (iryscQuizRepository.findById(id) != null)
                                iryscQuizIds.add(id);
                            else if (openQuizRepository.findById(id) != null)
                                openQuizIds.add(id);
                            else if (escapeQuizRepository.findById(id) != null)
                                escapeQuizIds.add(id);
                        }

                        if (iryscQuizIds.size() > 0) {
                            regularQuizController
                                    .registry(studentId,
                                            user.getString("phone"),
                                            user.getString("mail"),
                                            iryscQuizIds,
                                            transaction.getInteger("amount"),
                                            transaction.getObjectId("_id"),
                                            user.getString("first_name") + " " + user.getString("last_name")
                                    );

                            tashrihiQuizController.registry(studentId,
                                    user.getString("phone"),
                                    user.getString("mail"),
                                    iryscQuizIds,
                                    transaction.getInteger("amount"),
                                    transaction.getObjectId("_id"),
                                    user.getString("first_name") + " " + user.getString("last_name")
                            );
                        }

                        if (openQuizIds.size() > 0)
                            openQuiz.registry(studentId,
                                    user.getString("phone"),
                                    user.getString("mail"),
                                    openQuizIds,
                                    transaction.getInteger("amount"),
                                    transaction.getObjectId("_id"),
                                    user.getString("first_name") + " " + user.getString("last_name")
                            );

                        if (escapeQuizIds.size() > 0)
                            escapeQuizController.registry(studentId,
                                    user.getString("phone"),
                                    user.getString("mail"),
                                    escapeQuizIds,
                                    transaction.getInteger("amount"),
                                    transaction.getObjectId("_id"),
                                    user.getString("first_name") + " " + user.getString("last_name")
                            );
                        new Thread(() -> {
                            products.forEach(productId ->
                                    PointController.addPointForAction(studentId, Action.BUY_EXAM, productId, null)
                            );
                            BadgeController.checkForUpgrade(studentId, Action.BUY_EXAM);
                        }).start();
                    } else {
                        regularQuizController.registry(transaction.getList("student_ids", ObjectId.class),
                                user.getString("phone"),
                                user.getString("mail"),
                                products,
                                transaction.getInteger("amount"));
                        // todo: group registration for tashrihi
                        new Thread(() -> {
                            transaction.getList("student_ids", ObjectId.class).forEach(stdId -> {
                                BadgeController.checkForUpgrade(stdId, Action.BUY_EXAM);
                                products.forEach(productId ->
                                        PointController.addPointForAction(stdId, Action.BUY_EXAM, productId, null)
                                );
                            });
                        }).start();
                    }
                } else if (transaction.getString("section").equals(OffCodeSections.OPEN_EXAM.getName())) {
                    List<ObjectId> products = transaction.getList("products", ObjectId.class);
                    openQuiz
                            .registry(studentId,
                                    user.getString("phone"),
                                    user.getString("mail"),
                                    products,
                                    transaction.getInteger("amount"),
                                    transaction.getObjectId("_id"),
                                    user.getString("first_name") + " " + user.getString("last_name")
                            );
                    new Thread(() -> {
                        BadgeController.checkForUpgrade(studentId, Action.BUY_EXAM);
                        products.forEach(productId ->
                                PointController.addPointForAction(studentId, Action.BUY_EXAM, productId, null)
                        );
                    }).start();

                }

                if (transaction.containsKey("off_code") &&
                        transaction.get("off_code") != null) {
                    Document off = offcodeRepository.findById(
                            transaction.getObjectId("off_code")
                    );
                    if (off != null) {

                        BasicDBObject update;

                        if (off.containsKey("is_public") &&
                                off.getBoolean("is_public")
                        ) {
                            List<ObjectId> students = off.getList("students", ObjectId.class);
                            students.add(studentId);
                            update = new BasicDBObject("students", students);
                        } else
                            update = new BasicDBObject("used", true)
                                    .append("used_at", System.currentTimeMillis())
                                    .append("used_section", transaction.getString("section"))
                                    .append("used_for", transaction.get("products"));

                        offcodeRepository.updateOne(
                                off.getObjectId("_id"),
                                new BasicDBObject("$set", update)
                        );
                    }
                }

            }

        }

    }

    public static String[] checkPay(
            String refId,
            String refCode,
            Long saleOrderId,
            Long saleRefId
    ) {

        Document transaction = null;

        if (refId != null) {
            transaction = transactionRepository.findOne(
                    eq("ref_id", refId), null
            );

            if (transaction == null)
                return null;

            if (transaction.getObjectId("user_id").toString().equals("635bff221f3dac4e5d0da698")) {

                transaction.put("sale_ref_id", saleRefId);
                transaction.put("status", "success");

                transactionRepository.replaceOne(
                        transaction.getObjectId("_id"),
                        transaction
                );

                completePay(transaction);

                return new String[]{
                        refId, transaction.getString("section"),
                        transaction.getObjectId("_id").toString()
                };
            }
        }

        if (refCode.equalsIgnoreCase("0")) {

            if (transaction == null)
                return null;

            String res = execPHP("verify.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);
//            System.out.println(res);

            if (res.startsWith("0")) {

                try {
                    transaction.put("sale_ref_id", saleRefId);
                    transaction.put("status", "success");

                    res = execPHP("settle.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);
                    transactionRepository.replaceOne(transaction.getObjectId("_id"), transaction);
                } catch (Exception ignore) {
                }

                completePay(transaction);

//                System.out.println(res);
                return new String[]{
                        refId, transaction.getString("section"),
                        transaction.getObjectId("_id").toString()
                };
            } else if (res.startsWith("43"))
                return new String[]{
                        refId, transaction.getString("section"),
                        transaction.getObjectId("_id").toString()
                };
            else {
                transaction.put("status", "fail");
                transactionRepository.replaceOne(transaction.getObjectId("_id"), transaction);
                return null;
            }
        }

        return null;
    }

    public static String chargeAccount(ObjectId userId, int amount) {

        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document doc =
                new Document("user_id", userId)
                        .append("amount", amount)
                        .append("created_at", System.currentTimeMillis())
                        .append("status", "init")
                        .append("order_id", orderId)
                        .append("section", "charge")
                        .append("off_code", null);

        return goToPayment(amount, doc);
    }

    public static String goToPayment(int price, Document transaction) {

        String output = execPHP("pay.php", (price * 10) + " " + transaction.getLong("order_id"));

        if (output.startsWith("0,")) {
            transaction.append("ref_id", output.substring(2));
            transactionRepository.insertOne(transaction);
            return generateSuccessMsg("refId", output.substring(2),
                    new PairValue("action", "pay"),
                    new PairValue("transactionId", "")
            );
        }

        return JSON_NOT_UNKNOWN;
    }

    public static String myTransactions(ObjectId userId) {

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("user_id", userId));
        filters.add(eq("status", "success"));

        ArrayList<Document> transactions = transactionRepository.find(and(filters), null, Sorts.descending("created_at"));
        JSONArray jsonArray = new JSONArray();

        for (Document transaction : transactions) {
            JSONObject jsonObject = new JSONObject()
                    .put("id", transaction.getObjectId("_id").toString())
                    .put("for", getTransactionTitle(transaction))
                    .put("account", transaction.getOrDefault("account_money", 0))
                    .put("offAmount", transaction.getOrDefault("off_amount", 0))
                    .put("paid", transaction.get("amount"))
                    .put("refId", transaction.getOrDefault("ref_id", ""))
                    .put("createdAt", getSolarDate(transaction.getLong("created_at")));

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

}
