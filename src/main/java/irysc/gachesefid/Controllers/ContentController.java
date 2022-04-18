package irysc.gachesefid.Controllers;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.gradeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_PARAMS;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

public class ContentController {

//    public static String addCourse(JSONObject jsonObject, ObjectId lessonId) {
//
//        Document lesson = gradeRepository.findById(lessonId);
//        if (lesson == null)
//            return JSON_NOT_VALID_PARAMS;
//
//        List<Document> courses = lesson.getList("courses", Document.class);
//        if (Utility.searchInDocumentsKeyVal(courses, "name", jsonObject.getString("name")) != null)
//            return new JSONObject().put("status", "nok").put("msg", "This course already exist").toString();
//
//        ObjectId newCourseId = ObjectId.get();
//        Document newDoc = new Document("_id", newCourseId);
//
//        for (String key : jsonObject.keySet())
//            newDoc.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), jsonObject.get(key));
//
//        if(!newDoc.containsKey("min_age"))
//            newDoc.append("min_age", -1);
//
//        if(!newDoc.containsKey("max_age"))
//            newDoc.append("max_age", -1);
//
//        if(!newDoc.containsKey("certificate_en"))
//            newDoc.append("certificate_en", true);
//
//        if(!newDoc.containsKey("exam_en"))
//            newDoc.append("exam_en", true);
//
//        courses.add(newDoc);
//
//        new Thread(() ->
//                gradeRepository.updateOne(eq("_id", lessonId), set("courses", courses))
//        ).start();
//
//        return new JSONObject().put("status", "ok").put("id", newCourseId).toString();
//    }
//
//    public static String updateCourse(ObjectId lessonId, ObjectId courseId, JSONObject jsonObject) {
//
//        Document lesson = gradeRepository.findById(lessonId);
//        if (lesson == null)
//            return JSON_NOT_VALID_PARAMS;
//
//        List<Document> courses = lesson.getList("courses", Document.class);
//        int idx = Utility.searchInDocumentsKeyValIdx(courses, "_id", courseId);
//
//        if (idx == -1)
//            return JSON_NOT_VALID_PARAMS;
//
//        boolean nameChange = false;
//        Document course = courses.get(idx);
//
//        if (jsonObject.has("name") &&
//                !course.getString("name").equals(jsonObject.getString("name"))) {
//
//            nameChange = true;
//
//            if (Utility.searchInDocumentsKeyValIdx(courses, "name", jsonObject.getString("name")) != -1)
//                return new JSONObject().put("status", "nok").put("msg", "Course already exists").toString();
//
//            course.put("name", jsonObject.getString("name"));
//        }
//
//        for (String key : jsonObject.keySet())
//            course.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), jsonObject.get(key));
//
//        lesson.put("courses", courses);
//
//        boolean finalNameChange = nameChange;
//
//        new Thread(() -> {
//
//            gradeRepository.updateOne(eq("_id", lessonId), set("courses", courses));
//
//            if (finalNameChange) {
//                termRepository.updateMany(and(
//                        eq("course._id", courseId),
//                        eq("lesson._id", lessonId)
//                ), set("course.name", jsonObject.getString("name")));
//
//                termRepository.updateCacheByCourse(courseId, jsonObject.getString("name"));
//            }
//        }).start();
//
//        return JSON_OK;
//    }
//
//    public static String addTerm(ObjectId lessonId, ObjectId courseId, JSONObject term) {
//
//        if (!term.getString("pre").equals("-1") &&
//                !ObjectIdValidator.isValid(term.getString("pre")))
//            return JSON_NOT_VALID_PARAMS;
//
//        Document lesson = gradeRepository.findById(lessonId);
//        if (lesson == null)
//            return JSON_NOT_VALID_PARAMS;
//
//        List<Document> courses = lesson.getList("courses", Document.class);
//        Document course = Utility.searchInDocumentsKeyVal(courses, "_id", courseId);
//        if (course == null)
//            return JSON_NOT_VALID_PARAMS;
//
//        if (termRepository.exist(and(
//                eq("lesson.name", lesson.getString("name")),
//                eq("course.name", course.getString("name")),
//                eq("name", term.getString("name"))
//                )
//        )
//        )
//            return new JSONObject().put("status", "nok").put("msg", "This term already exist").toString();
//
//        if (!markRepository.exist(eq("mark", term.getString("minMark"))))
//            return JSON_NOT_VALID_PARAMS;
//
//        ObjectId preId = (term.getString("pre").equals("-1")) ? null : new ObjectId(term.getString("pre"));
//
//        if (preId != null) {
//            Document preTerm = termRepository.findById(preId);
//            if (preTerm == null)
//                return JSON_NOT_VALID_PARAMS;
//        }
//
//        return termRepository.insertOneWithReturn(new Document("name", term.getString("name"))
//                .append("lesson", new Document("_id", lesson.getObjectId("_id")).append("name", lesson.getString("name")))
//                .append("course", new Document("_id", course.getObjectId("_id")).append("name", course.getString("name")))
//                .append("book", term.getString("book"))
//                .append("min_mark", term.getString("minMark"))
//                .append("pre", preId)
//        );
//    }
//
//    public static String editTerm(ObjectId termId, JSONObject jsonObject) {
//
//        if (jsonObject.has("pre") &&
//                !jsonObject.getString("pre").equals("-1") &&
//                !ObjectIdValidator.isValid(jsonObject.getString("pre")))
//            return JSON_NOT_VALID_PARAMS;
//
//        if (jsonObject.has("courseId") != jsonObject.has("lessonId"))
//            return JSON_NOT_VALID_PARAMS;
//
//        Document term = termRepository.findById(termId);
//        if (term == null)
//            return JSON_NOT_VALID_PARAMS;
//
//        Document newLesson, newCourse;
//
//        if (jsonObject.has("courseId") &&
//                !jsonObject.getString("courseId").equals(((Document) term.get("course")).getObjectId("_id").toString())) {
//
//            Document lesson = gradeRepository.findById(new ObjectId(jsonObject.getString("lessonId")));
//
//            if (lesson == null)
//                return JSON_NOT_VALID_PARAMS;
//
//            List<Document> courses = lesson.getList("courses", Document.class);
//            Document course = Utility.searchInDocumentsKeyVal(courses, "_id", new ObjectId(jsonObject.getString("courseId")));
//
//            if (course == null)
//                return JSON_NOT_VALID_PARAMS;
//
//            newLesson = new Document("_id", lesson.getObjectId("_id"))
//                    .append("name", lesson.getString("name"));
//
//            newCourse = new Document("_id", course.getObjectId("_id"))
//                    .append("name", course.getString("name"));
//        } else {
//            newLesson = (Document) term.get("lesson");
//            newCourse = (Document) term.get("course");
//        }
//
//        if (jsonObject.has("name") &&
//                !term.getString("name").equals(jsonObject.getString("name"))) {
//
//            if (termRepository.exist(and(
//                    eq("lesson.name", newLesson.getString("name")),
//                    eq("course.name", newCourse.getString("name")),
//                    eq("name", jsonObject.getString("name"))
//                    )
//            )
//            )
//                return new JSONObject().put("status", "nok").put("msg", "This term already exist").toString();
//        }
//
//        if (jsonObject.has("minMark") &&
//                !jsonObject.getString("minMark").equals(term.getString("minMark")) &&
//                !markRepository.exist(eq("mark", jsonObject.getString("minMark"))))
//            return JSON_NOT_VALID_PARAMS;
//
//        ObjectId preId = term.getObjectId("pre");
//
//        if (jsonObject.has("pre")) {
//
//            preId = (jsonObject.getString("pre").equals("-1")) ? null : new ObjectId(jsonObject.getString("pre"));
//
//            if (preId != null) {
//                Document preTerm = termRepository.findById(preId);
//                if (preTerm == null)
//                    return JSON_NOT_VALID_PARAMS;
//            }
//        }
//
//
//        if (jsonObject.has("name"))
//            term.put("name", jsonObject.getString("name"));
//        if (jsonObject.has("minMark"))
//            term.put("minMark", jsonObject.getString("minMark"));
//        if (jsonObject.has("book"))
//            term.put("book", jsonObject.getString("book"));
//
//        term.put("lesson", newLesson);
//        term.put("course", newCourse);
//        term.put("pre", preId);
//
//        new Thread(() -> termRepository.replaceOne(eq("_id", termId), term)).start();
//
//        return JSON_OK;
//    }
//
//    public static String deleteTerm(ObjectId termId) {
//
//        if (classRepository.exist(eq("term_id", termId)))
//            return new JSONObject().put("status", "nok")
//                    .put("msg", "ترم مورد نظر در یک/چند کلاس به کار رفته است که باید ابتدا آن ها را حذف نمایید.")
//                    .toString();
//
//        if (userRepository.exist(eq("passed.term_id", termId)))
//            return new JSONObject().put("status", "nok")
//                    .put("msg", "ترم مورد نظر را نمی توان حذف نمایید.")
//                    .toString();
//
//
//        new Thread(() -> termRepository.deleteOne(eq("_id", termId))).start();
//
//        return JSON_OK;
//    }
//
//    public static String all() {
//
//        JSONArray jsonArray = new JSONArray();
//        ArrayList<Document> docs = gradeRepository.find(null, null);
//
//        for (Document doc : docs) {
//
//            List<Document> courses = doc.getList("courses", Document.class);
//            JSONObject jsonObject = new JSONObject()
//                    .put("name", doc.getString("name"))
//                    .put("id", doc.getObjectId("_id").toString());
//
//            JSONArray coursesJSON = new JSONArray();
//
//            for (Document course : courses) {
//
//                JSONObject courseJSON = new JSONObject()
//                        .put("name", course.getString("name"))
//                        .put("id", course.getObjectId("_id").toString())
//                        .put("price", course.getInteger("price"))
//                        .put("priceForNew", course.getInteger("price_for_new"))
//                        .put("minAge", course.getInteger("min_age"))
//                        .put("maxAge", course.getInteger("max_age"))
//                        .put("certificateEn", course.getBoolean("certificate_en"))
//                        .put("examEn", course.getBoolean("exam_en"))
//                        .put("isEnable", !course.containsKey("is_enable") || course.getBoolean("is_enable"))
//                        .put("description", course.getString("description"));
//
//                if(!courseJSON.getBoolean("isEnable"))
//                    courseJSON.put("disableDescription", course.getString("disable_description"));
//
//                courseJSON.put("certificateDisDesc", course.getOrDefault("certificate_dis_desc", ""));
//                courseJSON.put("examDisDesc", course.getOrDefault("exam_dis_desc", ""));
//
//                JSONObject term = new JSONObject(getTerms(
//                        doc.getObjectId("_id"),
//                        course.getObjectId("_id"))
//                );
//
//                courseJSON.put("terms", term.getJSONArray("terms"));
//                coursesJSON.put(courseJSON);
//            }
//
//            jsonObject.put("courses", coursesJSON);
//            jsonArray.put(jsonObject);
//
//        }
//
//        return new JSONObject().put("status", "ok").put("lessons", jsonArray).toString();
//    }
//
//    public static String getTerms(ObjectId lessonId, ObjectId courseId) {
//
//        ArrayList<Document> terms = termRepository.find(
//                and(
//                        eq("lesson._id", lessonId),
//                        eq("course._id", courseId)
//                ),
//                new BasicDBObject("name", 1).append("_id", 1)
//                        .append("min_mark", 1).append("pre", 1)
//                        .append("book", 1)
//        );
//
//        JSONArray jsonArray = new JSONArray();
//        for (Document term : terms) {
//            if (term.get("pre") == null) {
//                jsonArray.put(new JSONObject().put("id", term.getObjectId("_id").toString())
//                        .put("name", term.getString("name"))
//                        .put("book", term.getString("book"))
//                        .put("minMark", term.getString("min_mark"))
//                        .put("preId", -1)
//                        .put("pre", -1)
//                );
//            } else {
//
//                Document pre = termRepository.findById(term.getObjectId("pre"));
//                if (pre == null)
//                    continue;
//
//                jsonArray.put(new JSONObject().put("id", term.getObjectId("_id").toString())
//                        .put("name", term.getString("name"))
//                        .put("book", term.getString("book"))
//                        .put("minMark", term.getString("min_mark"))
//                        .put("preId", term.getObjectId("pre").toString())
//                        .put("pre", pre.getString("name"))
//                );
//            }
//
//        }
//
//        return new JSONObject().put("status", "ok").put("terms", jsonArray).toString();
//    }
//
//    public static String allTerms() {
//
//        ArrayList<Document> terms = termRepository.find(null, null);
//        JSONArray jsonArray = new JSONArray();
//
//        for (Document term : terms) {
//
//            Document lesson = (Document) term.get("lesson");
//            Document course = (Document) term.get("course");
//
//            if (term.get("pre") == null) {
//
//                jsonArray.put(new JSONObject()
//                        .put("lesson", new JSONObject()
//                                .put("name", lesson.getString("name"))
//                                .put("id", lesson.getObjectId("_id").toString())
//                        )
//                        .put("course", new JSONObject()
//                                .put("name", course.getString("name"))
//                                .put("id", course.getObjectId("_id").toString())
//                                .put("description", course.getString("description"))
//                                .put("price", course.getInteger("price"))
//                        )
//                        .put("minMark", term.getString("min_mark"))
//                        .put("name", term.getString("name"))
//                        .put("book", term.getString("book"))
//                        .put("id", term.getObjectId("_id").toString())
//                        .put("pre", -1)
//                        .put("preId", -1)
//                );
//            } else {
//
//                String preTermName = "";
//
//                for (Document term2 : terms) {
//                    if (term2.getObjectId("_id").equals(term.getObjectId("pre"))) {
//                        preTermName = term2.getString("name");
//                        break;
//                    }
//                }
//
//                jsonArray.put(new JSONObject()
//                        .put("lesson", new JSONObject()
//                                .put("name", lesson.getString("name"))
//                                .put("id", lesson.getObjectId("_id").toString())
//                        )
//                        .put("course", new JSONObject()
//                                .put("name", course.getString("name"))
//                                .put("id", course.getObjectId("_id").toString())
//                                .put("description", course.getString("description"))
//                                .put("price", course.getInteger("price"))
//                        )
//                        .put("minMark", term.getString("min_mark"))
//                        .put("name", term.getString("name"))
//                        .put("book", term.getString("book"))
//                        .put("id", term.getObjectId("_id").toString())
//                        .put("pre", preTermName)
//                        .put("preId", term.getObjectId("pre").toString())
//                );
//
//            }
//        }
//
//        return new JSONObject().put("status", "ok").put("terms", jsonArray).toString();
//    }
//
//    public static JSONArray coursesWithNotInFilter(Set<ObjectId> objectIds, ObjectId userId, int age, boolean justVisibles) {
//
//        JSONArray jsonArray = new JSONArray();
//        ArrayList<Document> docs = gradeRepository.find(null, null);
//
//        for (Document doc : docs) {
//
//            List<Document> courses = doc.getList("courses", Document.class);
//
//            for (Document course : courses) {
//
//                if (objectIds != null &&
//                        objectIds.contains(course.getObjectId("_id")))
//                    continue;
//
//                if (justVisibles &&
//                        course.containsKey("visibility") &&
//                        !course.getBoolean("visibility"))
//                    continue;
//
//                if (course.getInteger("min_age") != -1 &&
//                        course.getInteger("min_age") > age
//                )
//                    continue;
//
//                if (course.getInteger("max_age") != -1 &&
//                        course.getInteger("max_age") < age
//                )
//                    continue;
//
//                JSONObject requestHistory = new JSONObject();
//
//                requestHistory.put("exam", interviewRepository.exist(and(
//                        eq("user_id", userId),
//                        eq("course_id", course.getObjectId("_id"))
//                )));
//                requestHistory.put("certificate", certificationRepository.exist(and(
//                        eq("user_id", userId),
//                        ne("status", "init"),
//                        eq("course_id", course.getObjectId("_id"))
//                )));
//
//                JSONObject jsonObject = new JSONObject()
//                        .put("name", course.getString("name"))
//                        .put("id", course.getObjectId("_id").toString())
//                        .put("requestHistory", requestHistory)
//                        .put("isEnable", !course.containsKey("is_enable") || course.getBoolean("is_enable"))
//                        .put("description", course.getString("description"))
//                        .put("certificateEn", course.getBoolean("certificate_en"))
//                        .put("examEn", course.getBoolean("exam_en"))
//                        .put("price", course.getInteger("price"))
//                        .put("lesson", new JSONObject()
//                                .put("name", doc.getString("name"))
//                                .put("id", doc.getObjectId("_id").toString())
//                        );
//
//                if(!jsonObject.getBoolean("isEnable"))
//                    jsonObject.put("disableDescription", course.getString("disable_description"));
//
//                if(!jsonObject.getBoolean("certificateEn"))
//                    jsonObject.put("certificateDisDesc", course.getOrDefault("certificate_dis_desc", ""));
//
//                if(!jsonObject.getBoolean("examEn"))
//                    jsonObject.put("examDisDesc", course.getOrDefault("exam_dis_desc", ""));
//
//                jsonArray.put(jsonObject);
//            }
//        }
//
//        return jsonArray;
//    }
//
//    public static String getLessons() {
//
//        JSONArray jsonArray = new JSONArray();
//
//        ArrayList<Document> docs = gradeRepository.find(null, new BasicDBObject("name", 1).append("_id", 1));
//        for (Document doc : docs) {
//            jsonArray.put(new JSONObject().put("id", doc.getObjectId("_id").toString())
//                    .put("name", doc.getString("name")));
//        }
//
//        return new JSONObject().put("status", "ok").put("lessons", jsonArray).toString();
//    }

