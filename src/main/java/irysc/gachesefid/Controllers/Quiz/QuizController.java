package irysc.gachesefid.Controllers.Quiz;

import com.google.common.base.CaseFormat;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.DescMode;
import irysc.gachesefid.Models.KindQuiz;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.ArrayList;


public class QuizController {

    public static Document store(ObjectId userId, JSONObject data
    ) throws Exception {

        Document newDoc = new Document();

        for(String key : data.keySet()) {
            newDoc.put(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key),
                    data.get(key)
            );
        }

        if (newDoc.containsKey("desc_after_mode") &&
                newDoc.getString("desc_after_mode").equals(DescMode.FILE.getName()) &&
                newDoc.containsKey("desc_after")
        )
            throw new InvalidFieldsException("زمانی که فایل توضیحات بعد آزمون را بر روی فایل ست می کنید نباید فیلد descAfter را ست نمایید.");

        if(!newDoc.containsKey("desc_after_mode") ||
                newDoc.getString("desc_after_mode").equals(DescMode.FILE.getName())
        )
            newDoc.put("desc_after_mode", DescMode.NONE.getName());

        Utility.isValid(newDoc);

        newDoc.put("visibility", true);
        newDoc.put("created_by", userId.toString());
        newDoc.put("created_at", System.currentTimeMillis());

        //todo: consider other modes
        if(newDoc.getString("mode").equals(KindQuiz.REGULAR.getName()) ||
                newDoc.getString("mode").equals(KindQuiz.OPEN.getName())
        )
            newDoc.put("questions", new ArrayList<>());

        return newDoc;
    }

}
