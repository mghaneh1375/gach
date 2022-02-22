package irysc.gachesefid.models;

public enum RequestType {

    ADVISOR, SCHOOL, NAMAYANDE;

    public String getName() {
        return name().toLowerCase();
    }
}
