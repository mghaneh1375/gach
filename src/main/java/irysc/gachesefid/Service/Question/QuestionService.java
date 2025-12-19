package irysc.gachesefid.Service.Question;

import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.AllKindQuiz;
import irysc.gachesefid.Models.QuestionLevel;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Service.MyService;
import irysc.gachesefid.Service.Question.model.AddBatchQuestionResult;
import irysc.gachesefid.Service.Question.model.AddBatchQuestionServiceResponse;
import irysc.gachesefid.Service.Question.model.References;
import irysc.gachesefid.Service.Quiz.IryscQuizService;
import irysc.gachesefid.Service.Quiz.OpenQuizService;
import irysc.gachesefid.Service.Quiz.QuizService;
import irysc.gachesefid.Service.Quiz.model.AddQuestionToQuizResult;
import irysc.gachesefid.Utility.Excel;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Validator.EnumValidatorImp;
import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropResponse;
import irysc.gachesefid.adaptor.crop.service.CropService;
import irysc.gachesefid.entity.QuestionEntity;
import irysc.gachesefid.entity.quiz.QuizEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.Excel.getCellValue;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;

@Service
public class QuestionService extends MyService {

    private final CropService cropService;
    private final OpenQuizService openQuizService;
    private final IryscQuizService iryscQuizService;

    public QuestionService(
            CropService cropService,
            OpenQuizService openQuizService,
            IryscQuizService iryscQuizService
    ) {
        this.cropService = cropService;
        this.openQuizService = openQuizService;
        this.iryscQuizService = iryscQuizService;
    }

