package irysc.gachesefid.Models;

public enum TeachRequestStatus {
    ACCEPT, PENDING, PAID, REJECT, CANCEL;
    public String getName() {
        return name().toLowerCase();
    }
}
