package irysc.gachesefid.Test.Finantial;

import irysc.gachesefid.Exception.InvalidFieldsException;
import org.json.JSONObject;

import static irysc.gachesefid.Test.Utility.adminToken;
import static irysc.gachesefid.Test.Utility.sendPostReq;

public class OffCodeTestController {

    public static void createOff(JSONObject data) throws InvalidFieldsException {
        sendPostReq("admin/off/store", adminToken, data);
    }

}
