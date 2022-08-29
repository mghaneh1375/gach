package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.BSON;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.stateRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class StudentReportController {

    public static String getRanking(Common db, boolean isAdmin,
                                    ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (
                    !quiz.containsKey("report_status") ||
                            !quiz.containsKey("ranking_list") ||
                            !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            if (!isAdmin &&
                    !quiz.getBoolean("show_results_after_correction"))
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            JSONArray jsonArray = new JSONArray();

            ArrayList<ObjectId> userIds = new ArrayList<>();

            for (Document doc : quiz.getList("ranking_list", Document.class))
                userIds.add(doc.getObjectId("_id"));

            ArrayList<Document> studentsInfo = userRepository.findByIds(
                    userIds, true
            );

            HashMap<ObjectId, String> stateNames = new HashMap<>();
            int k = 0;

            for (Document doc : quiz.getList("ranking_list", Document.class)) {

                ObjectId cityId = studentsInfo.get(k).get("city", Document.class).getObjectId("_id");
                Object[] stat = QuizAbstract.decodeFormatGeneral(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("name", studentsInfo.get(k).getString("first_name") + " " + studentsInfo.get(k).getString("last_name"))
                        .put("taraz", stat[0])
                        .put("cityRank", stat[3])
                        .put("stateRank", stat[2])
                        .put("rank", stat[1]);

                if (stateNames.containsKey(cityId))
                    jsonObject.put("state", stateNames.get(cityId));
                else {
                    Document city = cityRepository.findById(cityId);
                    Document state = stateRepository.findById(city.getObjectId("state_id"));
                    stateNames.put(cityId, state.getString("name"));
                    jsonObject.put("state", stateNames.get(cityId));
                }

                jsonObject.put("city", studentsInfo.get(k).get("city", Document.class).getString("name"));
                jsonObject.put("school", studentsInfo.get(k).get("school", Document.class).getString("name"));

                jsonArray.put(jsonObject);
                k++;
            }

            return generateSuccessMsg(
                    "data", jsonArray
            );

        } catch (InvalidFieldsException e) {
            return JSON_NOT_ACCESS;
        }


    }
}
