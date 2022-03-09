package irysc.gachesefid.Models;

public enum DescMode {

    FILE, LINK, STRING, NONE;

    public String getName() {
        return name().toLowerCase();
    }

}
