package irysc.gachesefid.Utility;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Kavenegar.KavenegarApi;
import irysc.gachesefid.Kavenegar.excepctions.ApiException;
import irysc.gachesefid.Kavenegar.excepctions.HttpException;
import irysc.gachesefid.Kavenegar.models.SendResult;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Validator.DateValidator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;

public class Utility {

    private static final Pattern postalCodePattern = Pattern.compile("^\\d{10}$");
    private static final Pattern justNumPattern = Pattern.compile("^\\d+$");
    private static final Pattern passwordStrengthPattern = Pattern.compile("^(?=.*[0-9])(?=.*[A-z])(?=\\S+$).{8,}$");
    private static final Pattern mailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String ABC = "0123456789";
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

    public static String dayFormatter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String dayFormatterDut(String str) {
        switch (str.toLowerCase()) {
            case "sun":
                str = "son";
                break;
            case "tue":
                str = "die";
                break;
            case "wed":
                str = "mit";
                break;
            case "thu":
                str = "don";
        }

        return camel(str.substring(0, 2), true);
    }

    public static String convertStringToDate(String date, String delimeter) {
        return date.substring(0, 4) + delimeter + date.substring(4, 6) + delimeter + date.substring(6, 8);
    }

    public static int convertStringToDate(String date) {
        return Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10));
    }

    public static int convertTimeToInt(String time) {
        return Integer.parseInt(time.replace(":", ""));
    }

    public static String convertIntToTime(int time) {
        String timeStr = (time < 1000) ? "0" + time : time + "";
        return timeStr.substring(0, 2) + ":" + timeStr.substring(2);
    }

    public static String getToday(String delimeter) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar();
        return String.valueOf(sc.year) + delimeter + String.format(loc, "%02d",
                sc.month) + delimeter + String.format(loc, "%02d", sc.date);
    }

    public static int getToday() {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar();
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
    }

    public static int getTomorrow() {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar((1000 * 60 * 60 * 24));
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
    }

    public static int getPast(int days) {
        Locale loc = new Locale("en_US");
        SolarCalendar sc = new SolarCalendar(-ONE_DAY_MIL_SEC * days);
        return Integer.parseInt(String.valueOf(sc.year) + String.format(loc, "%02d",
                sc.month) + String.format(loc, "%02d", sc.date));
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
        return passwordStrengthPattern.matcher(convertPersianDigits(in)).matches();
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

    public static String randomPhone(int len) {

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++)
            sb.append(ABC.charAt(rnd.nextInt(ABC.length())));

        return sb.toString();
    }

    public static boolean sendSMS(int code, String phoneNum) {

        if(DEV_MODE)
            return true;

        try {
            KavenegarApi api = new KavenegarApi("2B7376456266497A53474479625A47755345324C73413D3D");
            SendResult Result = api.send("", convertPersianDigits(phoneNum), code + "");
            return true;
        } catch (HttpException ex) {
            System.out.print("HttpException  : " + ex.getMessage());
        } catch (ApiException ex) {
            System.out.print("ApiException : " + ex.getMessage());
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

    public static void convertPersian(JSONObject jsonObject) {

        for (String key : jsonObject.keySet()) {
            if (jsonObject.get(key) instanceof Integer)
                jsonObject.put(key, Integer.parseInt(Utility.convertPersianDigits(jsonObject.getInt(key) + "")));
            else if (jsonObject.get(key) instanceof String)
                jsonObject.put(key, Utility.convertPersianDigits(jsonObject.getString(key)));
        }
    }

    public static int calcYearDiff(String birthDay) {

        JalaliCalendar.YearMonthDate yearMonthDate = null;

        if (birthDay.contains("/") || birthDay.contains("-")) {
            String[] splited = (birthDay.contains("/")) ? birthDay.split("/") : birthDay.split("-");

            if (splited.length == 3) {
                String year = splited[0];
                String month = splited[1];
                String day = splited[2];
                if (month.length() == 1)
                    month = "0" + month;
                if (day.length() == 1)
                    day = "0" + day;

                yearMonthDate = new JalaliCalendar.YearMonthDate(year, month, day);
            }

        }

        if (yearMonthDate == null)
            yearMonthDate = JalaliCalendar.jalaliToGregorian(new JalaliCalendar.YearMonthDate(birthDay.substring(0, 4),
                    birthDay.substring(5, 7), birthDay.substring(8, 10)));
        else
            yearMonthDate = JalaliCalendar.jalaliToGregorian(yearMonthDate);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        try {

            String month = (yearMonthDate.getMonth() < 10) ? "0" + yearMonthDate.getMonth() : yearMonthDate.getMonth() + "";
            String day = (yearMonthDate.getDate() < 10) ? "0" + yearMonthDate.getDate() : yearMonthDate.getDate() + "";

            Date d1 = sdf.parse(day + "-" + month + "-" + yearMonthDate.getYear());

            long difference_In_Time
                    = System.currentTimeMillis() - d1.getTime();

            return (int) Math.floor((difference_In_Time / (1000 * 60.0 * 60 * 24)) / 365.0);

        } catch (Exception x) {
            printException(x);
        }

        return -1;
    }

    public static boolean sendMail(String to, String msg, String subject, String mode, String username) {

        if (DEV_MODE)
            return true;

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "mail.okft.org");
        prop.put("mail.smtp.port", "587");

        try {

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUserName, mailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("no-reply@okft.org", "??kft"));

            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            String html = "<div style='margin-right: 10%; margin-left: 10%; width: 80%;'>";
            html += "<div style='height: 8px; margin-bottom: 5px; background-color: #BB0000; width: 100%'></div>";
            html += "<img style='width: 200px; margin-bottom: 5px;' src='https://statics.okft.org/logo.png'>";
            html += "<h3 style='font-size: 1.4em'>??sterreichisches Kulturforums Teheran</h3>";
            html += "<div style='height: 8px; margin-top: 10px; background-color: #BB0000; width: 100%'></div>";
            html += "<div style='margin: 20px'>";
            if (username == null)
                html += "<p style='font-size: 1.1em; margin-bottom: 25px'>Hello, </p>";
            else
                html += "<p style='font-size: 1.1em; margin-bottom: 25px'>Hello, Dear " + username + "</p>";

            if (mode.equals("signUp"))
                html += "<p>We are happy you signed up for ??KF Teheran LMS. To start exploring, please use this verification code:</p>";
            else if (mode.equals("certReqRes"))
                html += "<p>You have requested us to determine your level of language proficiency.</p>";
            else if (mode.equals("setInterviewReqTime"))
                html += "<p>Your enrollment request for language proficiency exam have been approved.</p>";
            else if (mode.equals("forget"))
                html += "<p>There was a request to change your password, please use this verification code:</p>";
            else if (mode.equals("changeMail"))
                html += "<p>You have requested to change your email account in ??KF LMS, tap the button below to change your email:</p>";
            else if (mode.equals("createOffCode")) {
                String[] splited = msg.split("__");
                html += "<p>You have earned " + splited[0] + " IRR credit on okft.org valid until " + splited[1] + "</p>";
            }

            if (mode.equals("signUp") || mode.equals("forget"))
                html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>Your verification code is : </p>";
            else if (mode.equals("certReqRes"))
                html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>your level of language proficiency is</p>";
            else if (mode.equals("examReqRes"))
                html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>Your language proficiency exam result is:</p>";
            else if (mode.equals("setInterviewReqTime"))
                html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>Time and Date</p>";
            else if (mode.equals("changeMail"))
                html += "<a target='_blank' style='background-color: #BB0000; color: white; font-size: 1.4em; font-weight: bolder; padding-top: 10px; padding-bottom: 10px; padding-left: 20px; padding-right: 20px; text-decoration: none' href='" + msg + "'>Change Email</a>";
            else if (mode.equals("createOffCode"))
                html += "<p>This credit will be automatically deducted from all your payments on the website until it expires.</p>";

            if (!mode.equals("changeMail") && !mode.equals("createOffCode"))
                html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>" + msg + "</p>";
            else if (!mode.equals("createOffCode")) {
                html += "<p style='font-size: 1.4em; font-weight: bolder;'>You have to use the same browser as you have requested to change your Email. If not, your Email will not change.<br/>";
                html += "<span style='color: #BB0000;'>This button is valid for 5 minutes.</span></p>";
                html += "<p style='margin-top: 20px; font-size: 1.4em; font-weight: bolder;'>If you don't want to change your Email, Simply ignore this mail.</p>";
            }

            if (mode.equals("signUp"))
                html += "<p style='margin-top: 10px; font-size: 1.1em'>You must enter this code on the sign up page, as requested.</p>";
            else if (mode.equals("forget"))
                html += "<p style='margin-top: 10px; font-size: 1.1em'>You must enter this code on the forget password page, as requested.</p>";
            else if (mode.equals("certReqRes") || mode.equals("examReqRes"))
                html += "<p style='margin-top: 10px; font-size: 1.1em'>If you have any complaints or objections, please share them with us. You can go to your LMS panel to do so.</p>";
            else if (mode.equals("setInterviewReqTime"))
                html += "<p style='font-size: 1.1em; color: black;'>North Sohrevardi St., Khorramshahr St., Arabali St., Alley 6th, Sibouyeh, No. 1</p>";

            html += "<p style='margin-top: 20px; font-size: 1.1em; font-weight: bolder;'>What is this email?</p>";

            if (mode.equals("signUp")) {
                html += "<p style='font-size: 1.1em'>We sent this email to verify that this address is your email address.</p>";
                html += "<p style='font-size: 1.1em'>If you didn't try to sign up or request this email, simply ignore or delete this email and don't worry. </p>";
            } else if (mode.equals("forget")) {
                html += "<p style='font-size: 1.1em'>We sent this email to reset your password as you requested.</p>";
                html += "<p style='font-size: 1.1em'>If you didn't forget your password or request this email, simply ignore or delete this email. Don't worry, your email address may have been entered by mistake.</p>";
            } else if (mode.equals("setInterviewReqTime") || mode.equals("createOffCode") ||
                    mode.equals("certReqRes") || mode.equals("examReqRes")) {
                html += "<p style='font-size: 1.1em'>We sent this email to inform you about the latest notifications.</p>";
                html += "<p style='font-size: 1.1em'>If you didn't ask for this mail, simply ignore or delete this email. Don't worry, your email address may have been entered by mistake.</p>";
            }


            html += "</div>";
            html += "<div style='height: 200px; font-weight: bolder; padding: 5px; margin-top: 20px; background-color: #2A2A2A; width: 100%'>";
            html += "<p style='color: white'>??sterreichisches Kulturforums Teheran</p>";
            html += "<p style='color: white; margin-top: 60px; font-size: 0.9em'>Need any help? Please visit our website: </p>";
            html += "<div style='color: white; font-size: 0.9em'>https://okft.org</div>";
            html += "<p style='color: white; margin-top: 10px; font-size: 0.9em'>This message was sent to you by okft.org</p>";
            html += "<p style='color: white; font-size: 0.9em'>North Sohrevardi St., Khorramshahr St., Arabali St., Alley 6th, Sibouyeh, No. 1 +982188765525</p>";
            html += "</div>";
            html += "</div>";
            mimeBodyPart.setContent(html, "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart, "text/html; charset=UTF-8");

            Transport.send(message);

            new Thread(() -> mailRepository.insertOne(new Document("created_at", System.currentTimeMillis()).append("recp", to).append("subject", subject))).start();
        } catch (Exception x) {
            printException(x);
            return false;
        }

        return true;
    }

    public static boolean sendClassRegistryMail(String to, String msg, String term, String endRegistry,
                                                String username, String mode, String price, String classId) {

        if (DEV_MODE)
            return true;

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "mail.okft.org");
        prop.put("mail.smtp.port", "587");

        try {

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUserName, mailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("no-reply@okft.org", "??kft"));

            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Enrollment information");

            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            String html = "<div style='margin-right: 10%; margin-left: 10%; width: 80%;'>";
            html += "<div style='height: 8px; margin-bottom: 5px; background-color: #BB0000; width: 100%'></div>";
            html += "<img style='width: 200px; margin-bottom: 5px;' src='https://statics.okft.org/logo.png'>";
            html += "<h3 style='font-size: 1.4em'>??sterreichisches Kulturforums Teheran</h3>";
            html += "<div style='height: 8px; margin-top: 10px; background-color: #BB0000; width: 100%'></div>";
            html += "<div style='margin: 20px'>";
            if (username == null)
                html += "<p style='font-size: 1.1em; margin-bottom: 25px'>Hello, </p>";
            else
                html += "<p style='font-size: 1.1em; margin-bottom: 25px'>Hello, Dear " + username + "</p>";

            switch (mode) {
                case "classRegistry":
                    html += "<p style='font-size: 1.6em; color: black; font-weight: bolder;'>You are accepted for class ";
                    break;
                case "successClassRegistry":
                    html += "<p>You have been successfully registered in class:</p>";
                    break;
                case "classQueue":
                    html += "<p>Unfortunately, Your enrollment request for " + term + " have been approved.</p>";
                    break;
            }

            if (mode.equals("classRegistry"))
                html += msg + "</p>";
            else
                html += "<p style='font-size: 1.5em; color: black; font-weight: bolder;'>" + msg + "</p>";

            switch (mode) {
                case "classRegistry":
                    html += "<p style='margin-top: 10px; font-size: 1.5em; font-weight: bolder;'>Please deposit the amount of " + price + " Tomans till " + endRegistry + " through the following link.</p>";
                    html += "<a href='https://okft.org/goToPayment/" + classId + "' target='_blank' style='margin-top: 20px; font-size: 1.4em; color: #0000ff; font-weight: bolder;'>https://okft.org/goToPayment/" + classId + "</a>";
                    break;
                case "classQueue":
                    html += "<p style='margin-top: 10px; font-size: 1.1em'>You should wait for further notification.</p>";
                    break;
                case "successClassRegistry":
                    html += "<p style='margin-top: 10px; font-color: red; font-size: 1.1em'>You will receive detailed information about your virtual class user name and password.</p>";
                    break;
            }

            if (mode.equals("classRegistry")) {
                html += "<p style='margin-top: 10px; font-size: 1.2em'>Naturally after " + endRegistry + " the pay-link will be deactivated for you and your turn will be given to the waiting list names.<br/>" +
                        "Sincerely <br/>" +
                        "Austrian Cultural Forum in Tehran (??KF)</p>";
            }

            html += "<p style='margin-top: 20px; font-size: 1.1em; font-weight: bolder;'>What is this email?</p>";

            html += "<p style='font-size: 1.1em'>We sent this email to inform you about the latest notifications.</p>";
            html += "<p style='font-size: 1.1em'>If you didn't ask for this mail, simply ignore or delete this email. Don't worry, your email address may have been entered by mistake.</p>";


            html += "</div>";
            html += "<div style='height: 200px; font-weight: bolder; padding: 5px; margin-top: 20px; background-color: #2A2A2A; width: 100%'>";
            html += "<p style='color: white'>??sterreichisches Kulturforums Teheran</p>";
            html += "<p style='color: white; margin-top: 60px; font-size: 0.9em'>Need any help? Please visit our website: </p>";
            html += "<div style='color: white; font-size: 0.9em'>https://okft.org</div>";
            html += "<p style='color: white; margin-top: 10px; font-size: 0.9em'>This message was sent to you by okft.org</p>";
            html += "<p style='color: white; font-size: 0.9em'>North Sohrevardi St., Khorramshahr St., Arabali St., Alley 6th, Sibouyeh, No. 1 +982188765525</p>";
            html += "</div>";
            html += "</div>";
            mimeBodyPart.setContent(html, "text/html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);

            Transport.send(message);
        } catch (Exception x) {
            printException(x);
            return false;
        }

        return true;
    }

    public static String formatPrice(int price) {
        return String.format("%,d", price);
    }

    public static int getCurrTime() {

        SimpleDateFormat df = new SimpleDateFormat("HHmm");
        df.setTimeZone(TimeZone.getTimeZone("GMT+4:30"));
        Date date = new Date();
        return Integer.parseInt(df.format(date));
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

    public static long getTimestamp(String date, String time) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            return formatter.parse(date + " " + time).getTime();
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
        }
        else
            dates.add(null);

        if (answerDateSolar != null) {
            String[] splited = answerDateSolar.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                    new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        }
        else
            dates.add(null);

        if (sendDateSolarEndLimit != null) {
            String[] splited = sendDateSolarEndLimit.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                    new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        }
        else
            dates.add(null);

        if (answerDateSolarEndLimit != null) {
            String[] splited = answerDateSolarEndLimit.split("-");
            dates.add(JalaliCalendar.jalaliToGregorian(
                    new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2]))
                    .format("-"));
        }
        else
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

    public static ArrayList<Object> paginate(Common repository,
                                             ArrayList<Bson> constraints,
                                             Integer page, Integer perPage,
                                             Integer startRow, Integer endRow) {

        ArrayList<Object> arrayList = new ArrayList<>();
        Bson skip = null, limit = null;
        Integer totalPages = null;
        Integer lastRow = null;

        if (page != null && page > 0) {
            int perPageItems = (perPage == null) ? ITEMS_PER_PAGE : perPage;
            skip = Aggregates.skip((page - 1) * perPageItems);
            limit = Aggregates.limit(perPageItems);
            int totalRows = repository.count((constraints.size() == 0) ? null : Filters.and(constraints));
            totalPages = (int) Math.ceil((totalRows * 1.0) / perPageItems);
        }

        if (skip == null && startRow != null && endRow != null) {
            skip = Aggregates.skip(Math.max(0, startRow - 1));
            int perPageItems = Math.max(0, endRow - startRow);
            limit = Aggregates.limit(perPageItems);

            int totalRows = repository.count((constraints.size() == 0) ? null : and(constraints));

            int currentLastRow = startRow + totalRows;
            lastRow = currentLastRow <= endRow ? currentLastRow : -1;
            totalPages = (int) Math.ceil((totalRows * 1.0) / perPageItems);
        }

        arrayList.add(skip);
        arrayList.add(limit);
        arrayList.add(totalPages);
        arrayList.add(lastRow);

        return arrayList;
    }

    public static String getSolarDate(long time) {
        Date d = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] dateTime = simpleDateFormat.format(d).split(" ");
        String[] splited = dateTime[0].split("-");
        return JalaliCalendar.gregorianToJalali(new JalaliCalendar.YearMonthDate(splited[0], splited[1], splited[2])).format("/") + " - " + dateTime[1];
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

        System.out.println(x.getMessage());
        int limit = x.getStackTrace().length > 5 ? 5 : x.getStackTrace().length;
        for (int i = 0; i < limit; i++)
            System.out.println(x.getStackTrace()[i]);

    }

    public static void fillJSONWithUser(JSONObject jsonObject, Document user) {

        //todo: customize with common user info
        jsonObject.put("student", new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("first_name") + " " + user.getString("last_name"))
                .put("phone", user.getOrDefault("phone", ""))
                .put("mail", user.getOrDefault("mail", ""))
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                .put("NID", user.getString("NID"))
        );

    }

    public static boolean validationNationalCode(String code) {

        if (code.length() != 10)
            return false;

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
    }

    public static String returnAddResponse(JSONArray excepts,
                                           JSONArray added) {
        return returnBatchResponse(excepts, added, "??????????");
    }

    public static String returnRemoveResponse(JSONArray excepts,
                                              JSONArray removeIds) {
        return returnBatchResponse(excepts, removeIds, "??????");
    }

    public static String returnBatchResponse(JSONArray j1, JSONArray j2, String doneFa) {

        if (j1.length() == 0)
            return generateSuccessMsg(
                    "excepts", "?????????? ?????????? ???? ?????????? " + doneFa + " ??????????????",
                    new PairValue("doneIds", j2)
            );

        return generateSuccessMsg(
                "excepts",
                "?????? ?????????? ?????? ???????????? ???? ?????????? " + doneFa + " ??????????????. " + j1,
                new PairValue("doneIds", j2)
        );
    }
}
