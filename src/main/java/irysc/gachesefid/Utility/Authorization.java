package irysc.gachesefid.Utility;

import irysc.gachesefid.Models.Access;

public class Authorization {

    public static boolean isAdmin(String userAccess) {
        return userAccess.equals(Access.ADMIN.getName()) ||
                userAccess.equals(Access.SUPERADMIN.getName());
    }

    public static boolean isTeacher(String userAccess) {
        return userAccess.equals(Access.ADMIN.getName()) ||
                userAccess.equals(Access.SUPERADMIN.getName()) ||
                userAccess.equals(Access.TEACHER.getName());
    }

    public static boolean isPureStudent(String userAccess) {
        return userAccess.equals(Access.STUDENT.getName());
    }

    public static boolean isStudent(String userAccess) {
        return userAccess.equals(Access.ADMIN.getName()) ||
                userAccess.equals(Access.SUPERADMIN.getName()) ||
                userAccess.equals(Access.STUDENT.getName());
    }
}
