package irysc.gachesefid.Models;

public enum NotifVia {

    SMS, MAIL, SITE;

    public String getName() {
        return name().toLowerCase();
    }
}
