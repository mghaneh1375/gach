package irysc.gachesefid.Models;

public enum OffCodeSections {

    ALL("همه"), GACH_EXAM("آزمون گچ سفید"),
    BANK_EXAM("آزمون شخصی ساز"), RAVAN_EXAM("آزمون روان شناسی"),
    CLASSES("کلاس خصوصی"), BOOK("فروش کتاب"),
    COUNSELING("مشاوره"), COUNSELING_QUIZ("آزمون مشاوره"),
    OPEN_EXAM("آزمون های باز"), CONTENT("بسته های آموزشی"),
    SCHOOL_QUIZ("آزمون مدرسه ای"), SCHOOL_HW("تکلیف مدرسه ای");

    private String faTranslate;
    OffCodeSections(String faTranslate) {
        this.faTranslate = faTranslate;
    }

    public String getFaTranslate() {
        return faTranslate;
    }

    public String getName() {
        return name().toLowerCase();
    }

}
