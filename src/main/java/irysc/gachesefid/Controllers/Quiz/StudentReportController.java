package irysc.gachesefid.Controllers.Quiz;

import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.OpenQuizRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.GeneralKindQuiz;
import irysc.gachesefid.Utility.Excel;
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
import static irysc.gachesefid.Controllers.Quiz.Utility.hasProtectedAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.stateRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_ACCESS;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal;

public class StudentReportController {


    public static String getRanking(Common db, boolean isAdmin,
                                    ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = db instanceof IRYSCQuizRepository ?
                    hasAccess(db, userId, quizId) :
                    hasProtectedAccess(db, isAdmin ? null : userId, quizId);

            if (
                    !quiz.containsKey("report_status") ||
                            !quiz.containsKey("ranking_list") ||
                            !quiz.getString("report_status").equalsIgnoreCase("ready")
            )
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            if (!isAdmin &&
                    !(boolean)quiz.getOrDefault("show_results_after_correction", true))
                return generateErr("زمان رویت نتایج آزمون هنوز فرا نرسیده است.");

            if(!isAdmin && db instanceof OpenQuizRepository) {

                List<Document> students = quiz.getList("students", Document.class);

                Document userDocInQuiz = searchInDocumentsKeyVal(
                        students, "_id", userId
                );

                if(userDocInQuiz == null)
                    return JSON_NOT_ACCESS;

                int neededTime = new RegularQuizController().calcLen(quiz);
                long startAt = userDocInQuiz.getLong("start_at");
                long curr = System.currentTimeMillis();

                int reminder = neededTime -
                        (int) ((curr - startAt) / 1000);

                if(reminder > 0)
                    return generateErr("هنوز زمان مشاهده نتایج فرا نرسیده است.");

                if(quiz.getLong("last_build_at") == null ||
                        quiz.getLong("last_build_at") < quiz.getLong("last_finished_at")
                ) {
                    new RegularQuizController.Taraz(quiz, openQuizRepository);
                    openQuizRepository.clearFromCache(quiz.getObjectId("_id"));
                    quiz = openQuizRepository.findById(quizId);
                }
            }

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

                Object[] stat = QuizAbstract.decodeFormatGeneral(doc.get("stat", Binary.class).getData());

                JSONObject jsonObject = new JSONObject()
                        .put("id", doc.getObjectId("_id").toString())
                        .put("name", studentsInfo.get(k).getString("first_name") + " " + studentsInfo.get(k).getString("last_name"))
                        .put("taraz", stat[0])
                        .put("cityRank", stat[3])
                        .put("stateRank", stat[2])
                        .put("rank", stat[1]);


                if(!studentsInfo.get(k).containsKey("city") ||
                        studentsInfo.get(k).get("city") == null) {
                    jsonObject.put("state", "نامشخص");
                    jsonObject.put("city", "نامشخص");
                }
                else {

                    ObjectId cityId = studentsInfo.get(k).get("city", Document.class).getObjectId("_id");

                    if (stateNames.containsKey(cityId))
                        jsonObject.put("state", stateNames.get(cityId));
                    else {
                        Document city = cityRepository.findById(cityId);
                        Document state = stateRepository.findById(city.getObjectId("state_id"));
                        stateNames.put(cityId, state.getString("name"));
                        jsonObject.put("state", stateNames.get(cityId));
                    }

                    jsonObject.put("city", studentsInfo.get(k).get("city", Document.class).getString("name"));
                }

                if(
                        !studentsInfo.get(k).containsKey("school") ||
                                studentsInfo.get(k).get("school") == null
                )
                    jsonObject.put("school", "آیریسک");
                else
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
