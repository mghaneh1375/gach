package irysc.gachesefid.Validator;

import irysc.gachesefid.Utility.Utility;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class TimeValidator implements
        ConstraintValidator<TimeConstraint, String> {

    private static final String regex = "(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]";
    private static final Pattern pattern = Pattern.compile(regex);

    public static boolean isValid(String s) {

        if (s.length() == 4)
            s = "0" + s;

        return pattern.matcher(Utility.convertPersianDigits(s)).matches();
    }

    public static boolean lessThanNow(String date, String time) {

        if (time.length() == 4)
            time = "0" + time;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try {
            Date date2 = simpleDateFormat.parse(date + " " + time);
            return System.currentTimeMillis() > date2.getTime();
        } catch (Exception x) {
            return false;
        }
    }

    public static int addToTime(String time, int amount) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        Date d = df.parse(time);
        Calendar wantedTime = Calendar.getInstance();
        wantedTime.setTime(d);
        wantedTime.add(Calendar.MINUTE, amount);
        int m = wantedTime.get(Calendar.MINUTE);
        if (m < 10)
            return Integer.parseInt(wantedTime.get(Calendar.HOUR_OF_DAY) + "0" + m);

        return Integer.parseInt(wantedTime.get(Calendar.HOUR_OF_DAY) + "" + m);
    }

    @Override
    public void initialize(TimeConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {

        if (s.length() == 4)
            s = "0" + s;

        return pattern.matcher(Utility.convertPersianDigits(s)).matches();
    }

}
