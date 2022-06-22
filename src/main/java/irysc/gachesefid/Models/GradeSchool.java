package irysc.gachesefid.Models;

public enum GradeSchool {

    DABESTAN, MOTEVASETEAVAL, MOTEVASETEDOVOM;

    public String getName() {
        return name().toLowerCase();
    }
}
