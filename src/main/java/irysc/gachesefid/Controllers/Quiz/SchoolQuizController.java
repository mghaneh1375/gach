package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Question.Utilities;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.KindQuiz;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Controllers.Question.Utilities.checkAnswer;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.Utility.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class SchoolQuizController {


    public static String addBatchQuestions(MultipartFile file, ObjectId quizId, ObjectId userId) {

        Document quiz;

        try {
            quiz = hasAccess(schoolQuizRepository, userId, quizId);

            if(quiz.getString("status").equals("finish"))
                return generateErr("آزمون موردنظر نهایی شده است و امکان افزودن/ویرایش سوالات وجود ندارد");

            if(quiz.getBoolean("database"))
                return generateErr("امکان آپلود سوال برای این آزمون وجود ندارد");

            Document questions = quiz.get("questions", Document.class);
            int currQSize = 0;

            if(questions.containsKey("_ids"))
                currQSize = questions.getList("_ids", ObjectId.class).size();

            String filename = FileUtils.uploadTempFile(file);
            ArrayList<Row> rows = Excel.read(filename);
            FileUtils.removeTempFile(filename);

            if (rows == null)
                return generateErr("File is not valid");

            rows.remove(0);

            Document config = getConfig();

            int maxQ = (int)config.getOrDefault("max_question_per_quiz", 20);
            if (maxQ < currQSize + rows.size())
                return generateErr("حداکثر تعداد سوال در هر آزمون می تواند " + maxQ + " باشد");

            // excel format:
            // 1- row no 2- question file name 3- subject id
            // 4- needed time 5- answer
            // 6- level[easy, mid, hard]
            // 7- choicesCount : optional
            // 8- answer file name : optional

            JSONArray excepts = new JSONArray();
            int rowIdx = 0;

            JSONArray errs = new JSONArray();

            List<WriteModel<Document>> writes = new ArrayList<>();
            List<Document> addedQuestions = new ArrayList<>();

            for (Row row : rows) {

                rowIdx++;

                try {

                    if(row.getCell(1) == null)
                        break;

                    if (row.getLastCellNum() < 7) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "تعداد ستون ها نامعتیر است."));
                        continue;
                    }

                    String questionFilename = row.getCell(1).getStringCellValue();
                    if (!FileUtils.checkExist(questionFilename, "school_quizzes/questions")) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "فایل سوال موجود نیست."));
                        continue;
                    }

                    String answerFilename = null;
                    Cell cell = row.getCell(7);

                    if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                        answerFilename = cell.getStringCellValue();
                        if (!FileUtils.checkExist(answerFilename, "school_quizzes/questions")) {
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

                    String level = row.getCell(5).getStringCellValue();
                    if (!EnumValidatorImp.isValid(level, QuestionLevel.class)) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "سطح سختی نامعتیر است."));
                        continue;
                    }

                    jsonObject.put("level", level);
                    jsonObject.put("neededTime", (int) row.getCell(3).getNumericCellValue());

                    cell = row.getCell(4);
                    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        if (Math.floor(cell.getNumericCellValue()) == cell.getNumericCellValue())
                            jsonObject.put("answer", (int) cell.getNumericCellValue());
                        else
                            jsonObject.put("answer", cell.getNumericCellValue());
                    } else
                        jsonObject.put("answer", row.getCell(4).getStringCellValue());


                    cell = row.getCell(6);
                    if(cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "تعداد گزینه نامعتبر است"));
                    }

                    jsonObject.put("choicesCount", (int) cell.getNumericCellValue());

                    checkAnswer(jsonObject);

                    questionFilename = FileUtils.renameFile("school_quizzes/questions", questionFilename, null);

                    if (questionFilename == null) {
                        errs.put(batchRowErr(rowIdx, "بارگذاری فایل صورت سوال با خطا مواجه شده است"));
                        excepts.put(rowIdx);
                        continue;
                    }

                    if (answerFilename != null) {
                        answerFilename = FileUtils.renameFile("school_quizzes/questions", answerFilename, null);

                        if (answerFilename == null) {
                            errs.put(batchRowErr(rowIdx, "بارگذاری فایل پاسخ سوال با خطا مواجه شده است"));
                            excepts.put(rowIdx);
                            continue;
                        }
                    }

                    jsonObject.put("question_file", questionFilename);
                    jsonObject.put("answer_file", answerFilename);
                    jsonObject.put("createdAt", System.currentTimeMillis());

                    Document newDoc = new Document("subject_id", subjectId)
                            .append("user_id", userId);

                    for (String str : jsonObject.keySet())
                        newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

                    addedQuestions.add(newDoc);
                    writes.add(new InsertOneModel<>(newDoc));

                } catch (Exception ignore) {
                    printException(ignore);
                    excepts.put(rowIdx);
                    errs.put(batchRowErr(rowIdx, ignore.getMessage()));
                }
            }

            if(writes.size() > 0) {

                schoolQuestionRepository.bulkWrite(writes);

                List<Double> marks = questions.containsKey("marks") ? questions.getList("marks", Double.class) : new ArrayList<>();
                List<ObjectId> ids = questions.containsKey("_ids") ? questions.getList("_ids", ObjectId.class) : new ArrayList<>();

                for (Document addedQuestion : addedQuestions) {
                    marks.add(3.0);
                    ids.add(addedQuestion.getObjectId("_id"));
                }

                byte[] answersByte;

                if (questions.containsKey("answers"))
                    answersByte = questions.get("answers", Binary.class).getData();
                else
                    answersByte = new byte[0];

                for (Document question : addedQuestions) {
                    answersByte = Utility.addAnswerToByteArr(answersByte, "test",
                            new PairValue(question.getInteger("choices_count"), question.get("answer"))
                    );
                }

                questions.put("answers", answersByte);

                questions.put("marks", marks);
                questions.put("_ids", ids);
                quiz.put("questions", questions);

                schoolQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);
            }

            if (excepts.length() == 0)
                return generateSuccessMsg(
                        "excepts", "تمامی سوالات به درستی به آزمون اضافه شدند"
                );

            return generateSuccessMsg(
                    "excepts",
                    "بجز ردیف های زیر سایرین به درستی به آزمون اضافه گردیدند. " + excepts,
                    new PairValue("errs", errs)
            );

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

}
