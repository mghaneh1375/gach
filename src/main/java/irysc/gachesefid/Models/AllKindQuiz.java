package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM, OPEN, CONTENT, HW;

    public String getName() {
        return name().toLowerCase();
    }
}
