package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM;

    public String getName() {
        return name().toLowerCase();
    }
}
