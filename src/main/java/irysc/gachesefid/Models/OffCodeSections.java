package irysc.gachesefid.Models;

public enum OffCodeSections {

    ALL, GACH_EXAM, BANK_EXAM;

    public String getName() {
        return name().toLowerCase();
    }

}
