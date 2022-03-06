package irysc.gachesefid.models;

public enum KindAnswer {

    VOICE, TEXT, CHOICE, FILE, MULTI_SELECT, MULTI_FILE, TEXTAREA;

    public String getName() {
        return name().toLowerCase();
    }
}
