package irysc.gachesefid.Statics;

public class Alerts {

    public static final String cannotDeleteAppointment = "شخص/اشخاصی در زمان مورد نظر زمان مصاحبه دارند. ابتدا همه آنها را به زمانی دیگر منتقل کنید سپس عملیات حذف را انجام دهید.";

    public static String createOffCode(int amount, String type, String section, String expireAt) {
        return "به شما یک کد تخفیف " + amount + " با تاریخ انقضای " + expireAt + " تعلق گرفته است.";
    }

    public static String unsetAppointment(String time) {
        return "وقت مصاحبه " + time + " توسط ادمین از شما گرفته شد.";
    }

    public static String cancelAppointment(String time) {
        return "وقت مصاحبه " + time + " کنسل شد.";
    }

    public static String cancelTeacherAppointment(String time, String user) {
        return user + " کارشناس مصاحبه " + time + " انصراف داد.";
    }

    public static String setAppointment(String time, String user) {
        return "شما در زمان " + time + " با " + user + " وقت مصاحبه دارید.";
    }

    public static String setQuizAppointment(String time) {
        return "شما در زمان " + time + " آزمون تعیین سطح دارید. ";
    }

    public static String appointmentResult(String result, String date) {
        return "نتیجه مصاحبه شما در تاریخ " + date + " " + result + " می باشد.";
    }

    public static String examResult(String result, String date) {
        return "نتیجه آزمون تعیین سطح شما در تاریخ " + date + " " + result + " می باشد.";
    }

    public static String certificateRequestResult(String result) {
        return "نتیجه بررسی مدارک شما" + " " + result + " می باشد.";
    }

    public static String initFormRequestResult(String result) {
        return "نتیجه بررسی فرم شما" + " " + result + " می باشد.";
    }

    public static String newInitFormRequest() {
        return "درخواست جدیدی موجود است.";
    }

    public static String classRegistryResult(String result) {
        return "You have been successfully registered in class:<br/>" + result +
                "You will receive detailed information about your virtual class user name and password.";
    }

}
