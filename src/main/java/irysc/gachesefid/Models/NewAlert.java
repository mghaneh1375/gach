package irysc.gachesefid.Models;

public enum NewAlert {

    NEW_TICKETS,
    OPEN_TICKETS_WAIT_FOR_ADMIN, NEW_CERTIFICATE_REQUESTS;

    public String getName() {
        return name().toLowerCase();
    }
}
