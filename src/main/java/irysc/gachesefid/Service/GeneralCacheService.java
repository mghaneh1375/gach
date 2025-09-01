package irysc.gachesefid.Service;

import irysc.gachesefid.DB.Repository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;

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

        Repository.removeFromCache("general", "first");
        Repository.addToCache("general", generalCache, "first", 1, 60 * 60 * 3);
    }

}
