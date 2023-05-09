package irysc.gachesefid.Utility;

import java.util.regex.Pattern;

public class Digit {

    private static final Pattern regex = Pattern.compile("^\\d+$");

    public static boolean validate(Object o) {

        return true;
//        if(o.getClass().equals(String.class))
//            return regex.matcher(o.toString()).matches();
//
//        return false;
    }
}
