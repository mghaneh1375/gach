package irysc.gachesefid.Validator;


import irysc.gachesefid.Utility.Utility;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DateValidator implements
        ConstraintValidator<DateConstraint, String> {

    private static final String regex = "^[1-4]\\d{3}\\/((0[1-6]\\/((3[0-1])|([1-2][0-9])|(0[1-9])))|((1[0-2]|(0[7-9]))\\/(30|([1-2][0-9])|(0[1-9]))))$";
    private static final String regex2 = "^[1-4]\\d{3}-((0[1-6]-((3[0-1])|([1-2][0-9])|(0[1-9])))|((1[0-2]|(0[7-9]))-(30|([1-2][0-9])|(0[1-9]))))$";
    private static final Pattern pattern = Pattern.compile(regex);
    private static final Pattern pattern2 = Pattern.compile(regex2);

    public static boolean isValid(String s) {
        return pattern.matcher(Utility.convertPersianDigits(s)).matches();
    }

    public static boolean isValid2(String s) {
        return pattern2.matcher(Utility.convertPersianDigits(s)).matches();
    }

    public static boolean gt(String d1, String d2) {
        return Utility.convertStringToDate(d1) - Utility.convertStringToDate(d2) > 0;
    }

    public static boolean gte(String d1, String d2) {
        return Utility.convertStringToDate(d1) - Utility.convertStringToDate(d2) >= 0;
    }

    @Override
    public void initialize(DateConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return pattern.matcher(Utility.convertPersianDigits(s)).matches();
    }
}
