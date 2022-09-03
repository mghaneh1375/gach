package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.gt;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx;

public class PackageController {

    public static String createPackage(JSONObject jsonObject) {

        ObjectId gradeId = new ObjectId(jsonObject.getString("gradeId"));

        Document grade = gradeRepository.findById(gradeId);

        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        ObjectId lessonId = new ObjectId(jsonObject.getString("lessonId"));

        if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                grade.getList("lessons", Document.class),
                "_id", lessonId
        ) == -1)
            return JSON_NOT_VALID_PARAMS;

        Document newDoc = new Document("title", jsonObject.getString("title"))
                .append("off_percent", jsonObject.getInt("offPercent"))
                .append("min_select", jsonObject.getInt("minSelect"))
                .append("description", jsonObject.has("description") ? jsonObject.getString("description") : "")
                .append("quizzes", new ArrayList<>())
                .append("lesson_id", lessonId)
                .append("grade_id", gradeId)
                .append("buyers", 0)
                .append("expire_at", System.currentTimeMillis());

        return packageRepository.insertOneWithReturn(newDoc);
    }

    public static String editPackage(ObjectId packageId, JSONObject jsonObject) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        if (jsonObject.has("lessonId") && jsonObject.has("gradeId")) {

            ObjectId gradeId = new ObjectId(jsonObject.getString("gradeId"));

            Document grade = gradeRepository.findById(gradeId);

            if (grade == null)
                return JSON_NOT_VALID_PARAMS;

            ObjectId lessonId = new ObjectId(jsonObject.getString("lessonId"));

            if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    grade.getList("lessons", Document.class),
                    "_id", lessonId
            ) == -1)
                return JSON_NOT_VALID_PARAMS;

            packageDoc.put("lesson_id", lessonId);
            packageDoc.put("grade_id", gradeId);

        }

        if (jsonObject.has("title"))
            packageDoc.put("title", jsonObject.getString("title"));

        if (jsonObject.has("offPercent"))
            packageDoc.put("off_percent", jsonObject.getInt("offPercent"));

        if (jsonObject.has("minSelect"))
            packageDoc.put("min_select", jsonObject.getInt("minSelect"));

        if (jsonObject.has("description"))
            packageDoc.put("description", jsonObject.getString("description"));

        packageRepository.replaceOne(packageId, packageDoc);
        return JSON_OK;
    }

    public static String addQuizzesToPackage(ObjectId packageId, JSONArray jsonArray) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);
        long curr = System.currentTimeMillis();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id))
                continue;

            ObjectId quizId = new ObjectId(id);

            Document quiz = iryscQuizRepository.findById(quizId);
            if (quiz == null)
                continue;

            long endRegistry = quiz.containsKey("end_registry") ?
                    quiz.getLong("end_registry") : quiz.getLong("end");

            if (endRegistry < curr)
                continue;
//                return generateErr("زمان ثبت نام آزمون موردنظر به اتمام رسیده است.");

            if (quizzes.contains(quizId))
                continue;
