package irysc.gachesefid.Utility;


import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Quiz.*;
import org.json.JSONObject;


public class StaticValues {

    public static final OnlineStandingController onlineStandingController =
            new OnlineStandingController();
    public static final EscapeQuizController escapeQuizController =
            new EscapeQuizController();
    public static final OpenQuiz openQuiz = new OpenQuiz();
    public static final RegularQuizController regularQuizController = new RegularQuizController();
    public static final TashrihiQuizController tashrihiQuizController = new TashrihiQuizController();

    public final static String STATICS_SERVER = "https://statics.irysc.com/";
    public final static String VIDEO_STATICS_SERVER = "https://v1.irysc.com/";

//    public final static String STATICS_SERVER = "http://localstaticgach.com/";


    public final static String mailUserName = "noreply@irysc.com";
    public final static String mailPassword = "wb1DPR8PZ"; //wb1DPR8PZ

    public static final String KEY = "okfbogendesignmohammadghane1375!";

    public static final long ONE_MIN_MSEC = 60000;
    public static final long ONE_HOUR_MIL_SEC = 60 * ONE_MIN_MSEC;
    public static final long ONE_DAY_MIL_SEC = 24 * ONE_HOUR_MIL_SEC;
    public static final long TWO_DAY_MIL_SEC = 86400000 * 2;

    public final static long TOKEN_EXPIRATION_MSEC = 60 * 60 * 24 * 7 * 1000;
    public final static int TOKEN_EXPIRATION = 60 * 60 * 24 * 7;

    public final static int ONE_MB = 1024 * 1024;
    public final static int MAX_QUIZ_ATTACH_SIZE = 5 * ONE_MB; // MB

    public static int STUDENTS = 0;
    public static int QUESTIONS = 0;
    public static int SCHOOLS = 0;

    public final static int SMS_RESEND_SEC = 60; // 300
    public final static int SMS_RESEND_MSEC = 1000 * SMS_RESEND_SEC;
    public final static int SMS_VALIDATION_EXPIRATION_MSEC = 1000 * SMS_RESEND_SEC;
    public final static int SMS_VALIDATION_EXPIRATION_MSEC_LONG = 1000 * SMS_RESEND_SEC * 3;

    public final static long PAY_SEMI_PRIVATE_CLASS_EXPIRATION_MSEC = ONE_HOUR_MIL_SEC * 24;
    public final static long PAY_SCHEDULE_EXPIRATION_MSEC = ONE_HOUR_MIL_SEC * 3;
    public final static long SET_STATUS_TEACH_REQUEST_EXPIRATION_MSEC = ONE_HOUR_MIL_SEC * 3;

    public final static int CLASS_LIMIT_CACHE_SIZE = 300;
    public final static int CLASS_EXPIRATION_SEC = 60 * 60 * 24 * 2;

    public final static int MAX_TICKET_FILE_SIZE = 1024 * 1024 * 6;

    public final static int MAX_QUESTION_FILE_SIZE = ONE_MB;

    public final static int MAX_FILE_SIZE = ONE_MB * 6;
    public final static int MAX_ADV_FILE_SIZE = ONE_MB * 15;
    public final static int MAX_DAILY_ADV_FILE_SIZE = ONE_MB * 25;

    public final static long SERVER_TOKEN_EXPIRATION_MSEC = 20 * 1000; // 20 s

    public final static boolean LOCAL = true;
    public final static boolean DEV_MODE = false;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public final static String SERVER = (StaticValues.DEV_MODE) ?
            (StaticValues.LOCAL) ? "http://localhost:3000/" :
                    "http://185.239.106.26:8085/" : "https://e.irysc.com/";

    public final static int MAX_OBJECT_ID_SIZE = 100;
    public final static int MIN_OBJECT_ID_SIZE = 20;

    public final static BasicDBObject TASHRIHI_QUIZ_DIGEST_FOR_TEACHERS =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("correctors", 1).append("students", 1)
                    .append("questions", 1)
                    .append("start", 1).append("end", 1);

