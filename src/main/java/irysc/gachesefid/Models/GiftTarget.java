package irysc.gachesefid.Models;

public enum GiftTarget {

    PUBLIC, PACKAGE, QUIZ, ALL_PACKAGE, ALL_QUIZ;

    public String getName() {
        return name().toLowerCase();
    }
}
