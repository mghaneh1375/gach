package irysc.gachesefid.Controllers.Question;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.EscapeQuizQuestionRepository;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.Validator.ObjectIdValidator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.FileUtils.uploadImageFile;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

public class QuestionController extends Utilities {

    // get answer from db should always be general because input type in object

    public static String addQuestion(ObjectId subjectId,
                                     MultipartFile questionFile,
                                     MultipartFile answerFile,
                                     JSONObject jsonObject) {

        jsonObject.put("subjectId", subjectId.toString());

        try {
            checkAnswer(jsonObject);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        if (questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

        String fileType = uploadImageFile(questionFile);

        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        if (answerFile != null) {
            if (answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            fileType = uploadImageFile(answerFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        String questionFileName = FileUtils.uploadFile(questionFile, QuestionRepository.FOLDER);
        if (questionFileName == null)
            return JSON_NOT_VALID_FILE;

        String answerFileName = null;
        if (answerFile != null) {
            answerFileName = FileUtils.uploadFile(answerFile, QuestionRepository.FOLDER);

            if (answerFileName == null) {
                FileUtils.removeFile(questionFileName, QuestionRepository.FOLDER);
                return JSON_NOT_VALID_FILE;
            }

        }

        Document newDoc = new Document("visibility", !jsonObject.has("visibility") || jsonObject.getBoolean("visibility"))
                .append("question_file", questionFileName);

        if (answerFileName != null)
            newDoc.append("answer_file", answerFileName);

        for (String str : jsonObject.keySet()) {

            if (str.equalsIgnoreCase("authorId") || str.equalsIgnoreCase("tags"))
                continue;

            newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));
        }

        if(!newDoc.containsKey("is_public"))
            newDoc.append("is_public", true);

        Document subject = subjectRepository.findById(subjectId);
        if (subject != null) {
            int qNo = ((int) subject.getOrDefault("q_no", 0) + 1);
            subject.put("q_no", qNo);
            subjectRepository.updateOne(subjectId,
                    set("q_no", qNo)
            );
        }

        ObjectId authorId = new ObjectId(jsonObject.get("authorId").toString());

        Document user = authorRepository.findById(authorId);
        if (user != null) {
            int qNo = ((int) user.getOrDefault("q_no", 0) + 1);

            user.put("q_no", qNo);
            userRepository.updateOne(authorId,
                    set("q_no", qNo)
            );
            newDoc.put("author", user.getString("name"));
        }

        if (jsonObject.has("tags")) {
            ArrayList<String> tags = new ArrayList<>();

            for (int j = 0; j < jsonObject.getJSONArray("tags").length(); j++) {

                String tag = jsonObject.getJSONArray("tags").get(j).toString();

                if (!questionTagRepository.exist(
                        eq("tag", tag)
                )) {
                    int tagCode = getRandIntForTag();

                    while (questionTagRepository.exist(
                            eq("code", tagCode)
                    ))
                        tagCode = getRandIntForTag();

                    questionTagRepository.insertOne(
                            new Document("tag", tag).append("code", tagCode)
                    );
                }

                tags.add(tag);
            }
            newDoc.put("tags", tags);
        }

        return questionRepository.insertOneWithReturn(newDoc);
    }

    public static String addEscapeQuizQuestion(
            MultipartFile questionFile,
            MultipartFile answerFile,
            JSONObject jsonObject) {

        if (escapeQuizQuestionRepository.exist(
                eq("organization_id", jsonObject.getString("organizationId"))
        ))
            return generateErr("کد سازمانی سوال در سامانه موجود است.");

        if (questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

        String fileType = uploadImageFile(questionFile);

        if (fileType == null)
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

        if (answerFile != null) {
            if (answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            fileType = uploadImageFile(answerFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        String questionFileName = FileUtils.uploadFile(questionFile, EscapeQuizQuestionRepository.FOLDER);
        if (questionFileName == null)
            return JSON_NOT_VALID_FILE;

        String answerFileName = null;
        if (answerFile != null) {
            answerFileName = FileUtils.uploadFile(answerFile, EscapeQuizQuestionRepository.FOLDER);

            if (answerFileName == null) {
                FileUtils.removeFile(questionFileName, QuestionRepository.FOLDER);
                return JSON_NOT_VALID_FILE;
            }

        }

        Document newDoc = new Document("question_file", questionFileName);

        if (answerFileName != null)
            newDoc.append("answer_file", answerFileName);

        for (String str : jsonObject.keySet()) {

            if (str.equalsIgnoreCase("authorId") || str.equalsIgnoreCase("tags"))
                continue;

            newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        }

        return escapeQuizQuestionRepository.insertOneWithReturn(newDoc);
    }

    public static JSONObject convertDocToJSON(Document doc) {
        return new JSONObject()
                .put("id", doc.getObjectId("_id").toString())
                .put("organizationId", doc.getString("organization_id"));
    }

    public static String updateQuestion(ObjectId questionId,
                                        MultipartFile questionFile,
                                        MultipartFile answerFile,
                                        JSONObject jsonObject) {

        Document question = questionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if (!jsonObject.has("kindQuestion"))
            jsonObject.put("kindQuestion", question.getOrDefault("kind_question", "test").toString());

        if (jsonObject.getString("kindQuestion").equals(
                QuestionType.MULTI_SENTENCE.getName()
        ) && !jsonObject.has("sentencesCount") &&
                question.containsKey("sentences_count")
        )
            jsonObject.put("sentencesCount", question.getInteger("sentences_count"));

        if (jsonObject.has("organizationId") &&
                jsonObject.getString("organizationId").equalsIgnoreCase(
                        question.getString("organization_id")
                )
        )
            jsonObject.remove("organizationId");

        if (jsonObject.has("subjectId") &&
                jsonObject.getString("subjectId").equalsIgnoreCase(
                        question.getObjectId("subject_id").toString()
                )
        )
            jsonObject.remove("subjectId");

        if (jsonObject.has("authorId") &&
                jsonObject.getString("authorId").equalsIgnoreCase(
                        question.getObjectId("author_id").toString()
                )
        )
            jsonObject.remove("authorId");

        try {
            checkAnswer(jsonObject);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        if (questionFile != null) {
            if (questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            String fileType = uploadImageFile(questionFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        if (answerFile != null) {
            if (answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            String fileType = uploadImageFile(answerFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        String questionFileName = null;
        if (questionFile != null) {
            questionFileName = FileUtils.uploadFile(questionFile, QuestionRepository.FOLDER);
            if (questionFileName == null)
                return JSON_NOT_VALID_FILE;
        }

        String answerFileName = null;
        if (answerFile != null) {
            answerFileName = FileUtils.uploadFile(answerFile, QuestionRepository.FOLDER);

            if (answerFileName == null) {
                FileUtils.removeFile(questionFileName, QuestionRepository.FOLDER);
                return JSON_NOT_VALID_FILE;
            }

        }

        if (jsonObject.has("subjectId")) {

            Document subject = subjectRepository.findById(question.getObjectId("subject_id"));
            if (subject != null) {
                subject.put("q_no", subject.getInteger("q_no") - 1);
                subjectRepository.replaceOne(subject.getObjectId("_id"), subject);
            }

            Document newSubject = subjectRepository.findById(
                    (ObjectId) jsonObject.get("subjectId")
            );

            if (newSubject != null) {
                newSubject.put("q_no", newSubject.getInteger("q_no") + 1);
                subjectRepository.replaceOne(newSubject.getObjectId("_id"), newSubject);
            }
        }

        if (jsonObject.has("authorId")) {

            Document author = authorRepository.findById(question.getObjectId("author_id"));
            if (author != null) {
                author.put("q_no", author.getInteger("q_no") - 1);
                authorRepository.replaceOne(author.getObjectId("_id"), author);
            }

            Document newAuthor = authorRepository.findById(
                    (ObjectId) jsonObject.get("authorId")
            );

            if (newAuthor != null) {
                newAuthor.put("q_no", newAuthor.getInteger("q_no") + 1);
                authorRepository.replaceOne(newAuthor.getObjectId("_id"), newAuthor);
            }

        }

        if (questionFileName != null) {
            FileUtils.removeFile(question.getString("question_file"), QuestionRepository.FOLDER);
            question.put("question_file", questionFileName);
        }

        if (answerFileName != null) {
            FileUtils.removeFile(question.getString("answer_file"), QuestionRepository.FOLDER);
            question.put("answer_file", answerFileName);
        }

        for (String str : jsonObject.keySet())
            question.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        questionRepository.replaceOne(questionId, question);
        return JSON_OK;

    }

    public static String updateEscapeQuizQuestion(ObjectId questionId,
                                                  MultipartFile questionFile,
                                                  MultipartFile answerFile,
                                                  JSONObject jsonObject) {

        Document question = escapeQuizQuestionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if (jsonObject.has("organizationId") &&
                jsonObject.getString("organizationId").equalsIgnoreCase(
                        question.getString("organization_id")
                )
        )
            jsonObject.remove("organizationId");

        if (jsonObject.has("organizationId")) {
            if (escapeQuizQuestionRepository.exist(
                    eq("organization_id", jsonObject.getString("organizationId"))
            ))
                return generateErr("کد سازمانی سوال در سامانه موجود است.");
        }

        if (questionFile != null) {

            if (questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            String fileType = uploadImageFile(questionFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        if (answerFile != null) {

            if (answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_QUESTION_FILE_SIZE + " مگ است.");

            String fileType = uploadImageFile(answerFile);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");
        }

        String questionFileName = null;
        if (questionFile != null) {
            questionFileName = FileUtils.uploadFile(questionFile, EscapeQuizQuestionRepository.FOLDER);
            if (questionFileName == null)
                return JSON_NOT_VALID_FILE;
        }

        String answerFileName = null;
        if (answerFile != null) {

            answerFileName = FileUtils.uploadFile(answerFile, EscapeQuizQuestionRepository.FOLDER);

            if (answerFileName == null) {
                FileUtils.removeFile(questionFileName, EscapeQuizQuestionRepository.FOLDER);
                return JSON_NOT_VALID_FILE;
            }

        }

        if (questionFileName != null) {
            FileUtils.removeFile(question.getString("question_file"), EscapeQuizQuestionRepository.FOLDER);
            question.put("question_file", questionFileName);
        }

        if (answerFileName != null) {
            FileUtils.removeFile(question.getString("answer_file"), EscapeQuizQuestionRepository.FOLDER);
            question.put("answer_file", answerFileName);
        }

        for (String str : jsonObject.keySet())
            question.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        escapeQuizQuestionRepository.replaceOne(questionId, question);
        return JSON_OK;
    }

    public static String deleteQuestion(JSONArray jsonArray) {

        JSONArray excepts = new JSONArray();
        ArrayList<ObjectId> deleted = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {

            String id = jsonArray.getString(i);

            if (!ObjectIdValidator.isValid(id)) {
                excepts.put(id);
                continue;
            }

            ObjectId questionId = new ObjectId(jsonArray.getString(i));

            Document question = questionRepository.findById(questionId);
            if (question == null) {
                excepts.put(id);
                continue;
            }

            if (question.containsKey("used") &&
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

        if (excepts.length() == 0)
            return JSON_OK;

        return generateErr(
                "بجز موارد بیان شده بقیه به درستی از سامانه حذف شدند.",
                new PairValue("excepts", excepts)
        );
    }

//    public static String uploadFiles(MultipartFile file) {
//
//
//
//    }

    public static String addBatch(MultipartFile file) {

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            return generateErr("File is not valid");

        rows.remove(0);

        // excel format:
        // 1- row no 2- question file name 3- subject id
        // 4- author id 5- kindQuestion[test, short_answer, multi_sentence, tashrihi]
        // 6- needed time 7- answer 8- organizationId
        // 9- level[easy, mid, hard] 10- answer file name : optional
        // 11- sentencesCount : optional 12- telorance : optional
        // 13- choicesCount : optional 14- neededLines : optional

        JSONArray excepts = new JSONArray();
        int rowIdx = 0;

        JSONArray errs = new JSONArray();

        boolean addAtLeastOne = false;

        for (Row row : rows) {

            rowIdx++;

            try {

                if (row.getCell(1) == null)
                    break;

                if (row.getLastCellNum() < 9) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "تعداد ستون ها نامعتیر است."));
                    continue;
                }

                String questionFilename = row.getCell(1).getStringCellValue();
                if (!FileUtils.checkExist(questionFilename, QuestionRepository.FOLDER)) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "فایل سوال موجود نیست."));
                    continue;
                }

                String answerFilename = null;
                Cell cell = row.getCell(9);

                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                    answerFilename = cell.getStringCellValue();
                    if (!FileUtils.checkExist(answerFilename, QuestionRepository.FOLDER)) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "فایل پاسخ سوال موجود نیست."));
                        continue;
                    }
                }

                JSONObject jsonObject = new JSONObject();

                int code = (int) getCellValue(row.getCell(2));
                Document subject = subjectRepository.findBySecKey(String.format("%03d", code));

                if (subject == null) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "کد مبحث نامعتیر است."));
                    continue;
                }

                ObjectId subjectId = subject.getObjectId("_id");

                int authorCode = (int) getCellValue(row.getCell(3));
                Document author = authorRepository.findBySecKey(String.format("%03d", authorCode));
                if (author == null) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "کد مولف نامعتبر است."));
                    continue;
                }

                String kindQuestion = row.getCell(4).getStringCellValue();
                if (!EnumValidatorImp.isValid(kindQuestion, QuestionType.class)) {
                    errs.put(batchRowErr(rowIdx, "نوع سوال نامعتیر است."));
                    excepts.put(rowIdx);
                    continue;
                }
                jsonObject.put("kindQuestion", kindQuestion);


                String level = row.getCell(8).getStringCellValue();
                if (!EnumValidatorImp.isValid(level, QuestionLevel.class)) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "سطح سختی نامعتیر است."));
                    continue;
                }
                jsonObject.put("level", level);

                jsonObject.put("neededTime", (int) row.getCell(5).getNumericCellValue());

                cell = row.getCell(6);
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (Math.floor(cell.getNumericCellValue()) == cell.getNumericCellValue())
                        jsonObject.put("answer", (int) cell.getNumericCellValue());
                    else
                        jsonObject.put("answer", cell.getNumericCellValue());
                } else
                    jsonObject.put("answer", row.getCell(6).getStringCellValue());

                jsonObject.put("organizationId", row.getCell(7).getStringCellValue());

                cell = row.getCell(10);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("sentencesCount", (int) cell.getNumericCellValue());

                cell = row.getCell(11);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("telorance", cell.getNumericCellValue());

                cell = row.getCell(12);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("choicesCount", (int) cell.getNumericCellValue());

                cell = row.getCell(13);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("neededLine", (int) cell.getNumericCellValue());

                ArrayList<String> tags = new ArrayList<>();

                cell = row.getCell(14);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("year", getCellValue(cell));

                for (int i = 15; i < 20; i++) {
                    cell = row.getCell(i);
                    if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                        try {
                            int tagCode = (int) cell.getNumericCellValue();

                            Document t = questionTagRepository.findBySecKey(tagCode);
                            if (t != null) {
                                String tt = t.getString("tag");
                                if (!tags.contains(tt))
                                    tags.add(tt);
                            }

                        } catch (Exception x) {
                            String t = cell.getStringCellValue();
                            if (!questionTagRepository.exist(
                                    eq("tag", t)
                            )) {
                                int tagCode = getRandIntForTag();
                                while (questionTagRepository.exist(
                                        eq("code", tagCode)
                                ))
                                    tagCode = getRandIntForTag();

                                questionTagRepository.insertOne(
                                        new Document("tag", t).append("code", tagCode)
                                );
                            }

                            tags.add(t);
                        }
                    }
                }

                checkAnswer(jsonObject);
                jsonObject.put("tags", tags);

                cell = row.getCell(21);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    jsonObject.put("is_public", cell.getBooleanCellValue());
                else
                    jsonObject.put("is_public", true);

                questionFilename = FileUtils.renameFile(QuestionRepository.FOLDER, questionFilename, null);

                if (questionFilename == null) {
                    errs.put(batchRowErr(rowIdx, "بارگذاری فایل صورت سوال با خطا مواجه شده است"));
                    excepts.put(rowIdx);
                    continue;
                }

                if (answerFilename != null) {
                    answerFilename = FileUtils.renameFile(QuestionRepository.FOLDER, answerFilename, null);

                    if (answerFilename == null) {
                        errs.put(batchRowErr(rowIdx, "بارگذاری فایل پاسخ سوال با خطا مواجه شده است"));
                        excepts.put(rowIdx);
                        continue;
                    }
                }

                jsonObject.put("question_file", questionFilename);
                jsonObject.put("answer_file", answerFilename);
                jsonObject.put("visibility", true);
                jsonObject.put("createdAt", System.currentTimeMillis());

                Document newDoc = new Document("subject_id", subjectId)
                        .append("author", author.getString("name"));

                for (String str : jsonObject.keySet())
                    newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

                questionRepository.insertOne(newDoc);
                addAtLeastOne = true;

            } catch (Exception ignore) {
                printException(ignore);
                excepts.put(rowIdx);
                errs.put(batchRowErr(rowIdx, ignore.getMessage()));
            }
        }

        if (addAtLeastOne)
            new Thread(() -> new Jobs.CalcSubjectQuestions().run()).start();

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی سوالات به درستی به سامانه اضافه شدند"
            );

        return generateSuccessMsg(
                "excepts",
                "بجز ردیف\u200Cهای زیر سایرین به درستی به سامانه اضافه گردیدند. " + excepts,
                new PairValue("errs", errs)
        );
    }


    public static String addBatchEscapeQuizQuestions(MultipartFile file) {

        String filename = FileUtils.uploadTempFile(file);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            return generateErr("File is not valid");

        rows.remove(0);

        // excel format:
        // 1- row no 2- question file name 3- subject id
        // 4- author id 5- kindQuestion[test, short_answer, multi_sentence, tashrihi]
        // 6- needed time 7- answer 8- organizationId
        // 9- level[easy, mid, hard] 10- answer file name : optional
        // 11- sentencesCount : optional 12- telorance : optional
        // 13- choicesCount : optional 14- neededLines : optional

        JSONArray excepts = new JSONArray();
        int rowIdx = 0;

        JSONArray errs = new JSONArray();

        for (Row row : rows) {

            rowIdx++;

            try {

                if (row.getCell(1) == null)
                    break;

                if (row.getLastCellNum() < 4) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "تعداد ستون ها نامعتیر است."));
                    continue;
                }

                String questionFilename = row.getCell(1).getStringCellValue();

                if (!FileUtils.checkExist(questionFilename, EscapeQuizQuestionRepository.FOLDER)) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "فایل سوال موجود نیست."));
                    continue;
                }

                String answerFilename = null;
                Cell cell = row.getCell(4);

                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                    answerFilename = cell.getStringCellValue();
                    if (!FileUtils.checkExist(answerFilename, EscapeQuizQuestionRepository.FOLDER)) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "فایل پاسخ سوال موجود نیست."));
                        continue;
                    }
                }

                JSONObject jsonObject = new JSONObject();

                cell = row.getCell(2);
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (Math.floor(cell.getNumericCellValue()) == cell.getNumericCellValue())
                        jsonObject.put("answer", (int) cell.getNumericCellValue());
                    else
                        jsonObject.put("answer", cell.getNumericCellValue());
                } else
                    jsonObject.put("answer", cell.getStringCellValue());

                jsonObject.put("organizationId", row.getCell(3).getStringCellValue());

                if (escapeQuizRepository.exist(
                        eq("organization_id", jsonObject.getString("organizationId"))
                )) {
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, "کد سازمانی سوال موجود نیست"));
                    continue;
                }

                questionFilename = FileUtils.renameFile(EscapeQuizQuestionRepository.FOLDER, questionFilename, null);

                if (questionFilename == null) {
                    errs.put(batchRowErr(rowIdx, "بارگذاری فایل صورت سوال با خطا مواجه شده است"));
                    excepts.put(rowIdx);
                    continue;
                }

                if (answerFilename != null) {
                    answerFilename = FileUtils.renameFile(EscapeQuizQuestionRepository.FOLDER, answerFilename, null);

                    if (answerFilename == null) {
                        errs.put(batchRowErr(rowIdx, "بارگذاری فایل پاسخ سوال با خطا مواجه شده است"));
                        excepts.put(rowIdx);
                        continue;
                    }
                }

                jsonObject.put("question_file", questionFilename);
                jsonObject.put("answer_file", answerFilename);
                jsonObject.put("createdAt", System.currentTimeMillis());

                Document newDoc = new Document();

                for (String str : jsonObject.keySet())
                    newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

                escapeQuizQuestionRepository.insertOne(newDoc);

            } catch (Exception ignore) {
                printException(ignore);
                excepts.put(rowIdx);
                errs.put(batchRowErr(rowIdx, ignore.getMessage()));
            }
        }

        if (excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "تمامی سوالات به درستی به سامانه اضافه شدند"
            );

        return generateSuccessMsg(
                "excepts",
                "بجز ردیف\u200Cهای زیر سایرین به درستی به سامانه اضافه گردیدند. " + excepts,
                new PairValue("errs", errs)
        );
    }

    public static String search(boolean isForAdmin,
                                boolean isSubjectsNeeded,
                                boolean isAuthorsNeeded,
                                Boolean justUnVisible,
                                String organizationId,
                                String organizationLike,
                                ObjectId subjectId,
                                ObjectId lessonId,
                                ObjectId questionId,
                                ObjectId quizId,
                                ObjectId authorId,
                                String level,
                                String kindQuestion,
                                String sortBy) {

        ArrayList<Bson> filters = new ArrayList<>();

        if (!isForAdmin)
            filters.add(eq("visibility", true));
        else if (justUnVisible)
            filters.add(eq("visibility", false));

        if (kindQuestion != null &&
                EnumValidatorImp.isValid(kindQuestion, QuestionType.class)
        )
            filters.add(eq("kind_question", kindQuestion));

        if (level != null &&
                EnumValidatorImp.isValid(level, QuestionLevel.class)
        )
            filters.add(eq("level", level));

        if (questionId != null && ObjectId.isValid(questionId.toString()))
            filters.add(eq("_id", questionId));

        if (authorId != null && ObjectId.isValid(authorId.toString()))
            filters.add(eq("authorId", authorId));

        if (subjectId != null && ObjectId.isValid(subjectId.toString()))
            filters.add(eq("subject_id", subjectId));

        if (organizationId != null)
            filters.add(eq("organization_id", organizationId));

        if (organizationLike != null)
            filters.add(regex("organization_id", Pattern.compile(Pattern.quote(organizationLike), Pattern.CASE_INSENSITIVE)));

        if (lessonId != null && ObjectId.isValid(lessonId.toString())) {
            ArrayList<ObjectId> subjectIds = subjectRepository.findJustIds(eq("lesson._id", lessonId));
            filters.add(in("subject_id", subjectIds));
        }

        //todo : quizId filter
        Bson sortByFilter = null;

        //todo : sort by complete
        if (sortBy != null) {
            switch (sortBy) {
                case "created_at_desc":
                    sortByFilter = Sorts.descending("created_at");
                    break;
                case "created_at_asc":
                    sortByFilter = Sorts.ascending("created_at");
                    break;
            }

        }

        JSONArray jsonArray = Utilities.convertList(
                sortBy == null ?
                        questionRepository.find(filters.size() > 0 ?
                                and(filters) : null, null
                        ) :
                        questionRepository.find(filters.size() > 0 ?
                                        and(filters) : null, null,
                                sortByFilter
                        ), isSubjectsNeeded, isAuthorsNeeded, false,
                false, true, true
        );

        return generateSuccessMsg(
                "data", jsonArray
        );
    }

    public static String getEscapeQuizQuestions(String organizationCode) {

        ArrayList<Document> questions = escapeQuizQuestionRepository.find(
                organizationCode == null || organizationCode.isEmpty() ? null : eq("organization_id", organizationCode),
                null
        );

        return generateSuccessMsg("data", convertEscapeQuestionsList(
                questions, true, true, true
        ));
    }

    public static String subjectQuestions(Boolean isQuestionNeeded,
                                          Integer criticalThresh,
                                          String organizationCode,
                                          ObjectId subjectId,
                                          ObjectId lessonId,
                                          ObjectId gradeId,
                                          boolean isAdmin
    ) {

        ArrayList<Bson> filters = new ArrayList<>();
        ArrayList<Document> docs;

        if (organizationCode != null) {

            Document question = questionRepository.findOne(
                    isAdmin ?
                            eq("organization_id", organizationCode.replaceAll("\\s+", "")) :
                            and(
                                    or(
                                            exists("is_public", false),
                                            eq("is_public", true)
                                    ),
                                    eq("organization_id", organizationCode.replaceAll("\\s+", ""))
                            ),
                    new BasicDBObject("subject_id", true)
            );

            if (question == null)
                return generateSuccessMsg("data", new JSONArray());

            Document doc = subjectRepository.findById(
                    question.getObjectId("subject_id")
            );

            if (doc == null)
                return generateSuccessMsg("data", new JSONArray());

            docs = new ArrayList<>();
            docs.add(doc);
        } else {

            if (subjectId != null)
                filters.add(eq("_id", subjectId));

            if (lessonId != null)
                filters.add(eq("lesson._id", lessonId));

            if (gradeId != null)
                filters.add(eq("grade._id", gradeId));

            if (criticalThresh != null)
                filters.add(or(
                        exists("q_no", false),
                        lte("q_no", criticalThresh)
                ));

            docs = subjectRepository.find(
                    filters.size() == 0 ? null : and(filters),
                    null
            );
        }

        JSONArray jsonArray = new JSONArray();

        for (Document doc : docs) {

            JSONObject jsonObject = new JSONObject()
                    .put("id", doc.getObjectId("_id").toString())
                    .put("qNo", doc.getOrDefault("q_no", 0))
                    .put("subject", new JSONObject()
                            .put("name", doc.getString("name"))
                            .put("id", doc.getObjectId("_id").toString()))
                    .put("lesson", new JSONObject()
                            .put("name", ((Document) doc.get("lesson")).getString("name"))
                            .put("id", ((Document) doc.get("lesson")).getObjectId("_id").toString())
                    )
                    .put("grade", new JSONObject()
                            .put("name", ((Document) doc.get("grade")).getString("name"))
                            .put("id", ((Document) doc.get("grade")).getObjectId("_id").toString())
                    );

            if (isQuestionNeeded != null && isQuestionNeeded) {

                ArrayList<Document> questions;

                if(isAdmin)
                    questions = questionRepository.find(
                            organizationCode == null ?
                                    eq("subject_id", doc.getObjectId("_id")) :
                                    and(
                                            eq("subject_id", doc.getObjectId("_id")),
                                            eq("organization_id", organizationCode)
                                    ), null
                    );
                else
                    questions = questionRepository.find(
                            organizationCode == null ?
                                    and(
                                            eq("subject_id", doc.getObjectId("_id")),
                                            or(
                                                    exists("is_public", false),
                                                    eq("is_public", true)
                                            )
                                    ) :
                                    and(
                                            eq("subject_id", doc.getObjectId("_id")),
                                            eq("organization_id", organizationCode),
                                            or(
                                                    exists("is_public", false),
                                                    eq("is_public", true)
                                            )
                                    ), null
                    );

                jsonObject.put("questions", convertList(
                        questions, false, true,
                        true, true, true, true
                ));
            }

            jsonArray.put(jsonObject);
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static ByteArrayInputStream getQuestionTagsExcel() {
        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> docs = questionTagRepository.find(null, null);
        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("code", doc.getInteger("code"))
                    .put("tag", doc.getString("tag"))
            );
        }
        return Excel.write(jsonArray);
    }

    public static String getTagsKeyVals() {
        JSONArray jsonArray = new JSONArray();
        ArrayList<Document> docs = questionTagRepository.find(null, null);
        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("id", doc.getInteger("code"))
                    .put("name", doc.getString("tag"))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String getAllFlags() {

        JSONArray jsonArray = new JSONArray();
        Document config = getConfig();

        ArrayList<Document> subjects = subjectRepository.find(
                config.containsKey("min_question_for_custom_quiz") ?
                        gt("q_no", config.getInteger("min_question_for_custom_quiz")) :
                        gt("q_no", 0)
                ,
                new BasicDBObject("_id", 1)
                        .append("q_no", 1)
                        .append("q_no_hard", 1)
                        .append("q_no_mid", 1)
                        .append("q_no_easy", 1)
                        .append("name", 1)
                        .append("lesson", 1)
                        .append("grade", 1)
        );

        HashMap<ObjectId, Document> lessons = new HashMap<>();
        HashMap<ObjectId, Document> grades = new HashMap<>();

        for (Document subject : subjects) {

            int qNoEasy = (int) subject.getOrDefault("q_no_easy", 0);
            int qNoMid = (int) subject.getOrDefault("q_no_mid", 0);
            int qNoHard = (int) subject.getOrDefault("q_no_hard", 0);

            ObjectId lessonId = subject.get("lesson", Document.class).getObjectId("_id");
            String lesson = subject.get("lesson", Document.class).getString("name");

            ObjectId gradeId = subject.get("grade", Document.class).getObjectId("_id");
            String grade = subject.get("grade", Document.class).getString("name");

            jsonArray.put(new JSONObject()
                    .put("limitEasy", qNoEasy)
                    .put("limitMid", qNoMid)
                    .put("limitHard", qNoHard)
                    .put("name", subject.getString("name"))
                    .put("desc", subject.getString("name") + " در " +
                            lesson + " در " + grade)
                    .put("id", subject.getObjectId("_id").toString())
                    .put("lessonId", lessonId.toString())
                    .put("section", "subject")
            );

            if (lessons.containsKey(lessonId)) {
                lessons.get(lessonId).put("q_no_easy", lessons.get(lessonId).getInteger("q_no_easy") + qNoEasy);
                lessons.get(lessonId).put("q_no_mid", lessons.get(lessonId).getInteger("q_no_mid") + qNoMid);
                lessons.get(lessonId).put("q_no_hard", lessons.get(lessonId).getInteger("q_no_hard") + qNoHard);
            } else {
                lessons.put(lessonId,
                        new Document("name", lesson)
                                .append("q_no_easy", qNoEasy)
                                .append("q_no_mid", qNoMid)
                                .append("q_no_hard", qNoHard)
                                .append("grade_id", gradeId.toString())
                                .append("desc", lesson + " در " + grade)
                );
            }

            if (grades.containsKey(gradeId)) {
                grades.get(gradeId)
                        .put("q_no_easy", grades.get(gradeId).getInteger("q_no_easy") + qNoEasy);
                grades.get(gradeId)
                        .put("q_no_mid", grades.get(gradeId).getInteger("q_no_mid") + qNoMid);
                grades.get(gradeId)
                        .put("q_no_hard", grades.get(gradeId).getInteger("q_no_hard") + qNoHard);
            } else {
                grades.put(gradeId,
                        new Document("name",
                                subject.get("grade", Document.class).getString("name")
                        ).append("q_no_easy", qNoEasy)
                                .append("q_no_mid", qNoMid)
                                .append("q_no_hard", qNoHard)
                );
            }

        }

        for (ObjectId lessonId : lessons.keySet()) {
            jsonArray.put(new JSONObject()
                    .put("limitEasy", lessons.get(lessonId).getInteger("q_no_easy"))
                    .put("limitMid", lessons.get(lessonId).getInteger("q_no_mid"))
                    .put("limitHard", lessons.get(lessonId).getInteger("q_no_hard"))
                    .put("name", lessons.get(lessonId).getString("name"))
                    .put("gradeId", lessons.get(lessonId).getString("grade_id"))
                    .put("id", lessonId.toString())
                    .put("desc", lessons.get(lessonId).getString("desc"))
                    .put("section", "lesson")
            );
        }

        for (ObjectId gradeId : grades.keySet()) {
            jsonArray.put(new JSONObject()
                    .put("limitEasy", grades.get(gradeId).getInteger("q_no_easy"))
                    .put("limitHard", grades.get(gradeId).getInteger("q_no_hard"))
                    .put("limitMid", grades.get(gradeId).getInteger("q_no_mid"))
                    .put("name", grades.get(gradeId).getString("name"))
                    .put("desc", grades.get(gradeId).getString("name"))
                    .put("id", gradeId.toString())
                    .put("section", "grade")
            );
        }

        ArrayList<Document> docs = questionTagRepository.find(
                config.containsKey("min_question_for_custom_quiz") ?
                        gt("q_no", config.getInteger("min_question_for_custom_quiz")) :
                        gt("q_no", 0)
                , null);

        for (Document doc : docs) {
            jsonArray.put(new JSONObject()
                    .put("id", doc.getInteger("code"))
                    .put("name", doc.getString("tag"))
                    .put("desc", doc.getString("tag"))
                    .put("limitEasy", doc.getOrDefault("q_no_easy", 1))
                    .put("limitMid", doc.getOrDefault("q_no_mid", 1))
                    .put("limitHard", doc.getOrDefault("q_no_hard", 1))
                    .put("section", "tag")
            );
        }

//        ArrayList<Document> authors = authorRepository.find(
//                config.containsKey("min_question_for_custom_quiz") ?
//                        gt("q_no", config.getInteger("min_question_for_custom_quiz")) :
//                        gt("q_no", 0)
//                , null);
//
//        for (Document doc : authors) {
//            jsonArray.put(new JSONObject()
//                    .put("id", doc.getObjectId("_id").toString())
//                    .put("name", doc.getString("name"))
//                    .put("desc", doc.getString("name"))
//                    .put("limitEasy", doc.getOrDefault("q_no_easy", 1))
//                    .put("limitMid", doc.getOrDefault("q_no_mid", 1))
//                    .put("limitHard", doc.getOrDefault("q_no_hard", 1))
//                    .put("section", "author")
//            );
//        }

        return generateSuccessMsg("data", jsonArray);
    }

    public static String checkAvailableQuestions(
            ObjectId userId,
            String tag,
            ObjectId gradeId,
            ObjectId lessonId,
            ObjectId subjectId,
            int questionsNo,
            String level,
            String author
    ) {

        try {

            ArrayList<Bson> filters = fetchFilter(tag, gradeId,
                    lessonId, subjectId, level, author
            );

            if (questionRepository.count(and(filters)) < questionsNo)
                return generateErr("تعداد سوالات سامانه کمتر از فیلتر انتخابی شما می باشد.");

        } catch (Exception x) {
            return generateErr(x.getMessage());
        }


        return JSON_OK;
    }
}
