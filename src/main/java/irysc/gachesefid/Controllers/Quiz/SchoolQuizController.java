package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.Utility.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class SchoolQuizController {

    final static String[] mandatoryFields = {
            "start", "end", "isOnline", "showResultsAfterCorrection",
    };

    final static String[] forbiddenFields = {
            "topStudentsGiftCoin", "topStudentsGiftMoney",
            "topStudentsCount", "startRegistry", "price",
            "capacity", "endRegistry",
    };


    public static String addBatchQuestions(MultipartFile file, ObjectId quizId, ObjectId userId) {


        Document quiz = null;
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
            // 4- kindQuestion[test, short_answer, multi_sentence]
            // 5- needed time 6- answer
            // 7- level[easy, mid, hard] 8- answer file name : optional
            // 9- sentencesCount : optional 10- telorance : optional
            // 11- choicesCount : optional

            JSONArray excepts = new JSONArray();
            int rowIdx = 0;

            JSONArray errs = new JSONArray();

            boolean addAtLeastOne = false;

            for (Row row : rows) {

                rowIdx++;

                try {

                    if(row.getCell(1) == null)
                        break;

                    if (row.getLastCellNum() < 8) {
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
                    Cell cell = row.getCell(8);

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

//                    checkAnswer(jsonObject);
                    jsonObject.put("tags", tags);

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
                    "بجز ردیف های زیر سایرین به درستی به سامانه اضافه گردیدند. " + excepts,
                    new PairValue("errs", errs)
            );

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

}
