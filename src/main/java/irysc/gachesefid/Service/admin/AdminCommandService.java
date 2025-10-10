package irysc.gachesefid.Service.admin;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

@Service
public class AdminCommandService {

    public void syncStudentsInAdvisors() {
        ArrayList<Document> advisors = userRepository.find(
                and(
                        exists("students.0"),
                        eq("accesses", "advisor")
                )
                , new BasicDBObject("students", 1));
        List<ObjectId> allStudentsId = advisors
                .stream()
                .map(advisor -> advisor.getList("students", Document.class))
                .flatMap(Collection::stream)
                .map(std -> std.getObjectId("_id"))
                .distinct()
                .collect(Collectors.toList());

        List<Document> students = userRepository.findByIdsWithNull(
                allStudentsId, new BasicDBObject("advisor_id", 1)
        );

        List<WriteModel<Document>> writes = new ArrayList<>();
        advisors.forEach(advisor -> {
            List<Document> validStdList = advisor.getList("students", Document.class)
                    .stream()
                    .filter(stdDoc -> {
                        Optional<Document> wantedStd = students
                                .stream()
                                .filter(std -> std.getObjectId("_id").equals(stdDoc.getObjectId("_id")))
                                .findFirst();
                        return wantedStd.isPresent() && Objects.equals(wantedStd.get().getOrDefault("advisor_id", null), advisor.getObjectId("_id"));
                    })
                    .distinct()
                    .collect(Collectors.toList());

            if(validStdList.size() != advisor.getList("students", Object.class).size()) {
                writes.add(new UpdateOneModel<Document>(
                        eq("_id", advisor.getObjectId("_id")),
                        set("students", validStdList)
                ));
            }
        });

        if(writes.size() > 0)
            userRepository.bulkWrite(writes);
    }

}
