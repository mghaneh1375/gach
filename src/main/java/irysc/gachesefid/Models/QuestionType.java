package irysc.gachesefid.Models;

public enum QuestionType {

    TEST, TASHRIHI, SHORT_ANSWER, MULTI_SENTENCE;

    public String getName() {
        return name().toLowerCase();
    }
}
