package irysc.gachesefid.Utility;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.KavenegarApi;
import irysc.gachesefid.Kavenegar.excepctions.ApiException;
import irysc.gachesefid.Kavenegar.excepctions.HttpException;
import irysc.gachesefid.Kavenegar.models.SendResult;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Validator.DateValidator;
import irysc.gachesefid.Validator.PhoneValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.gt;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.EmailUtil.emailFooterSection;
import static irysc.gachesefid.Utility.StaticValues.*;

public class Utility {

    private static final Pattern postalCodePattern = Pattern.compile("^\\d{10}$");
    private static final Pattern justNumPattern = Pattern.compile("^\\d+$");
    private static final Pattern passwordStrengthPattern = Pattern.compile("^(?=.*[0-9])(?=.*[A-z])(?=\\S+$).{8,}$");
    private static final Pattern mailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String ABC = "0123456789";
    private static final String ABCD = "ABCDEFGHIJKLMNOPQRSTUVWXY";

    private static Random random = new Random();
    private static SecureRandom rnd = new SecureRandom();

    public static ArrayList<Document> searchInDocumentsKeyValMulti(List<Document> arr, String key, Object val) {

        if (arr == null)
            return null;

        ArrayList<Document> docs = new ArrayList<>();

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val))
                docs.add(doc);
        }

        return docs;
    }

    public static Document searchInDocumentsKeyVal(List<Document> arr, String key, Object val) {

        if (arr == null)
            return null;

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val))
                return doc;
        }

        return null;
    }

    public static Document searchInDocumentsKeyVal(List<Document> arr, String key, Object val,
                                                   String key2, Object val2) {

        if (arr == null)
            return null;

        for (Document doc : arr) {
            if (doc.containsKey(key) && doc.get(key).equals(val) &&
                    doc.containsKey(key2) && (
                    (val2 == null && doc.get(key2) == null) ||
                            (doc.get(key2) != null && doc.get(key2).equals(val2))
            ))
                return doc;
        }

        return null;
    }

    public static int searchInDocumentsKeyValIdx(List<Document> arr, String key, Object val,
                                                 String key2, Object val2) {

        if (arr == null)
            return -1;

        for (int i = 0; i < arr.size(); i++) {
            Document doc = arr.get(i);
            if (doc.containsKey(key) && doc.get(key).equals(val) && (
                    (val2 == null && doc.get(key2) == null) ||
                            (doc.get(key2) != null && doc.get(key2).equals(val2))
            ))
                return i;
        }

        return -1;
    }

    public static int searchInDocumentsKeyValIdx(List<Document> arr, String key, Object val) {

        if (arr == null)
            return -1;

        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i).containsKey(key) && arr.get(i).get(key).equals(val))
                return i;
        }

        return -1;
    }

    public static int convertStringToDate(String date) {
        return Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10));
    }

    public static String getToday(String delimeter) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar();
        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);
    }

    public static int getFirstDayOfMonth() {

        String dd = getToday("/");
        int d = Integer.parseInt(dd.split("/")[2]);

        if (d == 1)
            return d;

        for (int i = 1; i <= 31; i++) {
            dd = getPast("/", i);
            d = Integer.parseInt(dd.split("/")[2]);

            if (d == 1) {
                return convertStringToDate(dd);
            }
        }

        return -1;
    }

    public static int getFirstDayOfLastMonth() {

        String dd = getToday("/");
        int m = Integer.parseInt(dd.split("/")[1]), d;

        if (m == 1)
            m = 12;
        else
            m--;

//        LocalDate todaydate = LocalDate.now();

        for (int i = 0; i <= 62; i++) {

            dd = getPast("/", i);

            d = Integer.parseInt(dd.split("/")[2]);
            int mm = Integer.parseInt(dd.split("/")[1]);

            if (d == 1 && mm == m) {
//                todaydate = todaydate.minusDays(i);
//                return todaydate.toEpochDay() * ONE_DAY_MIL_SEC;
                return convertStringToDate(dd);
            }
        }

        return -1;
    }

    public static String getPersianDate(String delimeter, long ts) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar(ts);
        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);
    }

    public static int getToday() {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar();
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
    }

    public static String getPast(String delimeter, int days) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar(-ONE_DAY_MIL_SEC * days);
        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);
    }

    public static String getPastMilady(int days) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(new Date().getTime() - ONE_DAY_MIL_SEC * days));
    }

    public static boolean isValidMail(String in) {
        return mailPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidPostalCode(String in) {
        return justNumPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidPassword(String in) {
        return in.length() >= 6;
//        return passwordStrengthPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static boolean isValidNum(String in) {
        return justNumPattern.matcher(convertPersianDigits(in)).matches();
    }

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public static String simpleRandomString(int len) {

        StringBuilder sb = new StringBuilder(len);
        sb.append(ABCD.charAt(rnd.nextInt(ABCD.length()))).append("_");

        for (int i = 0; i < len - 1; i++)
            sb.append(ABC.charAt(rnd.nextInt(ABC.length())));

        return sb.toString();
    }

    public static String randomPhone(int len) {

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++)
            sb.append(ABC.charAt(rnd.nextInt(ABC.length())));

        return sb.toString();
    }

    public static boolean sendSMS(String receptor, String token,
                                  String token2, String token3,
                                  String template
    ) {

        if (DEV_MODE)
            return true;

        receptor = convertPersianDigits(receptor);

        if (!PhoneValidator.isValid(receptor)) {
            System.out.println("not valid phone num");
            return false;
        }

        try {
            KavenegarApi api = new KavenegarApi("79535344745641433164454E622F6F2B436F7741744B637442576673554B636A");
            SendResult Result = api.verifyLookup(receptor, token, token2, token3, template);

            if (Result.getStatus() == 6 ||
                    Result.getStatus() == 11 ||
                    Result.getStatus() == 13 ||
                    Result.getStatus() == 14 ||
                    Result.getStatus() == 100
            )
                return false;

            return true;
        } catch (HttpException ex) {
            System.out.print("HttpException  : " + ex.getMessage());
        } catch (ApiException ex) {
            System.out.print("ApiException : " + ex.getMessage());
        }

        return false;
    }

    public static boolean sendSMSWithoutTemplate(String receptor, String msg) {

        if (DEV_MODE)
            return true;

        receptor = convertPersianDigits(receptor);

        try {

//            HttpResponse<String> response = Unirest.post("https://panel.asanak.com/webservice/v1rest/msgstatus")
//                    .queryString("username", "gachesefid")
//                    .queryString("password", "9DGr7JwEUXLtyVee")
//                    .queryString("msgid", "3376025189,3376025190,3376025191,3376025192") //"3375133058"
//                    .header("content-type", "application/x-www-form-urlencoded")
//                    .header("accept", "application/json")
//                    .header("cache-control", "no-cache")
//                    .asString();

            HttpResponse<String> response = Unirest.post("https://panel.asanak.com/webservice/v1rest/sendsms")
                    .queryString("username", "bogenGach") // bogenGach
                    .queryString("password", "9DGr7JwEUXLtyVee")
                    .queryString("source", "9821700013860625")
                    .queryString("destination", receptor)
                    .queryString("send_to_blacklist", 1)
                    .queryString("message", msg)
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("accept", "application/json")
                    .header("cache-control", "no-cache")
                    .asString();

            if (response != null)
                System.out.println(response.getBody());

            return true;
        } catch (HttpException ex) {
            System.out.print("HttpException  : " + ex.getMessage());
        } catch (ApiException ex) {
            System.out.print("ApiException : " + ex.getMessage());
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return false;
    }


    public static boolean sendSMSWithTemplate(String receptor, int templateId, PairValue... paramsPair) {

        if (DEV_MODE)
            return true;

        receptor = convertPersianDigits(receptor);

        try {
            JSONObject jsonObject = new JSONObject()
                    .put("destination", receptor)
                    .put("send_to_blacklist", 1)
                    .put("template_id", templateId);

            JSONObject params = new JSONObject();
            for (PairValue p : paramsPair)
                params.put(p.getKey().toString(), p.getValue());

            jsonObject.put("parameters", params);

            HttpResponse<String> response = Unirest.post("https://api.asanak.com/v1/sms/template")
                    .header("api_key", "bogenGach")
                    .header("api_secret", "bd23cb13fa2c49c20673504ed2a19729e61feb22f951819fbe5042a18efab840")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(jsonObject)
                    .asString();

            if (response != null)
                System.out.println(response.getBody());

            return true;
        } catch (HttpException ex) {
            System.out.print("HttpException  : " + ex.getMessage());
        } catch (ApiException ex) {
            System.out.print("ApiException : " + ex.getMessage());
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String convertPersianDigits(String number) {

        char[] chars = new char[number.length()];
        for (int i = 0; i < number.length(); i++) {

            char ch = number.charAt(i);

            if (ch >= 0x0660 && ch <= 0x0669)
                ch -= 0x0660 - '0';
            else if (ch >= 0x06f0 && ch <= 0x06F9)
                ch -= 0x06f0 - '0';

            chars[i] = ch;
        }

        return new String(chars);
    }

    public static JSONObject convertPersian(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            if (key.toLowerCase().contains("password") ||
                    key.toLowerCase().contains("newpass") ||
                    key.toLowerCase().contains("rnewpass") ||
                    key.equalsIgnoreCase("code") ||
                    key.equals("NID")
            )
                jsonObject.put(key, Utility.convertPersianDigits(jsonObject.get(key).toString()));
            else if (jsonObject.get(key) instanceof Integer)
                jsonObject.put(key, Integer.parseInt(Utility.convertPersianDigits(jsonObject.getInt(key) + "")));
            else if (jsonObject.get(key) instanceof String) {
                String str = Utility.convertPersianDigits(jsonObject.getString(key));
                if (str.charAt(0) == '0' && (
                        str.length() == 1 || str.charAt(1) != '.') ||
                        key.equalsIgnoreCase("phone") ||
                        key.equalsIgnoreCase("tel")
                )
                    jsonObject.put(key, str);
                else {
                    try {
                        jsonObject.put(key, Integer.parseInt(str));
                    } catch (Exception x) {
                        try {
                            jsonObject.put(key, Double.parseDouble(str));
                        } catch (Exception ignore) {
                            jsonObject.put(key, str);
                        }
                    }
                }
            }
        }

        return jsonObject;
    }

    public static boolean sendMail(String to, String msg, String mode, String username) {

        if (DEV_MODE)
            return true;

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "mail1.limoo.host");
        prop.put("mail.smtp.port", "587");

        try {

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUserName, mailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@irysc.com", "noreply@irysc.com"));
            String title = null;

            String subject = "";

            if (mode.equalsIgnoreCase("signUp")
                    || mode.equalsIgnoreCase("forget"))
                subject = "کد تایید";
            else if (mode.equalsIgnoreCase("successTransaction"))
                subject = "شارژ حساب";
            else if (mode.equalsIgnoreCase("successSignUp"))
                subject = "ثبت نام موفق در سایت";
            else if (mode.equalsIgnoreCase("successQuiz"))
                subject = "خرید/ساخت آزمون";
            else if (mode.equalsIgnoreCase("notif")) {
                subject = "پیام جدید";
                String[] splited = to.split("___");
                title = splited[0];
                to = splited[1];
            } else if (mode.equalsIgnoreCase("quizReminder"))
                subject = "یادآوری آزمون";
            else if (mode.equalsIgnoreCase("offcode"))
                subject = "ایجاد کد تخفیف";
            else if (mode.equalsIgnoreCase("karname"))
                subject = "ایجاد کارنامه / ساخت تراز ";

            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            StringBuilder html = new StringBuilder();
            EmailUtil.headerSection(html);

            if (title == null)
                html.append("<h3 style='color: rgb(1, 50, 67);")
                        .append("font-size: 20px;")
                        .append("margin-top: 20px; float: right;'>")
                        .append(subject)
                        .append("</h3>");
            else
                html.append("<h3 style='color: rgb(1, 50, 67);")
                        .append("font-size: 20px;")
                        .append("margin-top: 20px; float: right;'>")
                        .append(title)
                        .append("</h3>");

            html.append("<img style='height: 60px;")
                    .append("margin-bottom: 20px;")
                    .append("width: 104px; float: left' src='https://e.irysc.com/static/media/irysc.69b93d83702c4996d2a3.png'>");

            html.append("</div>");
            html.append("<div style='margin: 20px; direction: rtl; text-align: right'>");
            if (username == null)
                html.append("<p style='font-size: 1.1em; margin-bottom: 25px'>سلام, </p>");
            else
                html.append("<p style='font-size: 1.1em; margin-bottom: 25px'>")
                        .append(username)
                        .append(" عزیز </p>");

            if (mode.equalsIgnoreCase("notif"))
                html.append(msg.replaceAll("<img", "<img style='max-width: 100%; height: auto;'"));
            else if (mode.equalsIgnoreCase("signUp") ||
                    mode.equalsIgnoreCase("forget")) {
                html.append("<p>کد تایید ایمیل شما برای ثبت در سامانه آیریسک</p>");
                html.append("<p style='text-align: center; font-size: 1.6em; color: rgb(1, 50, 67); font-weight: bolder;'>").append(msg).append("</p>");
            } else if (mode.equalsIgnoreCase("successSignUp"))
                html.append("<p style='text-align: center; font-size: 1.4em; color: rgb(1, 50, 67); font-weight: bolder;'>ثبت نام شما در سامانه آزمون و آموزش آیریسک با موفقیت انجام شد.</p>");
            else if (mode.equalsIgnoreCase("quizReminder")) {
                html.append("<p style='text-align: center; font-size: 1.4em; color: rgb(1, 50, 67); font-weight: bolder;'>فردا آزمون ").append(msg).append(" در سامانه آیریسک برگزار میشود.</p>");
                html.append("<p>فراموش نکنی!</p>");
            } else if (mode.equalsIgnoreCase("successTransaction")) {
                String[] splited = msg.split("_");
                html.append("<p style='font-size: 1.6em; color: rgb(1, 50, 67); font-weight: bolder;'>حساب شما در آیریسک ").append(Utility.formatPrice(Integer.parseInt(splited[0]))).append(" تومان شارژ شد.</p>");
                html.append("<p>")
                        .append("<span>برای مشاهده فاکتور پرداخت بر روی لینک زیر کلیک کنید: </span>")
                        .append("<br /><a href='")
                        .append(splited[1])
                        .append("'>")
                        .append(splited[1])
                        .append("</a>")
                        .append("</p>");
            } else if (mode.equalsIgnoreCase("offcode")) {
                html.append("<p style='font-size: 1.6em; color: rgb(1, 50, 67); font-weight: bolder;'>شارژ تشویقی شما به صورت اختصاصی در پیشخوان کاربری تان قرار گرفت. از بخش تخفیف ها آن را ببینید</p>");
                html.append("<p>")
                        .append("<span>و یا بر روی لینک زیر کلیک کنید: </span>")
                        .append("<br /><a href='")
                        .append(msg)
                        .append("'>")
                        .append(msg)
                        .append("</a>")
                        .append("</p>");

            } else if (mode.equalsIgnoreCase("karname")) {
                String[] splited = msg.split("_");
                html.append("<p style='font-size: 1.6em; color: rgb(1, 50, 67); font-weight: bolder;'>کارنامه آزمون ").append(splited[0]).append(" در سایت آیریسک قرار گرفت. می توانی در بخش مرور آزمون پاسخ\u200Cهای تشریحی را هم بررسی کنی</p>");
                html.append("<p>")
                        .append("<span>و یا بر روی لینک زیر کلیک کنید: </span>")
                        .append("<br /><a href='")
                        .append(splited[1])
                        .append("'>")
                        .append(splited[1])
                        .append("</a>")
                        .append("</p>");
            } else if (mode.equals("changeMail"))
                html.append("<p>You have requested to change your email account in ÖKF LMS, tap the button below to change your email:</p>");
            else if (mode.equalsIgnoreCase("successQuiz")) {
                html.append("<p style='font-size: 1.6em; color: rgb(1, 50, 67); font-weight: bolder;'>شما موفق شدی در آزمون ثبت نام کنی.</p>");
                html.append("<p>")
                        .append("<span>برای مشاهده فاکتور پرداخت بر روی لینک زیر کلیک کنید: </span>")
                        .append("<br /><a href='")
                        .append(msg)
                        .append("'>")
                        .append(msg)
                        .append("</a>")
                        .append("</p>");
            }

            if (mode.equals("changeMail"))
                html.append("<a target='_blank' style='background-color: #BB0000; color: white; font-size: 1.4em; font-weight: bolder; padding-top: 10px; padding-bottom: 10px; padding-left: 20px; padding-right: 20px; text-decoration: none' href='").append(msg).append("'>Change Email</a>");

            html.append("</div>");
            emailFooterSection(html);
            html.append("</div>");
            html.append("</body></html>");

            mimeBodyPart.setContent(html.toString(), "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart, "text/html; charset=UTF-8");
            Transport.send(message);

            String finalSubject = subject;
            String finalTo = to;
            new Thread(() -> mailRepository.insertOne(new Document("created_at", System.currentTimeMillis()).append("recp", finalTo).append("subject", finalSubject))).start();

        } catch (Exception x) {
            printException(x);
            return false;
        }

        return true;
    }

    private static HashMap<String, DataSource> mailAttaches = new HashMap<>();

    public static boolean sendMailWithAttach(String to, String msg, String username, String filename) {

        if (DEV_MODE || to == null)
            return true;

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "mail1.limoo.host");
        prop.put("mail.smtp.port", "587");

        try {
            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUserName, mailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@irysc.com", "noreply@irysc.com"));
            String title;

            String subject = "پیام جدید";
            if (to.contains("___")) {
                String[] splited = to.split("___");
                title = splited[0];
                to = splited[1];
            } else
                title = subject;

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            StringBuilder html = new StringBuilder();
            EmailUtil.headerSection(html);

            if (title == null)
                html.append("<h3 style='color: rgb(1, 50, 67);")
                        .append("font-size: 20px;")
                        .append("margin-top: 20px; float: right;'>")
                        .append(subject)
                        .append("</h3>");
            else
                html.append("<h3 style='color: rgb(1, 50, 67); font-size: 20px; margin-top: 20px; float: right;'>")
                        .append(title)
                        .append("</h3>");

            html.append("<img style='height: 60px;")
                    .append("margin-bottom: 20px;")
                    .append("width: 104px; float: left' src='https://e.irysc.com/static/media/irysc.69b93d83702c4996d2a3.png'>")
            ;

            html.append("</div>");
            html.append("<div style='margin: 20px; font-family: IRANSans; direction: rtl; text-align: right'>");
            if (username == null)
                html.append("<p style='font-size: 1.1em; margin-bottom: 25px'>سلام, </p>");
            else
                html.append("<p style='font-size: 1.1em; margin-bottom: 25px'>").append(username).append(" عزیز </p>");

            html.append(msg.replaceAll("<img", "<img style='max-width: 100%; height: auto;'"));
            html.append("</div>");

            emailFooterSection(html);
            html.append("</div>");
            html.append("</body></html>");

            mimeBodyPart.setContent(html.toString(), "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            if (filename != null && !filename.isEmpty()) {
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                DataSource source;

                if (mailAttaches.containsKey(filename))
                    source = mailAttaches.get(filename);
                else {
                    String file = (DEV_MODE
                            ? FileUtils.uploadDir_dev
                            : FileUtils.uploadDir
                    ) + "notifs/" + filename;
                    source = new FileDataSource(file);

                    if (mailAttaches.keySet().size() >= 100)
                        mailAttaches = new HashMap<>();

                    mailAttaches.put(filename, source);
                }

                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(filename);

                multipart.addBodyPart(messageBodyPart);
            }

            message.setContent(multipart, "text/html; charset=UTF-8");
            Transport.send(message);

            String finalTo = to;
            new Thread(() -> mailRepository.insertOne(new Document("created_at", System.currentTimeMillis()).append("recp", finalTo).append("subject", subject))).start();

        } catch (Exception x) {
            printException(x);
            return false;
        }

        return true;
    }

    public static String formatPrice(int price) {
        return String.format("%,d", price);
    }

    public static long getTimestamp(String date) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(date).getTime();
        } catch (ParseException e) {
            printException(e);
        }

        return -1;
    }

    public static ArrayList<String> checkDatesConstriant(String sendDateSolar, String answerDateSolar,
                                                         String sendDateSolarEndLimit, String answerDateSolarEndLimit) {
        if (
                (sendDateSolar != null && !DateValidator.isValid2(sendDateSolar)) ||
                        (answerDateSolar != null && !DateValidator.isValid2(answerDateSolar)) ||
                        (sendDateSolarEndLimit != null && !DateValidator.isValid2(sendDateSolarEndLimit)) ||
                        (answerDateSolarEndLimit != null && !DateValidator.isValid2(answerDateSolarEndLimit))
        )
            return null;

        ArrayList<String> dates = new ArrayList<>();

        if (sendDateSolar != null) {
            String[] splited = sendDateSolar.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(null);

        if (answerDateSolar != null) {
            String[] splited = answerDateSolar.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(null);

        if (sendDateSolarEndLimit != null) {
            String[] splited = sendDateSolarEndLimit.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(null);

        if (answerDateSolarEndLimit != null) {
            String[] splited = answerDateSolarEndLimit.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                            new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        } else
            dates.add(null);

        return dates;
    }

    public static int randInt() {

        if (DEV_MODE)
            return 111111;

        int r = 0;
        for (int i = 0; i < 6; i++) {
            int x = random.nextInt(10);

            while (x == 0)
                x = random.nextInt(10);

            r += x * Math.pow(10, i);
        }

        return r;
    }

    public static String getRandIntForStudentId(String firstSection) {

        int r = 0;
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(10);

            while (x == 0)
                x = random.nextInt(10);

            r += x * Math.pow(10, i);
        }

        String studentId = firstSection + r;

        if (userRepository.exist(eq("studentId", studentId)))
            return getRandIntForStudentId(firstSection);

        return studentId;
    }

    public static Object getOrDefault(JSONObject jsonObject, String key, Object defaultValue) {

        if (!jsonObject.has(key))
            return defaultValue;

        return jsonObject.get(key);
    }

    public static ObjectId getOrDefaultObjectId(JSONObject jsonObject, String key) {

        if (!jsonObject.has(key))
            return null;

        return new ObjectId(jsonObject.get(key).toString());
    }

    public static String getRandIntForSubjectId() {

        int number = Math.abs(random.nextInt(999));
        if (number < 100)
            number += 100;

        String code = String.format("%03d", number);

        if (subjectRepository.exist(eq("code", code)))
            return getRandIntForSubjectId();

        return code;
    }

    public static int getRandIntForTag() {

        int number = Math.abs(random.nextInt(999));
        if (number < 100)
            number += 100;

        if (questionTagRepository.exist(eq("code", number)))
            return getRandIntForTag();

        return number;
    }

    public static int getRandIntForGift(int upper) {
        return Math.abs(random.nextInt(upper));
    }

    public static String getSolarDate(long time) {

        if (time < 1610494635)
            return "";

        Date d = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] dateTime = simpleDateFormat.format(d).split(" ");
        String[] splited = dateTime[0].split("-");
        return JalaliCalendar.gregorianToJalali(new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2])).format("/") + " - " + dateTime[1];
    }

    public static String getSimpleSolarDate(long time) {
        if (time < 1610494635)
            return "";

        Date d = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String[] splited = simpleDateFormat.format(d).split("-");
        return JalaliCalendar.gregorianToJalali(new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2])).format("/");
    }

    public static String getSolarDateWithSecond(long time) {
        if (time < 1610494635)
            return "";

        Date d = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] dateTime = simpleDateFormat.format(d).split(" ");
        String[] splited = dateTime[0].split("-");
        return JalaliCalendar.gregorianToJalali(new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2])).format("/") + " - " + dateTime[1];
    }

    public static String getMonthSolarDate(long time) {

//        if(time < 1610494635)
//            return "";

        Date d = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] dateTime = simpleDateFormat.format(d).split(" ");
        String[] splited = dateTime[0].split("-");
        String y = JalaliCalendar.gregorianToJalali(new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2])).format("/") + " - " + dateTime[1];

        String tmp = y.split("-")[0].replace(" ", "");
        String month = tmp.split("/")[1];
        String year = tmp.split("/")[0];
        String monthLabel;

        switch (month) {
            case "1":
            case "01":
            default:
                monthLabel = "فروردین";
                break;
            case "2":
            case "02":
                monthLabel = "اردیبهشت";
                break;
            case "3":
            case "03":
                monthLabel = "خرداد";
                break;
            case "4":
            case "04":
                monthLabel = "تیر";
                break;
            case "5":
            case "05":
                monthLabel = "مرداد";
                break;
            case "6":
            case "06":
                monthLabel = "شهریور";
                break;
            case "7":
            case "07":
                monthLabel = "مهر";
                break;
            case "8":
            case "08":
                monthLabel = "آبان";
                break;
            case "9":
            case "09":
                monthLabel = "آذر";
                break;
            case "10":
                monthLabel = "دی";
                break;
            case "11":
                monthLabel = "بهمن";
                break;
            case "12":
                monthLabel = "اسفند";
                break;
        }

        return monthLabel + " " + year;
    }

    public static String camel(String text, boolean firstLetterCapital) {
        String[] words = text.split("[\\W_]+");
        StringBuilder builder = new StringBuilder();
        boolean firstLetter = false;

        for (int i = 0; i < words.length; i++) {

            String word = words[i];
            if (word.isEmpty())
                continue;

            char first = Character.toUpperCase(word.charAt(0));

            if (!firstLetter && !firstLetterCapital) {
                first = word.charAt(0);
                firstLetter = true;
            }

            word = first + word.substring(1).toLowerCase();
            builder.append(word);
        }
        return builder.toString();
    }

    public static String generateErr(String msg) {
        return new JSONObject()
                .put("status", "nok")
                .put("msg", msg)
                .toString();
    }

    public static String generateErr(String msg, PairValue... pairValues) {

        JSONObject jsonObject = new JSONObject()
                .put("status", "nok")
                .put("msg", msg);

        for (PairValue p : pairValues)
            jsonObject.put(p.getKey().toString(), p.getValue());

        return jsonObject.toString();
    }

    public static String generateSuccessMsg(String key, Object val, PairValue... pairValues) {

        JSONObject jsonObject = new JSONObject()
                .put("status", "ok")
                .put(key, val);

        for (PairValue p : pairValues)
            jsonObject.put(p.getKey().toString(), p.getValue());

        return jsonObject.toString();

    }

    public static Document getConfig() {
        return configRepository.findBySecKey(true);
    }

    public static String getPastAsString(int days) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar(-ONE_DAY_MIL_SEC * days);
        return String.valueOf(sc.year) + "/" + String.format(loc, "%02d",
                sc.month) + "/" + String.format(loc, "%02d", sc.date);
    }

    public static void printException(Exception x) {

        x.printStackTrace();

        System.out.println(x.getMessage());
        int limit = x.getStackTrace().length > 5 ? 5 : x.getStackTrace().length;
        for (int i = 0; i < limit; i++)
            System.out.println(x.getStackTrace()[i]);

    }

    private static void fillJSONWithUserCommonInfo(JSONObject jsonObject1, Document user) {

        if (user.containsKey("city")) {
            Document city = (Document) user.get("city");
            if (city != null && city.containsKey("name"))
                jsonObject1.put("city", city.getString("name"));
        }

        if (!jsonObject1.has("city"))
            jsonObject1.put("city", "");

        if (user.containsKey("school")) {
            Document school = (Document) user.get("school");
            if (school != null && school.containsKey("name"))
                jsonObject1.put("school", school.getString("name"));
        }

        if (!jsonObject1.has("school"))
            jsonObject1.put("school", "");

        if (user.containsKey("grade")) {
            Document grade = (Document) user.get("grade");
            if (grade != null && grade.containsKey("name"))
                jsonObject1.put("grade", grade.getString("name"));
        }

        if (!jsonObject1.has("grade"))
            jsonObject1.put("grade", "");


        if (user.containsKey("branches")) {
            List<Document> branches = user.getList("branches", Document.class);
            if (branches.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Document branch : branches) {
                    sb.append(branch.getString("name")).append(" - ");
                }
                jsonObject1.put("branches", sb.substring(0, sb.toString().length() - 3));
            } else jsonObject1.put("branches", "");
        }

        if (!jsonObject1.has("branches"))
            jsonObject1.put("branches", "");

        if (user.containsKey("rank"))
            jsonObject1.put("rank", user.get("rank"));
        else {
            Document rank = tarazRepository.findOne(eq("user_id", user.getObjectId("_id")), JUST_RANK);
            jsonObject1.put("rank", rank == null ? -1 : rank.get("rank"));
        }
    }

    private static void simpleFillJSONWithUserCommonInfo(JSONObject jsonObject1, Document user) {

        if (user.containsKey("city")) {
            Document city = (Document) user.get("city");
            if (city != null && city.containsKey("name"))
                jsonObject1.put("city", city.getString("name"));
        }

        if (!jsonObject1.has("city"))
            jsonObject1.put("city", "");

        if (user.containsKey("school")) {
            Document school = (Document) user.get("school");
            if (school != null && school.containsKey("name"))
                jsonObject1.put("school", school.getString("name"));
        }

        if (!jsonObject1.has("school"))
            jsonObject1.put("school", "");

        if (user.containsKey("grade")) {
            Document grade = (Document) user.get("grade");
            if (grade != null && grade.containsKey("name"))
                jsonObject1.put("grade", grade.getString("name"));
        }

        if (!jsonObject1.has("grade"))
            jsonObject1.put("grade", "");


        if (user.containsKey("branches")) {
            List<Document> branches = user.getList("branches", Document.class);
            if (branches.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Document branch : branches) {
                    sb.append(branch.getString("name")).append(" - ");
                }
                jsonObject1.put("branches", sb.substring(0, sb.toString().length() - 3));
            } else jsonObject1.put("branches", "");
        }

        if (!jsonObject1.has("branches"))
            jsonObject1.put("branches", "");

        jsonObject1.put("rank", -1);
    }

    public static void fillJSONWithUser(JSONObject jsonObject, Document user) {

        //todo: customize with common user info
        JSONObject jsonObject1 = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("phone", user.getOrDefault("phone", ""))
                .put("mail", user.getOrDefault("mail", ""))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("NID", user.getString("NID"));

        fillJSONWithUserCommonInfo(jsonObject1, user);
        jsonObject.put("student", jsonObject1);
    }

    public static void simpleFillJSONWithUser(JSONObject jsonObject, Document user) {
        JSONObject jsonObject1 = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("phone", user.getOrDefault("phone", ""))
                .put("mail", user.getOrDefault("mail", ""))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("NID", user.getString("NID"));

        simpleFillJSONWithUserCommonInfo(jsonObject1, user);
        jsonObject.put("student", jsonObject1);
    }

    public static JSONObject fillJSONWithUser(Document user) {
        //todo: customize with common user info
        JSONObject jsonObject1 = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("phone", user.getOrDefault("phone", ""))
                .put("mail", user.getOrDefault("mail", ""))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("NID", user.getString("NID"));

        fillJSONWithUserCommonInfo(jsonObject1, user);
        return jsonObject1;
    }

    public static void fillJSONWithUserPublicInfo(JSONObject jsonObject, Document user) {

        //todo: customize with common user info
        JSONObject jsonObject1 = new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"));

        fillJSONWithUserCommonInfo(jsonObject1, user);
        jsonObject.put("student", jsonObject1);
    }

    public static boolean validationNationalCode(String code) {

        if (code.length() != 10)
            return false;

        try {
            long nationalCode = Long.parseLong(code);
            byte[] arrayNationalCode = new byte[10];

            //extract digits from number
            for (int i = 0; i < 10; i++) {
                arrayNationalCode[i] = (byte) (nationalCode % 10);
                nationalCode = nationalCode / 10;
            }

            //Checking the control digit
            int sum = 0;
            for (int i = 9; i > 0; i--)
                sum += arrayNationalCode[i] * (i + 1);
            int temp = sum % 11;
            if (temp < 2)
                return arrayNationalCode[0] == temp;
            else
                return arrayNationalCode[0] == 11 - temp;
        } catch (Exception e) {
            return false;
        }
    }

    public static String batchRowErr(int rowIdx, String err) {
        return String.format("ردیف %d : %s\n", rowIdx, err);
    }

    public static String returnAddResponse(JSONArray excepts,
                                           JSONArray added) {
        return returnBatchResponse(excepts, added, "اضافه");
    }

    public static String returnRemoveResponse(JSONArray excepts,
                                              JSONArray removeIds) {
        return returnBatchResponse(excepts, removeIds, "حذف");
    }

    public static String returnBatchResponse(JSONArray j1, JSONArray j2, String doneFa) {

        if (j1 == null || j1.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی موارد به درستی " + doneFa + " گردیدند",
                    new PairValue("doneIds", j2)
            );

        return generateSuccessMsg(
                "excepts",
                "بجز موارد زیر سایرین به درستی " + doneFa + " گردیدند. " + j1,
                new PairValue("doneIds", j2)
        );
    }

    public static Document validateOffCode(String offcode, ObjectId userId,
                                           long curr, String section) {

        return offcodeRepository.findOne(
                and(
                        exists("code"),
                        eq("code", offcode),
                        or(
                                and(
                                        exists("user_id"),
                                        eq("user_id", userId)
                                ),
                                and(
                                        exists("is_public"),
                                        eq("is_public", true)
                                )
                        ),
                        or(
                                exists("used", false),
                                and(
                                        exists("used"),
                                        eq("used", false)
                                )
                        ),
                        or(
                                eq("section", OffCodeSections.ALL.getName()),
                                eq("section", section)
                        ),
                        nin("students", userId),
                        gt("expire_at", curr)
                ), null
        );
    }

    public static Document findAccountOff(ObjectId userId,
                                          long curr,
                                          String section
    ) {
        return offcodeRepository.findOne(
                and(
                        exists("code", false),
                        exists("used"),
                        exists("user_id"),
                        eq("user_id", userId),
                        eq("used", false),
                        or(
                                eq("section", OffCodeSections.ALL.getName()),
                                eq("section", section)
                        ),
                        gt("expire_at", curr)
                ), null
        );
    }

    public static List<ObjectId> pluckIds(List<Document> docs) {

        ArrayList<ObjectId> tmp = new ArrayList<>();

        for (Document doc : docs) {
            tmp.add(doc.getObjectId("_id"));
        }

        return tmp;
    }

    public static int getDayIndex(String day) {

        switch (day) {
            case "شنبه":
            default:
                return 0;
            case "یک شنبه":
            case "يکشنبه":
                return 1;
            case "دوشنبه":
            case "دو شنبه":
                return 2;
            case "سه شنبه":
                return 3;
            case "چهار شنبه":
            case "چهارشنبه":
                return 4;
            case "پنج شنبه":
                return 5;
            case "جمعه":
                return 6;
        }
    }

    public static String getFirstDayOfCurrWeek() {

        SolarCalendar sc = new SolarCalendar();
        Locale loc = new Locale("en_US");

        long nextWeek = -ONE_DAY_MIL_SEC * getDayIndex(sc.strWeekDay);
        String delimeter = "/";

        sc = new SolarCalendar(nextWeek);

        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);

    }

    public static String getFirstDayOfFutureWeek(int weeks) {
        SolarCalendar sc = new SolarCalendar();
        Locale loc = new Locale("en_US");

        long nextWeek = ONE_DAY_MIL_SEC * ((weeks - 1) * 7 + 7 - getDayIndex(sc.strWeekDay));
        String delimeter = "/";

        sc = new SolarCalendar(nextWeek);

        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);
    }

    public static boolean isValidURL(String url) {

        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }

        return true;
    }

    public static void logForOffCodeUsage(
            Document offDoc, ObjectId userId,
            String usedSection, Object usedFor
    ) {

        BasicDBObject update;

        if (offDoc.containsKey("is_public") &&
                offDoc.getBoolean("is_public")
        ) {
            List<ObjectId> students = offDoc.getList("students", ObjectId.class);
            students.add(userId);
            update = new BasicDBObject("students", students);
        } else {

            update = new BasicDBObject("used", true)
                    .append("used_at", System.currentTimeMillis())
                    .append("used_section", usedSection)
                    .append("used_for", usedFor);
        }

        offcodeRepository.updateOne(
                offDoc.getObjectId("_id"),
                new BasicDBObject("$set", update)
        );
    }

    public static Document findOff(String offCode, ObjectId userId) throws InvalidFieldsException {

        Document offDoc;
        long curr = System.currentTimeMillis();

        if (offCode == null) {
            offDoc = Utility.findAccountOff(
                    userId, curr, OffCodeSections.CLASSES.getName()
            );
        } else {
            offDoc = validateOffCode(
                    offCode, userId, curr,
                    OffCodeSections.CLASSES.getName()
            );

            if (offDoc == null)
                throw new InvalidFieldsException("کد تخفیف وارد شده معتبر نمی باشد.");
        }

        return offDoc;
    }

    private static Document doCreateNotif(Document wantedUser, String textMessage, String mode) {
        String wantedUserName = wantedUser.getString("first_name") + " " + wantedUser.getString("last_name");
        long curr = System.currentTimeMillis();

        String title;
        String msg = "<p>" + "سلام " + wantedUserName + "<br/>";

        switch (mode) {
            case "agentJoin":
                title = "درخواست دعوت از نمایندگی " + textMessage;
                msg += "نمایندگی " + textMessage + " از شما دعوت کرده است تا زیرمجموعه ایشان باشید. تیکتی برای بررسی این موضوع برای شما ارسال شده است و می توانید از طریق لینک درون آن تیکت این درخواست را قبول کنید.";
                break;
            case "schoolAcceptAgentInvite":
                title = "قبول شدن درخواست دعوت";
                msg += textMessage + " درخواست اضافه شدن به عنوان زیرمجموعه نمایندگی شما را قبول کرده است و از این پس زیرمجموعه شما می باشد";
                break;
            case "schoolAcceptAgentInviteButPending":
                title = "قبول شدن درخواست دعوت";
                msg += textMessage + " درخواست اضافه شدن به عنوان زیرمجموعه نمایندگی شما را قبول کرده است و پس از تایید ادمین، این مدرسه به عنوان زیرمجموعه شما خواهد بود";
                break;
            case "finalize":
                title = "پرداخت و نهایی سازی مشاور توسط دانش آموز";
                msg += "هزینه یک ماه مشاوره توسط " + textMessage + " پرداخت شد." + "<br/>" + "اکنون می\u200Cتوانید طبق برنامه\u200Cای که در بسته\u200Cهای مشاوره به ایشان اطلاع\u200Cرسانی شده، برنامه\u200Cی خود را آغاز کنید.";
                break;
            case "finalizeTeach":
                title = "پرداخت کلاس " + textMessage + " انجام شد.";
                msg += "پرداخت هزینۀ کلاس در زمان " + getSolarDate(System.currentTimeMillis()) + " توسط " + textMessage + " انجام شده." + "<br/>" + "لطفاً در روز انتخاب شده، لینک کلاس را " + "<a href='" + SERVER + "" + "'>از اینجا</a>" + " بسازید.";
                break;
            case "acceptTeach":
                title = "درخواست کلاس با استاد " + textMessage + " تایید شد";
                msg += "درخواست کلاس شما توسط استاد " + textMessage + " تایید شد." + "با دنبال کردن لینک زیر، هزینۀ کلاس را پرداخت کنید:" + "<br/><br/>" + "<a href='" + SERVER + "myScheduleRequests'>" + "لینک پرداخت" + "</a>";
                break;
            case "rejectTeach":
                title = "درخواست کلاس با استاد " + textMessage + " رد شد";
                msg += "استاد " + textMessage + " درخواست برگزاری کلاس شما را تأیید نکرد. برای این زمان می\u200Cتوانی کلاس\u200Cهای دیگر اساتید را بررسی کنی یا زمان دیگری را برای برگزاری کلاس انتخاب کنی." + "<br/>" + "همچنین از این لینک (ارسال پیغام به پشتیبانی) می\u200Cتوانی دلیل رد درخواست کلاس را پیگیری کنی." + "<br/><br/>" + "<a href='" + SERVER + "ticket'>" + "درخواست پشتیبانی" + "</a>";
                break;
            case "teachMinCap":
                String[] tmp2 = textMessage.split("__");
                title = "کلاس شما با استاد " + tmp2[0] + " به حد نصاب رسید.";
                msg += "کلاس انتخابی شما با استاد " + tmp2[0] + " در زمان " + tmp2[1] + " به حد نصاب رسید." + "<br/>" + "برای پرداخت هزینۀ کامل از لینک زیر اقدام کنید:" + "<br/>" + "<a href='" + tmp2[2] + "'>لینک پرداخت</a>";
                break;
            case "newTeachRequest":
                title = "درخواست کلاس توسط " + textMessage;
                String[] tmp = textMessage.split("__");
                msg += "دانش آموز " + tmp[1] + " برای کلاس " + tmp[0] + " درخواست داده است." + "<br/>" + "از " + "<a href='" + SERVER + "myTeachRequests" + "'>این لینک</a>" + " می\u200Cتوانید درخواست را تأیید کنید.";
                break;
            case "cancelRequest":
                String[] splited = textMessage.split("__");
                title = "کلاس " + splited[0] + " لغو شد!";
                msg += "دانش آموز " + splited[1] + " از درخواست کلاس منصرف شد. زمان شما در گچ\u200Cسفید آزاد شده و دانش\u200Cآموزان دیگر می\u200Cتوانند آن را انتخاب کنند.";
                break;
            case "request":
                title = "درخواست مشاوره";
                msg += "دانش آموز " + textMessage + " از شما درخواست کرده تا یک ماه مشاور ایشان باشید." + "<br/>" + "از پیشخوان بخش مشاوره سوابق ایشان را بررسی کرده و در صورت موافقت، تایید کنید." + "<br/>" + "شاد باشید";
                break;
            case "acceptRequest":
                title = "تایید دانش آموز توسط مشاور";
                msg += "درخواست شما برای مشاوره، توسط " + textMessage + " پذیرفته شد." + "<br/>" + "با نهایی کردن پرداخت، فرایند مشاوره آغاز می\u200Cشود.";
                break;
            case "rejectRequest":
                title = "رد دانش آموز توسط مشاور";
                msg += "درخواست شما برای مشاوره، توسط " + textMessage + " رد شد.";
                break;
            case "cancelAdvisorRequest":
                title = "انصراف درخواست مشاوره توسط دانش آموز";
                msg += "درخواست مشاوره، توسط " + textMessage + " لغو شد.";
                break;
            case "createRoom":
                title = "ایجاد اتاق جلسه";
                msg += "یک جلسه\u200Cی مشاوره آنلاین در اسکای\u200Cروم ساخته شد." + "<br/>" + "نام کاربری و رمزعبور شما در صورتی که از قبل اکانتی نداشته باشید، کد ملی شما خواهد بود." + "<br/>" + "لینک: " + "<a href='" + textMessage + "'>" + textMessage + "</a>" + "<br/><br/>" + "سؤالات خود را قبل از جلسه روی کاغذ بنویس تا بهترین استفاده را از این زمان داشته باشی." + "<br/>" + "خوش بگذره";
                break;
            case "createTeachRoom":
                title = "ایجاد اتاق جلسه";
                msg += "یک جلسه\u200Cی تدریس آنلاین در اسکای\u200Cروم ساخته شد." + "<br/>" + "نام کاربری و رمزعبور شما در صورتی که از قبل اکانتی نداشته باشید، کد ملی شما خواهد بود." + "<br/>" + "لینک: " + "<a href='" + textMessage + "'>" + textMessage + "</a>" + "<br/><br/>" + "سؤالات خود را قبل از جلسه روی کاغذ بنویس تا بهترین استفاده را از این زمان داشته باشی." + "<br/>" + "خوش بگذره";
                break;
            case "advisorQuiz":
                title = "تعریف آزمون توسط مشاور";
                msg += "یک آزمون ویژه توسط مشاور برای تو ساخته شده است." + "<br/>" + "این آزمون در بخش مشاور -> آزمون ها در دسترس است." + "<br/>" + "خودت را محک بزن!";
                break;
            case "schoolQuiz":
                title = "تعریف آزمون توسط مدرسه";
                msg += "یک آزمون ویژه توسط مدرسه برای تو ساخته شده است." + "<br/>" + "این آزمون در بخش مدرسه من -> آزمون ها در دسترس است." + "<br/>" + "خودت را محک بزن!";
                break;
            case "nextLevel":
                title = "باریکلا، تلاش خوبی کردی و حالا سطح تو در گچ\u200Cسفید به " + textMessage + " رسید.";
                msg += "جایزه\u200Cهای این سطح رو از " + "<a href='https://www.irysc.com/%d8%b1%d8%a7%d9%87%d9%86%d9%85%d8%a7%db%8c-%da%af%da%86-%d8%b3%d9%81%db%8c%d8%af-%d8%a2%db%8c%d8%b1%db%8c%d8%b3%da%a9/%d8%a7%d9%85%d8%aa%db%8c%d8%a7%d8%b2-%d9%85%d8%af%d8%a7%d9%84-%da%af%da%86-%d8%b3%d9%81%db%8c%d8%af/'>اینجا</a> ببین.";
                break;
            case "badge":
                String[] split = textMessage.split("__");
                title = "خیلی کِیف کردیم که مدال " + split[0] + " رو تونستی بگیری.";
                msg += "خوشحال\u200Cتر می\u200Cشیم که بقیۀ مدال\u200Cهای اینجا و زندگی رو هم درو کنی." + "<br/>" + "برای اینکه تو هم خوشحال بشی، " + split[1] + " ایکس\u200Cپول به حسابت اضافه شد :)";
                break;
            case "birthday":
                title = "تولدت مبارک!";
                msg += "امیدواریم سال\u200Cها چرخت به خوبی و بدون لنگ زدن بچرخه و هر سال برات پر از خاطرات خوب باشه. برای شیرین\u200Cتر شدن شروع امسالت، " + textMessage + "امتیاز از آیریسک هدیه گرفتی!" + "<br/><br/>" + "حالش رو ببر";
                break;
            case "hw":
                title = "تعریف تمرین توسط مدرسه";
                msg += "یک تمرین ویژه توسط مدرسه برای تو ساخته شده است." + "<br/>" + "این آزمون در بخش مدرسه من -> تمرین ها در دسترس است." + "<br/>" + "خودت را محک بزن!";
                break;
            case "adviceWillExpireTomorrow":
                String[] tmpSplited3 = textMessage.split("__");
                title = "یک روز از زمان مشاورهٔ یک ماهه شما با استاد " + tmpSplited3[0] + "، باقی\u200Cمانده است.";
                msg += "از " + "<a href=" + tmpSplited3[1] + "> این لینک </a>" + "می\u200Cتونی بسته\u200Cهای مشاوره رو ببینی و برای ماه\u200Cهای بعد تمدید کنی. " + "<br/><br/>" + " راستی ارسال نظر و امتیاز برای مشاور فراموش نشه! بچه\u200Cهای دیگه با نظر تو می\u200Cتونن بهترین مشاورها رو انتخاب کنن.";
                break;
            case "adviceWillExpireSoon":
                String[] tmpSplited2 = textMessage.split("__");
                title = "سه روز از زمان مشاورهٔ یک ماهه شما با استاد " + tmpSplited2[0] + "، باقی\u200Cمانده است.";
                msg += "از " + "<a href=" + tmpSplited2[1] + "> این لینک </a>" + "می\u200Cتونی بسته\u200Cهای مشاوره رو ببینی و برای ماه\u200Cهای بعد تمدید کنی. " + "<br/><br/>" + " راستی ارسال نظر و امتیاز برای مشاور فراموش نشه! بچه\u200Cهای دیگه با نظر تو می\u200Cتونن بهترین مشاورها رو انتخاب کنن.";
                break;
            case "adviceWillExpireNextWeek":
                String[] tmpSplited = textMessage.split("__");
                title = "۷ روز از زمان مشاورهٔ یک ماهه شما با استاد " + tmpSplited[0] + "، باقی\u200Cمانده است.";
                msg += "از " + "<a href=" + tmpSplited[1] + "> این لینک </a>" + "می\u200Cتونی بسته\u200Cهای مشاوره رو ببینی و برای ماه\u200Cهای بعد تمدید کنی. " + "<br/><br/>" + " راستی ارسال نظر و امتیاز برای مشاور فراموش نشه! بچه\u200Cهای دیگه با نظر تو می\u200Cتونن بهترین مشاورها رو انتخاب کنن.";
                break;
            case "karbarg":
            case "karbargDone":
                if (mode.equals("karbarg"))
                    title = "به روزرسانی برنامه توسط مشاور";
                else
                    title = "به روزرسانی برنامه توسط دانش\u200Cآموز";
                msg += "برنامه هفتگی توسط " + textMessage + " به\u200Cروزرسانی شد." + "<br/>" + "شاد باشید :)";
                break;
            case "acceptComment":
                title = "تایید نظر";
                msg += textMessage + " مورد تایید قرار گرفت و امتیاز آن برای شما لحاظ گردید";
                break;
            case "rejectComment":
                title = "رد نظر";
                msg += textMessage + " رد شد";
                break;
            default:
                title = "";
                msg = "";
        }

        msg += "</p>";

        ObjectId notifId = new ObjectId();
        Document notif = new Document("_id", notifId)
                .append("users_count", 1)
                .append("title", title)
                .append("text", msg)
                .append("send_via", "site")
                .append("created_at", curr)
                .append("users", new ArrayList<ObjectId>() {{
                    add(wantedUser.getObjectId("_id"));
                }});

        List<Document> events = (List<Document>) wantedUser.getOrDefault("events", new ArrayList<Document>());
        events.add(
                new Document("created_at", curr)
                        .append("notif_id", notifId)
                        .append("seen", false)
        );

        wantedUser.put("events", events);
        return notif;
    }

    public static void createNotifAndSendSMS(Document wantedUser, String textMessage, String mode) {
        String wantedUserName = wantedUser.getString("first_name") + " " + wantedUser.getString("last_name");

        if (wantedUser.containsKey("phone"))
            sendSMSWithTemplate(wantedUser.getString("phone"), 815,
                    new PairValue("name", wantedUserName)
            );

        notifRepository.insertOne(doCreateNotif(wantedUser, textMessage, mode));
    }

    public static void createJustNotif(Document wantedUser, String textMessage, String mode) {
        notifRepository.insertOne(doCreateNotif(wantedUser, textMessage, mode));
    }
}
