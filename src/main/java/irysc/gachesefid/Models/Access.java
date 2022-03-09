package irysc.gachesefid.Models;

public enum Access {

    ADVISOR, SUPERADMIN, TEACHER, ADMIN, STUDENT, NAMAYANDE, SCHOOL;

    public String getName() {
        return name().toLowerCase();
    }
}
