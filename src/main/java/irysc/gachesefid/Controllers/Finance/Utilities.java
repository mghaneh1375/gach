package irysc.gachesefid.Controllers.Finance;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.offcodeRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID;

public class Utilities {

    private static String token = "9cc0e270b91f9bd94a494a81cdaa4ee9e114234a5dd2ea60d27f7625602cddbd";
    private static String BASE_URL = "https://api.payping.ir/v1/";
    private static String RETURN_URL = "https://okft.org/validatePayment";

    // {payAmount, {offCodeId, offCodeFinalVal}}
    public static PairValue calcPrice(int price,
                                      int userMoney,
                                      ObjectId userId,
                                      String offCodeSection) {

        int pay = Math.max(0, price - userMoney);
        int today = Utility.getToday();
        PairValue offCode = null;

        if (pay > 0) {

            ArrayList<Document> offs = offcodeRepository.find(and(
                    gte("expire_at", today),
                    gt("amount", 0),
                    eq("used", false),
                    eq("user_id", userId),
                    or(
                            eq("section", "all"),
                            eq("section", offCodeSection)
                    )
                    ), null
            );

            Document off = null;

            for (int i = 0; i < offs.size(); i++) {
                if (
                        off == null ||
                                off.getInteger("amount") < offs.get(i).getInteger("amount")
                )
                    off = offs.get(i);
            }

            if (off != null) {

                int offcodeValFinal = off.getInteger("amount");

                if (off.getString("type").equals("percent")) {
                    if (off.getInteger("amount") > 100) {
                        pay = 0;
                        offcodeValFinal = 100;
                    } else
                        pay *= (100 - off.getInteger("amount")) / 100.0;
                } else {

                    if (off.getInteger("amount") > pay)
                        offcodeValFinal = pay;

                    pay = Math.max(0, pay - off.getInteger("amount"));
                }

                offCode = new PairValue(off.getObjectId("_id"), offcodeValFinal);
            }

        }

        return new PairValue(pay, offCode);
    }

    public static String goToPayment(int price, ObjectId userId, ObjectId transactionId) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("amount", price);
        jsonObject.put("payerIdentity", userId);
        jsonObject.put("returnUrl", RETURN_URL);
        jsonObject.put("clientRefId", transactionId);

        try {

            HttpResponse<JsonNode> response = Unirest.post(BASE_URL + "pay")
                    .header("Content-type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(jsonObject).asJson();


            if (response.getStatus() == 200) {
                String payUrl = BASE_URL + "pay/gotoipg/" + response.getBody().getObject().get("code");

                return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                        "action", "pay", new PairValue("url", payUrl)
                );

            }

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return JSON_NOT_VALID;
    }

}
