package irysc.gachesefid.Digests;

import org.bson.types.ObjectId;
import org.json.JSONObject;

public class User {

    private String username;
    private String name;
    private ObjectId id;

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public ObjectId getId() {
        return id;
    }

    public User(String username, String name, ObjectId id) {
        this.username = username;
        this.name = name;
        this.id = id;
    }

    public JSONObject createJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", username);
        jsonObject.put("id", id.toString());
        jsonObject.put("name", name);
        return jsonObject;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
