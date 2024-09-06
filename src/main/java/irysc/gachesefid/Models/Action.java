package irysc.gachesefid.Models;

public enum Action {

    BUY_CONTENT("خرید بسته آموزشی"), BUY_EXAM("خرید آزمون"),
    SET_ADVISOR("تعیین مشاور"), GET_TEACH_CLASS("تدریس خصوصی"),
    COMPLETE_PROFILE("تکمیل پروفایل"), DAILY_ADV("مشاهده تبلیغات روزانه"),
    DAILY_POINT("امتیاز زمان عضویت روزانه"), COMMENT("نوشتن نظر"),
    BIRTH_DAY("امتیاز روز تولد"), BUY_QUESTION("خرید سوال"),
    CORRECT_ANSWER("حل سوال"), RANK_IN_QUIZ("رتبه برتر در آزمون"),
    TEACHER_RATE("امتیاز دبیر به دانش آموز");

    private String faTranslate;
    Action(String fa) {
        faTranslate = fa;
    }

    public String getName() {
        return name().toLowerCase();
    }

    public String getFaTranslate() {
        return faTranslate;
    }
}
