package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.DB.HWRepository;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.HWAnswerType;
import irysc.gachesefid.Models.OffCodeSections;
import irysc.gachesefid.Models.OffCodeTypes;
import irysc.gachesefid.Models.QuestionLevel;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Controllers.Finance.PayPing.goToPayment;
import static irysc.gachesefid.Controllers.Question.Utilities.checkAnswer;
import static irysc.gachesefid.Controllers.Quiz.QuizController.payFromWallet;
import static irysc.gachesefid.Controllers.Quiz.Utility.hasAccess;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class SchoolQuizController {

    public static String addBatchQuestions(MultipartFile file, ObjectId quizId, ObjectId userId) {

        Document quiz;

        try {
            quiz = hasAccess(schoolQuizRepository, userId, quizId);

            if (quiz.getString("status").equals("finish"))
                return generateErr("آزمون موردنظر نهایی شده است و امکان افزودن/ویرایش سوالات وجود ندارد");

            if (quiz.getBoolean("database"))
                return generateErr("امکان آپلود سوال برای این آزمون وجود ندارد");

            Document questions = quiz.get("questions", Document.class);
            int currQSize = 0;

            if (questions.containsKey("_ids"))
                currQSize = questions.getList("_ids", ObjectId.class).size();

            String filename = FileUtils.uploadTempFile(file);
            ArrayList<Row> rows = Excel.read(filename);
            FileUtils.removeTempFile(filename);

            if (rows == null)
                return generateErr("File is not valid");

            rows.remove(0);

            Document config = getConfig();

            int maxQ = (int) config.getOrDefault("max_question_per_quiz", 20);
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

                    if (row.getCell(1) == null)
                        break;

                    if (row.getLastCellNum() < 7) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "تعداد ستون ها نامعتیر است."));
                        continue;
                    }

                    String questionFilename = row.getCell(1).getStringCellValue();
                    if (!FileUtils.checkExist(questionFilename, "SCHOOL_HWzes/questions")) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "فایل سوال موجود نیست."));
                        continue;
                    }

                    String answerFilename = null;
                    Cell cell = row.getCell(7);

                    if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                        answerFilename = cell.getStringCellValue();
                        if (!FileUtils.checkExist(answerFilename, "SCHOOL_HWzes/questions")) {
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
                    if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                        excepts.put(rowIdx);
                        errs.put(batchRowErr(rowIdx, "تعداد گزینه نامعتبر است"));
                    }

                    jsonObject.put("choicesCount", (int) cell.getNumericCellValue());

                    checkAnswer(jsonObject);

                    questionFilename = FileUtils.renameFile("SCHOOL_HWzes/questions", questionFilename, null);

                    if (questionFilename == null) {
                        errs.put(batchRowErr(rowIdx, "بارگذاری فایل صورت سوال با خطا مواجه شده است"));
                        excepts.put(rowIdx);
                        continue;
                    }

                    if (answerFilename != null) {
                        answerFilename = FileUtils.renameFile("SCHOOL_HWzes/questions", answerFilename, null);

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

            if (writes.size() > 0) {

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

    public static String createHW(ObjectId userId, JSONObject jsonObject, boolean isAdvisor) {

        if (jsonObject.getInt("maxUploadSize") > 20)
            return generateErr("حداکثر حجم مجاز برای آپلود 20 مگابایت می باشد");

        if (jsonObject.getLong("start") < System.currentTimeMillis())
            return generateErr("زمان شروع تمرین باید از امروز بزرگتر باشد");

        if (jsonObject.getLong("end") < jsonObject.getLong("start"))
            return generateErr("زمان پایان تمرین باید بزرگ تر از زمان آغاز آن باشد");

        if (jsonObject.has("delayEnd") && jsonObject.getLong("delayEnd") < jsonObject.getLong("end"))
            return generateErr("زمان پایان ثبت تمرین با تاخیر باید بزرگ تر از زمان اتمام آن باشد");

        if (jsonObject.has("delayPenalty") && jsonObject.getInt("delayPenalty") > 100)
            return generateErr("حداکثر جریمه روزانه می تواند کسر 100% نمره باشد");

        jsonObject.put("isForAdvisor", isAdvisor);

        Document newDoc = new Document();

        for (String key : jsonObject.keySet()) {

            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    jsonObject.get(key)
            );
        }

        newDoc.put("students", new ArrayList<>());
        newDoc.put("attaches", new ArrayList<>());
        newDoc.put("created_by", userId);
        newDoc.put("registered", 0);
        newDoc.put("created_at", System.currentTimeMillis());

        newDoc.put("status", "init");
        newDoc.put("visibility", false);

        if(newDoc.containsKey("delay_penalty") && newDoc.get("delay_penalty").toString().equalsIgnoreCase("0"))
            newDoc.put("delay_penalty", 0);

        hwRepository.insertOne(newDoc);

        return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                "quiz", new RegularQuizController()
                        .convertHWDocToJSON(newDoc, false, null)
        );
    }

    public static String updateHW(ObjectId userId, ObjectId hwId, JSONObject jsonObject) {

        try {

            Document hw = hasAccess(hwRepository, userId, hwId);

            if (jsonObject.getInt("maxUploadSize") > 20)
                return generateErr("حداکثر حجم مجاز برای آپلود 20 مگابایت می باشد");

            if (jsonObject.getLong("start") != hw.getLong("start") &&
                    jsonObject.getLong("start") < System.currentTimeMillis())
                return generateErr("زمان شروع تمرین باید از امروز بزرگتر باشد");

            if (jsonObject.getLong("end") != hw.getLong("end") &&
                    jsonObject.getLong("end") < jsonObject.getLong("start"))
                return generateErr("زمان پایان تمرین باید بزرگ تر از زمان آغاز آن باشد");

            if (!jsonObject.has("delayEnd") && hw.containsKey("delay_end"))
                jsonObject.put("delayEnd", hw.getLong("delay_end"));

            if (jsonObject.has("delayEnd") && jsonObject.getLong("delayEnd") < jsonObject.getLong("end"))
                return generateErr("زمان پایان ثبت تمرین با تاخیر باید بزرگ تر از زمان اتمام آن باشد");

            for (String key : jsonObject.keySet()) {

                hw.put(
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                        jsonObject.get(key)
                );
            }

            hwRepository.replaceOne(hwId, hw);
            return JSON_OK;

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String copy(ObjectId userId, ObjectId quizId, JSONObject data) {

        try {

            Document quiz = hasAccess(schoolQuizRepository, userId, quizId);

            Document newDoc = new Document("_id", new ObjectId())
                    .append("created_at", System.currentTimeMillis())
                    .append("created_by", userId)
                    .append("status", "init")
                    .append("visibility", false)
                    .append("title", data.getString("title"))
                    .append("start", data.getLong("start"))
                    .append("end", data.getLong("end"))
                    .append("launch_mode", data.getString("launchMode"))
                    .append("database", quiz.get("database"))
                    .append("duration", quiz.get("duration"))
                    .append("desc_after", quiz.get("desc_after"))
                    .append("desc", quiz.get("desc"))
                    .append("minus_mark", quiz.get("minus_mark"))
                    .append("show_results_after_correction", quiz.get("show_results_after_correction"))
                    .append("removed_questions", quiz.get("removed_questions"))
                    .append("attaches", quiz.get("attaches"))
                    .append("questions", quiz.get("questions"))
                    .append("mode", quiz.get("mode"))
                    .append("tags", quiz.get("tags"));

            if (data.getBoolean("copyStudents")) {
                newDoc.append("students", quiz.get("students"))
                        .append("registered", quiz.get("registered"));
            } else {
                newDoc.append("students", new ArrayList<>())
                        .append("registered", 0);
            }

            schoolQuizRepository.insertOne(newDoc);

            return generateSuccessMsg("data",
                    new RegularQuizController().convertSchoolDocToJSON(newDoc, true, true, true));

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

    private static PairValue isHWReadyForPay(Document hw) throws InvalidFieldsException {

        int studentsCount = hw.getList("students", Document.class).size();
        if (studentsCount == 0)
            throw new InvalidFieldsException("لطفا ابتدا دانش آموز/دانش آموزان خود را به تمرین اضافه کنید");

        Document config = getConfig();
        int maxStd = (int) config.getOrDefault("max_student_quiz_per_day", 10);

        if (maxStd < studentsCount)
            throw new InvalidFieldsException("حداکثر تعداد دانش آموز در یک تمرین می تواند " + maxStd + " باشد");

        return new PairValue(studentsCount,
                config.getOrDefault("hw_per_student_price", 1000)
        );
    }


    public static String recp(ObjectId hwId, ObjectId userId) {

        try {
            Document quiz = hasAccess(hwRepository, userId, hwId);

            PairValue p = isHWReadyForPay(quiz);
            int studentsCount = (int) p.getKey();
            JSONArray jsonArray = new JSONArray();
            int price = (int) p.getValue();
            jsonArray.put(new JSONObject()
                    .put("price", price)
                    .put("totalPrice", studentsCount * price)
                    .put("count", studentsCount)
            );

            return generateSuccessMsg("data", jsonArray);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }


    public static String finalizeHW(ObjectId hwId, ObjectId userId,
                                      String off, double money) {

        try {

            Document hw = hasAccess(hwRepository, userId, hwId);

            PairValue p = isHWReadyForPay(hw);
            int studentsCount = (int) p.getKey();

            int price = (int) p.getValue();
            int total = studentsCount * price;

            long curr = System.currentTimeMillis();
            Document offDoc;

            if (off == null)
                offDoc = findAccountOff(
                        userId, curr, OffCodeSections.SCHOOL_HW.getName()
                );
            else {

                offDoc = validateOffCode(
                        off, userId, curr,
                        OffCodeSections.SCHOOL_HW.getName()
                );

                if (offDoc == null)
                    return generateErr("کد تخفیف وارد شده معتبر نمی باشد.");
            }

            double offAmount = 0;
            double shouldPayDouble = total * 1.0;

            if (offDoc != null) {
                offAmount +=
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount")
                ;
                shouldPayDouble = total - offAmount;
            }

            int shouldPay = (int) shouldPayDouble;

            if (shouldPay - money <= 100) {

                if (shouldPay > 100)
                    money = payFromWallet(shouldPay, money, userId);

                Document doc = new Document("user_id", userId)
                        .append("amount", 0)
                        .append("account_money", shouldPay)
                        .append("created_at", curr)
                        .append("status", "success")
                        .append("section", OffCodeSections.SCHOOL_HW.getName())
                        .append("products", hwId);

                if (offDoc != null) {
                    doc.append("off_code", offDoc.getObjectId("_id"));
                    doc.append("off_amount", (int) offAmount);
                }

                ObjectId tId = transactionRepository.insertOneWithReturnId(doc);
                hw.put("status", "finish");
                hwRepository.replaceOne(hwId, hw);

                if (offDoc != null) {

                    BasicDBObject update;

                    if (offDoc.containsKey("is_public") &&
                            offDoc.getBoolean("is_public")
                    ) {
                        List<ObjectId> students = offDoc.getList("students", ObjectId.class);
                        students.add(userId);
                        update = new BasicDBObject("students", students);
                    } else {

                        update = new BasicDBObject("used", true)
                                .append("used_at", curr)
                                .append("used_section", OffCodeSections.SCHOOL_HW.getName())
                                .append("used_for", hwId);
                    }

                    offcodeRepository.updateOne(
                            offDoc.getObjectId("_id"),
                            new BasicDBObject("$set", update)
                    );
                }

                return irysc.gachesefid.Utility.Utility.generateSuccessMsg(
                        "action", "success",
                        new PairValue("refId", money),
                        new PairValue("transactionId", tId.toString())
                );
            }

            long orderId = Math.abs(new Random().nextLong());
            while (transactionRepository.exist(
                    eq("order_id", orderId)
            )) {
                orderId = Math.abs(new Random().nextLong());
            }

            Document doc =
                    new Document("user_id", userId)
                            .append("account_money", money)
                            .append("amount", (int) (shouldPay - money))
                            .append("created_at", curr)
                            .append("status", "init")
                            .append("order_id", orderId)
                            .append("products", hwId)
                            .append("section", OffCodeSections.SCHOOL_HW.getName());

            if (off != null) {
                doc.append("off_code", offDoc.getObjectId("_id"));
                doc.append("off_amount", (int) offAmount);
            }

            return goToPayment((int) (shouldPay - money), doc);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }
    }

    public static String getTotalHWPrice(ObjectId hwId, ObjectId userId, double money) {

        try {

            Document hw = hasAccess(hwRepository, userId, hwId);
            if(hw.getString("status").equalsIgnoreCase("finish"))
                return generateErr("این تمرین قبلا نهایی شده است");

            PairValue p = isHWReadyForPay(hw);

            int studentsCount = (int) p.getKey();
            int price = (int) p.getValue();
            int total = studentsCount * price;

            long curr = System.currentTimeMillis();

            Document offDoc = findAccountOff(
                    userId, curr, OffCodeSections.SCHOOL_HW.getName()
            );

            JSONObject jsonObject = new JSONObject()
                    .put("total", total);

            double shouldPayDouble = total;

            if (offDoc != null) {

                double offAmount =
                        offDoc.getString("type").equals(OffCodeTypes.PERCENT.getName()) ?
                                shouldPayDouble * offDoc.getInteger("amount") / 100.0 :
                                offDoc.getInteger("amount");

                jsonObject.put("off", offAmount);
                shouldPayDouble -= offAmount;
            } else
                jsonObject.put("off", 0);

            if (shouldPayDouble > 0) {
                if (money >= shouldPayDouble) {
                    jsonObject.put("usedFromWallet", shouldPayDouble);
                    shouldPayDouble = 0;
                } else {
                    jsonObject.put("usedFromWallet", money);
                    shouldPayDouble -= money;
                }
            } else
                jsonObject.put("usedFromWallet", 0);

            shouldPayDouble = Math.max(0, shouldPayDouble);
            jsonObject.put("shouldPay", (int) shouldPayDouble);

            return generateSuccessMsg("data", jsonObject);

        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

    }

    public static String setAnswer(ObjectId hwId, ObjectId studentId, MultipartFile file) {

        Document hw = hwRepository.findById(hwId);

        if(hw == null)
            return JSON_NOT_VALID_ID;

        if(!hw.getBoolean("visibility") ||
                !hw.getString("status").equalsIgnoreCase("finish") ||
                !hw.containsKey("students")
        )
            return JSON_NOT_ACCESS;

        long curr = System.currentTimeMillis();
        long end = hw.containsKey("delay_end") ? hw.getLong("delay_end") : hw.getLong("end");

        if(hw.getLong("start") > curr || end < curr)
            return generateErr("در زمان بارگذاری تمرین قرار نداریم");

        List<Document> students = hw.getList("students", Document.class);
        Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                students, "_id", studentId
        );

        if(stdDoc == null)
            return JSON_NOT_ACCESS;

        if(file.getSize() > hw.getInteger("max_upload_size") * ONE_MB)
            return generateErr("حداکثر حجم قابل بارگذاری در این قسمت " + hw.getInteger("max_upload_size") + " مگابایت می باشد");

        String fileType = FileUtils.uploadDocOrMultimediaFile(file);
        String answerType = hw.getString("answer_type");

        if(fileType == null ||
                (answerType.equalsIgnoreCase(HWAnswerType.PDF.getName()) && !fileType.equals("pdf")) ||
                (answerType.equalsIgnoreCase(HWAnswerType.WORD.getName()) && !fileType.equals("word")) ||
                (answerType.equalsIgnoreCase(HWAnswerType.POWERPOINT.getName()) && !fileType.equals("powerpoint")) ||
                (answerType.equalsIgnoreCase(HWAnswerType.IMAGE.getName()) && !fileType.equals("image")) ||
                (answerType.equalsIgnoreCase(HWAnswerType.VIDEO.getName()) && !fileType.equals("video")) ||
                (answerType.equalsIgnoreCase(HWAnswerType.AUDIO.getName()) && !fileType.equals("voice"))
        )
            return generateErr("فرمت فایل موردنظر معتبر نمی باشد. باید یک فایل " + answerType + " آپلود نمایید.");

        String filename = FileUtils.uploadFile(file, HWRepository.FOLDER);
        if(filename == null)
            return JSON_NOT_UNKNOWN;

        if(stdDoc.containsKey("filename")) {

            String ext = stdDoc.getString("filename").substring(
                    stdDoc.getString("filename").lastIndexOf(".")
            );

            FileUtils.removeFile(stdDoc.getString("filename").split("__")[0] + ext, HWRepository.FOLDER);
        }

        stdDoc.put("filename", filename.substring(0, filename.lastIndexOf(".")) + "__" + file.getOriginalFilename());
        stdDoc.put("upload_at", curr);

        hwRepository.replaceOne(hwId, hw);

        return StudentQuizController.myHW(studentId, hwId);
    }

    public static PairValue downloadHw(ObjectId hwId, ObjectId studentId) {

        Document hw = hwRepository.findById(hwId);

        if(hw == null)
            return null;

        if(!hw.getBoolean("visibility") ||
                !hw.containsKey("students")
        )
            return null;

        List<Document> students = hw.getList("students", Document.class);
        Document stdDoc = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                students, "_id", studentId
        );

        if(stdDoc == null || !stdDoc.containsKey("filename"))
            return null;

        String filename = stdDoc.getString("filename");
        String[] splited = filename.split("__");
        String ext = filename.substring(filename.lastIndexOf("."));

        return new PairValue(
                new File(
                        DEV_MODE ?
                                FileUtils.uploadDir_dev + HWRepository.FOLDER + "/" + splited[0] + ext :
                                FileUtils.uploadDir + HWRepository.FOLDER + "/" + splited[0] + ext
                ),
                splited[0] + ext
        );
    }
}
