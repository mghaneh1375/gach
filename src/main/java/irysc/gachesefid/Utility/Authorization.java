package irysc.gachesefid.Utility;

import irysc.gachesefid.Models.Access;
import org.bson.types.ObjectId;

import java.util.List;

public class Authorization {

    public static boolean isAdmin(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName());
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
    public static boolean hasAccessToThisStudent(ObjectId studentId, ObjectId applicatorId) {
        return true;
    }

    // todo : complete this section
    public static boolean hasAccessToThisTeacher(ObjectId teacherId, ObjectId applicatorId) {
        return true;
    }
}
