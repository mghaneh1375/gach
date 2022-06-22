package irysc.gachesefid.Models;

public enum KindSchool {

    SAMPAD, NEMONE, SHAHED, HEYAT, DOLATI, GHEYR, SAYER;

    public String getName() {
        return name().toLowerCase();
    }
}
