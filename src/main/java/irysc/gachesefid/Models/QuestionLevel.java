package irysc.gachesefid.Models;

public enum QuestionLevel {

    EASY, MID, HARD;

    public String getName() {
        return name().toLowerCase();
    }
}
