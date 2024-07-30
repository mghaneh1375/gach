package irysc.gachesefid.Models;

public enum SettledStatus {

    REJECT, PAID, WAIT_FOR_PAY, PENDING;

    public String getName() {
        return name().toLowerCase();
    }
}
