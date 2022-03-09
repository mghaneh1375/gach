package irysc.gachesefid.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.ONE_DAY_MIL_SEC;

public class Utility {

    public static String adminToken = null;
    public static String adminUsername = "09214915905";
    public static String studentUsername = "mghaneh1375@ut.ac.ir";
    public static ObjectId studentId = new ObjectId("612c7ae6af377d3b48bf59fe");
    static String baseUrl = "http://localhost:8080/api/";

    public static String signIn(String username, String password)
            throws InvalidFieldsException {

        JSONObject res = sendPostReq("user/signIn", new JSONObject()
                .put("username", username)
                .put("password", password)
        );

        String token = "Bearer " + res.getString("token");

        if (username.equals(adminUsername))
            adminToken = token;

        return token;
    }

    static ObjectId findMyObjectId(String token) {

        try {
            URL url = new URL(baseUrl + "user/getInfo");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + token);
            con.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONObject res = new JSONObject(content.toString());

            if (res.has("status") && res.getString("status").equals("ok"))
                return new ObjectId(res.getJSONObject("user").getString("_id"));
        } catch (Exception x) {
            x.printStackTrace();
        }

        return null;


    }

    public static JSONObject sendPostReq(String addr, String token)
            throws InvalidFieldsException {

        try {
            return send(
                    Unirest.post(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("Authorization", token)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendPutReq(String addr, String token) throws InvalidFieldsException {
        try {
            return send(
                    Unirest.put(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("Authorization", token)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendGetReq(String addr, String token) throws InvalidFieldsException {
        try {
            return send(
                    Unirest.get(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("Authorization", token)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static void sendGetFileReq(String addr, String token) throws InvalidFieldsException {
        try {
            HttpResponse<String> res = Unirest.get(baseUrl + addr)
                    .header("Authorization", token)
                    .asString();

            if (res.getStatus() != 200 ||
                    res.getBody().length() < 10
            )
                throw new InvalidFieldsException("can not download file");

        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendDeleteReq(String addr, String token)
            throws InvalidFieldsException {

        try {
            return send(
                    Unirest.delete(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("Authorization", token)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendPostReq(String addr, JSONObject jsonObject)
            throws InvalidFieldsException {

        try {
            return send(
                    Unirest.post(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("content-type", "application/json")
                            .body(jsonObject)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendPostReq(String addr, String token, JSONObject jsonObject)
            throws InvalidFieldsException {

        try {
            return send(
                    Unirest.post(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("content-type", "application/json")
                            .header("Authorization", token)
                            .body(jsonObject)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    public static JSONObject sendPutReq(String addr, String token, JSONObject jsonObject)
            throws InvalidFieldsException {

        try {
            return send(
                    Unirest.put(baseUrl + addr)
                            .header("accept", "application/json")
                            .header("content-type", "application/json")
                            .header("Authorization", token)
                            .body(jsonObject)
                            .asJson()
            );
        } catch (UnirestException x) {
            throw new InvalidFieldsException("unirest");
        }
    }

    private static JSONObject send(HttpResponse<JsonNode> jsonResponse)
            throws InvalidFieldsException {

        JSONObject res;

        if (jsonResponse != null) {

            res = jsonResponse.getBody().getObject();

            if (res.has("status") && res.getString("status").equals("ok"))
                return res;

            else if (res.has("status")) {
                if (res.has("msg"))
                    throw new InvalidFieldsException("not ok status " + res.getString("msg"));
                else if (res.has("message"))
                    throw new InvalidFieldsException("not ok status " + res.getString("message"));
            }
        }

        throw new InvalidFieldsException("not ok status");
    }

    public static ArrayList<JSONObject> readTestCases(String folder) throws Exception {

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();

        File f = new File(s + "/src/main/java/bogen/austria/Test/" + folder + "/stories/");
        ArrayList<JSONObject> jsonObjects = new ArrayList<>();

        for (File itr : Objects.requireNonNull(f.listFiles())) {
            if (itr.getName().contains(".json")) {
                jsonObjects.add(checkInputFileVars(readFile(itr)));
            }
        }

        return jsonObjects;
    }

    private static JSONObject checkInputFileVars(JSONObject jsonObject) {

        for (String key : jsonObject.keySet()) {

            if (jsonObject.get(key) instanceof String &&
                    jsonObject.getString(key).contains("$")
            ) {

                switch (jsonObject.getString(key)) {

                    case "$randMail":
                        jsonObject.put(key, irysc.gachesefid.Utility.Utility.randomString(20) + "@gmail.com");
                        break;

                    case "$randStr":
                        jsonObject.put(key, irysc.gachesefid.Utility.Utility.randomString(20));
                        break;

                    case "$randNum":
                        jsonObject.put(key, irysc.gachesefid.Utility.Utility.randomPhone(10));
                        break;

                    case "$objectId":
                        jsonObject.put(key, new ObjectId());
                        break;

                    case "$termId":
                        jsonObject.put(key, "612b3932c590d07e793e238e");
                        break;

                    case "$futureTS":
                        jsonObject.put(key, System.currentTimeMillis() + ONE_DAY_MIL_SEC * 15);
                        break;

                    case "$veryFutureTS":
                        jsonObject.put(key, System.currentTimeMillis() + ONE_DAY_MIL_SEC * 30);
                        break;

                    case "$currentTS":
                        jsonObject.put(key, System.currentTimeMillis());
                        break;

                    case "$oldTS":
                        jsonObject.put(key, System.currentTimeMillis() - ONE_DAY_MIL_SEC * 10);
                        break;

                    case "$veryOldTS":
                        jsonObject.put(key, System.currentTimeMillis() - ONE_DAY_MIL_SEC * 30);
                        break;

                    case "$randPassedDate":

                        String date = "1370/";

                        int month = Integer.parseInt(irysc.gachesefid.Utility.Utility.randomPhone(2));
                        while (month > 12 || month == 0)
                            month = Integer.parseInt(irysc.gachesefid.Utility.Utility.randomPhone(2));

                        int day = Integer.parseInt(irysc.gachesefid.Utility.Utility.randomPhone(2));
                        while (day > 29 || day == 0)
                            day = Integer.parseInt(irysc.gachesefid.Utility.Utility.randomPhone(2));

                        date += (month < 10) ? "0" + month : month;
                        date += "/";
                        date += (day < 10) ? "0" + day : day;

                        jsonObject.put(key, date);
                        break;

                    case "$randPhone":

                        String phone = "0912" + irysc.gachesefid.Utility.Utility.randomPhone(7);
                        while (userRepository.findByUsername(phone) != null)
                            phone = "0912" + irysc.gachesefid.Utility.Utility.randomPhone(7);

                        jsonObject.put(key, "0912" + irysc.gachesefid.Utility.Utility.randomPhone(7));
                        break;

                }

            } else if (jsonObject.get(key) instanceof JSONObject)
                jsonObject.put(key, checkInputFileVars(jsonObject.getJSONObject(key)));

        }

        return jsonObject;
    }

    private static JSONObject readFile(File f) throws Exception {

        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);

        fr.close();

        return new JSONObject(sb.toString());
    }

    public static boolean uploadFile(String token, String addr, String method) {

        File f = new File("./1.jpg");

        if (!f.exists()) {
            try {
                if (!f.createNewFile())
                    return false;
            } catch (IOException e) {
                return false;
            }
        }

        try {
            HttpResponse<JsonNode> res = null;

            if (method.toLowerCase().equals("post"))
                res = Unirest.post(baseUrl + addr)
                        .header("Accept", "application/json")
                        .header("Authorization", token)
                        .field("file", f)
                        .asJson();
            else if (method.toLowerCase().equals("put"))
                res = Unirest.put(baseUrl + addr)
                        .header("Accept", "application/json")
                        .header("Authorization", token)
                        .field("file", f)
                        .asJson();

            return res != null && res.getStatus() == 200 && res.getBody().getObject().has("status") &&
                    res.getBody().getObject().getString("status").equals("ok");
        } catch (UnirestException ignore) {
        }

        return false;
    }

}
