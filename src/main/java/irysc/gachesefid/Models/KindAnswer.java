package irysc.gachesefid.Models;

public enum KindAnswer {

    TEXT, CHOICE, FILE, MULTI_SELECT, TEXTAREA;

    public String getName() {
        return name().toLowerCase();
    }
}
