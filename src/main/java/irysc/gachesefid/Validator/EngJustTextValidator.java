package irysc.gachesefid.Validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class EngJustTextValidator implements
    ConstraintValidator<EngJustTextConstraint, String> {

    private static final String regex = "^[a-zA-Z]+$";
    private static final Pattern pattern = Pattern.compile(regex);

    @Override
    public void initialize(EngJustTextConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return pattern.matcher(s.replace(" ", "")).matches();
    }

    public static boolean isValid(String s) {
        return pattern.matcher(s.replace(" ", "")).matches();
    }
}