    public final static BasicDBObject ADVISOR_PUBLIC_DIGEST =
            new BasicDBObject("first_name", 1).append("_id", 1)
                    .append("last_name", 1).append("accept_std", 1)
                    .append("rate", 1).append("birth_day", 1)
                    .append("rate_count", 1).append("tags", 1)
                    .append("students", 1).append("form_list", 1)
                    .append("advice_bio", 1).append("advice_video_link", 1)
                    .append("pic", 1).append("advisor_priority", 1)
            ;

    public final static BasicDBObject SCHEDULE_DIGEST_FOR_TEACHER =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start_at", 1).append("length", 1)
                    .append("created_at", 1).append("visibility", 1)
                    .append("price", 1).append("teach_mode", 1)
                    .append("students", 1).append("requests", 1)
                    .append("time", 1).append("min_cap", 1)
                    .append("max_cap", 1).append("user_id", 1)
                    .append("can_request", 1).append("need_registry_confirmation", 1)
                    .append("sky_room_url", 1).append("sessions_count", 1)
                    .append("sky_room_urls", 1)
                    .append("start_date", 1).append("end_date", 1)
                    .append("end_registration", 1);

    public final static BasicDBObject TEACH_PUBLIC_DIGEST =
            new BasicDBObject("first_name", 1).append("_id", 1)
                    .append("last_name", 1)
                    .append("teach_rate", 1).append("birth_day", 1)
                    .append("teach_rate_count", 1).append("teach_tags", 1)
                    .append("teach_bio", 1).append("teach_video_link", 1)
                    .append("pic", 1).append("teaches", 1)
                    .append("form_list", 1).append("teach_branches", 1)
                    .append("teach_lessons", 1).append("teach_grades", 1)
                    .append("teach_priority", 1)
            ;

    public final static BasicDBObject QUIZ_DIGEST =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start", 1).append("end", 1)
                    .append("start_registry", 1).append("pdf_quiz", 1)
                    .append("end_registry", 1)
                    .append("report_status", 1)
                    .append("tags", 1).append("mode", 1)
                    .append("price", 1).append("launch_mode", 1)
                    .append("capacity", 1).append("registered", 1)
            ;

    public final static BasicDBObject QUIZ_DIGEST_MANAGEMENT =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start", 1).append("end", 1)
                    .append("start_registry", 1).append("pdf_quiz", 1)
                    .append("end_registry", 1).append("q_no", 1)
                    .append("visibility", 1).append("question_file", 1)
                    .append("database", 1).append("status", 1)
                    .append("report_status", 1)
                    .append("tags", 1).append("mode", 1)
                    .append("price", 1).append("launch_mode", 1)
                    .append("capacity", 1).append("registered", 1)
                    .append("questions", 1)
                    .append("is_uploadable", 1)
                    .append("is_q_r_needed", 1)
                    .append("is_registrable", 1)
                    .append("max_teams", 1).append("per_team", 1)
            ;

    public final static BasicDBObject HW_DIGEST_MANAGEMENT =
            new BasicDBObject("title", 1).append("_id", 1)
                    .append("start", 1).append("end", 1)
                    .append("visibility", 1)
                    .append("status", 1)
                    .append("report_status", 1)
                    .append("registered", 1)
                    .append("registered", 1)
                    .append("attaches", 1)
                    .append("delay_end", 1)
            ;


    public final static BasicDBObject JUST_RANK = new BasicDBObject("rank", 1);

    public final static BasicDBObject CONTENT_DIGEST = new BasicDBObject("_id", 1)
            .append("title", 1).append("teacher", 1)
            .append("cert_id", 1).append("img", 1)
            .append("price", 1).append("slug", 1)
            .append("created_at", 1).append("tags", 1)
            .append("final_exam_id", 1).append("rate", 1)
            .append("off_type", 1).append("off", 1)
            .append("off_start", 1).append("off_expiration", 1)
            .append("level", 1)
            .append("duration", 1).append("sessions_count", 1);

