package irysc.gachesefid.Utility;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class SkyRoomUtils {

    private final static String SKY_ROOM_URL = "https://www.skyroom.online/skyroom/api/apikey-571737-51-0239041a552162f3ab913bf12a507863";
    private final static Integer MAX_ROOMS_COUNT = 50;
    public final static String SKY_ROOM_PUBLIC_URL = "https://www.skyroom.online/ch/irysc/";


    public static void deleteMeeting(int roomId) {

        try {
            Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject()
                            .put("action", "deleteRoom")
                            .put("params", new JSONObject().put("room_id", roomId))

            ).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

    }

    public static int createUser(String NID, String name) {

        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject()
                            .put("action", "getUser")
                            .put("params", new JSONObject().put("username", NID))
            ).asJson();
        } catch (UnirestException e) {
            return -1;
        }

        if (response.getStatus() == 200) {

            JSONObject jsonObject = response.getBody().getObject();

            if (!jsonObject.getBoolean("ok") ||
                    (jsonObject.has("error_code") && jsonObject.getInt("error_code") == 15)
            ) {

                try {

                    response = Unirest.post(
                            SKY_ROOM_URL
                    ).header("content-type", "application/json").body(
                            new JSONObject()
                                    .put("action", "createUser")
                                    .put("params", new JSONObject()
                                            .put("username", NID)
                                            .put("password", NID)
                                            .put("nickname", name)
                                            .put("status", 1)
                                            .put("is_public", true)
                                    )
                    ).asJson();

                    if (response.getStatus() == 200) {

                        jsonObject = response.getBody().getObject();

                        if (jsonObject.getBoolean("ok"))
                            return jsonObject.getInt("result");

                    }

                } catch (UnirestException e) {
                    return -1;
                }

            } else if (jsonObject.has("result") && jsonObject.getJSONObject("result").has("id"))
                return jsonObject.getJSONObject("result").getInt("id");
        }

        return -1;
    }

    public static boolean addUserToClass(
            List<Integer> studentsIdInSkyRoom, int advisorIdInSkyRoom,
            int roomId
    ) {

        HttpResponse<JsonNode> response;
        try {

            JSONArray participants = new JSONArray()
                    .put(new JSONObject()
                            .put("user_id", advisorIdInSkyRoom)
                            .put("access", 3)
                    );

            for (Integer id : studentsIdInSkyRoom) {
                participants.put(new JSONObject()
                        .put("user_id", id)
                        .put("access", 1)
                );
            }

            response = Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject()
                            .put("action", "addRoomUsers")
                            .put("params", new JSONObject()
                                    .put("room_id", roomId)
                                    .put("users", participants)
                            )
            ).asJson();

            if (response.getStatus() == 200) {

                JSONObject jsonObject = response.getBody().getObject();

                if (jsonObject.getBoolean("ok"))
                    return true;
            }


        } catch (UnirestException e) {
            return false;
        }

        return false;
    }

    public static int createMeeting(
            String title, String roomUrl,
            int maxUsers, boolean opLoginFirst
    ) {

        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject().put("action", "countRooms")
            ).asJson();
        } catch (UnirestException e) {
            return -1;
        }

        if (response.getStatus() == 200) {

            JSONObject jsonObject = response.getBody().getObject();

            if (jsonObject.getBoolean("ok")) {

                int rooms = jsonObject.getInt("result");
                if (rooms < MAX_ROOMS_COUNT) {

                    long curr = System.currentTimeMillis();

                    try {
                        response = Unirest.post(
                                SKY_ROOM_URL
                        ).header("content-type", "application/json").body(
                                new JSONObject().put("action", "createRoom")
                                        .put("params", new JSONObject()
                                                .put("name", roomUrl)
                                                .put("title", title + curr)
                                                .put("guest_login", false)
                                                .put("op_login_first", opLoginFirst)
                                                .put("max_users", maxUsers)
                                                .put("session_duration", 120)
                                        )
                        ).asJson();
                    } catch (UnirestException e) {
                        return -1;
                    }

                    if (response.getStatus() == 200) {

                        jsonObject = response.getBody().getObject();

                        if (jsonObject.getBoolean("ok")) {
                            return jsonObject.getInt("result");
                        }
                    }
                }
            }
        }

        return -1;
    }

}