    public ResponseEntity<ResponseDto<AddBatchQuestionServiceResponse>> cropAndAddQuestionsToQuiz(
            MultipartFile questionPDF,
            MultipartFile answerPDF,
            MultipartFile questionsInfo,
            ObjectId quizId,
            AllKindQuiz quizMode
    ) {
        QuizService quizService;
        switch (quizMode) {
            case IRYSC:
            default:
                quizService = iryscQuizService;
                break;
            case OPEN:
                quizService = openQuizService;
        }

        QuizEntity quiz = quizService.find(quizId);
        CropAdaptorResponse<CropResponse> cropQuestionsResponse;
        try {
            cropQuestionsResponse = cropService.crop(questionPDF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!cropQuestionsResponse.getResponse().getStatus().equals(HttpStatus.OK) ||
                cropQuestionsResponse.getResponse().getError() != null
        )
            throw new InvalidFieldsException(cropQuestionsResponse.getResponse().getError());

        CropAdaptorResponse<CropResponse> cropAnswerResponse = null;
        if (answerPDF != null) {
            try {
                cropAnswerResponse = cropService.crop(answerPDF);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!cropAnswerResponse.getResponse().getStatus().equals(HttpStatus.OK) ||
                    cropAnswerResponse.getResponse().getError() != null
            )
                throw new InvalidFieldsException(cropAnswerResponse.getResponse().getError());
        }

        String filename = FileUtils.uploadTempFile(questionsInfo);
        ArrayList<Row> rows = Excel.read(filename);
        FileUtils.removeTempFile(filename);

        if (rows == null)
            throw new InvalidFieldsException("File is not valid");

        rows.remove(0);
        AddBatchQuestionResult result = addQuestionsByExcelInfo(
                rows,
                cropQuestionsResponse.getResult().getFilenames(),
                cropAnswerResponse == null
                        ? null
                        : cropAnswerResponse.getResult().getFilenames()
        );

        if (result.getErrors() != null &&
                result.getErrors().size() > 0
        ) {
            return new ResponseEntity<>(
                    ResponseDto.builder(AddBatchQuestionServiceResponse.class)
                            .status("ok")
                            .data(
                                    AddBatchQuestionServiceResponse
                                            .builder()
                                            .message("برای افزودن اتومات سوالات به آزمون نباید خطایی در فایل موجود باشد")
                                            .errors(result.getErrors())
                                            .build()
                            )
                            .build(),
                    HttpStatus.OK
            );
        }

        AddQuestionToQuizResult addQuestionToQuizResult = quizService.addQuestionsToQuizAutomatically(
                quiz, result.getInsertedItems()
        );

        if (addQuestionToQuizResult.getErrors().size() > 0) {
            return new ResponseEntity<>(
                    ResponseDto.builder(AddBatchQuestionServiceResponse.class)
                            .status("ok")
                            .data(
                                    AddBatchQuestionServiceResponse
                                            .builder()
                                            .message("سوالات با موفقیت به سامانه افزوده شدند ولی در افزودن سوالات زیر در آزمون مشکلی رخ داده است")
                                            .errors(result.getErrors())
                                            .build()
                            )
                            .build(),
                    HttpStatus.OK
            );
        }

        return new ResponseEntity<>(
                ResponseDto.builder(AddBatchQuestionServiceResponse.class)
                        .status("ok")
                        .data(
                                AddBatchQuestionServiceResponse
                                        .builder()
                                        .message("تمامی سوالات با موفقیت به سامانه و آزمون مدنظر افزوده شدند.")
                                        .build()
                        )
                        .build(),
                HttpStatus.OK
        );
    }


    public AddBatchQuestionResult addQuestionsByExcelInfo(
            List<Row> rows,
            List<String> questionFiles,
            List<String> answerFiles
    ) {
        // excel format:
        // 1- row no 2- question file name 3- subject id
        // 4- author id 5- kindQuestion[test, short_answer, multi_sentence, tashrihi]
        // 6- needed time 7- answer 8- organizationId
        // 9- level[easy, mid, hard] 10- answer file name : optional
        // 11- sentencesCount : optional 12- telorance : optional
        // 13- choicesCount : optional 14- neededLines : optional
        // 22- mark

        List<String> errs = new ArrayList<>();
        Set<String> subjectCodes = new HashSet<>();
        Set<String> authorCodes = new HashSet<>();
        Set<Integer> tagCodes = new HashSet<>();
        Set<String> tagKeys = new HashSet<>();
        List<WriteModel<Document>> writes = new ArrayList<>();
        HashMap<Integer, QuestionEntity> questionEntities = new HashMap<>();

        validateRows(
                rows, errs,
                questionFiles, answerFiles,
                questionEntities, subjectCodes,
                authorCodes, tagCodes, tagKeys
        );
        References references = findReferences(
                questionEntities, subjectCodes,
                authorCodes, tagCodes, tagKeys
        );
        HashMap<String, String> renamed = new HashMap<>();
        AtomicBoolean hasAnyErr = new AtomicBoolean(errs.size() > 0);

        questionEntities
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int rowIdx = entry.getKey();
                    QuestionEntity questionEntity = questionEntities.get(rowIdx);
                    if (references.getDuplicateOrganizationIds().contains(questionEntity.getOrganizationId())) {
                        errs.add(batchRowErr(rowIdx, "کد سازمانی سوال در سامانه موجود است."));
                        hasAnyErr.set(true);
                        return;
                    }

                    if (!references.getSubjects().containsKey(questionEntity.getSubjectCode())) {
                        errs.add(batchRowErr(rowIdx, "کد مبحث نامعتیر است."));
                        hasAnyErr.set(true);
                        return;
                    } else
                        questionEntity.setSubjectId(references.getSubjects().get(questionEntity.getSubjectCode()));

                    if (!references.getAuthors().containsKey(questionEntity.getAuthorCode())) {
                        errs.add(batchRowErr(rowIdx, "کد مولف نامعتبر است."));
                        hasAnyErr.set(true);
                        return;
                    } else
                        questionEntity.setAuthor(references.getAuthors().get(questionEntity.getAuthorCode()));

                    Set<Object> tags = new HashSet<>();
                    for (Object tag : questionEntity.getTags()) {
                        if (tag instanceof Integer && tagCodes.contains(tag))
                            tags.add(references.getTagsByCode().get(tag));
                        else if (tag instanceof String) {
                            tags.add(tag.toString());
                            if (!references.getTagsByKey().contains(tag.toString())) {
                                int tagCode = getRandIntForTag();
                                while (questionTagRepository.exist(
                                        eq("code", tagCode)
                                ))
                                    tagCode = getRandIntForTag();

                                questionTagRepository.insertOne(
                                        new Document("tag", tag.toString()).append("code", tagCode)
                                );
                                references.getTagsByKey().add(tag.toString());
                            }
                        }
                    }
                    questionEntity.setTags(tags);

                    if(!hasAnyErr.get()) {
                        String questionFilename = FileUtils.renameFile(QuestionRepository.FOLDER, questionEntity.getQuestionFile(), null);
                        if (questionFilename == null) {
                            errs.add(batchRowErr(rowIdx, "بارگذاری فایل صورت سوال با خطا مواجه شده است"));
                            hasAnyErr.set(true);
                            return;
                        }
                        renamed.put(questionEntity.getQuestionFile(), questionFilename);
                        questionEntity.setQuestionFile(questionFilename);

                        if (questionEntity.getAnswerFile() != null) {
                            String answerFilename = FileUtils.renameFile(QuestionRepository.FOLDER, questionEntity.getAnswerFile(), null);
                            if (answerFilename == null) {
                                errs.add(batchRowErr(rowIdx, "بارگذاری فایل پاسخ سوال با خطا مواجه شده است"));
                                hasAnyErr.set(true);
                                return;
                            }
                            questionEntity.setAnswerFile(answerFilename);
                            renamed.put(questionEntity.getAnswerFile(), answerFilename);
                        }

                        Document qDoc = mapper.convertValue(questionEntity, Document.class);
                        qDoc.put("_id", new ObjectId(qDoc.get("_id").toString()));
                        qDoc.put("subject_id", new ObjectId(qDoc.get("subject_id").toString()));
                        writes.add(new InsertOneModel<>(qDoc));
                    }
                });

