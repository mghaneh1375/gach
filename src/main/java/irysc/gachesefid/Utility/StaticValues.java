package irysc.gachesefid.Utility;


import com.mongodb.BasicDBObject;
import org.json.JSONObject;


public class StaticValues {

    public final static String STATICS_SERVER = "https://statics.irysc.com/";
//    public final static String STATICS_SERVER = "http://192.168.0.106/";
//    public final static String STATICS_SERVER = "http://192.168.1.100/static/assets/";

    public final static String mailUserName = "no-reply@okft.org";
    public final static String mailPassword = "ASDFG12345!@#$%^assa"; //wb1DPR8PZ

    public static final String KEY = "okfbogendesignmohammadghane1375!";

    public static final long ONE_DAY_MIL_SEC = 86400000;

    public final static long TOKEN_EXPIRATION_MSEC = 60 * 60 * 24 * 7 * 1000;
    public final static int TOKEN_EXPIRATION = 60 * 60 * 24 * 7;

    public final static int MAX_QUIZ_ATTACH_SIZE = 5; // MB

    public static int STUDENTS = 0;
    public static int QUESTIONS = 0;
    public static int SCHOOLS = 0;

    public final static int SMS_RESEND_SEC = 60; // 300
    public final static int SMS_RESEND_MSEC = 1000 * SMS_RESEND_SEC;
    public final static int SMS_VALIDATION_EXPIRATION_MSEC = 1000 * SMS_RESEND_SEC;

    public final static int USER_LIMIT_CACHE_SIZE = 3000;
    public final static int USER_EXPIRATION_SEC = 60 * 60 * 24 * 7;

    public final static int CLASS_LIMIT_CACHE_SIZE = 100;
    public final static int CLASS_EXPIRATION_SEC = 60 * 60 * 24 * 7;

    public final static int MAX_TICKET_FILE_SIZE = 1024 * 1024 * 6;

    public final static int ONE_MB = 1024 * 1024;
    public final static int MAX_QUESTION_FILE_SIZE = ONE_MB;

    public final static int MAX_FILE_SIZE = ONE_MB * 6;

    public final static int ITEMS_PER_PAGE = 20;

    public final static boolean LOCAL = true;
    public final static boolean DEV_MODE = false;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public final static String SERVER = (StaticValues.DEV_MODE) ?
            (StaticValues.LOCAL) ? "http://localhost:3000/" :
                    "http://185.239.106.26:8085/" : "https://okft.org/";

    public final static int MAX_OBJECT_ID_SIZE = 100;
    public final static int MIN_OBJECT_ID_SIZE = 20;

    public final static BasicDBObject TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS =
            new BasicDBObject("title", 1).append("_id", 1)
            .append("start", 1).append("end", 1);

    public final static BasicDBObject QUIZ_DIGEST =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start", 1).append("end", 1)
                    .append("start_registry", 1)
                    .append("end_registry", 1)
                    .append("report_status", 1)
                    .append("tags", 1).append("mode", 1)
                    .append("price", 1).append("launch_mode", 1)
                    .append("capacity", 1).append("registered", 1)
            ;

    public final static BasicDBObject QUIZ_DIGEST_MANAGEMENT =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start", 1).append("end", 1)
                    .append("start_registry", 1)
                    .append("end_registry", 1)
                    .append("visibility", 1)
                    .append("report_status", 1)
                    .append("tags", 1).append("mode", 1)
                    .append("price", 1).append("launch_mode", 1)
                    .append("capacity", 1).append("registered", 1)
                    .append("questions", 1)
            ;

    public final static BasicDBObject USER_DIGEST = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("NID", 1)
            .append("pic", 1);

    public final static BasicDBObject USER_MANAGEMENT_INFO_DIGEST = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("NID", 1)
            .append("mail", 1)
            .append("phone", 1)
            .append("coin", 1)
            .append("accesses", 1)
            .append("status", 1)
            .append("school", 1);

    public static final BasicDBObject JUST_ID = new BasicDBObject("_id", 1);

    public static final String JSON_OK = new JSONObject().put("status", "ok").toString();
    public static final String JSON_NOT_VALID_TOKEN = new JSONObject().put("status", "nok").put("msg", "token is not valid").toString();
    public static final String JSON_NOT_ACCESS = new JSONObject().put("status", "nok").put("msg", "no access to this method").toString();
    public static final String JSON_NOT_VALID = new JSONObject().put("status", "nok").put("msg", "json not valid").toString();
    public static final String JSON_NOT_VALID_ID = new JSONObject().put("status", "nok").put("msg", "id is not valid").toString();
    public static final String JSON_NOT_VALID_PARAMS = new JSONObject().put("status", "nok").put("msg", "params is not valid").toString();
    public static final String JSON_NOT_UNKNOWN = new JSONObject().put("status", "nok").put("msg", "unknown exception has been occurred").toString();
    public static final String JSON_NOT_VERSION = new JSONObject().put("status", "nok").put("msg", "ورژن جدیدی از این api در دسترس است که باید از آن استفاده کنید.").toString();
    public static final String JSON_NOT_VALID_FILE = new JSONObject().put("status", "nok").put("msg", "شما در این قسمت می توانید تنها فایل PDF و یا یک فایل صوتی و یا یک تصویر آپلود نمایید.").toString();
    public static final String JSON_NOT_VALID_6_MB_SIZE = new JSONObject().put("status", "nok").put("msg", "حداکثر حجم مجاز، 6 مگ است.").toString();
    public static final String JSON_UNKNOWN_UPLOAD_FILE = new JSONObject().put("status", "nok").put("msg", "مشکلی در آپلود فایل مورد نظر رخ داده است. لطفا با پشتیبانی تماس بگیرید.").toString();
}
