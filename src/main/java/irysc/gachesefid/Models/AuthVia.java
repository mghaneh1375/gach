package irysc.gachesefid.Models;

public enum AuthVia {

    SMS, MAIL;

    public String getName() {
        return name().toLowerCase();
    }
}
