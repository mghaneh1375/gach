package irysc.gachesefid.Service;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JUST_ID;
import static irysc.gachesefid.Utility.StaticValues.JUST_TITLE;

@Service
public class GeneralCacheService {

    public void getInfo() {
        long curr = System.currentTimeMillis();
        Document generalCache = new Document()
                .append("_id", new ObjectId())
                .append("students", userRepository.count(eq("level", false)))
                .append("questions", questionRepository.count(null))
                .append("schools", schoolRepository.count(null))
                .append("activeIRYSCQuizzes", iryscQuizRepository.count(
                        and(
                                lt("start_registry", curr),
                                or(
                                        and(
                                                exists("end_registry", false),
                                                gt("end", curr)
                                        ),
                                        and(
                                                exists("end_registry", true),
                                                gt("end_registry", curr)
                                        )
                                )
                        )
                ))
                .append("openQuizzesCount", openQuizRepository.count(exists("_id")))
                .append("activeTeachersCount", userRepository.count(
                        and(
                                exists("teach"),
                                eq("teach", true)
                        )
                ))
                .append("activeAdvisorsCount", userRepository.count(
                        and(
                                exists("advice"),
                                eq("advice", true)
                        )
                ))
                .append("tutorialsCount", contentRepository.count(
                        eq("visibility", true)
                ));

        fillCacheWithCourseInfo(generalCache);
        Repository.removeFromCache("general", "first");
        Repository.addToCache("general", generalCache, "first", 1, 60 * 60 * 3);
    }

    private void fillCacheWithContentCourseInfo(Document generalCache, ArrayList<Document> courses) {
        HashMap<String, Integer> contentsPerCourse = new HashMap<>();
        HashMap<String, List<PairValue>> bestContentsPerCourse = new HashMap<>();
        HashMap<String, Integer> sessionsPerCourse = new HashMap<>();

        for(Document course : courses) {
            String courseTitle = course.getString("title").replace("-", " ");
            ArrayList<Document> contentsInCourse = contentRepository.find(
                    eq("tags", courseTitle),
                    new BasicDBObject("sessions_count", true).append("title", 1)
                            .append("users._id", 1).append("slug", 1)
            );
            if(contentsInCourse.size() == 0)
                continue;

            contentsPerCourse.put(courseTitle, contentsInCourse.size());
            sessionsPerCourse.put(courseTitle, contentsInCourse.stream().mapToInt(document -> document.getInteger("sessions_count")).sum());
            contentsInCourse.sort(Comparator.comparingInt(o -> o.getList("users", Object.class).size()));
            if(contentsInCourse.size() > 1) {
                bestContentsPerCourse.put(courseTitle, new ArrayList<>() {{
                    add(
                            new PairValue(
                                    contentsInCourse.get(contentsInCourse.size() - 1).getString("title"),
                                    contentsInCourse.get(contentsInCourse.size() - 1).getString("slug")
                            )
                    );
                    add(
                            new PairValue(
                                    contentsInCourse.get(contentsInCourse.size() - 2).getString("title"),
                                    contentsInCourse.get(contentsInCourse.size() - 2).getString("slug")
                            )
                    );
                }});
            }
            else {
                bestContentsPerCourse.put(courseTitle, new ArrayList<>() {{
                    add(
                            new PairValue(
                                    contentsInCourse.get(contentsInCourse.size() - 1).getString("title"),
                                    contentsInCourse.get(contentsInCourse.size() - 1).getString("slug")
                            )
                    );
                }});
            }
        }

        generalCache.put("contentsPerCourse", contentsPerCourse);
        generalCache.put("sessionsPerCourse", sessionsPerCourse);
        generalCache.put("bestContentsPerCourse", bestContentsPerCourse);
    }

    private void fillCacheWithQuizCourseInfo(Document generalCache, List<Document> courses) {
        HashMap<String, Integer> quizzesPerCourse = new HashMap<>();
        HashMap<String, String> bestQuizzesPerCourse = new HashMap<>();

        for(Document course : courses) {
            String courseTitle = course.getString("title").replace("-", " ");
            ArrayList<Document> openQuizzessInCourse = openQuizRepository.find(
                    eq("tags", courseTitle),
                    new BasicDBObject("title", true).append("registered", 1)
            );
            if(openQuizzessInCourse.size() == 0)
                continue;

            quizzesPerCourse.put(courseTitle, openQuizzessInCourse.size());
            openQuizzessInCourse.sort(Comparator.comparingInt(o -> o.getInteger("registered")));

            bestQuizzesPerCourse.put(courseTitle,
                    openQuizzessInCourse.get(openQuizzessInCourse.size() - 1).getString("title")
            );
        }

        generalCache.put("quizzesPerCourse", quizzesPerCourse);
        generalCache.put("bestQuizzesPerCourse", bestQuizzesPerCourse);
    }

    private void fillCacheWithQuestionCourseInfo(Document generalCache, List<Document> courses) {
        HashMap<String, Integer> questionsPerCourse = new HashMap<>();

        for(Document course : courses) {
            String courseTitle = course.getString("title").replace("-", " ");

            questionsPerCourse.put(courseTitle, questionRepository.count(
                    in("subject_id",
                            subjectRepository.find(
                                    eq("grade.name", courseTitle),
                                    JUST_ID
                            ).stream().map(document -> document.getObjectId("_id")).collect(Collectors.toList())
                    )
            ));
        }

        generalCache.put("questionsPerCourse", questionsPerCourse);
    }

    private void fillCacheWithAdvisorCourseInfo(Document generalCache, List<Document> courses) {
        HashMap<String, Integer> advisorsPerCourse = new HashMap<>();

        for(Document course : courses) {
            String courseTitle = course.getString("title").replace("-", " ");
            Document grade = gradeRepository.findBySecKey(courseTitle);
            if(grade == null) continue;
            advisorsPerCourse.put(courseTitle, userRepository.count(
                    and(
                            eq("advice", true),
                            eq("teach_branches", grade.getObjectId("_id"))
                    )
            ));
        }

        generalCache.put("advisorsPerCourse", advisorsPerCourse);
    }

    private void fillCacheWithCourseInfo(Document generalCache) {
        ArrayList<Document> courses = courseIntroductionRepository.find(null, JUST_TITLE);
        fillCacheWithContentCourseInfo(generalCache, courses);
        fillCacheWithQuizCourseInfo(generalCache, courses);
        fillCacheWithQuestionCourseInfo(generalCache, courses);
        fillCacheWithAdvisorCourseInfo(generalCache, courses);
    }

}
