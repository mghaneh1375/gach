package irysc.gachesefid.Models;

public enum PasswordMode {

    LAST_4_DIGIT_NID, NID, RANDOM, SIMPLE, CUSTOM;

    public String getName() {
        return name().toLowerCase();
    }
}
