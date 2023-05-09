package irysc.gachesefid.Models;

public enum LaunchMode {

    ONLINE, PHYSICAL, HYBRID;

    public String getName() {
        return name().toLowerCase();
    }
}
