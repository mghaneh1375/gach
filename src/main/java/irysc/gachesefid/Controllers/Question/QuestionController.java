package irysc.gachesefid.Controllers.Question;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOptions;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Digests.Question;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidator;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.gradeRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal;

public class QuestionController extends Utilities {

    // get answer from db should always be general because input type in object

    public static String addQuestion(ObjectId subjectId,
                                     JSONObject jsonObject) {

        Document subject = subjectRepository.findById(subjectId);
        if (subject == null)
            return JSON_NOT_VALID_ID;

        try {
            checkAnswer(jsonObject);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        Document newDoc = new Document("subject_id", subjectId)
                .append("visibility", false);

        for (String str : jsonObject.keySet())
            newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        return questionRepository.insertOneWithReturn(newDoc);
    }

    public static String setQuestionFiles(ObjectId questionId,
                                          MultipartFile questionFile,
                                          MultipartFile answerFile) {

        Document question = questionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if(questionFile != null && questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز برای آپلود " + MAX_QUESTION_FILE_SIZE + " می باشد.");

        if(answerFile != null && answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز برای آپلود " + MAX_QUESTION_FILE_SIZE + " می باشد.");

        String questionFilename = null;
        if(questionFile != null) {
            questionFilename = FileUtils.uploadImageFile(questionFile);
            if (questionFilename == null)
                return generateErr("فایل موردنظر برای صورت سوال معتبر نمی باشد.");
        }

        String answerFilename = null;
        if(answerFile != null) {
            answerFilename = FileUtils.uploadImageFile(answerFile);
            if (answerFilename == null)
                return generateErr("فایل موردنظر برای پاسخ سوال معتبر نمی باشد.");
        }

        if(questionFilename != null) {

            questionFilename = FileUtils.uploadFile(questionFile, QuestionRepository.FOLDER);

            if(questionFilename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;
        }

        if(answerFilename != null) {

            answerFilename = FileUtils.uploadFile(answerFile, QuestionRepository.FOLDER);

            if(answerFilename == null) {

                if(questionFilename != null)
                    FileUtils.removeFile(questionFilename, QuestionRepository.FOLDER);

                return JSON_UNKNOWN_UPLOAD_FILE;
            }
        }

        if(questionFilename != null)
            question.put("question_file", questionFilename);

        if(answerFilename != null)
            question.put("answer_file", answerFilename);

        if(question.containsKey("question_file"))
            question.put("visibility", true);

        return JSON_OK;
    }

    public static String updateQuestion(ObjectId questionId,
                                        JSONObject jsonObject) {

        Document question = questionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if(jsonObject.has("visibility") &&
                jsonObject.getBoolean("visibility") &&
                !question.containsKey("question_file")
        )
            return generateErr("برای قابل مشاهده کردن سوال ابتدا باید فایل صورت سوال را آپلود نمایید.");

        if(jsonObject.has("subjectId")) {

            ObjectId subjectId = new ObjectId(jsonObject.getString("subjectId"));

            if(!question.getObjectId("subjectId").equals(subjectId)) {

                if (subjectRepository.findById(subjectId) == null)
                    return JSON_NOT_VALID_ID;

                question.put("subject_id", subjectId);
            }
        }

        if(!jsonObject.has("kindQuestion"))
            jsonObject.put("kindQuestion", question.getString("kind_question"));

        if(jsonObject.getString("kindQuestion").equals(
                QuestionType.MULTI_SENTENCE.getName()
        ) && !jsonObject.has("sentencesCount") &&
                question.containsKey("sentences_count")
        )
            jsonObject.put("sentencesCount", question.getInteger("sentences_count"));


        try {
            checkAnswer(jsonObject);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        for (String str : jsonObject.keySet())
            question.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        return JSON_OK;

    }

    public static String deleteQuestion(JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        ArrayList<ObjectId> deleted = new ArrayList<>();

        for(int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if(!ObjectIdValidator.isValid(id)) {
                excepts.put(id);
                continue;
            }

            ObjectId questionId = new ObjectId(jsonArray.getString(i));

            Document question = questionRepository.findById(questionId);
            if (question == null) {
                excepts.put(id);
                continue;
            }

            if(question.containsKey("used") &&
                    question.getInteger("used") > 0
            ) {
                excepts.put(question.getString("organization_id"));
                continue;
            }

            questionRepository.cleanReject(question);
            deleted.add(questionId);
        }

        questionRepository.deleteMany(in(
                "_id", deleted
        ));

        if(excepts.length() == 0)
            return JSON_OK;

        return generateErr(
                "بجز موارد بیان شده بقیه به درستی از سامانه حذف شدند.",
                new PairValue("excepts", excepts)
        );
    }

    public static String uploadFiles(MultipartFile file) {



    }

    public static String addBatch(MultipartFile file) {

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if(rows == null)
            return generateErr("File is not valid");

        rows.remove(0);
        boolean needUpdate = false;

        // excel format:
        // 1- row no 2- question file name 3- subject id
        // 4- author id 5- kindQuestion[test, short_answer, multi_sentence, tashrihi]
        // 6- needed time 7- answer 8- organizationId
        // 9- level[easy, mid, hard] 10- answer file name : optional
        // 11- sentencesCount : optional 12- telorance : optional
        // 13- choicesCount : optional 14- neededLines : optional

        JSONArray excepts = new JSONArray();
        int rowIdx = 0;

        for(Row row : rows) {

            rowIdx++;

            try {

                if (row.getLastCellNum() < 9) {
                    excepts.put(rowIdx);
                    continue;
                }

                String questionFilename = row.getCell(1).getStringCellValue();
                if(!FileUtils.checkExist(questionFilename, QuestionRepository.FOLDER)) {
                    excepts.put(rowIdx);
                    continue;
                }

                String answerFilename = null;

                if(!row.getCell(10).getStringCellValue().isEmpty()) {
                    answerFilename = row.getCell(1).getStringCellValue();
                    if (!FileUtils.checkExist(answerFilename, QuestionRepository.FOLDER)) {
                        excepts.put(rowIdx);
                        continue;
                    }
                }

                if(!ObjectIdValidator.isValid(row.getCell(2).getStringCellValue()) ||
                        !ObjectIdValidator.isValid(row.getCell(3).getStringCellValue())
                ) {
                    excepts.put(rowIdx);
                    continue;
                }

                JSONObject jsonObject = new JSONObject();

//                ObjectId subjectId = new ObjectId(row.getCell(2).getStringCellValue());
//                ObjectId authorId = new ObjectId(row.getCell(3).getStringCellValue());
//
//                if(subjectRepository.findById(subjectId) == null ||
//                        userRepository.findById(authorId) == null
//                ) {
//                    excepts.put(rowIdx);
//                    continue;
//                }

                jsonObject.put("subjectId", row.getCell(2).getStringCellValue());
                jsonObject.put("authorId", row.getCell(3).getStringCellValue());

                String kindQuestion = row.getCell(4).getStringCellValue();
                if(!EnumValidatorImp.isValid(kindQuestion, QuestionType.class)) {
                    excepts.put(rowIdx);
                    continue;
                }
                jsonObject.put("kindQuestion", kindQuestion);


                String level = row.getCell(8).getStringCellValue();
                if(!EnumValidatorImp.isValid(level, QuestionLevel.class)) {
                    excepts.put(rowIdx);
                    continue;
                }
                jsonObject.put("level", level);

                jsonObject.put("neededTime", (int) row.getCell(5).getNumericCellValue());

                try {
                    jsonObject.put("answer", row.getCell(6).getNumericCellValue());
                }
                catch (Exception x) {
                    jsonObject.put("answer", row.getCell(6).getStringCellValue());
                }

                jsonObject.put("organizationId", row.getCell(7).getStringCellValue());

                checkAnswer(jsonObject);

                questionFilename = FileUtils.renameFile(questionFilename, null);

                jsonObject.put("question_file", questionFilename);
                jsonObject.put("answer_file", answerFilename);




            } catch (Exception ignore) {
                excepts.put(rowIdx);
            }
        }

        if(!needUpdate)
            return JSON_OK;

        for(String key : isNew.keySet()) {
            if(isNew.get(key))
                gradeRepository.insertOne(grades.get(key));
            else
                gradeRepository.replaceOne(grades.get(key).getObjectId("_id"), grades.get(key));
        }

        return JSON_OK;
    }
}
