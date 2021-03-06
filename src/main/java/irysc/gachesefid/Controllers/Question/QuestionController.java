package irysc.gachesefid.Controllers.Question;

import com.google.common.base.CaseFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Sorts;
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

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;
import static irysc.gachesefid.Utility.Utility.printException;

public class QuestionController extends Utilities {

    // get answer from db should always be general because input type in object

    public static String addQuestion(ObjectId subjectId,
                                     JSONObject jsonObject) {

        jsonObject.put("subjectId", subjectId);

        try {
            checkAnswer(jsonObject);
        } catch (InvalidFieldsException e) {
            return generateErr(e.getMessage());
        }

        Document newDoc = new Document("visibility", false);

        for (String str : jsonObject.keySet())
            newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        return questionRepository.insertOneWithReturn(newDoc);
    }

    public static JSONObject convertDocToJSON(Document doc) {
        JSONObject jsonObject = new JSONObject()
                .put("id", doc.getObjectId("_id").toString())
                .put("organizationId", doc.getString("organization_id"));

        return jsonObject;
    }

    public static String setQuestionFiles(ObjectId questionId,
                                          MultipartFile questionFile,
                                          MultipartFile answerFile) {

        Document question = questionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if (questionFile != null && questionFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("???????????? ?????? ???????? ???????? ?????????? " + MAX_QUESTION_FILE_SIZE + " ???? ????????.");

        if (answerFile != null && answerFile.getSize() > MAX_QUESTION_FILE_SIZE)
            return generateErr("???????????? ?????? ???????? ???????? ?????????? " + MAX_QUESTION_FILE_SIZE + " ???? ????????.");

        String questionFilename = null;
        if (questionFile != null) {
            questionFilename = FileUtils.uploadImageFile(questionFile);
            if (questionFilename == null)
                return generateErr("???????? ?????????????? ???????? ???????? ???????? ?????????? ?????? ????????.");
        }

        String answerFilename = null;
        if (answerFile != null) {
            answerFilename = FileUtils.uploadImageFile(answerFile);
            if (answerFilename == null)
                return generateErr("???????? ?????????????? ???????? ???????? ???????? ?????????? ?????? ????????.");
        }

        if (questionFilename != null) {

            questionFilename = FileUtils.uploadFile(questionFile, QuestionRepository.FOLDER);

            if (questionFilename == null)
                return JSON_UNKNOWN_UPLOAD_FILE;
        }

        if (answerFilename != null) {

            answerFilename = FileUtils.uploadFile(answerFile, QuestionRepository.FOLDER);

            if (answerFilename == null) {

                if (questionFilename != null)
                    FileUtils.removeFile(questionFilename, QuestionRepository.FOLDER);

                return JSON_UNKNOWN_UPLOAD_FILE;
            }
        }

        if (questionFilename != null)
            question.put("question_file", questionFilename);

        if (answerFilename != null)
            question.put("answer_file", answerFilename);

        if (question.containsKey("question_file"))
            question.put("visibility", true);

        return JSON_OK;
    }

    public static String updateQuestion(ObjectId questionId,
                                        JSONObject jsonObject) {

        Document question = questionRepository.findById(questionId);
        if (question == null)
            return JSON_NOT_VALID_ID;

        if (jsonObject.has("visibility") &&
                jsonObject.getBoolean("visibility") &&
                !question.containsKey("question_file")
        )
            return generateErr("???????? ???????? ???????????? ???????? ???????? ?????????? ???????? ???????? ???????? ???????? ???? ?????????? ????????????.");


        if (!jsonObject.has("kindQuestion"))
            jsonObject.put("kindQuestion", question.getString("kind_question"));

        if (jsonObject.getString("kindQuestion").equals(
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
                "?????? ?????????? ???????? ?????? ???????? ???? ?????????? ???? ???????????? ?????? ????????.",
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

        for (Row row : rows) {

            rowIdx++;

            try {

                if (row.getLastCellNum() < 9) {
                    excepts.put(rowIdx);
                    continue;
                }

                String questionFilename = row.getCell(1).getStringCellValue();
                if (!FileUtils.checkExist(questionFilename, QuestionRepository.FOLDER)) {
                    excepts.put(rowIdx);
                    continue;
                }

                String answerFilename = null;
                Cell cell = row.getCell(9);

                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                    answerFilename = cell.getStringCellValue();
                    if (!FileUtils.checkExist(answerFilename, QuestionRepository.FOLDER)) {
                        excepts.put(rowIdx);
                        continue;
                    }
                }

                if (!ObjectIdValidator.isValid(row.getCell(2).getStringCellValue()) ||
                        !ObjectIdValidator.isValid(row.getCell(3).getStringCellValue())
                ) {
                    excepts.put(rowIdx);
                    continue;
                }

                JSONObject jsonObject = new JSONObject();

                jsonObject.put("subjectId", row.getCell(2).getStringCellValue());
                jsonObject.put("authorId", row.getCell(3).getStringCellValue());

                String kindQuestion = row.getCell(4).getStringCellValue();
                if (!EnumValidatorImp.isValid(kindQuestion, QuestionType.class)) {
                    excepts.put(rowIdx);
                    continue;
                }
                jsonObject.put("kindQuestion", kindQuestion);


                String level = row.getCell(8).getStringCellValue();
                if (!EnumValidatorImp.isValid(level, QuestionLevel.class)) {
                    excepts.put(rowIdx);
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
                    jsonObject.put("neededLines", (int) cell.getNumericCellValue());

                checkAnswer(jsonObject);

                questionFilename = FileUtils.renameFile(QuestionRepository.FOLDER, questionFilename, null);

                if (questionFilename == null) {
                    excepts.put(rowIdx);
                    continue;
                }

                if (answerFilename != null) {
                    answerFilename = FileUtils.renameFile(QuestionRepository.FOLDER, answerFilename, null);

                    if (answerFilename == null) {
                        excepts.put(rowIdx);
                        continue;
                    }
                }

                jsonObject.put("question_file", questionFilename);
                jsonObject.put("answer_file", answerFilename);
                jsonObject.put("visibility", true);
                jsonObject.put("createdAt", System.currentTimeMillis());

                Document newDoc = new Document();

                for (String str : jsonObject.keySet())
                    newDoc.append(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

                questionRepository.insertOne(newDoc);

            } catch (Exception ignore) {
                printException(ignore);
                excepts.put(rowIdx);
            }
        }

        if(excepts.length() == 0)
            return generateSuccessMsg(
                    "excepts", "?????????? ???????????? ???? ?????????? ???? ???????????? ?????????? ????????"
            );

        return generateSuccessMsg(
                "excepts",
                "?????? ???????? ?????? ?????? ???????????? ???? ?????????? ???? ???????????? ?????????? ??????????????. " + excepts
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

        if(!isForAdmin)
            filters.add(eq("visibility", true));
        else if(justUnVisible)
            filters.add(eq("visibility", false));

        if(kindQuestion != null &&
                EnumValidatorImp.isValid(kindQuestion, QuestionType.class)
        )
            filters.add(eq("kind_question", kindQuestion));

        if(level != null &&
                EnumValidatorImp.isValid(level, QuestionLevel.class)
        )
            filters.add(eq("level", level));

        if(questionId != null && ObjectId.isValid(questionId.toString()))
            filters.add(eq("_id", questionId));

        if(authorId != null && ObjectId.isValid(authorId.toString()))
            filters.add(eq("authorId", authorId));

        if(subjectId != null && ObjectId.isValid(subjectId.toString()))
            filters.add(eq("subject_id", subjectId));

        if(organizationId != null)
            filters.add(eq("organization_id", organizationId));

        if(organizationLike != null)
            filters.add(regex("organization_id", Pattern.compile(Pattern.quote(organizationLike), Pattern.CASE_INSENSITIVE)));

        if(lessonId != null && ObjectId.isValid(lessonId.toString())) {
            ArrayList<ObjectId> subjectIds = subjectRepository.findJustIds(eq("lesson._id", lessonId));
            filters.add(in("subject_id", subjectIds));
        }

        //todo : quizId filter
        Bson sortByFilter = null;

        //todo : sort by complete
        if(sortBy != null) {
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
                        ), isSubjectsNeeded, isAuthorsNeeded
        );

        return generateSuccessMsg(
                "data", jsonArray
        );
    }

    public static String subjectQuestions(Boolean isQuestionNeeded,
                                          ObjectId subjectId,
                                          ObjectId lessonId,
                                          ObjectId gradeId) {

        ArrayList<Bson> filters = new ArrayList<>();

        if(subjectId != null)
            filters.add(eq("_id", subjectId));

        if(lessonId != null)
            filters.add(eq("lesson._id", lessonId));

        if(gradeId != null)
            filters.add(eq("grade._id", gradeId));

        ArrayList<Document> docs = subjectRepository.find(
                filters.size() == 0 ? null : and(filters),
                null
        );

        JSONArray jsonArray = new JSONArray();

        for(Document doc : docs) {
            jsonArray.put(
                    new JSONObject()
                        .put("qNo", doc.getOrDefault("q_no", 0))
                        .put("subject", new JSONObject()
                                .put("name", doc.getString("name"))
                                .put("id", doc.getObjectId("_id").toString()))
                        .put("lesson", new JSONObject()
                                .put("name", ((Document)doc.get("lesson")).getString("name"))
                                .put("id", ((Document)doc.get("lesson")).getObjectId("_id").toString())
                        )
                        .put("grade", new JSONObject()
                                .put("name", ((Document)doc.get("grade")).getString("name"))
                                .put("id", ((Document)doc.get("grade")).getObjectId("_id").toString())
                        )

            );
        }

        return generateSuccessMsg("data", jsonArray);
    }
}
