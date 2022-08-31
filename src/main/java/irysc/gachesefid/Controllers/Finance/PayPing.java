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
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class PayPing {

    private static String execPHP(String scriptName, String param) {

        StringBuilder output = new StringBuilder();

        try {
            String line;
            Process p = Runtime.getRuntime().exec("php " + scriptName + " " + param);
            BufferedReader input =
                    new BufferedReader
                            (new InputStreamReader(p.getInputStream()));

            while ((line = input.readLine()) != null) {
                output.append(line);
            }
            input.close();
        }
        catch (Exception err) {
            err.printStackTrace();
        }

        return output.toString();
    }

    public static String pay() {
        String output = execPHP("pay.php", "20000 123 ''");
        return output;
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
