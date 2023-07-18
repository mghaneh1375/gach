package irysc.gachesefid.Utility;

import irysc.gachesefid.Models.Access;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.advisorRequestsRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

public class Authorization {

    public static boolean isAdmin(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName());
    }

    public static boolean isAgent(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.AGENT.getName());
    }

    public static boolean isSchool(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.SCHOOL.getName());
    }

    public static boolean isAdvisor(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.ADVISOR.getName());
    }

    public static boolean isTeacher(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.TEACHER.getName());
    }

    public static boolean isPureStudent(List<String> accesses) {
        return accesses.size() == 1 && accesses.contains(Access.STUDENT.getName());
    }

    public static boolean isStudent(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.STUDENT.getName());
    }


    // todo : complete this section
    public static boolean hasAccessToThisSchool(Document user, ObjectId agentId) {

        if(!user.containsKey("form_list"))
            return false;

        Document form = Utility.searchInDocumentsKeyVal(
                user.getList("form_list", Document.class),
                "role", "school"
        );

        if(form == null)
            return false;

        if(!form.containsKey("agent_id"))
            return false;

        return form.getObjectId("agent_id").equals(agentId);
    }

    // todo : complete this section
    public static boolean hasAccessToThisStudent(ObjectId studentId, ObjectId applicatorId) {

        if(studentId.equals(applicatorId))
            return true;

        Document applicator = userRepository.findById(applicatorId);
        if(applicator == null)
            return false;

        if(isAdmin(applicator.getList("accesses", String.class)))
            return true;

        if(isSchool(applicator.getList("accesses", String.class)))
            return applicator.getList("students", ObjectId.class).contains(studentId);

        if(isAdvisor(applicator.getList("accesses", String.class)))
            return Utility.searchInDocumentsKeyValIdx(
                    applicator.getList("students", Document.class), "_id", studentId
            ) > -1;

        return false;
    }

    public static boolean hasWeakAccessToThisStudent(ObjectId studentId, ObjectId applicatorId) {

        if(studentId.equals(applicatorId))
            return true;

        Document applicator = userRepository.findById(applicatorId);
        if(applicator == null)
            return false;

        if(isAdmin(applicator.getList("accesses", String.class)))
            return true;

        if(isSchool(applicator.getList("accesses", String.class)))
            return applicator.getList("students", ObjectId.class).contains(studentId);

        if(isAdvisor(applicator.getList("accesses", String.class))) {
            return Utility.searchInDocumentsKeyValIdx(
                    applicator.getList("students", Document.class), "_id", studentId
            ) > -1 || advisorRequestsRepository.count(
                    and(
                            eq("advisor_id", applicatorId),
                            eq("user_id", studentId),
                            eq("answer", "pending")
                    )
            ) > 0;
        }

        return false;
    }

}
