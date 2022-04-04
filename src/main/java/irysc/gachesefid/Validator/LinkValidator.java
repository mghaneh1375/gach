package irysc.gachesefid.Validator;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LinkValidator implements
        ConstraintValidator<LinkConstraint, String> {

    private static final String regex = "String regex = \"<\\\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>\";";
    private static final Pattern pattern = Pattern.compile(regex);

    public static boolean isValid(String s) {
        return pattern.matcher(s).matches();
    }

    @Override
    public void initialize(LinkConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return pattern.matcher(s).matches();
    }

}
