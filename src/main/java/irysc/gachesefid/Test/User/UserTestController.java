package irysc.gachesefid.Test.User;

import irysc.gachesefid.Test.TestController;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.gt;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

public class UserTestController extends TestController {

    @Override
    public void setWantedNotifKeys() {

    }

    @Override
    public void run(JSONObject testcase) throws Exception {

    }

    @Override
    public void clear() {
        if(startAt != -1) {
            System.out.println("cleaning test user");
            userRepository.deleteMany(gt("created_at", startAt));
        }
    }
}
