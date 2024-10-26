package irysc.gachesefid.Models;

public enum ActiveMode {

    ACTIVE, EXPIRED, NOT_START;

    public String getName() {
        return name().toLowerCase();
    }
}
