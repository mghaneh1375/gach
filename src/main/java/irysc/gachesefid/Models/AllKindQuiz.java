package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM, OPEN, CONTENT;

    public String getName() {
        return name().toLowerCase();
    }
}
