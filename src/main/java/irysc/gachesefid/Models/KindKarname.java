package irysc.gachesefid.Models;

public enum  KindKarname {

    SUBJECT, GENERAL, QUESTION;

    public String getName() {
        return name().toLowerCase();
    }
}
