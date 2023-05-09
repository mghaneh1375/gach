package irysc.gachesefid.Controllers;

import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.RequestType;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.requestRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.JSON_NOT_VALID_ID;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;

public class RequestController {

    public static String setRequestAnswer(ObjectId requestId,
                                          boolean answer,
                                          String description) {

        Document doc = requestRepository.findById(requestId);
        if(doc == null || doc.containsKey("answer"))
            return JSON_NOT_VALID_ID;

        doc.put("description", description);
        doc.put("answer", answer);
        doc.put("status", "finish");
        doc.put("answer_at", System.currentTimeMillis());

        requestRepository.replaceOne(requestId, doc);

        if(answer) {

            ObjectId userId = doc.getObjectId("user_id");
            Document user = userRepository.findById(userId);
            List<String> accesses = user.getList("accesses", String.class);
            boolean change = false;

            if (doc.getString("type").equals(RequestType.ADVISOR.getName()) &&
                    !accesses.contains(Access.ADVISOR.getName())
            ) {
                accesses.add(Access.ADVISOR.getName());
                change = true;
            }
            else if(doc.getString("type").equals(RequestType.NAMAYANDE.getName()) &&
                    !accesses.contains(Access.AGENT.getName())) {
                accesses.add(Access.AGENT.getName());
                change = true;
            }
            else if(doc.getString("type").equals(RequestType.SCHOOL.getName()) &&
                    !accesses.contains(Access.SCHOOL.getName())) {
                accesses.add(Access.SCHOOL.getName());
                change = true;
            }

            if(change) {
                //todo send mail
                userRepository.updateOne(userId, set("accesses", accesses));
            }
        }
        else {
            //todo send mail
        }

        return JSON_OK;
    }

    public static String getAll(Boolean isPending,
                                Boolean isFinished,
                                Boolean searchInArchive,
                                String type,
                                ObjectId userId,
                                String sendDate, String answerDate,
                                String sendDateEndLimit, String answerDateEndLimit) {
        //todo: implementation
        return null;
    }



}
