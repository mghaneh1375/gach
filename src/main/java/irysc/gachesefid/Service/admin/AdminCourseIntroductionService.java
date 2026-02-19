package irysc.gachesefid.Service.admin;

import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Dto.courseIntroduction.AddSeoToIntroductionCourseDto;
import irysc.gachesefid.Dto.courseIntroduction.CourseDigestDto;
import irysc.gachesefid.Dto.courseIntroduction.CreateCourseIntroductionDto;
import irysc.gachesefid.Dto.courseIntroduction.SuggestionDto;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static irysc.gachesefid.Main.GachesefidApplication.courseIntroductionRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

@Service
public class AdminCourseIntroductionService {

    public String list() {
        return generateSuccessMsg("data", courseIntroductionRepository.find(null, null)
                .stream().map(document -> new JSONObject()
                        .put("title", document.getString("title"))
                        .put("description", document.getString("description"))
                        .put("digest", document.getString("digest"))
                        .put("id", document.getObjectId("_id")))
                .collect(Collectors.toList())
        );
    }

    public String get(String slug) {
        Document document = courseIntroductionRepository.findBySecKey(slug);
        if (document == null)
            return generateSuccessMsg("data", new JSONObject()
                    .put("title", "")
                    .put("description", "")
            );

        return generateSuccessMsg("data", new JSONObject()
                .put("title", document.getString("title"))
                .put("description", document.getString("description"))
                .put("digest", document.getString("digest"))
        );
    }

    @CacheEvict(value = "all-courses", allEntries = true)
    public String store(CreateCourseIntroductionDto dto) {
        Document document = new Document();
        document.put("title", dto.getTitle());
        document.put("description", dto.getDescription());
        document.put("digest", dto.getDigest());
        return generateSuccessMsg("id", courseIntroductionRepository.insertOneWithReturnId(document));
    }

    @Cacheable("all-courses")
    public String getAllCourses() {
        ArrayList<Document> docs = courseIntroductionRepository.find(null, null);
        Document generalCache = Repository.isInCache("general", "first");

        return generateSuccessMsg("data", docs.stream().map(document -> {
                    String courseTitle = document.getString("title").replace("-", " ");
                    List<SuggestionDto> suggestions = new ArrayList<>();
                    if (generalCache != null && generalCache.containsKey("bestContentsPerCourse")) {
                        List<PairValue> contentSuggestions = ((HashMap<String, List<PairValue>>) (generalCache.get("bestContentsPerCourse", HashMap.class))).getOrDefault(courseTitle, null);
                        if (contentSuggestions != null)
                            suggestions.addAll(contentSuggestions
                                    .stream()
                                    .map(pairValue -> SuggestionDto
                                            .builder()
                                            .title("دوره " + pairValue.getKey())
                                            .slug(pairValue.getValue().toString())
                                            .build()
                                    ).collect(Collectors.toList()));
                    }

                    return CourseDigestDto
                            .builder()
                            .digest(document.getOrDefault("digest", "").toString())
                            .title(courseTitle)
                            .advisors(
                                    generalCache == null || !generalCache.containsKey("advisorsPerCourse")
                                            ? 0
                                            : ((HashMap<String, Integer>) (generalCache.get("advisorsPerCourse", HashMap.class))).getOrDefault(courseTitle, 0)
                            )
                            .packages(
                                    generalCache == null || !generalCache.containsKey("contentsPerCourse")
                                            ? 0
                                            : ((HashMap<String, Integer>) (generalCache.get("contentsPerCourse", HashMap.class))).getOrDefault(courseTitle, 0)
                            )
                            .sessions(
                                    generalCache == null || !generalCache.containsKey("sessionsPerCourse")
                                            ? 0
                                            : ((HashMap<String, Integer>) (generalCache.get("sessionsPerCourse", HashMap.class))).getOrDefault(courseTitle, 0)
                            )
                            .suggestions(suggestions)
                            .quizzes(
                                    generalCache == null || !generalCache.containsKey("quizzesPerCourse")
                                            ? 0
                                            : ((HashMap<String, Integer>) (generalCache.get("quizzesPerCourse", HashMap.class))).getOrDefault(courseTitle, 0)
                            )
                            .questions(
                                    generalCache == null || !generalCache.containsKey("questionsPerCourse")
                                            ? 0
                                            : ((HashMap<String, Integer>) (generalCache.get("questionsPerCourse", HashMap.class))).getOrDefault(courseTitle, 0)
                            )
                            .build();
                }).collect(Collectors.toList())
        );
    }

    @CacheEvict(value = "all-courses", allEntries = true)
    public String update(ObjectId id, CreateCourseIntroductionDto dto) {
        Document document = courseIntroductionRepository.findById(id);
        if (document == null)
            throw new InvalidFieldsException("id is incorrect");

        document.put("title", dto.getTitle());
        document.put("description", dto.getDescription());
        document.put("digest", dto.getDigest());

        courseIntroductionRepository.replaceOne(id, document);
        return JSON_OK;
    }

    @CacheEvict(value = "all-courses", allEntries = true)
    public String remove(JSONArray ids) {
        JSONArray excepts = new JSONArray();
        JSONArray removeIds = new JSONArray();

        for (int i = 0; i < ids.length(); i++) {
            String id = ids.getString(i);

            if (!ObjectId.isValid(id)) {
                excepts.put(i + 1);
                continue;
            }

            ObjectId oId = new ObjectId(id);
            if (courseIntroductionRepository.findById(oId) != null) {
                courseIntroductionRepository.deleteOne(oId);
                removeIds.put(oId);
            } else
                excepts.put(i + 1);
        }

        return Utility.returnRemoveResponse(excepts, removeIds);
    }

    public String addSeoTag(ObjectId id, AddSeoToIntroductionCourseDto dto) {
        Document document = courseIntroductionRepository.findById(id);
        if (document == null)
            throw new InvalidFieldsException("id is incorrect");

        List<Document> seoList = (List<Document>) document.getOrDefault("seo_list", new ArrayList<>());
        seoList.add(new Document("keyword", dto.getKey())
                .append("value", dto.getValue())
        );

        if (!document.containsKey("seo_list"))
            document.put("seo_list", seoList);

        courseIntroductionRepository.replaceOneWithoutClearCache(
                id, document
        );
        return JSON_OK;
    }

    public String removeSeoTag(ObjectId id, String keyword) {
        Document document = courseIntroductionRepository.findById(id);
        if (document == null)
            throw new InvalidFieldsException("id is incorrect");

        if (!document.containsKey("seo_list"))
            return JSON_OK;

        List<Document> seoList = document.getList("seo_list", Document.class);
        seoList.removeIf(document1 -> Objects.equals(document1.getString("keyword"), keyword));

        courseIntroductionRepository.replaceOneWithoutClearCache(
                id, document
        );
        return JSON_OK;
    }

    public String getSeoTags(ObjectId id) {
        Document document = courseIntroductionRepository.findById(id);
        if (document == null)
            throw new InvalidFieldsException("id is incorrect");

        List<Document> seoList = (List<Document>) document.getOrDefault("seo_list", new ArrayList<>());
        return generateSuccessMsg("data",
                seoList
                        .stream()
                        .map(document1 -> new JSONObject()
                                .put("keyword", document1.getString("keyword"))
                                .put("value", document1.getString("value"))
                        )
                        .collect(Collectors.toList()));
    }
}
