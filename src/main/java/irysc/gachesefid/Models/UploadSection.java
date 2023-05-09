package irysc.gachesefid.Models;

public enum UploadSection {

    QUESTION, CK;

    public String getName() {
        return name().toLowerCase();
    }
}
