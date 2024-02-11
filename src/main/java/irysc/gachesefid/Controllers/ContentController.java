package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOptions;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class ContentController {

    public static String addBatch(MultipartFile file) {

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            return generateErr("File is not valid");

        HashMap<String, Document> grades = new HashMap<>();
        HashMap<String, Boolean> isNew = new HashMap<>();

        ArrayList<Document> docs = gradeRepository.find(null, null);

        long curr = System.currentTimeMillis();

        for (Document doc : docs) {
            grades.put(doc.getString("name"), doc);
            isNew.put(doc.getString("name"), false);
        }

        rows.remove(0);
        boolean needUpdate = false;

        for (Row row : rows) {

            try {

                if(row.getCell(1) == null)
                    break;

                if (row.getLastCellNum() < 4)
                    continue;

                String grade = row.getCell(1).getStringCellValue();
                ObjectId gradeId;
                List<Document> lessons;

                if (!grades.containsKey(grade)) {
                    lessons = new ArrayList<>();
                    ObjectId newId = new ObjectId();

                    Document newDoc = new Document("name", grade)
                            .append("_id", newId)
                            .append("lessons", lessons)
                            .append("created_at", curr);

                    isNew.put(grade, true);
                    grades.put(grade, newDoc);
                    gradeId = newId;
                    needUpdate = true;
                } else {
                    gradeId = grades.get(grade).getObjectId("_id");
                    lessons = grades.get(grade).getList("lessons", Document.class);
                }

                String lesson = row.getCell(2).getStringCellValue();
                Document lessonDoc = searchInDocumentsKeyVal(lessons, "name", lesson);

                if (lessonDoc == null) {

                    ObjectId newLessonId = new ObjectId();
                    lessonDoc = new Document("name", lesson)
                            .append("_id", newLessonId)
                            .append("description",
                                    row.getCell(4) != null &&
                                            row.getCell(4).getStringCellValue() != null &&
                                            !row.getCell(4).getStringCellValue().isEmpty() ?
                                            row.getCell(4).getStringCellValue() : ""
                            );

                    lessons.add(lessonDoc);
                    needUpdate = true;
                }

                String subject = row.getCell(3).getStringCellValue();
                String code = getRandIntForSubjectId();

                Document newSubject = new Document("name", subject)
                        .append("created_at", curr)
                        .append("lesson", new Document("_id", lessonDoc.getObjectId("_id"))
                                .append("name", lesson)
                        )
                        .append("grade", new Document("_id", gradeId)
                                .append("name", grade)
                        )
                        .append("code", code)
                        .append("description", row.getCell(5) != null &&
                                row.getCell(5).getStringCellValue() != null &&
                                !row.getCell(5).getStringCellValue().isEmpty() ?
                                row.getCell(5).getStringCellValue() : "")
                        .append("easy_price", row.getCell(6) != null ?
                                (int) (row.getCell(6).getNumericCellValue()) : 0)
                        .append("mid_price", row.getCell(7) != null ?
                                (int) (row.getCell(7).getNumericCellValue()) : 0)
                        .append("hard_price", row.getCell(8) != null ?
                                (int) (row.getCell(8).getNumericCellValue()) : 0)
                        .append("school_easy_price", row.getCell(9) != null ?
                                (int) (row.getCell(9).getNumericCellValue()) : 0)
                        .append("school_mid_price", row.getCell(10) != null ?
                                (int) (row.getCell(10).getNumericCellValue()) : 0)
                        .append("school_hard_price", row.getCell(11) != null ?
                                (int) (row.getCell(11).getNumericCellValue()) : 0);

                subjectRepository.updateOne(
                        and(
                                eq("lesson._id", lessonDoc.getObjectId("_id")),
                                eq("grade._id", gradeId),
                                eq("name", subject)
                        ),
                        new BasicDBObject("$set", newSubject),
                        new UpdateOptions().upsert(true)
                );

            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        if (!needUpdate)
            return JSON_OK;

        for (String key : isNew.keySet()) {
            if (isNew.get(key))
                gradeRepository.insertOne(grades.get(key));
            else
                gradeRepository.replaceOne(grades.get(key).getObjectId("_id"), grades.get(key));
        }

        return JSON_OK;
    }

    public static String addBranch(String name) {

        if (branchRepository.exist(eq("name", name)))
            return generateErr("گروه آموزشی ای با این نام در سیستم موجود است.");

        return branchRepository.insertOneWithReturn(
                new Document("name", name)
                        .append("created_at", System.currentTimeMillis())
        );
    }

    public static String updateBranch(ObjectId branchId,
                                      String name,
                                      boolean isOlympiad
    ) {

        Document branch = branchRepository.findById(branchId);
        if (branch == null)
            return JSON_NOT_VALID_PARAMS;

        if (
                (isOlympiad && branchRepository.exist(and(
                            eq("name", name),
                            ne("_id", branchId)
                        )
                )) ||
                        (!isOlympiad && gradeRepository.exist(and(
                                eq("name", name),
                                ne("_id", branchId)
                                )
                        ))
        ) {
            if(isOlympiad)
                return generateErr("رشته ای با این نام در سیستم موجود است.");

            return generateErr("مقطعی با این نام در سیستم موجود است.");
        }

        if(isOlympiad) {
            if(branch.getString("name").equals(name))
                return JSON_OK;

            branch.put("name", name);
            branchRepository.updateOne(branchId, set("name", name));
        }
        else {
            if(userRepository.exist(
                    eq("branches._id", branchId)
            ))
                return generateErr("دانش آموزانی از این رشته استفاده می کنند و عملیات موردنظر مجاز نمی باشد.");

            branchRepository.deleteOne(branchId);
            gradeRepository.insertOne(
                    new Document("name", name)
                            .append("lessons", new ArrayList<>())
            );
        }

        return JSON_OK;
    }

    public static String addGrade(String name) {

        if (gradeRepository.exist(eq("name", name)))
            return generateErr("مقطعی با این نام در سیستم موجود است.");

        return gradeRepository.insertOneWithReturn(new Document("name", name)
                .append("lessons", new ArrayList<>())
                .append("created_at", System.currentTimeMillis())
        );
    }

    public static String updateGrade(ObjectId gradeId, String name, boolean isOlympiad) {

        Document grade = gradeRepository.findById(gradeId);
        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        if (
                (isOlympiad && branchRepository.exist(and(
                        eq("name", name),
                        ne("_id", gradeId)
                        )
                )) ||
                        (!isOlympiad && gradeRepository.exist(and(
                                eq("name", name),
                                ne("_id", gradeId)
                                )
                        ))
        ) {
            if(isOlympiad)
                return generateErr("رشته ای با این نام در سیستم موجود است.");

            return generateErr("مقطعی با این نام در سیستم موجود است.");
        }

        if(isOlympiad) {

            if(userRepository.exist(
                    eq("grade._id", gradeId)
            ))
                return generateErr("دانش آموزانی از این مقطع استفاده می کنند و عملیات موردنظر مجاز نمی باشد.");

            if(subjectRepository.exist(
                    eq("grade._id", gradeId)
            ))
                return generateErr("مباحثی از این مقطع استفاده می کنند و عملیات موردنظر مجاز نمی باشد.");

            gradeRepository.deleteOne(gradeId);
            branchRepository.insertOne(
                    new Document("name", name)
            );
        }
        else {

            if(grade.getString("name").equals(name))
                return JSON_OK;

            grade.put("name", name);
            gradeRepository.updateOne(gradeId, set("name", name));

            subjectRepository.updateMany(
                    eq("grade._id", gradeId),
                    set("grade.name", name)
            );

            subjectRepository.clearFormCacheByGradeId(gradeId);
        }

        return JSON_OK;
    }

    public static String deleteGrade(JSONArray ids) {

        JSONArray excepts = new JSONArray();
        JSONArray removeIds = new JSONArray();

        for (int i = 0; i < ids.length(); i++) {

            String id = ids.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId gradeId = new ObjectId(id);

            if (gradeRepository.findById(gradeId) != null) {

                if (subjectRepository.exist(eq("grade._id", gradeId))) {
                    excepts.put(i + 1);
                    continue;
                }

                gradeRepository.deleteOne(gradeId);
                gradeRepository.clearFromCache(gradeId);
                removeIds.put(gradeId);
            } else if (branchRepository.findById(gradeId) != null) {

                if (userRepository.exist(eq("branches._id", gradeId))) {
                    excepts.put(i + 1);
                    continue;
                }

                branchRepository.deleteOne(gradeId);
                branchRepository.clearFromCache(gradeId);

                removeIds.put(gradeId);
            } else
                excepts.put(i + 1);
        }

        return Utility.returnRemoveResponse(excepts, removeIds);
    }

    public static String addLesson(Common db, JSONObject jsonObject, ObjectId gradeId) {

        Document grade = db.findById(gradeId);
        if (grade == null)
            return JSON_NOT_VALID_PARAMS;

        List<Document> lessons = grade.containsKey("lessons") ?
                grade.getList("lessons", Document.class) : new ArrayList<>();

        if (Utility.searchInDocumentsKeyVal(lessons,
                "name", jsonObject.getString("name")) != null
        )
            return generateErr("درس موردنظر در مقطع موردنظر موجود است.");

        ObjectId newLessonId = ObjectId.get();

        lessons.add(new Document("_id", newLessonId)
                .append("name", jsonObject.getString("name"))
                .append("description", jsonObject.has("description") ?
                        jsonObject.getString("description") : ""
                )
        );

        if(lessons.size() == 1)
            grade.put("lessons", lessons);

        db.updateOne(gradeId, set("lessons", lessons));

        return generateSuccessMsg("id", newLessonId);
    }

    public static String updateLesson(Common db, ObjectId gradeId, ObjectId lessonId, JSONObject jsonObject) {

        Document grade = db.findById(gradeId);
        if (grade == null)
            return JSON_NOT_VALID_ID;

        List<Document> lessons = grade.getList("lessons", Document.class);
        Document lesson = Utility.searchInDocumentsKeyVal(lessons, "_id", lessonId);

        if (lesson == null)
            return JSON_NOT_VALID_ID;

        boolean nameChange = false;
        Document newGrade = null;

        if (jsonObject.has("gradeId") &&
                !jsonObject.getString("gradeId").equals(gradeId.toString())
        ) {

            newGrade = db.findById(new ObjectId(jsonObject.getString("gradeId")));
            if (newGrade == null)
                return JSON_NOT_VALID_PARAMS;
        }

        List<Document> lessonsInNewGrade = newGrade != null ?
                newGrade.getList("lessons", Document.class) : null;

        if (jsonObject.has("name") &&
                !lesson.getString("name").equals(
                        jsonObject.getString("name"))
        ) {

            nameChange = true;

            if (newGrade == null &&
                    Utility.searchInDocumentsKeyValIdx(lessons, "name", jsonObject.getString("name")) != -1
            )
                return generateErr("درس موردنظر در مقطع موردنظر موجود است.");
            else if (lessonsInNewGrade != null &&
                    Utility.searchInDocumentsKeyValIdx(lessonsInNewGrade, "name", jsonObject.getString("name")) != -1
            )
                return generateErr("درس موردنظر در مقطع موردنظر موجود است.");

            lesson.put("name", jsonObject.getString("name"));
        }

        if (jsonObject.has("description"))
            lesson.put("description", jsonObject.getString("description"));

        if (lessonsInNewGrade != null) {
            lessonsInNewGrade.add(Document.parse(lesson.toJson()));
            lessons.remove(lesson);
            newGrade.put("lessons", lessonsInNewGrade);
        }

        grade.put("lessons", lessons);

        boolean needClearCache = nameChange || newGrade != null;


        db.updateOne(gradeId, set("lessons", lessons));
        if (newGrade != null)
            db.updateOne(newGrade.getObjectId("_id"), set("lessons", lessonsInNewGrade));

        if (needClearCache) {

            if (newGrade == null)
                subjectRepository.updateMany(and(
                        eq("grade._id", gradeId),
                        eq("lesson._id", lessonId)
                ), set("lesson.name", jsonObject.getString("name")));
            else
                subjectRepository.updateMany(and(
                        eq("grade._id", gradeId),
                        eq("lesson._id", lessonId)
                        ), new BasicDBObject("$set",
                                new BasicDBObject("lesson.name", jsonObject.getString("name"))
                                        .append("grade._id", newGrade.getObjectId("_id"))
                                        .append("grade.name", newGrade.getString("name"))
                        )
                );

            subjectRepository.clearFormCacheByLessonId(lessonId);
        }

        return JSON_OK;
    }

    public static String deleteLessons(Common db, JSONArray ids) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < ids.length(); i++) {

            String id = ids.getString(i);
            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId lessonId = new ObjectId(id);
            if (subjectRepository.exist(eq("lesson._id", lessonId))) {
                excepts.put(i + 1);
                continue;
            }
//                return generateErr("درس مورد نظر در یک/چند مبحث به کار رفته است که باید ابتدا آن ها را حذف نمایید.");

            Document grade = db.findOne(eq("lessons._id", lessonId), null);
            if (grade == null) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId gradeId = grade.getObjectId("_id");
            grade = db.findById(gradeId);

            List<Document> lessons = grade.getList("lessons", Document.class);
            int idx = Utility.searchInDocumentsKeyValIdx(lessons, "_id", lessonId);

            if (idx == -1) {
                excepts.put(i + 1);
                continue;
            }

            lessons.remove(idx);
            db.updateOne(
                    gradeId,
                    set("lessons", lessons)
            );
            doneIds.put(id);
        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public static String addSubject(ObjectId gradeId, ObjectId lessonId, JSONObject subject) {

        Document grade = gradeRepository.findById(gradeId);
        if (grade == null)
            return JSON_NOT_VALID_ID;

        List<Document> lessons = grade.getList("lessons", Document.class);
        Document lesson = Utility.searchInDocumentsKeyVal(lessons, "_id", lessonId);
        if (lesson == null)
            return JSON_NOT_VALID_PARAMS;

        if (subjectRepository.exist(
                and(
                        eq("grade._id", gradeId),
                        eq("lesson._id", lessonId),
                        eq("name", subject.getString("name"))
                )
        ))
            return generateErr("مبحث موردنظر در درس موردنظر موجود است.");

        String code = getRandIntForSubjectId();

        ObjectId oId = subjectRepository.insertOneWithReturnId(
                new Document("name", subject.getString("name"))
                        .append("code", code)
                        .append("grade", new Document("_id", gradeId).append("name", grade.getString("name")))
                        .append("lesson", new Document("_id", lessonId).append("name", lesson.getString("name")))
                        .append("easy_price", subject.getInt("easyPrice"))
                        .append("mid_price", subject.getInt("midPrice"))
                        .append("hard_price", subject.getInt("hardPrice"))
                        .append("school_easy_price", subject.getInt("schoolEasyPrice"))
                        .append("school_mid_price", subject.getInt("schoolMidPrice"))
                        .append("school_hard_price", subject.getInt("schoolHardPrice"))
                        .append("description", subject.has("description") ? subject.getString("description") : "")
                        .append("created_at", System.currentTimeMillis())
        );

        return generateSuccessMsg("id", oId.toString(), new PairValue("code", code));
    }

    public static String editSubject(ObjectId subjectId, JSONObject jsonObject) {

        if (jsonObject.has("lessonId") != jsonObject.has("gradeId"))
            return JSON_NOT_VALID_PARAMS;

        Document subject = subjectRepository.findById(subjectId);
        if (subject == null)
            return JSON_NOT_VALID_ID;

        Document newLesson, newGrade;

        if (jsonObject.has("lessonId") &&
                !jsonObject.getString("lessonId").equals(
                        ((Document) subject.get("lesson")).getObjectId("_id").toString()
                )
        ) {

            Document grade = gradeRepository.findById(new ObjectId(jsonObject.getString("gradeId")));

            if (grade == null)
                return JSON_NOT_VALID_PARAMS;

            Document lesson = Utility.searchInDocumentsKeyVal(
                    grade.getList("lessons", Document.class),
                    "_id", new ObjectId(jsonObject.getString("lessonId")));

            if (lesson == null)
                return JSON_NOT_VALID_PARAMS;

            newGrade = new Document("_id", grade.getObjectId("_id"))
                    .append("name", grade.getString("name"));

            newLesson = new Document("_id", lesson.getObjectId("_id"))
                    .append("name", lesson.getString("name"));

        } else {
            newGrade = (Document) subject.get("grade");
            newLesson = (Document) subject.get("lesson");
        }

        if (jsonObject.has("name") &&
                !subject.getString("name").equals(jsonObject.getString("name"))) {

            if (subjectRepository.exist(and(
                    eq("lesson._id", newLesson.getObjectId("_id")),
                    eq("grade._id", newGrade.getObjectId("_id")),
                    eq("name", jsonObject.getString("name"))
                    )
            )
            )
                return generateErr("مبحث موردنظر در درس موردنظر وجود دارد.");
        }

        if (jsonObject.has("name"))
            subject.put("name", jsonObject.getString("name"));

        if (jsonObject.has("description"))
            subject.put("description", jsonObject.getString("description"));

        if (jsonObject.has("easyPrice"))
            subject.put("easy_price", jsonObject.getInt("easyPrice"));

        if (jsonObject.has("midPrice"))
            subject.put("mid_price", jsonObject.getInt("midPrice"));

        if (jsonObject.has("hardPrice"))
            subject.put("hard_price", jsonObject.getInt("hardPrice"));

        if (jsonObject.has("schoolEasyPrice"))
            subject.put("school_easy_price", jsonObject.getInt("schoolEasyPrice"));

        if (jsonObject.has("schoolMidPrice"))
            subject.put("school_mid_price", jsonObject.getInt("schoolMidPrice"));

        if (jsonObject.has("schoolHardPrice"))
            subject.put("school_hard_price", jsonObject.getInt("schoolHardPrice"));

        subject.put("lesson", newLesson);
        subject.put("grade", newGrade);

        subjectRepository.replaceOne(subjectId, subject);
        return JSON_OK;
    }

    public static String deleteSubjects(JSONArray ids) {

        JSONArray excepts = new JSONArray();
        JSONArray doneIds = new JSONArray();

        for (int i = 0; i < ids.length(); i++) {

            String id = ids.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId subjectId = new ObjectId(id);

            if (questionRepository.exist(eq("subject_id", subjectId))) {
                excepts.put(i + 1);
                continue;
            }
//                return generateErr("مبحث مورد نظر در یک/چند سوال به کار رفته است که باید ابتدا آن ها را حذف نمایید.");

            subjectRepository.deleteOne(subjectId);
            subjectRepository.clearFromCache(subjectId);
            doneIds.put(id);
        }

        return returnRemoveResponse(excepts, doneIds);
    }

    public static String all(ObjectId lessonId, ObjectId gradeId,
                             String subject, String code
    ) {

        ArrayList<Bson> filters = new ArrayList<>();
        ArrayList<Document> docs;

        if(subject != null || code != null) {

            if(subject != null)
                filters.add(eq("name", Pattern.compile(Pattern.quote(subject), Pattern.CASE_INSENSITIVE)));

            if(code != null)
                filters.add(eq("code", Utility.convertPersianDigits(code)));

            JSONArray jsonArray = new JSONArray();

            getSubjects(
                    jsonArray, null, null, filters
            );

            return generateSuccessMsg("data", jsonArray);
        }

        if (lessonId != null)
            filters.add(eq("lesson._id", lessonId));

        if (gradeId != null)
            filters.add(eq("grade._id", gradeId));

        docs = gradeRepository.find(
                filters.size() == 0 ? null : and(filters), null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            List<Document> lessons = doc.getList("lessons", Document.class);

            for (Document lesson : lessons) {
                getSubjects(
                        jsonArray,
                        doc.getObjectId("_id"),
                        lesson.getObjectId("_id"),
                        null
                );
            }
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getLessons(Common db) {

        ArrayList<Document> docs = db.find(null, null);
        JSONArray lessonsJSON = new JSONArray();

        for (Document doc : docs) {

            JSONObject gradeJSON = new JSONObject()
                    .put("name", doc.getString("name"))
                    .put("id", doc.getObjectId("_id").toString());

            if(doc.containsKey("lessons")) {

                for (Document lesson : doc.getList("lessons", Document.class)) {

                    JSONObject lessonJSON = new JSONObject()
                            .put("grade", gradeJSON)
                            .put("name", lesson.getString("name"))
                            .put("id", lesson.getObjectId("_id").toString())
                            .put("description", lesson.getString("description"));

                    lessonsJSON.put(lessonJSON);
                }
            }
        }

        return generateSuccessMsg("data", lessonsJSON);
    }


    public static String getLessonsDigest(Common db, ObjectId parentId) {

        ArrayList<Document> docs = db.find(parentId == null ? null : eq("_id", parentId), null);
        JSONArray lessonsJSON = new JSONArray();

        for (Document doc : docs) {

            List<Document> lessons = doc.getList("lessons", Document.class);

            for (Document lesson : lessons) {
                lessonsJSON.put(new JSONObject()
                        .put("name", parentId == null ?
                                lesson.getString("name") + " در " + doc.getString("name") :
                                lesson.getString("name")
                        )
                        .put("id", lesson.getObjectId("_id").toString())
                );
            }

        }

        return generateSuccessMsg("data", lessonsJSON);
    }

    public static String gradeLessonsInGradesAndBranches() {

        ArrayList<Document> docs = gradeRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            List<Document> lessons = doc.getList("lessons", Document.class);

            JSONArray lessonsJSON = new JSONArray();
            JSONObject gradeJSON = new JSONObject()
                    .put("name", doc.getString("name"))
                    .put("id", doc.getObjectId("_id").toString());

            for (Document lesson : lessons) {
                lessonsJSON.put(new JSONObject()
                        .put("name", lesson.getString("name"))
                        .put("id", lesson.getObjectId("_id").toString())
                );
            }

            gradeJSON.put("lessons", lessonsJSON);
            jsonArray.put(gradeJSON);
        }

        docs = branchRepository.find(null, null);

        for (Document doc : docs) {

            if(!doc.containsKey("lessons"))
                continue;

            List<Document> lessons = doc.getList("lessons", Document.class);

            JSONArray lessonsJSON = new JSONArray();
            JSONObject gradeJSON = new JSONObject()
                    .put("name", doc.getString("name"))
                    .put("id", doc.getObjectId("_id").toString());

            for (Document lesson : lessons) {
                lessonsJSON.put(new JSONObject()
                        .put("name", lesson.getString("name"))
                        .put("id", lesson.getObjectId("_id").toString())
                );
            }

            gradeJSON.put("lessons", lessonsJSON);
            jsonArray.put(gradeJSON);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String gradeLessons() {

        ArrayList<Document> docs = gradeRepository.find(null, null);
        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            List<Document> lessons = doc.getList("lessons", Document.class);

            JSONArray lessonsJSON = new JSONArray();
            JSONObject gradeJSON = new JSONObject()
                    .put("name", doc.getString("name"))
                    .put("id", doc.getObjectId("_id").toString());

            for (Document lesson : lessons) {
                lessonsJSON.put(new JSONObject()
                        .put("name", lesson.getString("name"))
                        .put("id", lesson.getObjectId("_id").toString())
                );
            }

            gradeJSON.put("lessons", lessonsJSON);
            jsonArray.put(gradeJSON);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    private static void getSubjects(JSONArray jsonArray,
                                    ObjectId gradeId,
                                    ObjectId lessonId,
                                    List<Bson> filters
    ) {

        ArrayList<Document> subjects = subjectRepository.find(
                filters == null ?
                and(
                        eq("lesson._id", lessonId),
                        eq("grade._id", gradeId)
                ) : and(filters), null
        );

        for (Document subject : subjects) {

            Document grade = ((Document) subject.get("grade"));
            Document lesson = ((Document) subject.get("lesson"));

            jsonArray.put(new JSONObject().put("id", subject.getObjectId("_id").toString())
                    .put("name", subject.getString("name"))
                    .put("midPrice", subject.getInteger("mid_price"))
                    .put("easyPrice", subject.getInteger("easy_price"))
                    .put("hardPrice", subject.getInteger("hard_price"))
                    .put("schoolMidPrice", subject.getInteger("school_mid_price"))
                    .put("schoolEasyPrice", subject.getInteger("school_easy_price"))
                    .put("schoolHardPrice", subject.getInteger("school_hard_price"))
                    .put("description", subject.getString("description"))
                    .put("code", subject.getString("code"))
                    .put("grade", new JSONObject()
                            .put("name", grade.getString("name"))
                            .put("id", grade.getObjectId("_id").toString())
                    )
                    .put("lesson", new JSONObject()
                            .put("name", lesson.getString("name"))
                            .put("id", lesson.getObjectId("_id").toString())
                    )
            );
        }
    }

    public static String allSubjects(ObjectId gradeId, ObjectId lessonId) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (gradeId != null)
            filters.add(eq("grade._id", gradeId));

        if (lessonId != null)
            filters.add(eq("lesson._id", lessonId));

        ArrayList<Document> subjects = subjectRepository.find(
                filters.size() == 0 ? null : and(filters), null);
        JSONArray jsonArray = new JSONArray();

        for (Document subject : subjects) {

            Document grade = (Document) subject.get("grade");
            Document lesson = (Document) subject.get("lesson");

            jsonArray.put(new JSONObject()
                    .put("grade", new JSONObject()
                            .put("name", grade.getString("name"))
                            .put("id", grade.getObjectId("_id").toString())
                    )
                    .put("lesson", new JSONObject()
                            .put("name", lesson.getString("name"))
                            .put("id", lesson.getObjectId("_id").toString())
                            .put("description", lesson.getString("description"))
                    )
                    .put("id", subject.getObjectId("_id").toString())
                    .put("midPrice", subject.getInteger("mid_price"))
                    .put("easyPrice", subject.getInteger("easy_price"))
                    .put("hardPrice", subject.getInteger("hard_price"))
                    .put("schoolMidPrice", subject.getInteger("school_mid_price"))
                    .put("schoolEasyPrice", subject.getInteger("school_easy_price"))
                    .put("schoolHardPrice", subject.getInteger("school_hard_price"))
                    .put("description", subject.getString("description"))
                    .put("name", subject.getString("name"))
            );
        }

        return generateSuccessMsg("subjects", jsonArray);
    }
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

    public static String getGradesAndBranches() {

        JSONArray jsonArray = new JSONArray();

        ArrayList<Document> docs = gradeRepository.find(null, new BasicDBObject("_id", 1).append("name", 1));
        for (Document doc : docs) {
            jsonArray.put(
                    new JSONObject()
                            .put("id", doc.getObjectId("_id").toString())
                            .put("name", doc.getString("name"))
                            .put("isOlympiad", false)
            );
        }

        docs = branchRepository.find(null, new BasicDBObject("_id", 1).append("name", 1));
        for (Document doc : docs) {
            jsonArray.put(
                    new JSONObject()
                            .put("id", doc.getObjectId("_id").toString())
                            .put("name", doc.getString("name"))
                            .put("isOlympiad", true)
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getGradesOrBranches(Common db) {

        JSONArray jsonArray = new JSONArray();

        ArrayList<Document> docs = db.find(null, new BasicDBObject("_id", 1).append("name", 1));
        for (Document doc : docs) {
            jsonArray.put(
                    new JSONObject()
                            .put("id", doc.getObjectId("_id").toString())
                            .put("name", doc.getString("name"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String search(String key) {

        ArrayList<Bson> constraints = new ArrayList<>();

        constraints.add(regex("name", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE)));
        constraints.add(regex("lessons.name", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE)));

        ArrayList<Document> docs = gradeRepository.find(or(constraints), null);
        JSONArray output = new JSONArray();

        for (Document doc : docs) {

            if (doc.getString("name").contains(key))
                output.put(new JSONObject()
                        .put("section", "grade")
                        .put("name", doc.getString("name"))
                        .put("id", doc.getObjectId("_id").toString())
                );

            List<Document> lessons = doc.getList("lessons", Document.class);

            for (Document lesson : lessons) {

                if (lesson.getString("name").contains(key))
                    output.put(new JSONObject()
                            .put("section", "lesson")
                            .put("name", lesson.getString("name"))
                            .put("id", lesson.getObjectId("_id").toString())
                    );

            }
        }

        ArrayList<Document> subjects = subjectRepository.find(
                regex("name", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE)),
                null
        );

        for (Document subject : subjects) {
            output.put(new JSONObject()
                    .put("id", subject.getObjectId("_id").toString())
                    .put("name", subject.getString("name"))
                    .put("section", "subject")
            );
        }

        return generateSuccessMsg("data", output);
    }

    public static String refreshSubjectQNo() {

        ArrayList<Document> subjects = subjectRepository.find(null, null);

        for (Document subject : subjects) {

            subject.put("q_no", questionRepository.count(
                    eq("subject_id", subject.getObjectId("_id"))
            ));

            subjectRepository.replaceOne(subject.getObjectId("_id"), subject);
            subjectRepository.clearFromCache(subject);
        }

        return JSON_OK;
    }

    public static String getSubjectsKeyVals(ObjectId lessonId) {

        ArrayList<Document> subjects = subjectRepository.find(
                lessonId == null ? null : eq("lesson._id", lessonId), null
        );

        JSONArray jsonArray = new JSONArray();

        for (Document subject : subjects) {
            if(lessonId == null) {
                Document lesson = subject.get("lesson", Document.class);
                Document grade = subject.get("grade", Document.class);

                jsonArray.put(
                        new JSONObject()
                                .put("name", subject.getString("name") + " در " + lesson.getString("name") + " در " + grade.getString("name"))
                                .put("id", subject.getObjectId("_id").toString())
                );
            }
            else {
                jsonArray.put(
                        new JSONObject()
                                .put("name", subject.getString("name"))
                                .put("id", subject.getObjectId("_id").toString())
                );
            }

        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static ByteArrayInputStream getSubjectCodesExcel() {

        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> docs = subjectRepository.find(null, null);

        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("grade", doc.get("grade", Document.class).getString("name"))
                    .put("lesson", doc.get("lesson", Document.class).getString("name"))
                    .put("title", doc.getString("name"))
                    .put("code", doc.get("code"))
            );
        }

        return Excel.write(jsonArray);
    }

//
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
