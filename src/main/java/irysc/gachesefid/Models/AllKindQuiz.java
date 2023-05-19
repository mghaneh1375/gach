package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM, OPEN, CONTENT, HW, ONLINESTANDING;

    public String getName() {
        return name().toLowerCase();
    }
}
