package irysc.gachesefid.models;

public enum  KindKarname {

    SUBJECT, GENERAL, QUESTION;

    public String getName() {
        return name().toLowerCase();
    }
}
