package irysc.gachesefid.Models;

public enum TeachMode {
    PRIVATE, SEMI_PRIVATE;
    public String getName() {
        return name().toLowerCase();
    }
}
