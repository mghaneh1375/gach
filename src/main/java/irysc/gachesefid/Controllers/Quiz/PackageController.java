package irysc.gachesefid.Controllers.Quiz;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.gt;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.Utility.*;

public class PackageController {

    public static String createPackage(JSONObject jsonObject) {

        ObjectId gradeId = new ObjectId(jsonObject.getString("gradeId"));

        Document grade = gradeRepository.findById(gradeId);

        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        ObjectId lessonId = null;

        if(jsonObject.has("lessonId")) {
            lessonId = new ObjectId(jsonObject.getString("lessonId"));

            if (irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    grade.getList("lessons", Document.class),
                    "_id", lessonId
            ) == -1)
                return JSON_NOT_VALID_PARAMS;
        }

        Document newDoc = new Document("title", jsonObject.getString("title"))
                .append("off_percent", jsonObject.getInt("offPercent"))
                .append("min_select", jsonObject.getInt("minSelect"))
                .append("priority", jsonObject.getInt("priority"))
                .append("description", jsonObject.has("description") ? jsonObject.getString("description") : "")
                .append("quizzes", new ArrayList<>())
                .append("grade_id", gradeId)
                .append("buyers", 0)
                .append("expire_at", System.currentTimeMillis());

        if(lessonId != null)
            newDoc.append("lesson_id", lessonId);

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

        if(!jsonObject.has("lessonId"))
            packageDoc.remove("lesson_id");

        if (jsonObject.has("title"))
            packageDoc.put("title", jsonObject.getString("title"));

        if (jsonObject.has("offPercent"))
            packageDoc.put("off_percent", jsonObject.getInt("offPercent"));

        if (jsonObject.has("priority"))
            packageDoc.put("priority", jsonObject.getInt("priority"));

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