    public final static BasicDBObject CONTENT_DIGEST_FOR_ADMIN = new BasicDBObject("_id", 1)
            .append("title", 1).append("teacher", 1)
            .append("cert_id", 1).append("img", 1)
            .append("created_at", 1).append("tags", 1)
            .append("duration", 1).append("users", 1)
            .append("final_exam_id", 1).append("price", 1)
            .append("slug", 1).append("priority", 1)
            .append("level", 1)
            .append("sessions_count", 1).append("visibility", 1);

    public final static BasicDBObject USER_DIGEST = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("NID", 1)
            .append("pic", 1);

    public final static BasicDBObject TEACHER_PUBLIC_INFO = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("rate", 1)
            .append("teach_rate", 1)
            .append("pic", 1);

    public final static BasicDBObject ADVISOR_PUBLIC_INFO = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("rate", 1)
            .append("rate", 1)
            .append("pic", 1);

    public final static BasicDBObject USER_TEACH_INFO = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("school", 1)
            .append("grade", 1)
            .append("branches", 1)
            .append("rank", 1)
            .append("pic", 1)
            .append("teach_rate", 1);
    public final static BasicDBObject USER_MANAGEMENT_INFO_DIGEST = new BasicDBObject("_id", 1)
            .append("first_name", 1)
            .append("last_name", 1)
            .append("NID", 1)
            .append("mail", 1)
            .append("phone", 1)
            .append("coin", 1)
            .append("money", 1)
            .append("accesses", 1)
            .append("status", 1)
            .append("school", 1)
            .append("sex", 1)
            .append("city", 1)
            .append("branches", 1)
            .append("created_at", 1)
            .append("grade", 1)
            .append("teach_priority", 1)
            .append("advisor_priority", 1);

    public static final BasicDBObject JUST_ID = new BasicDBObject("_id", 1);
    public static final BasicDBObject JUST_USER_ID = new BasicDBObject("user_id", 1);
    public static final BasicDBObject JUST_NAME = new BasicDBObject("first_name", true).append("last_name", true);
    public static final BasicDBObject JUST_NAME_ = new BasicDBObject("name", true);
    public static final BasicDBObject JUST_TITLE = new BasicDBObject("title", true);

    public static final String JSON_OK = new JSONObject().put("status", "ok").toString();
    public static final String JSON_NOT_VALID_TOKEN = new JSONObject().put("status", "nok").put("msg", "token is not valid").toString();
    public static final String JSON_NOT_ACCESS = new JSONObject().put("status", "nok").put("msg", "no access to this method").toString();
    public static final String JSON_NOT_VALID = new JSONObject().put("status", "nok").put("msg", "json not valid").toString();
    public static final String JSON_NOT_VALID_ID = new JSONObject().put("status", "nok").put("msg", "id is not valid").toString();
    public static final String JSON_NOT_VALID_PARAMS = new JSONObject().put("status", "nok").put("msg", "params is not valid").toString();
    public static final String JSON_NOT_UNKNOWN = new JSONObject().put("status", "nok").put("msg", "unknown exception has been occurred").toString();
    public static final String JSON_NOT_VALID_FILE = new JSONObject().put("status", "nok").put("msg", "شما در این قسمت می توانید تنها فایل PDF و یا یک فایل صوتی و یا یک تصویر آپلود نمایید.").toString();
    public static final String JSON_NOT_VALID_6_MB_SIZE = new JSONObject().put("status", "nok").put("msg", "حداکثر حجم مجاز، 6 مگ است.").toString();
    public static final String JSON_UNKNOWN_UPLOAD_FILE = new JSONObject().put("status", "nok").put("msg", "مشکلی در آپلود فایل مورد نظر رخ داده است. لطفا با پشتیبانی تماس بگیرید.").toString();
}
