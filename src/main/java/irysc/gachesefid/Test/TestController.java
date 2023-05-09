package irysc.gachesefid.Test;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Test.User.UserTestController;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static irysc.gachesefid.Test.Utility.getNotifs;

public abstract class TestController {

    public static long startAt = -1;
    public String prefix;
    public String adminToken;
    public ObjectId id;
    protected int assertionIterator = 0;
    protected UserTestController userTestController = null;
    protected ArrayList<String> wantedKeys;
    protected HashMap<String, Integer> wantedKeysWithValues;


    protected void checkAlerts(ArrayList<String> wantedKeys, int iteration,
                               HashMap<String, Integer> expected, PairValue... wantedForInc
    ) throws Exception {

        for(PairValue p : wantedForInc)
            expected.put(p.getKey().toString(), expected.get(p.getKey().toString()) + (int)p.getValue());

        doCheckAlerts(wantedKeys, iteration, expected);
    }

    private void doCheckAlerts(ArrayList<String> wantedKeys, int iteration,
                                      HashMap<String, Integer> expected
    ) throws Exception {

        JSONObject alerts = getNotifs();
        try {
            for (String key : alerts.keySet()) {

                if (!wantedKeys.contains(key))
                    continue;

                assert alerts.getInt(key) == expected.get(key) :
                        generateAssertString(key, iteration, expected.get(key), alerts.getInt(key));
            }
        }
        catch (AssertionError x) {
            clear();
            throw x;
        }

    }

    public static String generateAssertString(String key, int iteration, Object expected, Object real) {
        return key + " not expected in iteration: " + iteration + " *** " + expected + " vs. " + real;
    }

    public static int searchInJSONArray(JSONArray jsonArray, String key, Object val) {

        for(int i = 0; i < jsonArray.length(); i++) {
            if(jsonArray.getJSONObject(i).get(key).equals(val))
                return i;
        }

        return -1;
    }

    public void init(String folder, String apiPrefix) throws Exception {

        if (startAt == -1)
            startAt = System.currentTimeMillis();

        prefix = apiPrefix;
        adminToken = Utility.signIn(Utility.adminUsername);

        ArrayList<JSONObject> testCases = Utility.readTestCases(folder);

        setWantedNotifKeys();

        for (JSONObject testCase : testCases) {

            try {
                assertionIterator = 0;
                run(testCase);
            } catch (Exception x) {
                superClear();
                throw x;
            }

            superClear();
            break;
        }

    }

    private void superClear() {

        if(userTestController != null)
            userTestController.clear();

        clear();
    }

    public abstract void clear();

    public abstract void setWantedNotifKeys();

    public abstract void run(JSONObject testcase) throws Exception;
}