    private final static Map<String, String> tagsColor = Stream.of(new String[][] {
            { "شیمی", "#00FF00" },
            { "ریاضی", "#FF0000" },
            { "فیزیک", "#0000FF" },
            { "زیست", "#00FFFF" },
            { "ادبی", "#FFFF00" },
            { "default", "#ffefce" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    public static String getPackages(List<String> accesses, ObjectId userId,
                              ObjectId gradeId, ObjectId lessonId, ObjectId id,
                              ObjectId quizIdFilter
    ) {

        if(quizIdFilter != null) {

            Document quiz = iryscQuizRepository.findById(quizIdFilter);
            if(quiz == null) {

                quiz = openQuizRepository.findById(quizIdFilter);
                if(quiz == null) {

                    quiz = onlineStandQuizRepository.findById(quizIdFilter);
                    if (quiz == null)
                        return JSON_NOT_VALID_PARAMS;
                }

            }

            if(searchInDocumentsKeyValIdx(
                    quiz.getList("students", Document.class), "_id", userId
            ) != -1)
                return generateSuccessMsg("data", new JSONObject()
                        .put("registered", true)
                );
        }

        boolean isAdmin = accesses != null && Authorization.isAdmin(accesses);
        boolean isSchool = !isAdmin && accesses != null && Authorization.isSchool(accesses);

        ArrayList<Bson> filters = new ArrayList<>();

        filters.add(gt("expire_at", System.currentTimeMillis()));

        if (id != null)
            filters.add(eq("_id", id));

        if (gradeId != null)
            filters.add(eq("grade_id", gradeId));

        if (lessonId != null)
            filters.add(and(
                    exists("lesson_id"),
                    eq("lesson_id", lessonId)
            ));

        ArrayList<Document> packages = quizIdFilter != null ? new ArrayList<>() : packageRepository.find(
                and(filters), null, Sorts.ascending("priority")
        );

        JSONArray jsonArray = new JSONArray();
        long curr = System.currentTimeMillis();

//        HashMap<String, ArrayList<String>> tags = new HashMap<>();

        ArrayList<String> tags = new ArrayList<>();
        ArrayList<ObjectId> fetched = new ArrayList<>();
        JSONObject data = new JSONObject();

        ArrayList<String> allMonth = new ArrayList<>();

        for (Document packageDoc : packages) {

            Document grade = gradeRepository.findById(packageDoc.getObjectId("grade_id"));
            if (grade == null)
                continue;

            Document lesson = null;

            if(packageDoc.containsKey("lesson_id"))
                lesson = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
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
                    .put("type", "package")
                    .put("offPercent", packageDoc.getInteger("off_percent"))
                    .put("minSelect", packageDoc.getInteger("min_select"));

            if(lesson != null) {
                jsonObject.put("lesson", new JSONObject()
                        .put("id", lesson.getObjectId("_id").toString())
                        .put("name", lesson.getString("name"))
                );
            }

            List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);

//            ArrayList<String> subTags;
//            if (tags.containsKey(grade.getString("name")))
//                subTags = tags.get(grade.getString("name"));
//            else
//                subTags = new ArrayList<>();
//
//            if (lesson != null && !subTags.contains(lesson.getString("name")))
//                subTags.add(lesson.getString("name"));
//
//            tags.put(grade.getString("name"), subTags);

//            if(lesson != null) {
//                Document finalLesson = lesson;
//                jsonObject.put("tags", new ArrayList<>() {{
//                    add(finalLesson.getString("name"));
//                }});
//            }

            JSONArray quizzesDoc = new JSONArray();
            int totalPrice = 0;
            int registrable = 0;
            ArrayList<String> packageMonth = new ArrayList<>();

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

                String month = getMonthSolarDate(quiz.getLong("start"));

                if(!packageMonth.contains(month))
                    packageMonth.add(month);

                if(!allMonth.contains(month))
                    allMonth.add(month);

                QuizAbstract quizAbstract;

                if (KindQuiz.REGULAR.getName().equals(quiz.getOrDefault("mode", "regular").toString()))
                    quizAbstract = new RegularQuizController();
                else
                    quizAbstract = new TashrihiQuizController();

                JSONObject quizDoc = quizAbstract.convertDocToJSON(quiz, true, false,
                        false, true
                );

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
                    .put("month", packageMonth)
                    .put("realPrice", totalPrice * ((100.0 - packageDoc.getInteger("off_percent")) / 100.0))
                    .put("quizzesDoc", quizzesDoc);

            if(jsonObject.has("registrable") &&
                    jsonObject.getInt("registrable") > 0) {

                if(!tags.contains(grade.getString("name"))) {
                    tags.add(grade.getString("name"));
                    jsonObject.put("tags", new ArrayList<>() {{
                        add(grade.getString("name"));
                    }});
                }

                jsonArray.put(jsonObject.put("type", "package"));

            }
            else if(!jsonObject.has("registrable"))
                jsonArray.put(jsonObject);
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

            if(isSchool) {
                data.put("groupRegistrationOff",
                        Utility.getConfig().getInteger("school_off_percent")
                );
            }

            if(id == null) {
                ArrayList<Bson> filtersForQuizzes = new ArrayList<>();

                if(fetched.size() > 0)
                    filtersForQuizzes.add(nin("_id", fetched));

                filtersForQuizzes.add(eq("visibility", true));
                filtersForQuizzes.add(lte("start_registry", curr));

                if(quizIdFilter != null)
                    filtersForQuizzes.add(eq("_id", quizIdFilter));

                filtersForQuizzes.add(or(
                        exists("end_registry", false),
                        gt("end_registry", curr)
                ));

                filtersForQuizzes.add(or(
                        and(
                                exists("is_registrable"),
                                eq("is_registrable", true),
                                exists("end", false)
                        ),
                        gt("end", curr)
                ));

                if (userId != null)
                    filtersForQuizzes.add(nin("students._id", userId));

                ArrayList<Document> docs = iryscQuizRepository.find(
                        and(filtersForQuizzes), null, Sorts.ascending("priority")
                );

                if (!isSchool) {

                    ArrayList<Bson> openQuizFilter = new ArrayList<>();

                    openQuizFilter.add(eq("visibility", true));

                    if(quizIdFilter != null)
                        openQuizFilter.add(eq("_id", quizIdFilter));

                    if(userId != null)
                        openQuizFilter.add(nin("students._id", userId));

                    docs.addAll(openQuizRepository.find(
                            and(openQuizFilter), null, Sorts.ascending("priority")
                    ));

                    ArrayList<Bson> onlineStandingQuizFilter = new ArrayList<>();

                    onlineStandingQuizFilter.add(eq("visibility", true));
                    onlineStandingQuizFilter.add(lte("start_registry", curr));
                    onlineStandingQuizFilter.add(gte("end_registry", curr));

                    if(userId != null)
                        onlineStandingQuizFilter.add(nin("students._id", userId));

                    if(quizIdFilter != null)
                        onlineStandingQuizFilter.add(eq("_id", quizIdFilter));

                    docs.addAll(onlineStandQuizRepository.find(
                            and(onlineStandingQuizFilter), null, Sorts.ascending("priority")
                    ));

                }

                RegularQuizController quizController = new RegularQuizController();
                OpenQuiz openQuiz = new OpenQuiz();
                TashrihiQuizController tashrihiQuizController = new TashrihiQuizController();
                OnlineStandingController onlineStandingController = new OnlineStandingController();


                for (Document doc : docs) {

                    String month = getMonthSolarDate(doc.getLong("created_at"));

                    if(!allMonth.contains(month))
                        allMonth.add(month);

                    String backColor = tagsColor.get("default");

                    if (doc.containsKey("tags")) {
                        List<String> t = doc.getList("tags", String.class);
                        if (t.size() > 0) {

                            for(String key : tagsColor.keySet()) {
                                if(t.get(0).contains(key)) {
                                    backColor = tagsColor.get(key);
                                    break;
                                }
                            }

                            for (String itr : t) {
                                if (!tags.contains(itr))
                                    tags.add(itr);
                            }

//                        ArrayList<String> subTags;
//
//                        if (tags.containsKey("المپیاد"))
//                            subTags = tags.get("المپیاد");
//                        else
//                            subTags = new ArrayList<>();
//
//                        for (String itr : t) {
//                            if (!subTags.contains(itr))
//                                subTags.add(itr);
//                        }
//
//                        tags.put("المپیاد", subTags);
                        }
                    }
                    JSONObject object;
                    if(doc.containsKey("max_teams"))
                        object = onlineStandingController.convertDocToJSON(
                                doc, true, false, false, true
                        ).put("type", "quiz");
                    else if (doc.containsKey("launch_mode"))
                        object = quizController.convertDocToJSON(
                                doc, true, false, false, true
                        ).put("type", "quiz");
                    else if(doc.containsKey("mode") && doc.get("mode") != null &&
                            doc.getString("mode").equalsIgnoreCase(KindQuiz.TASHRIHI.getName()))
                        object = tashrihiQuizController.convertDocToJSON(
                                doc, true, false, false, true
                        ).put("type", "quiz");
                    else
                        object = openQuiz.convertDocToJSON(
                                doc, true, false, false, true
                        ).put("type", "quiz");

                    object.put("backColor", backColor);
                    object.put("month", month);
                    jsonArray.put(object);
                }

                HashMap<String, ArrayList<String>> tmpHash = new HashMap<>();
                tmpHash.put("تگ ها", tags);
                data.put("tags", tmpHash);
                data.put("month", allMonth);
            }
        }

        data.put("items", jsonArray);

        return generateSuccessMsg("data", data);
    }

    public static String getPackagesDigest(ObjectId gradeId, ObjectId lessonId) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (gradeId != null)
            filters.add(eq("grade_id", gradeId));

        if (lessonId != null)
            filters.add(and(
                    exists("lesson_id"),
                    eq("lesson_id", lessonId)
            ));

        ArrayList<Document> packages = packageRepository.find(
                filters.size() == 0 ? null : and(filters), null
        );

        JSONArray jsonArray = new JSONArray();
        JSONObject data = new JSONObject();

        for (Document packageDoc : packages) {

            Document grade = gradeRepository.findById(packageDoc.getObjectId("grade_id"));
            if (grade == null)
                continue;

            Document lesson = null;
            if(packageDoc.containsKey("lesson_id"))
                lesson = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                        grade.getList("lessons", Document.class),
                        "_id", packageDoc.getObjectId("lesson_id")
                );

            JSONObject jsonObject = new JSONObject()
                    .put("id", packageDoc.getObjectId("_id").toString())
                    .put("title", packageDoc.getString("title"))
                    .put("description", packageDoc.getOrDefault("description", ""))
                    .put("buyers", packageDoc.getOrDefault("buyers", 0))
                    .put("grade", new JSONObject()
                            .put("id", grade.getObjectId("_id").toString())
                            .put("name", grade.getString("name"))
                    )
                    .put("type", "package")
                    .put("offPercent", packageDoc.getInteger("off_percent"))
                    .put("priority", packageDoc.getInteger("priority"))
                    .put("minSelect", packageDoc.getInteger("min_select"));

            if(lesson != null) {
                jsonObject.put("lesson", new JSONObject()
                        .put("id", lesson.getObjectId("_id").toString())
                        .put("name", lesson.getString("name"))
                );
            }

            List<ObjectId> quizzes = packageDoc.getList("quizzes", ObjectId.class);
            jsonObject
                    .put("quizzes", quizzes.size());
            jsonArray.put(jsonObject);
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

            if (KindQuiz.REGULAR.getName().equals(quiz.getOrDefault("mode", "regular").toString()))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            jsonArray.put(quizAbstract.convertDocToJSON(quiz, true, isAdmin,
                    false, false
            ));
        }

        return generateSuccessMsg("data", jsonArray);
    }

}
