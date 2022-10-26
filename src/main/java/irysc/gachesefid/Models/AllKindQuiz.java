package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM, OPEN;

    public String getName() {
        return name().toLowerCase();
    }
}
