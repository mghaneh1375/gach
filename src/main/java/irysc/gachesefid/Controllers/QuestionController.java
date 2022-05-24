package irysc.gachesefid.Controllers;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import static irysc.gachesefid.Main.GachesefidApplication.questionRepository;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

public class QuestionController {

    public static String addQuestion(ObjectId subjectId,
                                     MultipartFile file,
                                     JSONObject jsonObject) {

        Document subject = subjectRepository.findById(subjectId);
        if(subject == null)
            return JSON_NOT_VALID_ID;

        Document newDoc = new Document("subject_id", subjectId);

        for(String str : jsonObject.keySet())
            newDoc.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str), jsonObject.get(str));

        FileUtils.uploadFile(file, "questions");

        return questionRepository.insertOneWithReturn(newDoc);
    }

}
