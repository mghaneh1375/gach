package irysc.gachesefid.Test.Content;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Test.TestController;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.gt;
import static irysc.gachesefid.Main.GachesefidApplication.gradeRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Test.Utility.*;
import static org.junit.Assert.assertEquals;

public class ContentTestController extends TestController {

    ObjectId gradeId, lessonId, subjectId;
    JSONObject grade, lesson, subject;

    public ContentTestController() throws Exception {

        gradeId = null;
        lessonId = null;
        subjectId = null;

        init("Content", "admin/content/");
    }

    @Override
    public void setWantedNotifKeys() {}

    @Override
    public void run(JSONObject testcase) throws Exception {

        grade = testcase.getJSONObject("grade");
        addGrade(grade);

        showSuccess("add grade successfully");

        try {
            addGrade(grade);
            throw new InvalidFieldsException("Err in duplicate grade name");
        }
        catch (Exception x) {
            showSuccess("prevent duplicate grade name successfully");
        }

        lesson = testcase.getJSONObject("lesson");
        addLesson(lesson);

        showSuccess("add lesson successfully");

        try {
            addLesson(lesson);
            throw new InvalidFieldsException("Err in duplicate lesson name");
        }
        catch (Exception x) {
            showSuccess("prevent duplicate lesson name successfully");
        }

        subject = testcase.getJSONObject("subject");
        addSubject(subject);

        showSuccess("add subject successfully");

        try {
            addSubject(subject);
            throw new InvalidFieldsException("Err in duplicate subject name");
        }
        catch (Exception x) {
            showSuccess("prevent duplicate subject name successfully");
        }

        JSONObject all = all();
        int idx = TestController.searchInJSONArray(all.getJSONArray("grades"), "id", gradeId.toString());
        if(idx == -1)
            throw new InvalidFieldsException("Grade not found in get all req");

        checkJSONPath(all.toString(), idx);

        showSuccess("Get all successfully");

        grade.put("name", "newTest" + grade.getString("name"));
        update("updateGrade/" + gradeId, grade);

        showSuccess("Update grade successfully");

        lesson.put("name", "newTest" + lesson.getString("name"));
        lesson.put("description", "new desc");

        update("updateLesson/" + gradeId + "/" + lessonId, lesson);

        showSuccess("Update lesson successfully");

        subject.put("name", "newTest" + subject.getString("name"));
        subject.put("description", "new desc");
        subject.put("easyPrice", subject.getInt("easyPrice") + 500);
        subject.put("hardPrice", subject.getInt("hardPrice") + 500);
        subject.put("midPrice", subject.getInt("midPrice") + 500);

        update("updateSubject/" + subjectId, subject);

        showSuccess("Update subject successfully");

        all = all();
        idx = TestController.searchInJSONArray(all.getJSONArray("grades"), "id", gradeId.toString());
        if(idx == -1)
            throw new InvalidFieldsException("Grade not found in get all req after update");

        checkJSONPath(all.toString(), idx);

        showSuccess("Get all successfully after update");

        JSONObject newLesson = new JSONObject(lesson.toString());
        newLesson.put("name", lesson.getString("name") + "new");

        ObjectId oldLessonId = lessonId;
        addLesson(newLesson);

        subject.put("lessonId", lessonId);
        subject.put("gradeId", gradeId);

        update("updateSubject/" + subjectId, subject);

        showSuccess("Update subject lesson successfully");

        JSONObject allSubjects = allSubjects(gradeId, lessonId);

        idx = TestController.searchInJSONArray(
                allSubjects.getJSONArray("subjects"), "id", subjectId.toString()
        );

        if(idx == -1)
            throw new InvalidFieldsException("Subject not found in get all subjects req");

        DocumentContext jsonContext = JsonPath.parse(allSubjects.toString());

        try {

            assertEquals(subject.getString("name"), jsonContext.read("$['subjects'][" + idx + "]['name']"));
            assertEquals(subject.get("easyPrice"), jsonContext.read("$['subjects'][" + idx + "]['easyPrice']"));
            assertEquals(subject.get("hardPrice"), jsonContext.read("$['subjects'][" + idx + "]['hardPrice']"));
            assertEquals(subject.get("midPrice"), jsonContext.read("$['subjects'][" + idx + "]['midPrice']"));

            if(subject.has("description"))
                assertEquals(subject.getString("description"), jsonContext.read("$['subjects'][" + idx + "]['description']"));

            assertEquals(newLesson.get("name"), jsonContext.read("$['subjects'][" + idx + "]['lesson']['name']"));
            assertEquals(lessonId.toString(), jsonContext.read("$['subjects'][" + idx + "]['lesson']['id']"));

            assertEquals(grade.get("name"), jsonContext.read("$['subjects'][" + idx + "]['grade']['name']"));
            assertEquals(gradeId.toString(), jsonContext.read("$['subjects'][" + idx + "]['grade']['id']"));

        }
        catch (AssertionError x) {
            clear();
            throw x;
        }

        showSuccess("Get all subjects successfully");

        allSubjects = allSubjects(gradeId, oldLessonId);

        idx = TestController.searchInJSONArray(
                allSubjects.getJSONArray("subjects"), "id", subjectId.toString()
        );

        if(idx != -1)
            throw new InvalidFieldsException("Subject found wrongly in get all subjects req");

        JSONObject searchResult = search("search?key=wTes");

        jsonContext = JsonPath.parse(searchResult.toString());

        try {
            assertEquals(gradeId.toString(), jsonContext.read("$['data'][0]['id']"));
            assertEquals(oldLessonId.toString(), jsonContext.read("$['data'][1]['id']"));
            assertEquals(lessonId.toString(), jsonContext.read("$['data'][2]['id']"));
            assertEquals(subjectId.toString(), jsonContext.read("$['data'][3]['id']"));
        }
        catch (AssertionError x) {
            clear();
            throw x;
        }

        searchResult = search("search?key=" + grade.getString("name").substring(8, 16));

        jsonContext = JsonPath.parse(searchResult.toString());

        try {
            assertEquals(gradeId.toString(), jsonContext.read("$['data'][0]['id']"));
            assertEquals("grade", jsonContext.read("$['data'][0]['section']"));
        }
        catch (AssertionError x) {
            clear();
            throw x;
        }

        showSuccess("Search successfully");

        try {
            delete("deleteGrade/" + gradeId);
            throw new InvalidFieldsException("delete grade with dependency");
        }
        catch (Exception x) {
            showSuccess("Prevent delete grade with dependency successfully");
        }

        delete("deleteLesson/" + gradeId + "/" + oldLessonId);
        showSuccess("Delete lesson without dependency successfully");

        try {
            delete("deleteLesson/" + gradeId + "/" + lessonId);
            throw new InvalidFieldsException("delete lesson with dependency");
        }
        catch (Exception x) {
            showSuccess("Prevent delete lesson with dependency successfully");
        }

        delete("deleteSubject/" + subjectId);

        Document tmp = subjectRepository.findById(subjectId);
        if(tmp != null)
            throw new InvalidFieldsException("Wrongly subject cache after deletion");

        delete("deleteLesson/" + gradeId + "/" + lessonId);
        delete("deleteGrade/" + gradeId);

        showSuccess("Delete all successfully");

        tmp = gradeRepository.findById(gradeId);
        if(tmp != null)
            throw new InvalidFieldsException("Wrongly grade cache after deletion");

        showSuccess("Test cache after delete all successfully");
    }