    public static String addGrade(String name) {

        if (gradeRepository.find(eq("name", name), new BasicDBObject("_id", 1)).size() > 0)
            return Utility.generateErr("مقطعی با این نام در سیستم موجود است.");

        new Thread(() ->
                gradeRepository.insertOne(new Document("name", name).append("lessons", new ArrayList<>()))
        ).start();

        return JSON_OK;
    }

    public static String updateGrade(ObjectId gradeId, String name) {

        Document grade = gradeRepository.findById(gradeId);
        if (grade == null || grade.getString("name").equals(name))
            return JSON_NOT_VALID_PARAMS;

        if (gradeRepository.exist(eq("name", name)))
            return Utility.generateErr("مقطعی با این نام در سیستم موجود است.");

        grade.put("name", name);

        new Thread(() -> {

            gradeRepository.updateOne(gradeId, set("name", name));

            subjectRepository.updateMany(
                    eq("grade._id", gradeId),
                    set("grade.name", name)
            );

//            subjectRepository.updateCacheByGrade(gradeId, name);

        }).start();

        return JSON_OK;
    }

//    public static String delete(ObjectId lessonId) {
//
//        if (termRepository.exist(eq("lesson._id", lessonId)))
//            return new JSONObject().put("status", "nok")
//                    .put("msg", "رشته مورد نظر در یک/چند مقطع به کار رفته است که باید ابتدا آن ها را حذف نمایید.")
//                    .toString();
//
//        new Thread(() -> gradeRepository.deleteOne(eq("_id", lessonId))).start();
//        return JSON_OK;
//    }
//
//    public static String deleteCourse(ObjectId lessonId, ObjectId courseId) {
//
//        if (termRepository.exist(eq("course._id", courseId)))
//            return new JSONObject().put("status", "nok")
//                    .put("msg", "رشته مورد نظر در یک/چند مقطع به کار رفته است که باید ابتدا آن ها را حذف نمایید.")
//                    .toString();
//
//        if (userRepository.exist(eq("passed.course_id", courseId)))
//            return new JSONObject().put("status", "nok")
//                    .put("msg", "رشته مورد نظر را نمی توان حذف نمایید.")
//                    .toString();
//
//        Document lesson = gradeRepository.findById(lessonId);
//        if (lesson == null)
//            return JSON_NOT_VALID_ID;
//
//        List<Document> courses = lesson.getList("courses", Document.class);
//        int idx = Utility.searchInDocumentsKeyValIdx(courses, "_id", courseId);
//
//        if (idx == -1)
//            return JSON_NOT_VALID_ID;
//
//        courses.remove(idx);
//        new Thread(() -> gradeRepository.updateOne(
//                eq("_id", lessonId),
//                set("courses", courses)
//        )).start();
//
//        return JSON_OK;
//    }
//
//    public static String toggleVisibilityCourse(ObjectId lessonId, ObjectId courseId, JSONObject jsonObject) {
//
//        Document lesson = gradeRepository.findById(lessonId);
//        if (lesson == null)
//            return JSON_NOT_VALID_ID;
//
//        List<Document> courses = lesson.getList("courses", Document.class);
//        int idx = Utility.searchInDocumentsKeyValIdx(courses, "_id", courseId);
//
//        if (idx == -1)
//            return JSON_NOT_VALID_ID;
//
//        if (!courses.get(idx).containsKey("is_enable") ||
//                courses.get(idx).getBoolean("is_enable")
//        ) {
//            if (!jsonObject.has("description") ||
//                    jsonObject.getString("description").isEmpty())
//                return JSON_NOT_VALID_PARAMS;
//
//            courses.get(idx).put("is_enable", false);
//            courses.get(idx).put("disable_description", jsonObject.getString("description"));
//        } else {
//            courses.get(idx).put("is_enable", true);
//            courses.get(idx).remove("disable_description");
//        }
//
//        new Thread(() -> gradeRepository.replaceOne(eq("_id", lessonId), lesson)).start();
//
//        return JSON_OK;
//    }
//
//    public static String allCourses() {
//        return gradeRepository.allCourses();
//    }
//
//    public static String getCourses(ObjectId lessonId) {
//        return gradeRepository.getCourses(lessonId);
//    }
//
//    public static String getCourse(ObjectId lessonId, ObjectId courseId) {
//        return gradeRepository.getCourse(lessonId, courseId);
//    }
}
