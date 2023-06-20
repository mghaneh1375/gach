package irysc.gachesefid.Controllers.Advisor;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Utility.Utility.getSolarDate;

public class Utility {

    private final static String SKY_ROOM_URL = "https://www.skyroom.online/skyroom/api/apikey-571737-51-0239041a552162f3ab913bf12a507863";

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

    public static String roomUrl(int roomId) {

        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject()
                            .put("action", "getRoomUrl")
                            .put("params", new JSONObject()
                                    .put("room_id", roomId)
                                    .put("language", "fa")
                            )
            ).asJson();
        } catch (UnirestException e) {
            return null;
        }

        if (response.getStatus() == 200) {

            JSONObject jsonObject = response.getBody().getObject();

            if (jsonObject.getBoolean("ok"))
                return jsonObject.getString("result");
        }

        return null;

    }

    public static int createMeeting(String title) {

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
                if (rooms < 50) {

                    long curr = System.currentTimeMillis();

                    try {
                        response = Unirest.post(
                                SKY_ROOM_URL
                        ).header("content-type", "application/json").body(
                                new JSONObject().put("action", "createRoom")
                                        .put("params", new JSONObject()
                                                .put("name", "consulting-" + curr)
                                                .put("title", title + curr)
                                                .put("guest_login", false)
                                                .put("op_login_first", false)
                                                .put("max_users", 2)
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
                    jsonObject.getInt("error_code") == 15
            ) {

                try {

                    response = Unirest.post(
                            SKY_ROOM_URL
                    ).header("content-type", "application/json").body(
                            new JSONObject()
                                    .put("action", "createUser")
                                    .put("params", new JSONObject()
                                            .put("username", NID)
                                            .put("password", "123456")
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

            }
        }

        return -1;
    }

    public static boolean addUserToClass(
            int studentIdInSkyRoom, int advisorIdInSkyRoom,
            int roomId
    ) {

        HttpResponse<JsonNode> response;
        try {

            response = Unirest.post(
                    SKY_ROOM_URL
            ).header("content-type", "application/json").body(
                    new JSONObject()
                            .put("action", "addRoomUsers")
                            .put("params", new JSONObject()
                                    .put("roomId", roomId)
                                    .put("users", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("user_id", studentIdInSkyRoom)
                                                    .put("access", 1)
                                            )
                                            .put(new JSONObject()
                                                    .put("user_id", advisorIdInSkyRoom)
                                                    .put("access", 3)
                                            )
                                    )
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

    public static JSONObject convertToJSONDigest(ObjectId stdId, Document advisor) {

        List<Document> students = (List<Document>) advisor.getOrDefault("students", new ArrayList<>());

        JSONObject jsonObject = new JSONObject()
                .put("name", advisor.getString("first_name") + " " + advisor.getString("last_name"))
                .put("acceptStd", advisor.getOrDefault("accept_std", true))
                .put("stdCount", students.size())
                .put("rate", advisor.getOrDefault("rate", 0))
                .put("bio", advisor.getString("bio"))
                .put("id", advisor.getObjectId("_id").toString())
                .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + advisor.getString("pic"));

        if (stdId != null) {

            Document std = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    students, "_id", stdId
            );

            if (std != null)
                jsonObject.put("myRate", std.getOrDefault("rate", 0));

        }


        return jsonObject;
    }

    static JSONObject convertFinanceOfferToJSONObject(Document doc, boolean fullAccess) {

        JSONObject jsonObject = new JSONObject()
                .put("id", doc.containsKey("_id") ? doc.getObjectId("_id").toString() : "-1")
                .put("price", doc.getInteger("price"))
                .put("title", doc.getString("title"))
                .put("description", doc.getOrDefault("description", ""))
                .put("videoCalls", doc.getInteger("video_calls"))
                .put("maxKarbarg", doc.getOrDefault("max_karbarg", -1))
                .put("maxExam", doc.getOrDefault("max_exam", -1))
                .put("maxChat", doc.getOrDefault("max_chat", -1));

        if(fullAccess) {
            jsonObject
                    .put("createdAt", getSolarDate(doc.getLong("created_at")))
                    .put("visibility", doc.getBoolean("visibility"))
                    .put("studentsCount", doc.getOrDefault("students", 0));
        }

        return jsonObject;
    }

    private static String getWeekDay(int dayIdx) {

        switch (dayIdx) {
            case 0:
            default:
                return "شنبه";
            case 1:
                return "یک شنبه";
            case 2:
                return "دوشنبه";
            case 3:
                return "سه شنبه";
            case 4:
                return "چهار شنبه";
            case 5:
                return "پنج شنبه";
            case 6:
                return "جمعه";
        }

    }

    static JSONArray convertLifeScheduleToJSON(Document schedule) {

        JSONArray jsonArray = new JSONArray();

        for (Document day : schedule.getList("days", Document.class)) {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("label", schedule.getString("label"))
                    .put("day", getWeekDay(day.getInteger("day")));

            JSONArray jsonArray1 = new JSONArray();
            for(Document item : day.getList("items", Document.class)) {
                JSONObject jsonObject1 = new JSONObject()
                        .put("tag", item.getString("tag"))
                        .put("id", item.getObjectId("_id").toString())
                        .put("duration", item.get("duration"));

                if(item.containsKey("start_at"))
                    jsonObject1.put("startAt", item.get("start_id"));

                jsonArray1.put(jsonObject1);
            }

            jsonObject.put("items", jsonArray1);
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

}