        if(hasAnyErr.get()) {
            questionFiles.forEach(q -> {
                String finalQuestionName = q;
                if(renamed.containsKey(finalQuestionName))
                    finalQuestionName = renamed.get(finalQuestionName);

                if (FileUtils.checkExist(finalQuestionName, QuestionRepository.FOLDER))
                    FileUtils.removeFile(finalQuestionName, QuestionRepository.FOLDER);
            });

            answerFiles.forEach(a -> {
                String finalAnswerName = a;
                if(renamed.containsKey(finalAnswerName))
                    finalAnswerName = renamed.get(finalAnswerName);

                if (FileUtils.checkExist(finalAnswerName, QuestionRepository.FOLDER))
                    FileUtils.removeFile(finalAnswerName, QuestionRepository.FOLDER);
            });

            return AddBatchQuestionResult
                    .builder()
                    .errors(errs)
                    .build();
        }

        if (writes.size() > 0) {
            questionRepository.bulkWrite(writes);
            new Thread(() -> new Jobs.CalcSubjectQuestions().run()).start();
        }

        return AddBatchQuestionResult
                .builder()
                .insertedItems(
                        new ArrayList<>(questionEntities.values())
                )
                .build();

//        if (errs.size() == 0)
//            return generateSuccessMsg(
//                    "excepts", "تمامی سوالات به درستی به سامانه اضافه شدند"
//            );
//
//        return generateSuccessMsg(
//                "excepts",
//                "بجز ردیف\u200Cهای زیر سایرین به درستی به سامانه اضافه گردیدند.",
//                new PairValue("errs", errs)
//        );
    }

    private void validateRows(
            List<Row> rows,
            List<String> errs,
            List<String> questionFiles,
            List<String> answerFiles,
            HashMap<Integer, QuestionEntity> questionEntities,
            Set<String> subjectCodes,
            Set<String> authorCodes,
            Set<Integer> tagCodes,
            Set<String> tagKeys
    ) {
        long curr = System.currentTimeMillis();
        for (Row row : rows) {
            try {
                if (row.getCell(0) == null)
                    break;

                if (row.getLastCellNum() < 9) {
                    errs.add(batchRowErr(row.getRowNum(), "تعداد ستون ها نامعتیر است."));
                    continue;
                }

                QuestionEntity questionEntity = new QuestionEntity();
                String questionFilename = questionFiles != null && questionFiles.size() >= row.getRowNum()
                        ? questionFiles.get(row.getRowNum() - 1)
                        : row.getCell(1).getStringCellValue();
                if (!FileUtils.checkExist(questionFilename, QuestionRepository.FOLDER)) {
                    errs.add(batchRowErr(row.getRowNum(), "فایل سوال موجود نیست."));
                    continue;
                }
                questionEntity.setQuestionFile(questionFilename);

                String answerFilename = null;
                Cell cell = row.getCell(9);
                if ((cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) ||
                        (answerFiles != null && answerFiles.size() >= row.getRowNum() && !answerFiles.get(row.getRowNum() - 1).isEmpty())
                ) {
                    answerFilename = answerFiles != null
                            ? answerFiles.get(row.getRowNum() - 1)
                            : cell.getStringCellValue();
                    if (!FileUtils.checkExist(answerFilename, QuestionRepository.FOLDER)) {
                        errs.add(batchRowErr(row.getRowNum(), "فایل پاسخ سوال موجود نیست."));
                        continue;
                    }
                }
                questionEntity.setAnswerFile(answerFilename);

                int code = (int) getCellValue(row.getCell(2));
                subjectCodes.add(String.format("%03d", code));
                questionEntity.setSubjectCode(String.format("%03d", code));

                int authorCode = (int) getCellValue(row.getCell(3));
                authorCodes.add(String.format("%03d", authorCode));
                questionEntity.setAuthorCode(String.format("%03d", authorCode));

                String kindQuestion = row.getCell(4).getStringCellValue();
                if (!EnumValidatorImp.isValid(kindQuestion, QuestionType.class)) {
                    errs.add(batchRowErr(row.getRowNum(), "نوع سوال نامعتیر است."));
                    continue;
                }
                questionEntity.setKindQuestion(kindQuestion);
                questionEntity.setNeededTime((int) row.getCell(5).getNumericCellValue());

                cell = row.getCell(6);
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (Math.floor(cell.getNumericCellValue()) == cell.getNumericCellValue())
                        questionEntity.setAnswer((int) cell.getNumericCellValue());
                    else
                        questionEntity.setAnswer(cell.getNumericCellValue());
                } else
                    questionEntity.setAnswer(row.getCell(6).getStringCellValue());

                questionEntity.setOrganizationId(row.getCell(7).getStringCellValue());

                String level = row.getCell(8).getStringCellValue();
                if (!EnumValidatorImp.isValid(level, QuestionLevel.class)) {
                    errs.add(batchRowErr(row.getRowNum(), "سطح سختی نامعتیر است."));
                    continue;
                }
                questionEntity.setLevel(level);

                cell = row.getCell(10);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setSentencesCount((int) cell.getNumericCellValue());

                cell = row.getCell(11);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setTelorance(cell.getNumericCellValue());

                cell = row.getCell(12);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setChoicesCount((int) cell.getNumericCellValue());

                cell = row.getCell(13);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setNeededLine((int) cell.getNumericCellValue());

                cell = row.getCell(14);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setYear(getCellValue(cell));

                for (int i = 15; i < 20; i++) {
                    cell = row.getCell(i);
                    if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                        try {
                            tagCodes.add((int) cell.getNumericCellValue());
                            questionEntity.getTags().add((int) cell.getNumericCellValue());
                        } catch (Exception x) {
                            String t = cell.getStringCellValue();
                            tagKeys.add(t);
                            questionEntity.getTags().add(t);
                        }
                    }
                }

                validateRow(questionEntity);

                cell = row.getCell(20);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setIsPublic(cell.getBooleanCellValue());

                cell = row.getCell(21);
                if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                    questionEntity.setMark(cell.getNumericCellValue());

                questionEntity.setCreatedAt(curr);
                questionEntities.put(row.getRowNum(), questionEntity);
            } catch (Exception e) {
                printException(e);
                errs.add(batchRowErr(row.getRowNum(), e.getMessage()));
            }
        }
    }

    private void validateRow(QuestionEntity questionEntity) {

        if (questionEntity.getKindQuestion() == null ||
                questionEntity.getKindQuestion().equals(QuestionType.TEST.getName())
        ) {
            if (!(questionEntity.getAnswer() instanceof Integer))
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");

            if ((int) questionEntity.getAnswer() < 1 || (int) questionEntity.getAnswer() > questionEntity.getChoicesCount())
                throw new InvalidFieldsException("پاسخ سوال باید گزینه صحیح باشد.");
        }

        if (questionEntity.getKindQuestion().equals(QuestionType.SHORT_ANSWER.getName())) {
            if (!(questionEntity.getAnswer() instanceof Number))
                throw new InvalidFieldsException("پاسخ سوال باید یک عدد باشد.");
        }

        if (questionEntity.getKindQuestion().equals(QuestionType.MULTI_SENTENCE.getName())) {
            if (questionEntity.getSentencesCount() == null)
                throw new InvalidFieldsException("تعداد گزاره ها را تعیین کنید.");

            if (!(questionEntity.getAnswer() instanceof String) &&
                    !questionEntity.getAnswer().toString().matches("[01]*")
            )
                throw new InvalidFieldsException("پاسخ سوال باید یک رشته از ۰ و ۱ باشد.");

            if (questionEntity.getAnswer().toString().length() !=
                    questionEntity.getSentencesCount()
            )
                throw new InvalidFieldsException("تعداد گزاره ها با پاسخ تعیین شده هماهنگ نیست.");

            questionEntity.setAnswer(questionEntity.getAnswer().toString());
        }
    }

    private References findReferences(
            HashMap<Integer, QuestionEntity> questionEntities,
            Set<String> subjectCodes,
            Set<String> authorCodes,
            Set<Integer> tagCodes,
            Set<String> tagKeys
    ) {
        return References
                .builder()
                .subjects(
                        subjectRepository
                                .find(in("code", new ArrayList<>(subjectCodes)), JUST_CODE)
                                .stream().collect(Collectors.toMap(
                                doc -> doc.getString("code"),
                                doc -> doc.getObjectId("_id")
                        ))
                )
                .authors(
                        authorRepository
                                .find(in("code", new ArrayList<>(authorCodes)), JUST_CODE_NAME)
                                .stream().collect(Collectors.toMap(
                                doc -> doc.getString("code"),
                                doc -> doc.getString("name")
                        ))
                )
                .tagsByCode(
                        questionTagRepository
                                .find(
                                        in("code", new ArrayList<>(tagCodes)), null
                                )
                                .stream().collect(Collectors.toMap(
                                doc -> doc.getInteger("code"),
                                doc -> doc.getString("tag")
                        ))
                )
                .tagsByKey(
                        questionTagRepository
                                .find(
                                        in("tag", new ArrayList<>(tagKeys)), null
                                )
                                .stream()
                                .map(doc -> doc.getString("tag"))
                                .collect(Collectors.toList())
                )
                .duplicateOrganizationIds(
                        questionRepository.find(
                                in("organization_id",
                                        questionEntities
                                                .values()
                                                .stream()
                                                .map(QuestionEntity::getOrganizationId)
                                                .collect(Collectors.toList())
                                ), JUST_ORGANIZATION_ID
                        ).stream().map(doc -> doc.getString("organization_id")).collect(Collectors.toList())
                )
                .build();
    }
}
