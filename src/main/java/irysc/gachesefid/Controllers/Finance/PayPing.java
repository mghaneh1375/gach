package irysc.gachesefid.Controllers.Finance;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.transactionRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_UNKNOWN;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

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

    public static void checkPay(
            String refId,
            String refCode,
            Long saleOrderId,
            Long saleRefId
    ) {
        if(refCode.equalsIgnoreCase("0")) {

            Document transaction = transactionRepository.findOne(
                    eq("ref_id", refId), null
            );

            if(transaction == null)
                return;

            transaction.put("sale_ref_id", saleRefId);
            String res = execPHP("verify.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);
            System.out.println(res);
//            if(res.startsWith("0"))
//                execPHP("settle.php", transaction.get("order_id").toString() + " " + saleOrderId + " " + saleRefId);
        }

    }


    public static String pay() {

        if (1 == 1)
            return generateSuccessMsg("data", "D2DA7137D0EAF22B");

        ObjectId orderId = new ObjectId();

        Document doc = new Document()
                .append("_id", orderId)
                .append("amount", 10000)
                .append("created_at", System.currentTimeMillis());

        String output = execPHP("pay.php", "10000 " + orderId);
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
