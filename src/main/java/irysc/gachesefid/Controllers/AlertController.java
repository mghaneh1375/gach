package irysc.gachesefid.Controllers;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static irysc.gachesefid.Main.GachesefidApplication.alertsRepository;

public class AlertController {

    public static String newAlerts() {

        JSONObject jsonObject = new JSONObject();
        //todo : imp
//        for(String itr : newThingsCache.keySet())
//            jsonObject.put(itr, newThingsCache.get(itr));

        return new JSONObject().put("status", "ok").put("data", jsonObject).toString();
    }

    public static void store(ObjectId userId, String msg, boolean needSMS) {
        alertsRepository.insertOne(
                new Document("msg", msg).append("owner", userId).append("seen", false).append("created_at", System.currentTimeMillis())
        );
    }

    public static void store(ObjectId userId, String msg, boolean needSMS, PairValue mail,
                             String mailMsg, String username, String subject) {

        alertsRepository.insertOne(
                new Document("msg", msg)
                        .append("owner", userId)
                        .append("seen", false)
                        .append("created_at", System.currentTimeMillis())
        );

        if(mail != null)
            Utility.sendMail((String) mail.getValue(), mailMsg,
                subject, (String)mail.getKey(), username
            );

    }

    public static void storeClassRegistryAlert(ObjectId userId, String msg, boolean needSMS, PairValue mail,
                             String mailMsg, String username, String term, String endRegistry, String price, String classId) {

        alertsRepository.insertOne(
                new Document("msg", msg).append("owner", userId).append("seen", false).append("created_at", System.currentTimeMillis())
        );

        if(mail != null)
            Utility.sendClassRegistryMail((String) mail.getValue(), mailMsg,
                    term, endRegistry, username, (String) mail.getKey(), price, classId
            );

    }

    public static void storeAdmin(String msg, boolean needSMS, boolean sendMail) {
        alertsRepository.insertOne(
                new Document("msg", msg).append("owner", "admin").append("seen", false).append("created_at", System.currentTimeMillis())
        );
    }
}
