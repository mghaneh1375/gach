package irysc.gachesefid.Service.admin;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.DB.Common;
import irysc.gachesefid.DB.Repository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;

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

    public void convertAttachesToDoc() {
        List<Document> contents = contentRepository.find(null, null);
        String[] faNums = new String[] {
            "اول", "دوم", "سوم", "چهارم", "پنجم",
            "ششم", "هفتم", "هشتم", "نهم", "دهم"
        };
        for(Document content : contents) {
            if(!content.containsKey("sessions"))
                continue;

            List<Document> sessions = content.getList("sessions", Document.class);
            boolean needUpdate = false;
            for(Document session : sessions) {
                if(!session.containsKey("attaches") ||
                        session.getList("attaches", Object.class).size() == 0
                )
                    continue;

                List<Object> attaches = session.getList("attaches", Object.class);
                boolean needChange = false;
                for(int i = 0; i < attaches.size(); i++) {
                    if(attaches.get(i) instanceof String) {
                        needChange = true;
                        attaches.set(i,
                                new Document("filename", attaches.get(i))
                                        .append("title", "فایل ضمیمه " + faNums[i])
                        );
                    }
                }
                if(needChange) {
                    session.put("attaches", attaches);
                    needUpdate = true;
                }
            }
            if(needUpdate) {
                content.put("sessions", sessions);
                contentRepository.replaceOne(
                        content.getObjectId("_id"),
                        content
                );
            }
        }
    }

    private void doNormalizeTags(List<Document> docs, Common repository) {

        for (Document doc : docs) {
            List<String> tags = doc.getList("tags", String.class).stream().map(s -> {
                if (s.equals("المپیاد زیست"))
                    return "المپیاد زیست\u200Cشناسی";
                if (s.equals("المپیاد نجوم"))
                    return "المپیاد نجوم و اخترفیزیک";
                if (s.equals("المپیاد اقتصاد"))
                    return "المپیاد مدیریت، اقتصاد و حکمرانی";
                return s;
            }).collect(Collectors.toList());

            repository.updateOne(doc.getObjectId("_id"), set("tags", tags));
        }
    }
    public ResponseEntity<Set<String>> normalizeTags() {

        Bson filter = or(
                eq("tags", "المپیاد زیست"),
                eq("tags", "المپیاد نجوم"),
                eq("tags", "المپیاد اقتصاد")
        );

        ArrayList<Document> documents = contentRepository.find(
                filter, null
        );
        doNormalizeTags(documents, contentRepository);

        documents = openQuizRepository.find(
                filter, null
        );
        doNormalizeTags(documents, openQuizRepository);

        documents = iryscQuizRepository.find(
                filter, null
        );
        doNormalizeTags(documents, iryscQuizRepository);

        Set<String> tags = new HashSet<>();
        contentRepository.find(null, new BasicDBObject("tags", 1)).forEach(document -> {
            document.getList("tags", String.class).stream().filter(s -> s.startsWith("المپیاد")).forEach(tags::add);
        });
        openQuizRepository.find(null, new BasicDBObject("tags", 1)).forEach(document -> {
            document.getList("tags", String.class).stream().filter(s -> s.startsWith("المپیاد")).forEach(tags::add);
        });
        iryscQuizRepository.find(null, new BasicDBObject("tags", 1)).forEach(document -> {
            document.getList("tags", String.class).stream().filter(s -> s.startsWith("المپیاد")).forEach(tags::add);
        });

        return ResponseEntity.ok().body(tags);
    }

}
