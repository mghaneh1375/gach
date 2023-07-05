package irysc.gachesefid.Models;

public enum OffCodeSections {

    ALL, GACH_EXAM, BANK_EXAM, RAVAN_EXAM, CLASSES, BOOK,
    COUNSELING, COUNSELING_QUIZ, OPEN_EXAM, CONTENT, SCHOOL_QUIZ, SCHOOL_HW;

    public String getName() {
        return name().toLowerCase();
    }

}