    private void addGrade(JSONObject jsonObject) throws Exception {
        JSONObject res = sendPostReq(prefix + "addGrade", adminToken, jsonObject);
        gradeId = new ObjectId(res.getString("id"));
    }

    private void addLesson(JSONObject jsonObject) throws Exception {
        JSONObject res = sendPostReq(prefix + "addLesson/" + gradeId, adminToken, jsonObject);
        lessonId = new ObjectId(res.getString("id"));
    }

    private void addSubject(JSONObject jsonObject) throws Exception {
        JSONObject res = sendPostReq(prefix + "addSubject/" + gradeId + "/" + lessonId, adminToken, jsonObject);
        subjectId = new ObjectId(res.getString("id"));
    }

    private JSONObject search(String address) throws InvalidFieldsException {
        return sendGetReq(prefix + address, adminToken);
    }

    private JSONObject all() throws InvalidFieldsException {
        return sendGetReq(prefix + "all", adminToken);
    }

    private JSONObject allSubjects(ObjectId gradeId, ObjectId lessonId) throws InvalidFieldsException {
        return sendGetReq(prefix + "allSubjects?gradeId=" + gradeId + "&lessonId=" + lessonId, adminToken);
    }

    private void checkJSONPath(String jsonStr, int idx) {

        DocumentContext jsonContext = JsonPath.parse(jsonStr);

        try {

            assertEquals(grade.getString("name"), jsonContext.read("$['grades'][" + idx + "]['name']"));
            assertEquals(lesson.getString("name"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['name']"));

            if(lesson.has("description"))
                assertEquals(lesson.getString("description"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['description']"));

            assertEquals(subject.getString("name"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['subjects'][0]['name']"));
            assertEquals(subject.get("easyPrice"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['subjects'][0]['easyPrice']"));
            assertEquals(subject.get("hardPrice"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['subjects'][0]['hardPrice']"));
            assertEquals(subject.get("midPrice"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['subjects'][0]['midPrice']"));

            if(subject.has("description"))
                assertEquals(subject.getString("description"), jsonContext.read("$['grades'][" + idx + "]['lessons'][0]['subjects'][0]['description']"));

        }
        catch (AssertionError x) {
            clear();
            throw x;
        }
    }

    private void update(String address, JSONObject jsonObject) throws InvalidFieldsException {
        sendPostReq(prefix + address, adminToken, jsonObject);
    }

    private void delete(String address) throws InvalidFieldsException {
        sendDeleteReq(prefix + address, adminToken);
    }

    @Override
    public void clear() {
        if(startAt != -1) {
            System.out.println("cleaning test contents");
            gradeRepository.deleteMany(gt("created_at", startAt));
            subjectRepository.deleteMany(gt("created_at", startAt));
        }
    }

}
