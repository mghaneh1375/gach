package irysc.gachesefid.Models;

public enum Access {

    ADVISOR, SUPERADMIN, TEACHER, ADMIN, STUDENT, AGENT, SCHOOL, CONTENT, EDITOR, WEAK_ADMIN;

    public String getName() {
        return name().toLowerCase();
    }
}
