package irysc.gachesefid.Validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class EngValidator implements
    ConstraintValidator<EngConstraint, String> {

    private static final String regex = "^[a-zA-Z0-9]+$";
    private static final Pattern pattern = Pattern.compile(regex);

    @Override
    public void initialize(EngConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return pattern.matcher(s).matches();
    }

    public static boolean isValid(String s){
        return pattern.matcher(s).matches();
    }
}
