package irysc.gachesefid.Models;

public enum Access {

    ADVISOR("مشاور/مدرس"), SUPERADMIN("ادمین کل"),
    TEACHER("مصحح"), ADMIN("ادمین"), STUDENT("دانش آموز"),
    AGENT("نماینده"), SCHOOL("مدرسه"),
    CONTENT("اپراتور محتوایی"), EDITOR("اپراتور ویراستار"),
    WEAK_ADMIN("ادمین ضعیف");
    private String translateFa;

    public String getTranslateFa() {
        return translateFa;
    }

    Access(String translateFa) {
        this.translateFa = translateFa;
    }

    public String getName() {
        return name().toLowerCase();
    }
}
