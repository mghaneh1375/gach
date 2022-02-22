package irysc.gachesefid.Test;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class Utility {


    static String baseUrl = "http://localhost:8080/api/";

    static class ParameterStringBuilder {
        static String getParamsString(Map<String, String> params)
                throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
            }

            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - 1)
                    : resultString;
        }

    }

    static JSONObject sendReq(String addr, String token, String method) {

        try {

            URL url = new URL(addr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("Accept", "application/json");

            if(token != null)
                con.setRequestProperty("Authorization", "Bearer " + token);

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

            if(res.has("status") && res.getString("status").equals("ok"))
                return res;

        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return null;
    }

    static JSONObject sendReq(String addr, String token, String method, Map<String, String> parameters) {

        try {

            URL url = new URL(addr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("Accept", "application/json");

            if(token != null)
                con.setRequestProperty("Authorization", "Bearer " + token);

            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(Utility.ParameterStringBuilder.getParamsString(parameters));
            out.flush();
            out.close();

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

            if(res.has("status") && res.getString("status").equals("ok"))
                return res;

        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return null;
    }

    static JSONObject sendReq(String addr, String token, String method, Map<String, String> customHeader, JSONObject jsonInputString) {

        try {

            URL url = new URL(addr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/json");

            if(customHeader != null) {
                for(String key : customHeader.keySet())
                    con.setRequestProperty(key, customHeader.get(key));
            }

            if(token != null)
                con.setRequestProperty("Authorization", "Bearer " + token);

            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());

            try(OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            out.flush();
            out.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            System.out.println(content.toString());

            JSONObject res = new JSONObject(content.toString());

            if(res.has("status") && res.getString("status").equals("ok"))
                return res;

        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return null;
    }

}
