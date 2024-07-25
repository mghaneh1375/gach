package irysc.gachesefid.Controllers;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class AdminStatsController {

    public static String dashboard() {
        return generateSuccessMsg("data", "");
    }

    public static String stats() {

        JSONObject data = new JSONObject();

        fillStat(
                transactionRepository, and(
                        eq("status", "success"),
                        eq("section", "bank_exam")
                ), "bankExam", data, false
        );

        fillStat(
                transactionRepository, and(
                        eq("status", "success"),
                        eq("section", "gach_exam")
                ), "gachExam", data, false
        );

        fillStat(
                transactionRepository, and(
                        eq("status", "success"),
                        eq("section", "content")
                ), "content", data, false
        );

        fillStat(
                userRepository, and(
                        eq("level", false),
                        eq("status", "active")
                ), "registry", data, false
        );

        fillStat(
                advisorRequestsRepository, and(
                        exists("user_id")
                ), "advisorRequest", data, false
        );

        return generateSuccessMsg("data", data);
    }

    private static void fillStat(Common db, Bson filter, String title, JSONObject jsonObject, boolean isNeedLastMonth) {

        long yesterday = System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC;
        long lastWeek = System.currentTimeMillis() - 7 * StaticValues.ONE_DAY_MIL_SEC;

        int statCount = db.count(filter);

        int statCountLast24 = db.count(
                and(
                        filter,
                        gte("created_at", yesterday)
                )
        );

        int statCountLastWeek = db.count(
                and(
                        filter,
                        gte("created_at", lastWeek)
                )
        );

        jsonObject.put(title, statCount)
                .put(title + "Last24", statCountLast24)
                .put(title + "LastWeek", statCountLastWeek);

        if (isNeedLastMonth) {
            jsonObject.put(title + "LastMonth", db.count(
                    and(
                            filter,
                            gte("created_at", System.currentTimeMillis() - 30 * StaticValues.ONE_DAY_MIL_SEC)
                    )
            ));
        }

    }

}
