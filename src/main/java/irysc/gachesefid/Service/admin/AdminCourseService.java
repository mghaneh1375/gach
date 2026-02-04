package irysc.gachesefid.Service.admin;

import irysc.gachesefid.Dto.courseIntroduction.CreateCourseIntroductionDto;
import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import static irysc.gachesefid.Main.GachesefidApplication.courseIntroductionRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

@Service
public class AdminCourseService {

    public String list() {
        return generateSuccessMsg("data", courseIntroductionRepository.find(null, null)
                .stream().map(document -> new JSONObject()
                        .put("title", document.getString("title"))
                        .put("description", document.getString("description"))
                        .put("id", document.getObjectId("_id")))
                .collect(Collectors.toList())
        );
    }

    public String get(String slug) {
        Document document = courseIntroductionRepository.findBySecKey(slug);
        if(document == null)
            return generateSuccessMsg("data", new JSONObject()
                    .put("title", "")
                    .put("description", "")
            );

        return generateSuccessMsg("data", new JSONObject()
                .put("title", document.getString("title"))
                .put("description", document.getString("description"))
        );
    }

    public String store(CreateCourseIntroductionDto dto) {
        Document document = new Document();
        document.put("title", dto.getTitle());
        document.put("description", dto.getDescription());
        return generateSuccessMsg("id", courseIntroductionRepository.insertOneWithReturnId(document));
    }

    public String update(ObjectId id, CreateCourseIntroductionDto dto) {
        Document document = courseIntroductionRepository.findById(id);
        if(document == null)
            throw new InvalidFieldsException("id is incorrect");

        document.put("title", dto.getTitle());
        document.put("description", dto.getDescription());

        courseIntroductionRepository.replaceOne(id, document);
        return JSON_OK;
    }

    public String remove(ObjectId id) {
        courseIntroductionRepository.deleteOne(id);
        return JSON_OK;
    }
}
