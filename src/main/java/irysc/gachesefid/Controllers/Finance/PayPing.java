package irysc.gachesefid.Controllers.Finance;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Quiz.RegularQuizController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
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
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_UNKNOWN;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.*;

public class PayPing {

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

    private static void completePay(Document transaction)  {

        ObjectId studentId = transaction.getObjectId("user_id");
        Document user = userRepository.findById(studentId);
        if(user != null) {
            user.put("money", 0);
            userRepository.replaceOne(
                    user.getObjectId("_id"), user
            );

            List<ObjectId> products = transaction.getList("products", ObjectId.class);

            new RegularQuizController()
                    .registry(studentId,
                            user.getString("phone"),
                            user.getString("mail"),
                            products,
                            transaction.getInteger("amount"));

            if(transaction.containsKey("off_code")) {
                Document off = offcodeRepository.findById(
                        transaction.getObjectId("off_code")
                );
                if(off != null) {

                    BasicDBObject update;

                    if(off.containsKey("is_public") &&
                            off.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = off.getList("students", ObjectId.class);
                        students.add(studentId);
                        update = new BasicDBObject("students", students);
                    }
                    else
                        update = new BasicDBObject("used", true)
                                .append("used_at", System.currentTimeMillis())
                                .append("used_section", transaction.getString("section"))
                                .append("used_for", products);

                    offcodeRepository.updateOne(
                            off.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );


                }
            }

        }

    }

    public static String checkPay(
            String refId,
            String refCode,
            Long saleOrderId,
            Long saleRefId
    ) {

        System.out.println("ref code is " + refCode);

        if(1 == 1) {

            Document transaction = transactionRepository.findOne(
                    eq("ref_id", refId), null
            );

            transaction.put("sale_ref_id", saleRefId);
            transaction.put("status", "success");

            transactionRepository.replaceOne(
                    transaction.getObjectId("_id"),
                    transaction
            );

            new Thread(() -> {
                completePay(transaction);
            }).start();

            return refId;
        }

        if (refCode.equalsIgnoreCase("0")) {

            Document transaction = transactionRepository.findOne(
                    eq("ref_id", refId), null
            );

            if (transaction == null)
                return null;

            String res = execPHP("verify.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);
            System.out.println(res);

            if (res.startsWith("0")) {

                transaction.put("sale_ref_id", saleRefId);
                transaction.put("status", "success");

                res = execPHP("settle.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);

                new Thread(() -> {
                    completePay(transaction);
                }).start();
                System.out.println(res);
                return refId;
            }
            else if(res.startsWith("43"))
                return refId;
            else {
                transaction.put("status", "fail");
                transactionRepository.replaceOne(transaction.getObjectId("_id"), transaction);
                return null;
            }
        }

        return null;
    }

    public static String goToPayment(int price, Document transaction) {

        String output = execPHP("pay.php", price + " " + transaction.getLong("order_id"));
        System.out.println(output);

        if (output.startsWith("0,")) {
            transaction.append("ref_id", output.substring(2));
            transactionRepository.insertOne(transaction);
            return generateSuccessMsg("refId", output.substring(2),
                    new PairValue("action", "pay")
            );
        }

        return JSON_NOT_UNKNOWN;
    }

    public static String pay() {

        long orderId = Math.abs(new Random().nextLong());
        while (transactionRepository.exist(
                eq("order_id", orderId)
        )) {
            orderId = Math.abs(new Random().nextLong());
        }

        Document doc = new Document()
                .append("order_id", orderId)
                .append("amount", 10000)
                .append("status", "init")
                .append("created_at", System.currentTimeMillis());

        System.out.println(orderId);
        String output = execPHP("pay.php", "50000 " + orderId);
        System.out.println(output);

        if (output.startsWith("0,")) {
            System.out.println("good");
            doc.append("ref_id", output.substring(2));
            transactionRepository.insertOne(doc);
            return generateSuccessMsg("data", output.substring(2));
        }

        return JSON_NOT_UNKNOWN;
    }

    public static String myTransactions(ObjectId userId,
                                        String usedFor,
                                        Boolean useOffCode) {

        if (usedFor != null &&
                !usedFor.equals("class") &&
                !usedFor.equals("pay_link")
        )
            return JSON_NOT_VALID_PARAMS;

        ArrayList<Bson> filters = new ArrayList<>();
        filters.add(eq("user_id", userId));
        filters.add(eq("status", "success"));

        if (usedFor != null)
            filters.add(eq("for", usedFor));

        if (useOffCode != null)
            filters.add(exists("off_code", useOffCode));

        ArrayList<Document> transactions = new ArrayList<>();
//        ArrayList<Document> transactions = transactionRepository.findWithJoin(match(
//                and(filters)
//                ), null, null, null, null, null, null,
//                null, null, null, null, null);

        JSONArray jsonArray = new JSONArray();

        for (Document transaction : transactions) {

            List<Document> offCodes = (transaction.containsKey("offCode")) ?
                    transaction.getList("offCode", Document.class) : null;

            Document offCode = (offCodes != null && offCodes.size() > 0) ? offCodes.get(0) : null;

            if (offCode != null)
                offCode.remove("_id");

            JSONObject jsonObject = new JSONObject()
                    .put("offCode", offCode)
                    .put("amount", transaction.getInteger("amount"))
                    .put("refId", transaction.get("ref_id"))
                    .put("createdAt", getSolarDate(transaction.getLong("created_at")));

//            addRightObjectToTransactionJSON(transaction, jsonObject);
            jsonArray.put(jsonObject);
        }

        return new JSONObject().put("status", "ok").put("data", jsonArray).toString();
    }

}
