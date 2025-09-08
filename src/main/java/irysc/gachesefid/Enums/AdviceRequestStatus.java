package irysc.gachesefid.Enums;

public enum AdviceRequestStatus {
    PENDING, ACCEPT, REJECT;

    public String getName() {
        return name().toLowerCase();
    }
}
