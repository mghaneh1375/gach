package irysc.gachesefid.Controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import irysc.gachesefid.Controllers.Quiz.StudentQuizController;
import irysc.gachesefid.Digests.Question;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Security.JwtTokenFilter.blackListTokens;
import static irysc.gachesefid.Security.JwtTokenFilter.validateTokens;
import static irysc.gachesefid.Utility.StaticValues.*;

public class Jobs implements Runnable {

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new TokenHandler(), 0, 86400000); // 1 day
        timer.schedule(new SiteStatsHandler(), 86400000, 86400000); // 1 day
        timer.schedule(new RemoveRedundantCustomQuizzes(), 0, 86400000);
    }

    class TokenHandler extends TimerTask {

        public void run() {

            synchronized (validateTokens) {
                validateTokens.removeIf(itr -> !itr.isValidateYet());
            }

            synchronized (blackListTokens) {
                blackListTokens.removeIf(itr -> itr.getValue() < System.currentTimeMillis());
            }

        }
    }


    class SiteStatsHandler extends TimerTask {

        public void run() {
            SCHOOLS = schoolRepository.count(exists("user_id"));
            QUESTIONS = questionRepository.count(null);
            STUDENTS = userRepository.count(eq("level", false));
        }
    }

    class RemoveRedundantCustomQuizzes extends TimerTask {

        @Override
        public void run() {

            customQuizRepository.deleteMany(
                    and(
                            eq("status", "wait"),
                            lt("created_at", System.currentTimeMillis() - 1200000) // 20min
                    )
            );

        }
    }

    class CalcSubjectQuestions extends TimerTask {

        @Override
        public void run() {

            HashMap<ObjectId, StudentQuizController.SubjectFilter> subjectFilterHashMap = new HashMap<>();
            ArrayList<Document> questions = questionRepository.find(null,
                    new BasicDBObject("level", 1).append("subject_id", 1)
            );

            ArrayList<ObjectId> subjectIds = new ArrayList<>();

            for (Document question : questions) {

                ObjectId sId = question.getObjectId("subject_id");
                String l = question.getString("level");

                if (subjectFilterHashMap.containsKey(sId))
                    subjectFilterHashMap.get(sId).add(1, l);
                else {
                    subjectFilterHashMap.put(sId, new StudentQuizController.SubjectFilter(sId, 1, l));
                    subjectIds.add(sId);
                }
            }

            ArrayList<Document> subjects = subjectRepository.findByIds(
                    subjectIds, true
            );

            int idx = 0;

            List<WriteModel<Document>> writes = new ArrayList<>();

            for(ObjectId sId : subjectFilterHashMap.keySet()) {

                subjects.get(idx).put("q_no", subjectFilterHashMap.get(sId).total());
                subjects.get(idx).put("q_no_easy", subjectFilterHashMap.get(sId).easy());
                subjects.get(idx).put("q_no_mid", subjectFilterHashMap.get(sId).mid());
                subjects.get(idx).put("q_no_hard", subjectFilterHashMap.get(sId).hard());

                writes.add(new UpdateOneModel<>(
                        eq("_id", sId),
                        new BasicDBObject("$set",
                                new BasicDBObject("q_no", subjectFilterHashMap.get(sId).total())
                                        .append("q_no_easy", subjectFilterHashMap.get(sId).easy())
                                        .append("q_no_mid", subjectFilterHashMap.get(sId).mid())
                                        .append("q_no_hard", subjectFilterHashMap.get(sId).hard())
                        )
                ));

                idx++;
            }

            subjectRepository.bulkWrite(writes);
        }
    }
}
