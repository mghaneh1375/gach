package irysc.gachesefid.Models;

public enum ActiveMode {

    ACTIVE, EXPIRED;

    public String getName() {
        return name().toLowerCase();
    }
}
