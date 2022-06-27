package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.SchoolQuizRepository;
import irysc.gachesefid.DB.UserRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.StaticValues;
import irysc.gachesefid.Validator.LinkValidator;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.FileUtils.uploadPdfOrMultimediaFile;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal;


public class QuizController {

    static Document hasAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null)
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        if (userId != null && !quiz.getObjectId("created_by").equals(userId))
            throw new InvalidFieldsException(JSON_NOT_ACCESS);

        return quiz;
    }

    static PairValue hasCorrectorAccess(Common db, ObjectId userId, ObjectId quizId
    ) throws InvalidFieldsException {

        Document quiz = db.findById(quizId);
        if (quiz == null || !quiz.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
            throw new InvalidFieldsException(JSON_NOT_VALID_ID);

        int idx = -1;

        if (userId != null && !quiz.getObjectId("created_by").equals(userId)) {

            List<Document> correctors = quiz.getList("correctors", Document.class);
            idx = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                    correctors, "_id", userId);

            if(idx == -1)
                throw new InvalidFieldsException(JSON_NOT_VALID_ID);
        }

        return new PairValue(quiz, idx);
    }

    public static Document store(ObjectId userId, JSONObject data
    ) throws InvalidFieldsException {

        Document newDoc = new Document();

        for (String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        Utility.isValid(newDoc);

//        if (!newDoc.containsKey("desc_after_mode") ||
//                newDoc.getString("desc_after_mode").equals(DescMode.FILE.getName())
//        )
//            newDoc.put("desc_after_mode", DescMode.NONE.getName());
//
//        if (!newDoc.containsKey("desc_mode") ||
//                newDoc.getString("desc_mode").equals(DescMode.FILE.getName())
//        )
//            newDoc.put("desc_mode", DescMode.NONE.getName());

        newDoc.put("visibility", true);
        newDoc.put("students", new ArrayList<>());
        newDoc.put("remove_questions", new ArrayList<>());
        newDoc.put("tags", new ArrayList<>());
        newDoc.put("attaches", new ArrayList<>());
        newDoc.put("created_by", userId);
        newDoc.put("created_at", System.currentTimeMillis());

        //todo: consider other modes
        if (newDoc.getString("mode").equals(KindQuiz.REGULAR.getName()) ||
                newDoc.getString("mode").equals(KindQuiz.OPEN.getName())
        )
            newDoc.put("questions", new ArrayList<>());

        if (newDoc.getString("mode").equals(KindQuiz.TASHRIHI.getName()))
            newDoc.put("correctors", new ArrayList<>());

        return newDoc;
    }

    public static String toggleVisibility(Common db, ObjectId userId, ObjectId quizId) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            quiz.put("visibility", quiz.getBoolean("visibility"));
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String forceRegistry(Common db, ObjectId userId,
                                       ObjectId quizId, JSONArray jsonArray) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            List<Document> students = quiz.getList("students", Document.class);
            JSONArray skipped = new JSONArray();

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if(!jsonObject.has("id") ||
                        !jsonObject.has("paid")
                ) {
                    skipped.put(i);
                    continue;
                }

                ObjectId studentId = new ObjectId(jsonObject.getString("id"));

                Document student = userRepository.findById(studentId);

                if (student == null) {
                    skipped.put(i);
                    continue;
                }

                if (userId != null && !Authorization.hasAccessToThisStudent(studentId, userId)) {
                    skipped.put(i);
                    continue;
                }

                quizAbstract.registry(student, quiz, jsonObject.getInt("paid"));
            }

            quiz.put("students", students);
            db.replaceOne(quizId, quiz);

            if (skipped.length() == 0)
                return JSON_OK;

            return new JSONObject()
                    .put("status", "nok")
                    .put("skipped", skipped)
                    .toString();
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String forceDeportation(Common db, ObjectId userId,
                                          ObjectId quizId, JSONArray jsonArray) {


        try {

            Document quiz = hasAccess(db, userId, quizId);
            List<Document> students = quiz.getList("students", Document.class);
            JSONArray skipped = new JSONArray();

            QuizAbstract quizAbstract;

            // todo : complete this section
            if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
                quizAbstract = new RegularQuizController();
            else
                quizAbstract = new TashrihiQuizController();

            for (int i = 0; i < jsonArray.length(); i++) {

                ObjectId studentId = new ObjectId(jsonArray.getString(i));

                Document student = userRepository.findById(studentId);

                if (student == null) {
                    skipped.put(i);
                    continue;
                }

                quizAbstract.quit(student, quiz);
            }

            quiz.put("students", students);
            db.replaceOne(quizId, quiz);

            if (skipped.length() == 0)
                return JSON_OK;

            return new JSONObject()
                    .put("status", "nok")
                    .put("skipped", skipped)
                    .toString();

        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String remove(Common db, ObjectId userId, ObjectId quizId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            // todo: imp

            return JSON_OK;
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }

    }

    public static String buy(Document user, ObjectId quizId) {

        Document quiz = iryscQuizRepository.findById(quizId);
        if (quiz == null || !quiz.getBoolean("visibility"))
            return JSON_NOT_ACCESS;

        QuizAbstract quizAbstract;

        // todo : complete this section
        if (KindQuiz.REGULAR.getName().equals(quiz.getString("mode")))
            quizAbstract = new RegularQuizController();
        else
            quizAbstract = new TashrihiQuizController();

        return quizAbstract.buy(user, quiz);

    }

    public static String getParticipants(Common db,
                                         ObjectId userId,
                                         ObjectId quizId,
                                         ObjectId studentId,
                                         Boolean isStudentAnswersNeeded,
                                         Boolean isResultsNeeded,
                                         Boolean justMarked,
                                         Boolean justNotMarked,
                                         Boolean justAbsents,
                                         Boolean justPresence) {

        try {
            Document quiz = hasAccess(db, userId, quizId);

            JSONArray jsonArray = new JSONArray();

            List<Document> students = quiz.getList("students", Document.class);

            for (Document student : students) {

                if (studentId != null && !student.getObjectId("_id").equals(studentId))
                    continue;

                if (justAbsents != null && justAbsents && student.containsKey("start_at"))
                    continue;

                if (justPresence != null && justPresence && !student.containsKey("start_at"))
                    continue;

                if (justMarked != null && justMarked &&
                        student.containsKey("all_marked") &&
                        !student.getBoolean("all_marked")
                )
                    continue;

                if (justNotMarked != null && justNotMarked &&
                        student.containsKey("all_marked") &&
                        student.getBoolean("all_marked")
                )
                    continue;

                Document user = userRepository.findById(student.getObjectId("_id"));
                if (user == null)
                    continue;

                JSONObject jsonObject = new JSONObject()
                        .put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"))
                        .put("username", user.getString("username"))
                        .put("pic", StaticValues.STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"))
                        .put("studentId", user.getObjectId("_id").toString())
                        .put("startAt", student.containsKey("start_at") ?
                                irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("start_at")) :
                                ""
                        )
                        .put("finishAt", student.containsKey("finish_at") ?
                                irysc.gachesefid.Utility.Utility.getSolarDate(student.getLong("finish_at")) :
                                ""
                        );

                if (student.containsKey("all_marked"))
                    jsonObject.put("allMarked", student.getBoolean("all_marked"));

                if (isResultsNeeded != null && isResultsNeeded)
                    jsonObject.put("totalMark", student.getOrDefault("total_mark", ""));

                if (isStudentAnswersNeeded != null && isStudentAnswersNeeded) {

                    if (!student.containsKey("answers"))
                        jsonObject.put("answers", new JSONArray());

                    else {
                        jsonObject.put("answers", Utility.getQuestions(
                                true, false,
                                quiz.getList("questions", Document.class),
                                student.getList("answers", Document.class),
                                db instanceof IRYSCQuizRepository ? IRYSCQuizRepository.FOLDER : SchoolQuizRepository.FOLDER
                        ));
                    }
                }


                jsonArray.put(jsonObject);
            }

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg("students", jsonArray);
        } catch (InvalidFieldsException x) {
            return generateErr(
                    x.getMessage()
            );
        }
    }

    public static String addAttach(Common db,
                                   ObjectId userId,
                                   ObjectId quizId,
                                   MultipartFile file,
                                   String title,
                                   String link) {

        try {

            if (file == null && link == null)
                return JSON_NOT_VALID_PARAMS;

            Document quiz = hasAccess(db, userId, quizId);

            List<Document> attaches = quiz.getList("attaches", Document.class);

            if (db instanceof SchoolQuizRepository) {

                Document config = irysc.gachesefid.Utility.Utility.getConfig();
                if (config.getBoolean("school_quiz_attaches_just_link") && file != null)
                    return JSON_NOT_ACCESS;

                if (attaches.size() >= config.getInteger("schoolQuizAttachesMax"))
                    return generateErr(
                            "شما می توانید حداکثر " + config.getInteger("schoolQuizAttachesMax") + " پیوست داشته باشید."
                    );
            }

            if (link != null && !LinkValidator.isValid(link))
                return generateErr(
                        "لینک موردنظر نامعتبر است."
                );

            ObjectId id = new ObjectId();

            Document doc = new Document("title", title)
                    .append("is_external_link", link != null)
                    .append("_id", id);

            if (link != null)
                doc.put("link", link);
            else {

                String base = db instanceof SchoolQuizRepository ?
                        SchoolQuizRepository.FOLDER :
                        IRYSCQuizRepository.FOLDER;

                if (db instanceof SchoolQuizRepository &&
                        file.getSize() > MAX_QUIZ_ATTACH_SIZE)
                    return generateErr(
                            "حداکثر حجم مجاز، " + MAX_QUIZ_ATTACH_SIZE + " مگ است."
                    );

                String fileType = uploadPdfOrMultimediaFile(file);
                if (fileType == null)
                    return generateErr(
                            "فرمت فایل موردنظر معتبر نمی باشد."
                    );

                String filename = FileUtils.uploadFile(file, base + "/attaches");
                if (filename == null)
                    return JSON_UNKNOWN_UPLOAD_FILE;

                doc.put("link", filename);
            }

            attaches.add(doc);
            db.replaceOne(quizId, quiz);

            return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                    "id", id.toString()
            );

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String removeAttach(Common db,
                                      ObjectId userId,
                                      ObjectId quizId,
                                      ObjectId attachId) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            List<Document> attaches = quiz.getList("attaches", Document.class);
            Document doc = searchInDocumentsKeyVal(
                    attaches, "_id", attachId
            );

            if(doc == null)
                return JSON_NOT_VALID_ID;

            if(!doc.getBoolean("is_external_link")) {

                String base = db instanceof SchoolQuizRepository ?
                        SchoolQuizRepository.FOLDER :
                        IRYSCQuizRepository.FOLDER;

                FileUtils.removeFile(doc.getString("link"), base + "/attaches");

            }

            attaches.remove(doc);
            db.replaceOne(quizId, quiz);

            return JSON_OK;
        } catch (Exception x) {
            return generateErr(x.getMessage());
        }
    }

    public static String extend(Common db, ObjectId userId,
                                ObjectId quizId,
                                Long start, Long end) {

        try {

            Document quiz = hasAccess(db, userId, quizId);

            if (start != null && end != null && start >= end)
                return JSON_NOT_VALID_PARAMS;

            long curr = System.currentTimeMillis();

            if (!DEV_MODE && (
                    (start != null && start < curr) ||
                            (end != null && end < curr)
            ))
                return generateErr(
                        "زمان ها باید از اکنون بزرگ تر باشند."
                );

            long endAt = quiz.getLong("end");

            if (!DEV_MODE && endAt < curr)
                return generateErr("زمان آزمون/تمرین موردنظر به پایان رسیده است.");

            long startAt = quiz.getLong("start");

            if (!DEV_MODE && startAt < curr && start != null)
                return generateErr("به دلیل شروع شدن آزمون/تمرین، شما نمی توانید زمان شروع را تغییر دهید.");

            startAt = start != null ? start : startAt;
            endAt = end != null ? end : endAt;

            if (quiz.containsKey("duration")) {

                long diff = (endAt - startAt) / 1000;
                int duration = quiz.getInteger("duration") * 60;

                if (duration > diff)
                    return generateErr("فاصله بین زمان شروع آزمون/تمرین و پایان آن باید حداقل " + (int) Math.ceil(duration / 60.0) + " دقیقه باشد.");
            }

            if (end != null)
                quiz.put("end", end);

            if (start != null)
                quiz.put("start", start);

            db.replaceOne(quizId, quiz);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(
                    e.getMessage()
            );
        }
    }

    public static String arrangeQuestions(Common db, ObjectId userId,
                                          ObjectId quizId, JSONObject jsonObject
                                          ) {
        try {
            Document doc = hasAccess(db, userId, quizId);

            if (doc.containsKey("permute") &&
                    doc.getBoolean("permute")) {
                return generateErr("آزمون/تمرین موردنظر دارای ویژگی بر زدن است و این ویژگی برای این آزمون/تمرین بکار نمی رود.");
            }

            long current = System.currentTimeMillis();

            if (doc.getLong("start") < current)
                return generateErr("زمان آزمون/تمرین مورد نظر فرارسیده است و امکان ویرایش سوالات وجود ندارد.");

            JSONArray questionIds = jsonObject.getJSONArray("questionIds");
            List<Document> questions = doc.getList("questions", Document.class);
            int totalQuestions = questions.size();

            if (totalQuestions != questionIds.length())
                return JSON_NOT_VALID_PARAMS;

            ArrayList<Document> newArrange = new ArrayList<>();

            for (int i = 0; i < questionIds.length(); i++) {

                for (int j = i + 1; j < questionIds.length(); j++) {
                    if (questionIds.getString(j).equals(questionIds.getString(i)))
                        return JSON_NOT_VALID_PARAMS;
                }

                if (!ObjectIdValidator.isValid(questionIds.getString(i)))
                    return JSON_NOT_VALID_PARAMS;

                Document question = searchInDocumentsKeyVal(questions, "_id",
                        new ObjectId(questionIds.getString(i)));

                if (question == null)
                    return JSON_NOT_VALID_PARAMS;

                newArrange.add(question);
            }

            doc.put("questions", newArrange);

            db.replaceOne(quizId, doc);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }
}
