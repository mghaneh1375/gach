package irysc.gachesefid.Models;

public enum RequestType {

    ADVISOR, SCHOOL, NAMAYANDE;

    public String getName() {
        return name().toLowerCase();
    }
}
