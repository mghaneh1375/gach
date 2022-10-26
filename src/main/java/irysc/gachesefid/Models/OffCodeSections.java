package irysc.gachesefid.Models;

public enum OffCodeSections {

    ALL, GACH_EXAM, BANK_EXAM, RAVAN_EXAM, CLASSES, BOOK, COUNSELING, OPEN_EXAM;

    public String getName() {
        return name().toLowerCase();
    }

}