//                return JSON_OK;

            if (endRegistry > packageDoc.getLong("expire_at"))
                packageDoc.put("expire_at", endRegistry);

            quizzes.add(quizId);
        }

        packageDoc.put("quizzes", quizzes);
        packageRepository.replaceOne(packageId, packageDoc);

        return getPackageQuizzes(packageId, true);
    }

    public static String removeQuizzesFromPackage(ObjectId packageId, JSONArray jsonArray) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectId.isValid(id))
                continue;

            ObjectId quizId = new ObjectId(id);

            Document quiz = iryscQuizRepository.findById(quizId);
            if (quiz == null)
                continue;

            if (!quizzes.contains(quizId))
                continue;

            long max = 13000000;

            for (ObjectId qId : quizzes) {

                if (qId.equals(quizId))
                    continue;

                Document q = iryscQuizRepository.findById(qId);

                long e = q.containsKey("end_registry") ?
                        q.getLong("end_registry") :
                        q.getLong("end");

                if (e > max)
                    max = e;
            }

            packageDoc.put("expire_at", max);
            quizzes.remove(quizId);
        }

        packageDoc.put("quizzes", quizzes);
        packageRepository.replaceOne(packageId, packageDoc);
        return getPackageQuizzes(packageId, true);
    }

    public static String getPackages(boolean isAdmin, ObjectId userId,
                                     ObjectId gradeId, ObjectId lessonId
    ) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (!isAdmin)
            filters.add(gt("expire_at", System.currentTimeMillis()));

        if (gradeId != null)
            filters.add(eq("grade_id", gradeId));

        if (lessonId != null)
            filters.add(eq("lesson_id", lessonId));

        ArrayList<Document> packages = packageRepository.find(
                filters.size() == 0 ? null : and(filters), null
        );

        JSONArray jsonArray = new JSONArray();
        long curr = System.currentTimeMillis();

        HashMap<String, ArrayList<String>> tags = isAdmin ? null : new HashMap<>();
        ArrayList<ObjectId> fetched = isAdmin ? null : new ArrayList<>();
        JSONObject data = new JSONObject();

        for (Document packageDoc : packages) {

            Document grade = gradeRepository.findById(packageDoc.getObjectId("grade_id"));
            if (grade == null)
                continue;

            Document lesson = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                    grade.getList("lessons", Document.class),
                    "_id", packageDoc.getObjectId("lesson_id")
            );

            JSONObject jsonObject = new JSONObject()
                    .put("id", packageDoc.getObjectId("_id").toString())
                    .put("title", packageDoc.getString("title"))
                    .put("description", packageDoc.getOrDefault("description", ""))
                    .put("buyers", isAdmin ? packageDoc.getInteger("buyers") : 0)
                    .put("grade", new JSONObject()
                            .put("id", grade.getObjectId("_id").toString())
                            .put("name", grade.getString("name"))
                    )
                    .put("lesson", new JSONObject()
                            .put("id", lesson.getObjectId("_id").toString())
                            .put("name", lesson.getString("name"))
                    )
                    .put("offPercent", packageDoc.getInteger("off_percent"))
                    .put("minSelect", packageDoc.getInteger("min_select"));

            List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);

            if (!isAdmin) {

                ArrayList<String> subTags;
                if (tags.containsKey(grade.getString("name")))
                    subTags = tags.get(grade.getString("name"));
                else
                    subTags = new ArrayList<>();

                if (!subTags.contains(lesson.getString("name")))
                    subTags.add(lesson.getString("name"));

                tags.put(grade.getString("name"), subTags);
                jsonObject.put("tags", new ArrayList<>() {{
                    add(lesson.getString("name"));
                }});

                JSONArray quizzesDoc = new JSONArray();
                int totalPrice = 0;
                int registrable = 0;

                for (ObjectId quizId : quizzes) {

                    Document quiz = iryscQuizRepository.findById(quizId);
                    fetched.add(quizId);

                    if (quiz == null || quiz.getLong("start_registry") > curr ||
                            (quiz.containsKey("end_registry") &&
                                    quiz.getLong("end_registry") < curr
                            ) ||
                            (!quiz.containsKey("end_registry") &&
                                    quiz.getLong("end") < curr
                            )
                    )
                        continue;

                    if(userId != null && searchInDocumentsKeyValIdx(
                            quiz.getList("students", Document.class),
                            "_id", userId
                    ) != -1)
                        continue;

                    QuizAbstract quizAbstract;

                    if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                        quizAbstract = new RegularQuizController();
                    else
                        quizAbstract = new TashrihiQuizController();

                    JSONObject quizDoc = quizAbstract.convertDocToJSON(quiz, true, false, false);
                    if ((quiz.containsKey("end_registry") &&
                            quiz.getLong("end_registry") > curr) ||
                            (!quiz.containsKey("end_registry") && quiz.getLong("end") > curr)
                    ) {
                        quizDoc.put("registrable", true);
                        totalPrice += quiz.getInteger("price");
                        registrable++;
                    } else
                        quizDoc.put("registrable", false);

                    quizzesDoc.put(quizDoc);
                }

                jsonObject
                        .put("quizzes", quizzesDoc.length())
                        .put("registrable", registrable)
                        .put("totalPrice", totalPrice)
                        .put("realPrice", totalPrice * ((100.0 - packageDoc.getInteger("off_percent")) / 100.0))
                        .put("quizzesDoc", quizzesDoc);
            } else {
                jsonObject
                        .put("quizzes", quizzes.size());
            }

            if(jsonObject.has("registrable") &&
                    jsonObject.getInt("registrable") > 0)
                jsonArray.put(jsonObject.put("type", "package"));
        }

        if (!isAdmin) {

            Document off = offcodeRepository.findOne(and(
                    exists("code", false),
                    eq("user_id", userId),
                    eq("used", false),
                    gt("expire_at", curr),
                    or(
                            eq("section", OffCodeSections.ALL.getName()),
                            eq("section", OffCodeSections.GACH_EXAM.getName())
                    )
            ), null, Sorts.descending("amount"));

            if (off != null)
                data.put("off", new JSONObject()
                        .put("type", off.getString("type"))
                        .put("amount", off.getInteger("amount"))
                );

            ArrayList<Document> docs = iryscQuizRepository.find(
                    and(
                            nin("_id", fetched),
                            eq("visibility", true),
                            lte("start_registry", curr),
                            or(
                                    exists("end_registry", false),
                                    gt("end_registry", curr)
                            ),
                            gt("end", curr)

                    ), null
            );
            System.out.println(docs.size());

            RegularQuizController quizController = new RegularQuizController();
            for (Document doc : docs) {
                if (doc.containsKey("tags")) {
                    List<String> t = doc.getList("tags", String.class);
                    if (t.size() > 0) {
                        ArrayList<String> subTags;
                        if (tags.containsKey("المپیاد"))
                            subTags = tags.get("المپیاد");
                        else
                            subTags = new ArrayList<>();

                        for (String itr : t) {
                            if (!subTags.contains(itr))
                                subTags.add(itr);
                        }

                        tags.put("المپیاد", subTags);
                    }
                }
                jsonArray.put(quizController.convertDocToJSON(
                        doc, true, false, false
                ).put("type", "quiz"));
            }
            data.put("tags", tags);
        }


        data.put("items", jsonArray);
        return generateSuccessMsg("data", data);
    }

    public static String getPackageQuizzes(ObjectId packageId, boolean isAdmin) {

        Document packageDoc = packageRepository.findById(packageId);
        if (packageDoc == null)
            return JSON_NOT_VALID_ID;

        JSONArray jsonArray = new JSONArray();
        for (ObjectId quizId : packageDoc.getList("quizzes", ObjectId.class)) {

            Document quiz = iryscQuizRepository.findById(quizId);

            if (quiz == null)
                continue;

            QuizAbstract quizAbstract;

            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, isAdmin, false));
        }

        return generateSuccessMsg("data", jsonArray);
    }

}